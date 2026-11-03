package com.rankfodinha.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DEATH_TIME = "2026-01-01T00:00:00Z"

/** Partida encerrada em que [eliminated] perdeu as vidas, na ordem dada. Quem sobra vence. */
private fun endedGame(id: String, players: List<String>, eliminated: List<String>): Game = Game(
    id = id,
    startedAt = DEATH_TIME,
    finishedAt = DEATH_TIME,
    playerIds = players,
    settings = GameSettings(),
    lives = players.associateWith { if (it in eliminated) 0 else 1 },
    winnerIds = players - eliminated.toSet(),
    eliminated = eliminated,
)

/** Partida ainda em andamento, com [eliminated] já fora. */
private fun runningGame(id: String, players: List<String>, eliminated: List<String>): Game =
    endedGame(id, players, eliminated).copy(finishedAt = null, winnerIds = emptyList())

private fun List<FirstDeathRow>.byId(id: String): FirstDeathRow = first { it.playerId == id }

private val ana = Player("ana", "Ana")
private val bruno = Player("bruno", "Bruno")
private val carla = Player("carla", "Carla")
private val diego = Player("diego", "Diego")

class FirstDeathRankingTest {

    @Test
    fun noPlayersGivesAnEmptyRanking() {
        assertTrue(buildFirstDeathRanking(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun everyPlayerAppearsEvenWithoutGames() {
        val rows = buildFirstDeathRanking(listOf(ana, bruno, carla), emptyList())
        assertEquals(setOf("ana", "bruno", "carla"), rows.map { it.playerId }.toSet())
    }

    @Test
    fun playersWithoutFinishedGamesHaveNoPosition() {
        val games = listOf(endedGame("g", listOf("ana", "bruno"), listOf("bruno")))
        val row = buildFirstDeathRanking(listOf(ana, bruno, diego), games).byId("diego")
        assertNull(row.position)
        assertEquals(0, row.played)
        assertEquals(0, row.firstDeaths)
    }

    @Test
    fun whoDiedFirstMostOftenComesFirst() {
        val games = listOf(
            endedGame("g1", listOf("ana", "bruno", "carla"), listOf("bruno", "carla")), // primeiro: Bruno
            endedGame("g2", listOf("ana", "bruno", "carla"), listOf("bruno", "ana")), // primeiro: Bruno
            endedGame("g3", listOf("ana", "bruno", "carla"), listOf("carla", "ana")), // primeiro: Carla
        )
        val rows = buildFirstDeathRanking(listOf(ana, bruno, carla), games)
        assertEquals(listOf("bruno", "carla", "ana"), rows.map { it.playerId })
        assertEquals(listOf(2, 1, 0), rows.map { it.firstDeaths })
        assertEquals(listOf(1, 2, 3), rows.map { it.position })
    }

    @Test
    fun onlyTheFirstPlayerEliminatedInAGameCountsAsFirstDeath() {
        val games = listOf(
            endedGame("g", listOf("ana", "bruno", "carla", "diego"), listOf("carla", "bruno", "diego")),
        )
        val rows = buildFirstDeathRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals(1, rows.byId("carla").firstDeaths)
        assertEquals(0, rows.byId("bruno").firstDeaths)
        assertEquals(0, rows.byId("diego").firstDeaths)
        assertEquals(0, rows.byId("ana").firstDeaths)
    }

    @Test
    fun tiedFirstDeathsShareThePosition() {
        val games = listOf(
            endedGame("g1", listOf("ana", "bruno", "carla"), listOf("ana", "bruno")), // primeiro: Ana
            endedGame("g2", listOf("ana", "bruno", "carla"), listOf("bruno", "ana")), // primeiro: Bruno
        )
        val rows = buildFirstDeathRanking(listOf(ana, bruno, carla), games)
        assertEquals(listOf("ana", "bruno", "carla"), rows.map { it.playerId })
        assertEquals(listOf(1, 1, 3), rows.map { it.position })
    }

    @Test
    fun winningDoesNotCountAsFirstDeath() {
        val games = listOf(
            endedGame("g1", listOf("ana", "bruno"), listOf("bruno")),
            endedGame("g2", listOf("ana", "bruno"), listOf("bruno")),
        )
        val rows = buildFirstDeathRanking(listOf(ana, bruno), games)
        assertEquals("bruno", rows.first().playerId)
        assertEquals(2, rows.first().firstDeaths)
        assertEquals(0, rows.byId("ana").firstDeaths)
    }

    @Test
    fun gamesInProgressDoNotCount() {
        val games = listOf(runningGame("aberta", listOf("ana", "bruno"), listOf("ana")))
        val rows = buildFirstDeathRanking(listOf(ana, bruno), games)
        assertEquals(0, rows.byId("ana").firstDeaths)
        assertEquals(0, rows.byId("ana").played)
        assertNull(rows.byId("ana").position)
    }

    @Test
    fun aFinishedGameWithoutEliminationOrderCountsAsPlayedButNoDeath() {
        // Partida antiga, de antes da ordem das mortes ser guardada.
        val legacy = endedGame("antiga", listOf("ana", "bruno"), emptyList())
        val rows = buildFirstDeathRanking(listOf(ana, bruno), listOf(legacy))
        assertEquals(1, rows.byId("ana").played)
        assertEquals(0, rows.byId("ana").firstDeaths)
        assertEquals(0, rows.byId("bruno").firstDeaths)
    }

    @Test
    fun aGameWhoseFirstDeathsWereTogetherCountsForNobody() {
        val together = endedGame("juntos", listOf("ana", "bruno", "carla"), listOf("ana", "bruno"))
            .copy(firstDeathTogether = true)
        val normal = endedGame("normal", listOf("ana", "bruno", "carla"), listOf("carla", "ana"))
        val rows = buildFirstDeathRanking(listOf(ana, bruno, carla), listOf(together, normal))
        assertEquals(1, rows.byId("carla").firstDeaths)
        assertEquals(0, rows.byId("ana").firstDeaths)
        assertEquals(0, rows.byId("bruno").firstDeaths)
        assertEquals(2, rows.byId("ana").played)
    }
}
