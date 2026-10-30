package com.rankfodinha.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOW = "2026-02-01T12:00:00Z"
private const val OLD_END = "2026-01-05T20:00:00Z"

private fun legacy(raw: String): LegacyFodinhaData =
    Json { ignoreUnknownKeys = true }.decodeFromString(LegacyFodinhaData.serializer(), raw)

/** Uma rodada no formato antigo. Só os campos que mexem nas vidas. */
private fun round(bids: String, tricks: String = "{}", adjustments: String = "{}"): String =
    """{"number":1,"cards":1,"dealerId":"a","bids":$bids,"tricks":$tricks,"adjustments":$adjustments}"""

/** Rodada em andamento no formato antigo, na fase de palpites. */
private fun current(bids: String, adjustments: String = "{}"): String =
    """{"number":3,"cards":1,"dealerId":"a","order":[],"bids":$bids,"tricks":{},"phase":"PALPITES","adjustments":$adjustments}"""

/** Partida no formato antigo. Os jogadores padrão são a, b e c. */
private fun game(
    players: String = """["a","b","c"]""",
    lives: Int = 5,
    mode: String = "DIFERENCA",
    rounds: List<String> = emptyList(),
    current: String? = null,
    finishedAt: String? = null,
    winners: String = "[]",
): String {
    val finished = finishedAt?.let { "\"finishedAt\":\"$it\"," } ?: ""
    val roundList = rounds.joinToString(prefix = "[", postfix = "]")
    return """{"id":"g1","startedAt":"2026-01-01T10:00:00Z",$finished""" +
        """"playerIds":$players,"settings":{"startingLives":$lives,"penaltyMode":"$mode","lastBidCannotClose":false},""" +
        """"maxCards":13,"rounds":$roundList,"current":${current ?: "null"},"winnerIds":$winners}"""
}

/** Migra um conjunto de partidas com três jogadores: Ana (a), Bruno (b) e Carla (c). */
private fun migrate(vararg games: String): FodinhaData =
    migrateLegacyData(
        legacy(
            """{"players":[{"id":"a","name":"Ana"},{"id":"b","name":"Bruno"},{"id":"c","name":"Carla"}],""" +
                """"games":[${games.joinToString(",")}]}""",
        ),
        migratedAt = NOW,
    )

class LegacyMigrationTest {

    // ---------- Vidas que aparecem na tela ----------

    @Test
    fun theLivesShownOnTheScreenAreKept() {
        val rounds = listOf(
            round(bids = """{"a":1,"b":0,"c":0}""", tricks = """{"a":0,"b":0,"c":1}"""),
            round(bids = """{"a":0,"b":2,"c":0}""", tricks = """{"a":0,"b":1,"c":0}""", adjustments = """{"a":2}"""),
        )
        val current = current(bids = """{"a":0,"b":0,"c":0}""", adjustments = """{"c":-1}""")
        val migrated = migrate(game(rounds = rounds, current = current)).games.single()

        assertEquals(mapOf("a" to 6, "b" to 4, "c" to 3), migrated.lives)
        assertNull(migrated.finishedAt)
        assertTrue(migrated.winnerIds.isEmpty())
    }

    @Test
    fun differenceModeLosesTheGapBetweenBidAndTricks() {
        val rounds = listOf(round(bids = """{"a":3,"b":0,"c":0}""", tricks = """{"a":0,"b":0,"c":0}"""))
        val migrated = migrate(game(lives = 3, rounds = rounds)).games.single()

        assertEquals(mapOf("a" to 0, "b" to 3, "c" to 3), migrated.lives)
        assertNull("ainda há dois jogadores com vida", migrated.finishedAt)
    }

    @Test
    fun oneLifeModeLosesOneLifeOnAMissAndNothingOnAHit() {
        val rounds = listOf(round(bids = """{"a":2,"b":1}""", tricks = """{"a":2,"b":0}"""))
        val migrated = migrate(game(players = """["a","b"]""", lives = 3, mode = "ERRO", rounds = rounds))

        assertEquals(mapOf("a" to 3, "b" to 2), migrated.games.single().lives)
    }

    @Test
    fun adjustmentsOfFinishedRoundsAreAddedToTheLives() {
        val rounds = listOf(round(bids = """{"a":0,"b":0,"c":0}""", adjustments = """{"b":-2}"""))
        val migrated = migrate(game(rounds = rounds)).games.single()

        assertEquals(mapOf("a" to 5, "b" to 3, "c" to 5), migrated.lives)
    }

