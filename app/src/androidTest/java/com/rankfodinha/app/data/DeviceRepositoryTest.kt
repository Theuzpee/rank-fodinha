package com.rankfodinha.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Persistência no aparelho, com um arquivo de teste separado.
 * Não usa o arquivo real do app, então os dados verdadeiros do aparelho não são tocados.
 */
class DeviceRepositoryTest {

    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: LocalFodinhaRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "teste-${UUID.randomUUID()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(produceFile = { file })
        repo = LocalFodinhaRepository(store, clock = { Instant.parse("2026-02-01T12:00:00Z") })
    }

    @Test
    fun anEmptyStoreLoadsAsNoData() {
        assertEquals(FodinhaData(), runBlocking { repo.load() })
    }

    @Test
    fun aFinishedAndARunningGameSurviveSavingAndLoadingOnTheDevice() {
        val finished = changeLives(
            createGame("g-1", "2026-01-01T10:00:00Z", listOf("ana", "bruno"), GameSettings(startingLives = 1)),
            "bruno",
            -1,
            "2026-01-01T10:30:00Z",
        )
        val running = changeLives(
            createGame("g-2", "2026-01-02T09:00:00Z", listOf("ana", "bruno", "carla"), GameSettings()),
            "carla",
            -2,
            "2026-01-02T09:30:00Z",
        )
        val data = FodinhaData(
            players = listOf(Player("ana", "Ana"), Player("bruno", "Bruno"), Player("carla", "Carla")),
            games = listOf(finished, running),
        )
        runBlocking { repo.save(data) }
        assertEquals(data, runBlocking { repo.load() })
    }

    @Test
    fun dataSavedInTheOldFormatIsMigratedOnTheFirstLoad() {
        val legacy = """
            {"players":[{"id":"a","name":"Ana"},{"id":"b","name":"Bruno"}],
             "games":[{"id":"g1","startedAt":"2026-01-01T10:00:00Z","playerIds":["a","b"],
               "settings":{"startingLives":3,"penaltyMode":"ERRO","lastBidCannotClose":false},
               "maxCards":19,
               "rounds":[{"number":1,"cards":1,"dealerId":"a","bids":{"a":0,"b":0},"tricks":{"a":0,"b":1},"adjustments":{}}],
               "current":null,"winnerIds":[]}]}
        """.trimIndent()
        runBlocking { store.edit { it[stringPreferencesKey("fodinha_data_v1")] = legacy } }

        val game = runBlocking { repo.load() }.games.single()

        assertEquals(mapOf("a" to 3, "b" to 2), game.lives)
        assertNull(game.finishedAt)
    }

    @Test
    fun savingAgainReplacesTheOldData() {
        runBlocking {
            repo.save(FodinhaData(players = listOf(Player("a", "Ana"))))
            val replacement = FodinhaData(players = listOf(Player("b", "Bruno")))
            repo.save(replacement)
            assertEquals(replacement, repo.load())
        }
    }
}
