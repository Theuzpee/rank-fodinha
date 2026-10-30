package com.rankfodinha.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val T = "2026-01-01T00:00:00Z"

private fun finished(
    id: String,
    players: List<String>,
    winners: List<String>,
    finishedAt: String? = T,
): Game = Game(
    id = id,
    startedAt = T,
    finishedAt = finishedAt,
    playerIds = players,
    settings = GameSettings(),
    lives = players.associateWith { if (it in winners) 1 else 0 },
    winnerIds = winners,
)

/** Monta N partidas de [player] contra [opponent]; as vitórias do jogador são as primeiras [wins]. */
private fun gamesFor(player: String, opponent: String, games: Int, wins: Int, prefix: String): List<Game> =
    (1..games).map { i ->
        val winner = if (i <= wins) player else opponent
        finished("$prefix-$i", listOf(player, opponent), listOf(winner))
    }

private fun List<RankingRow>.byId(id: String): RankingRow = first { it.playerId == id }

private val ana = Player("ana", "Ana")
private val bruno = Player("bruno", "Bruno")
private val carla = Player("carla", "Carla")
private val diego = Player("diego", "Diego")

class RankingTest {

    @Test
    fun emptyPlayerListGivesAnEmptyRanking() {
        assertTrue(buildRanking(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun everyPlayerAppearsEvenWithoutGames() {
        val rows = buildRanking(listOf(ana, bruno, carla), emptyList())
        assertEquals(setOf("ana", "bruno", "carla"), rows.map { it.playerId }.toSet())
    }

    @Test
    fun playersWithoutFinishedGamesHaveNoPositionAndNoRate() {
        val rows = buildRanking(listOf(ana, bruno), emptyList())
        assertTrue(rows.all { it.position == null && it.winRate == null && it.played == 0 && it.wins == 0 })
    }

    @Test
    fun unplayedPlayersComeAfterPlayedOnesAndUnrankedPlayersKeepNoPosition() {
        val rows = buildRanking(
            listOf(Player("carla", "Carla"), ana, bruno),
            listOf(finished("g", listOf("bruno", "ana"), listOf("bruno"))),
        )
        assertEquals(listOf("Bruno", "Ana", "Carla"), rows.map { it.name })
        assertEquals(listOf(1, 2, null), rows.map { it.position })
    }

    @Test
    fun gamesInProgressDoNotCount() {
        val games = listOf(
            finished("open", listOf("ana", "bruno"), emptyList(), finishedAt = null),
            finished("done", listOf("ana", "bruno"), listOf("ana")),
        )
        val rows = buildRanking(listOf(ana, bruno), games)
        assertEquals(1, rows.byId("ana").played)
        assertEquals(1, rows.byId("bruno").played)
        assertEquals(1, rows.byId("ana").wins)
    }

    @Test
    fun everyWinnerOfAGameGetsAWin() {
        val rows = buildRanking(
            listOf(ana, bruno, carla),
            listOf(finished("g", listOf("ana", "bruno", "carla"), listOf("ana", "carla"))),
        )
        assertEquals(1, rows.byId("ana").wins)
        assertEquals(0, rows.byId("bruno").wins)
        assertEquals(1, rows.byId("carla").wins)
    }

    @Test
    fun everyParticipantGetsAGamePlayedWhetherTheyWonOrNot() {
        val rows = buildRanking(
            listOf(ana, bruno, carla),
            listOf(finished("g", listOf("ana", "bruno", "carla"), listOf("ana"))),
        )
        assertTrue(rows.all { it.played == 1 })
    }

    @Test
    fun totalWinsEqualTheNumberOfWinnersAcrossFinishedGames() {
        val games = gamesFor("ana", "bruno", 5, 3, "a") + gamesFor("carla", "diego", 4, 1, "c")
        val rows = buildRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals(games.sumOf { it.winnerIds.size }, rows.sumOf { it.wins })
    }

    @Test
    fun morePlayerWinsComeFirst() {
        // Ana: 2 vitórias. Bruno: 1 vitória. Carla: nenhuma.
        val games = gamesFor("ana", "carla", 2, 2, "a") + gamesFor("bruno", "carla", 1, 1, "b")
        val rows = buildRanking(listOf(ana, bruno, carla), games)
        assertEquals(listOf("ana", "bruno", "carla"), rows.map { it.playerId })
        assertEquals(listOf(1, 2, 3), rows.map { it.position })
    }

    @Test
    fun equalWinsAreSortedByWinRate() {
        val games = listOf(
            finished("g1", listOf("bruno", "ana"), listOf("bruno")),
            finished("g2", listOf("diego", "carla"), listOf("diego")),
            finished("g3", listOf("diego", "ana"), listOf("ana")),
        )
        // Bruno: 1 vitória em 1 (100%). Diego: 1 vitória em 2 (50%). Ana: 1 vitória em 2 (50%).
        val rows = buildRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals("bruno", rows.first().playerId)
        assertEquals(100.0, rows.first().winRate!!, 1e-9)
        assertEquals(50.0, rows.byId("ana").winRate!!, 1e-9)
    }

    @Test
    fun sameWinsAndSameRateShareOnePosition() {
        val games = listOf(
            finished("x", listOf("ana", "bruno"), listOf("ana")),
            finished("y", listOf("carla", "diego"), listOf("carla")),
        )
        val rows = buildRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals(listOf("ana", "carla", "bruno", "diego"), rows.map { it.playerId })
        assertEquals(listOf(1, 1, 3, 3), rows.map { it.position })
    }

    @Test
    fun positionsSkipTheNumbersTakenByAThreeWayShare() {
        // Ana, Bruno e Carla ganham uma partida cada contra Diego (100% cada). Diego perde as três.
        val games = listOf(
            finished("g1", listOf("ana", "diego"), listOf("ana")),
            finished("g2", listOf("bruno", "diego"), listOf("bruno")),
            finished("g3", listOf("carla", "diego"), listOf("carla")),
        )
        val rows = buildRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals(listOf("ana", "bruno", "carla", "diego"), rows.map { it.playerId })
        assertEquals(listOf(1, 1, 1, 4), rows.map { it.position })
    }

    @Test
    fun winRateIsRoundedToOneDecimal() {
        val cases = listOf(
            Triple(3, 1, 33.3),   // 1 de 3
            Triple(3, 2, 66.7),   // 2 de 3
            Triple(8, 1, 12.5),   // 1 de 8
            Triple(16, 1, 6.3),   // 1 de 16 = 6,25% arredonda para 6,3
        )
        for ((games, wins, expected) in cases) {
            val rows = buildRanking(listOf(ana, bruno), gamesFor("ana", "bruno", games, wins, "r$games-$wins"))
            assertEquals("$wins de $games", expected, rows.byId("ana").winRate!!, 1e-9)
        }
    }

    @Test
    fun zeroWinsWithGamesIsZeroPercentNotMissing() {
        val rows = buildRanking(listOf(ana, bruno), gamesFor("ana", "bruno", 2, 0, "z"))
        assertEquals(0.0, rows.byId("ana").winRate!!, 1e-9)
        assertEquals(2, rows.byId("ana").position)
    }

    @Test
    fun winRateIsNullOnlyForPlayersWithoutGames() {
        val rows = buildRanking(listOf(ana, bruno, carla), gamesFor("ana", "bruno", 1, 1, "n"))
        assertEquals(null, rows.byId("carla").winRate)
        assertTrue(rows.filter { it.playerId != "carla" }.all { it.winRate != null })
    }

    @Test
    fun gamesWithUnknownPlayersDoNotBreakTheRanking() {
        val games = listOf(finished("g", listOf("ana", "fantasma"), listOf("fantasma")))
        val rows = buildRanking(listOf(ana, bruno), games)
        assertEquals(0, rows.byId("ana").wins)
        assertEquals(1, rows.byId("ana").played)
        assertEquals(setOf("ana", "bruno"), rows.map { it.playerId }.toSet())
    }

    @Test
    fun unplayedPlayersAreSortedByNameIgnoringCase() {
        val rows = buildRanking(
            listOf(Player("c", "carla"), Player("b", "Bruno"), Player("a", "ana")),
            emptyList(),
        )
        assertEquals(listOf("ana", "Bruno", "carla"), rows.map { it.name })
    }

    @Test
    fun playedPlayersWithEqualRecordAreSortedByName() {
        val games = listOf(
            finished("g1", listOf("bruno", "carla"), listOf("bruno")),
            finished("g2", listOf("ana", "diego"), listOf("ana")),
        )
        val rows = buildRanking(listOf(ana, bruno, carla, diego), games)
        assertEquals(listOf("ana", "bruno"), rows.take(2).map { it.playerId })
    }

    @Test
    fun rankingDoesNotDependOnTheOrderOfPlayersOrGames() {
        val players = listOf(ana, bruno, carla, diego)
        val games = gamesFor("ana", "carla", 3, 2, "a") + gamesFor("bruno", "diego", 2, 1, "b")
        val forward = buildRanking(players, games).map { it.playerId to it.position }
        val backward = buildRanking(players.reversed(), games.reversed()).map { it.playerId to it.position }
        assertEquals(forward.toSet(), backward.toSet())
        assertEquals(forward.map { it.second }, backward.map { it.second })
    }

    @Test
    fun rankingIsStableWhenCalledTwice() {
        val games = gamesFor("ana", "bruno", 4, 3, "s")
        assertEquals(buildRanking(listOf(ana, bruno), games), buildRanking(listOf(ana, bruno), games))
    }

    @Test
    fun playedCountIsOnlyTheFinishedGamesOfThePlayer() {
        val games = gamesFor("ana", "bruno", 3, 1, "p") + listOf(finished("open", listOf("ana", "bruno"), emptyList(), null))
        assertEquals(3, buildRanking(listOf(ana, bruno), games).byId("ana").played)
    }
}