    @Test
    fun livesNeverGoBelowZeroWhileMigrating() {
        val rounds = listOf(round(bids = """{"a":0,"b":0,"c":0}""", tricks = """{"a":9,"b":0,"c":0}"""))
        val migrated = migrate(game(lives = 2, rounds = rounds)).games.single()

        assertEquals(0, migrated.lives["a"])
    }

    @Test
    fun currentRoundAdjustmentsCountOnlyForPlayersInTheRound() {
        val current = current(bids = """{"a":0}""", adjustments = """{"a":-2,"b":-1}""")
        val migrated = migrate(game(current = current)).games.single()

        assertEquals(mapOf("a" to 3, "b" to 5, "c" to 5), migrated.lives)
    }

    @Test
    fun gameWithoutRoundsKeepsTheStartingLives() {
        val migrated = migrate(game(lives = 8)).games.single()

        assertEquals(mapOf("a" to 8, "b" to 8, "c" to 8), migrated.lives)
        assertNull(migrated.finishedAt)
    }

    @Test
    fun playersOutsideTheGameAreIgnoredInRounds() {
        val rounds = listOf(round(bids = """{"a":0,"fantasma":3}""", tricks = """{"a":0,"fantasma":0}"""))
        val migrated = migrate(game(rounds = rounds)).games.single()

        assertEquals(setOf("a", "b", "c"), migrated.lives.keys)
    }

    // ---------- Fim da partida ----------

    @Test
    fun activeGameWithOneSurvivorEndsAtTheMigrationTime() {
        val rounds = listOf(round(bids = """{"a":0,"b":0}""", tricks = """{"a":1,"b":0}"""))
        val migrated = migrate(game(players = """["a","b"]""", lives = 1, mode = "ERRO", rounds = rounds))
            .games.single()

        assertEquals(mapOf("a" to 0, "b" to 1), migrated.lives)
        assertEquals(NOW, migrated.finishedAt)
        assertEquals(listOf("b"), migrated.winnerIds)
    }

    @Test
    fun activeGameWhereEveryoneRanOutEndsWithoutAWinner() {
        val rounds = listOf(round(bids = """{"a":0,"b":0}""", tricks = """{"a":1,"b":1}"""))
        val migrated = migrate(game(players = """["a","b"]""", lives = 1, mode = "ERRO", rounds = rounds))
            .games.single()

        assertEquals(mapOf("a" to 0, "b" to 0), migrated.lives)
        assertEquals(NOW, migrated.finishedAt)
        assertTrue(migrated.winnerIds.isEmpty())
    }

    @Test
    fun finishedGamesKeepTheirWinnerAndTheirOwnEndTime() {
        val rounds = listOf(round(bids = """{"a":0,"b":0}""", tricks = """{"a":0,"b":1}"""))
        val migrated = migrate(
            game(players = """["a","b"]""", lives = 1, mode = "ERRO", rounds = rounds, finishedAt = OLD_END, winners = """["a"]"""),
        ).games.single()

        assertEquals(mapOf("a" to 1, "b" to 0), migrated.lives)
        assertEquals(OLD_END, migrated.finishedAt)
        assertEquals(listOf("a"), migrated.winnerIds)
    }

    // ---------- Dados e configurações ----------

    @Test
    fun onlyTheStartingLivesSurviveFromTheOldSettings() {
        val migrated = migrate(game(lives = 7, mode = "ERRO")).games.single()

        assertEquals(GameSettings(startingLives = 7), migrated.settings)
    }

    @Test
    fun playersAreKeptAsTheyWere() {
        val migrated = migrate(game())

        assertEquals(
            listOf(Player("a", "Ana"), Player("b", "Bruno"), Player("c", "Carla")),
            migrated.players,
        )
    }

    @Test
    fun gamesKeepTheirIdsStartTimeAndOrder() {
        val raw = """{"games":[
            {"id":"x","startedAt":"2026-01-01T10:00:00Z","playerIds":["a","b"]},
            {"id":"y","startedAt":"2026-01-02T10:00:00Z","playerIds":["a","b"]}
        ]}"""
        val migrated = migrateLegacyData(legacy(raw), migratedAt = NOW)

        assertEquals(listOf("x", "y"), migrated.games.map { it.id })
        assertEquals("2026-01-01T10:00:00Z", migrated.games.first().startedAt)
    }

    @Test
    fun emptyLegacyDataMigratesToEmptyData() {
        assertEquals(FodinhaData(), migrateLegacyData(LegacyFodinhaData(), migratedAt = NOW))
    }
}
