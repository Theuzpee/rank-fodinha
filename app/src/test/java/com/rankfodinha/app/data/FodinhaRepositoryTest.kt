package com.rankfodinha.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Persistência real (DataStore em arquivo temporário), sem Android. */
class FodinhaRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    /** Chaves usadas pelo app. Se alguma mudar, os dados salvos no aparelho deixam de ser lidos. */
    private val dataKey = stringPreferencesKey("fodinha_data_v2")
    private val legacyKey = stringPreferencesKey("fodinha_data_v1")

    private val migratedAt = Instant.parse("2026-02-01T12:00:00Z")

    /** Dados no formato antigo: uma partida ativa, com uma rodada de modo 1 vida por erro. */
    private val legacyJson = """
        {"players":[{"id":"a","name":"Ana"},{"id":"b","name":"Bruno"}],
         "games":[{"id":"g1","startedAt":"2026-01-01T10:00:00Z","playerIds":["a","b"],
           "settings":{"startingLives":3,"penaltyMode":"ERRO","lastBidCannotClose":false},
           "maxCards":19,
           "rounds":[{"number":1,"cards":1,"dealerId":"a","bids":{"a":0,"b":0},"tricks":{"a":0,"b":1},"adjustments":{}}],
           "current":null,"winnerIds":[]}]}
    """.trimIndent()

    @After
    fun closeStores() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
    }

    private fun newFile(): File = File(folder.root, "fodinha-${System.nanoTime()}.preferences_pb")

    private fun openStore(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    private fun repository(store: DataStore<Preferences>) =
        LocalFodinhaRepository(store, clock = { migratedAt })

    private fun closeLastStore() = runBlocking {
        scopes.last().coroutineContext[Job]!!.cancelAndJoin()
    }

    private fun rawValue(store: DataStore<Preferences>, key: Preferences.Key<String>): String? =
        runBlocking { store.data.first()[key] }

    private fun storeLegacy(store: DataStore<Preferences>, raw: String) = runBlocking {
        store.edit { it[legacyKey] = raw }
    }

    private fun sampleData(): FodinhaData {
        val finished = changeLives(
            createGame("g-1", "2026-01-01T10:00:00Z", listOf("ana", "bruno"), GameSettings(startingLives = 1)),
            "bruno",
            -1,
            "2026-01-01T10:30:00Z",
        )
        val active = changeLives(
            createGame("g-2", "2026-01-02T09:00:00Z", listOf("ana", "carla", "bruno"), GameSettings(startingLives = 4)),
            "carla",
            -2,
            "2026-01-02T09:30:00Z",
        )
        return FodinhaData(
            players = listOf(Player("ana", "Ana"), Player("bruno", "Bruno"), Player("carla", "Carla")),
            games = listOf(finished, active),
        )
    }

    // ---------- Formato atual ----------

    @Test
    fun loadingAnEmptyStoreGivesNoPlayersAndNoGames() {
        val repo = repository(openStore(newFile()))
        assertEquals(FodinhaData(), runBlocking { repo.load() })
    }

    @Test
    fun saveThenLoadRestoresEveryFieldExactly() {
        val repo = repository(openStore(newFile()))
        val data = sampleData()
        runBlocking {
            repo.save(data)
            assertEquals(data, repo.load())
        }
    }

    @Test
    fun saveReplacesThePreviousState() {
        val repo = repository(openStore(newFile()))
        runBlocking {
            repo.save(sampleData())
            val replacement = FodinhaData(players = listOf(Player("x", "Xis")))
            repo.save(replacement)
            assertEquals(replacement, repo.load())
        }
    }

    @Test
    fun dataSurvivesClosingAndReopeningTheStore() {
        val file = newFile()
        val data = sampleData()
        runBlocking { repository(openStore(file)).save(data) }
        closeLastStore()
        assertEquals(data, runBlocking { repository(openStore(file)).load() })
    }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() {
        val store = openStore(newFile())
        runBlocking {
            store.edit {
                it[dataKey] = """{"players":[{"id":"a","name":"Ana","email":"x@y.z"}],"games":[],"futureFlag":true}"""
            }
        }
        assertEquals(listOf(Player("a", "Ana")), runBlocking { repository(store).load().players })
    }

    @Test
    fun missingOptionalFieldsFallBackToDefaults() {
        val store = openStore(newFile())
        runBlocking {
            store.edit {
                it[dataKey] = """{"players":[{"id":"a","name":"Ana"}],"games":[""" +
                    """{"id":"g","startedAt":"2026-01-01T00:00:00Z","playerIds":["a"],"settings":{},"lives":{"a":3}}]}"""
            }
        }
        val game = runBlocking { repository(store).load().games.single() }
        assertNull(game.finishedAt)
        assertTrue(game.winnerIds.isEmpty())
        assertEquals(5, game.settings.startingLives)
        assertEquals(mapOf("a" to 3), game.lives)
    }

    @Test
    fun corruptDataFailsLoudlyInsteadOfBeingReplaced() {
        val store = openStore(newFile())
        runBlocking { store.edit { it[dataKey] = "{nao e json" } }
        assertThrows(SerializationException::class.java) {
            runBlocking { repository(store).load() }
        }
    }

    @Test
    fun storedDataUsesStableFieldNames() {
        val store = openStore(newFile())
        runBlocking { repository(store).save(sampleData()) }
        val root = Json.parseToJsonElement(rawValue(store, dataKey)!!).jsonObject

        assertEquals(setOf("players", "games"), root.keys)
        val finished = root.getValue("games").jsonArray[0].jsonObject
        assertEquals(
            setOf("id", "startedAt", "finishedAt", "playerIds", "settings", "lives", "winnerIds", "eliminated"),
            finished.keys,
        )
        assertEquals(setOf("startingLives"), finished.getValue("settings").jsonObject.keys)
    }

    @Test
    fun emptyDataIsStoredAsEmptyLists() {
        val store = openStore(newFile())
        runBlocking { repository(store).save(FodinhaData()) }
        val root = Json.parseToJsonElement(rawValue(store, dataKey)!!).jsonObject
        assertTrue(root.getValue("players").jsonArray.isEmpty())
        assertTrue(root.getValue("games").jsonArray.isEmpty())
    }

    @Test
    fun savingTheSameDataTwiceIsIdempotent() {
        val repo = repository(openStore(newFile()))
        val data = sampleData()
        runBlocking {
            repo.save(data)
            repo.save(data)
            assertEquals(data, repo.load())
        }
    }

    // ---------- Migração do formato antigo (chave v1) ----------

    @Test
    fun legacyDataIsMigratedWhenNoNewDataExists() {
        val store = openStore(newFile())
        storeLegacy(store, legacyJson)
        val game = runBlocking { repository(store).load().games.single() }

        assertEquals(mapOf("a" to 3, "b" to 2), game.lives)
        assertEquals(GameSettings(startingLives = 3), game.settings)
        assertNull(game.finishedAt)
    }

    @Test
    fun migrationKeepsTheLegacyDataInPlace() {
        val store = openStore(newFile())
        storeLegacy(store, legacyJson)
        runBlocking { repository(store).load() }
        assertEquals(legacyJson, rawValue(store, legacyKey))
    }

    @Test
    fun loadingDoesNotWriteAnythingUntilTheDataChanges() {
        val store = openStore(newFile())
        storeLegacy(store, legacyJson)
        val loaded = runBlocking { repository(store).load() }

        assertNull("abrir o app não grava", rawValue(store, dataKey))

        runBlocking { repository(store).save(loaded) }
        assertNotNull(rawValue(store, dataKey))
        assertEquals(loaded, runBlocking { repository(store).load() })
    }

    @Test
    fun newDataWinsOverTheLegacyDataOnceItIsSaved() {
        val store = openStore(newFile())
        storeLegacy(store, legacyJson)
        runBlocking { repository(store).load() }

        val data = sampleData()
        runBlocking { repository(store).save(data) }
        assertEquals(data, runBlocking { repository(store).load() })
    }

    @Test
    fun legacyDataIsIgnoredWhenNewDataAlreadyExists() {
        val store = openStore(newFile())
        val data = sampleData()
        runBlocking {
            repository(store).save(data)
            store.edit { it[legacyKey] = legacyJson }
        }
        assertEquals(data, runBlocking { repository(store).load() })
    }

    @Test
    fun migrationUsesTheClockForGamesThatEndWithOneSurvivor() {
        val oneSurvivorLegacy = """
            {"players":[{"id":"a","name":"Ana"},{"id":"b","name":"Bruno"}],
             "games":[{"id":"g1","startedAt":"2026-01-01T10:00:00Z","playerIds":["a","b"],
               "settings":{"startingLives":1,"penaltyMode":"ERRO","lastBidCannotClose":false},
               "rounds":[{"number":1,"cards":1,"dealerId":"a","bids":{"a":0,"b":0},"tricks":{"a":1,"b":0},"adjustments":{}}],
               "current":null,"winnerIds":[]}]}
        """.trimIndent()
        val store = openStore(newFile())
        storeLegacy(store, oneSurvivorLegacy)
        val game = runBlocking { repository(store).load().games.single() }

        assertEquals(migratedAt.toString(), game.finishedAt)
        assertEquals(listOf("b"), game.winnerIds)
    }

    @Test
    fun corruptLegacyDataFailsLoudlyAndIsNotDeleted() {
        val store = openStore(newFile())
        storeLegacy(store, "{quebrado")
        assertThrows(SerializationException::class.java) {
            runBlocking { repository(store).load() }
        }
        assertEquals("{quebrado", rawValue(store, legacyKey))
        assertNull(rawValue(store, dataKey))
    }
}
