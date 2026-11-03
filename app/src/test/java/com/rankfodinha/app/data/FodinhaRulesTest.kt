package com.rankfodinha.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private const val START = "2026-01-01T00:00:00Z"
private const val END = "2026-01-02T00:00:00Z"

private fun newGame(vararg players: String, lives: Int = 5): Game =
    createGame("g-test", START, players.toList(), GameSettings(startingLives = lives))

class FodinhaRulesTest {

    @Test
    fun defaultsMatchTheAppRules() {
        assertEquals(5, DEFAULT_STARTING_LIVES)
        assertEquals(5, GameSettings().startingLives)
        assertEquals(2, MIN_PLAYERS)
        assertEquals(1, MIN_STARTING_LIVES)
        assertEquals(20, MAX_STARTING_LIVES)
    }

    @Test
    fun startingLivesAcceptOnlyValuesFromOneToTwenty() {
        assertFalse(isValidStartingLives(0))
        assertFalse(isValidStartingLives(-3))
        assertTrue(isValidStartingLives(1))
        assertTrue(isValidStartingLives(20))
        assertFalse(isValidStartingLives(21))
    }

    // ---------- Criação da partida ----------

    @Test
    fun newGameGivesEveryPlayerTheStartingLives() {
        val game = newGame("Ana", "Bruno", "Carla", lives = 7)
        assertEquals(mapOf("Ana" to 7, "Bruno" to 7, "Carla" to 7), game.lives)
    }

    @Test
    fun newGameKeepsTheTableOrderAndSettings() {
        val game = createGame("x", START, listOf("Z", "Y", "X"), GameSettings(startingLives = 9))
        assertEquals(listOf("Z", "Y", "X"), game.playerIds)
        assertEquals(9, game.settings.startingLives)
    }

    @Test
    fun newGameIsRunningAndHasNoWinner() {
        val game = newGame("Ana", "Bruno")
        assertNull(game.finishedAt)
        assertTrue(game.winnerIds.isEmpty())
    }

    @Test
    fun theLivesMapHasExactlyTheGamePlayers() {
        val game = newGame("Ana", "Bruno", "Carla")
        assertEquals(setOf("Ana", "Bruno", "Carla"), game.lives.keys)
    }

    // ---------- Jogadores com vida ----------

    @Test
    fun alivePlayersKeepsTheTableOrder() {
        assertEquals(listOf("C", "A", "B"), alivePlayers(newGame("C", "A", "B")))
    }

    @Test
    fun alivePlayersLeavesOutPlayersWithoutLives() {
        val game = changeLives(newGame("A", "B", "C", lives = 1), "B", -1, END)
        assertEquals(listOf("A", "C"), alivePlayers(game))
    }

    // ---------- Perder e corrigir vidas ----------

    @Test
    fun losingALifeLowersTheCountByOne() {
        val game = changeLives(newGame("Ana", "Bruno"), "Ana", -1, END)
        assertEquals(4, game.lives["Ana"])
        assertEquals(5, game.lives["Bruno"])
    }

    @Test
    fun correctingUpAddsALife() {
        val game = changeLives(newGame("Ana", "Bruno"), "Ana", +1, END)
        assertEquals(6, game.lives["Ana"])
    }

    @Test
    fun severalChangesAddUp() {
        var game = newGame("Ana", "Bruno", "Carla")
        repeat(3) { game = changeLives(game, "Ana", -1, END) }
        game = changeLives(game, "Ana", +2, END)
        assertEquals(4, game.lives["Ana"])
    }

    @Test
    fun otherPlayersAreNotTouched() {
        val after = changeLives(newGame("Ana", "Bruno", "Carla"), "Ana", -2, END)
        assertEquals(5, after.lives["Bruno"])
        assertEquals(5, after.lives["Carla"])
    }

    @Test
    fun aZeroChangeKeepsTheGameAsItWas() {
        val game = newGame("Ana", "Bruno", "Carla")
        assertEquals(game, changeLives(game, "Ana", 0, END))
    }

    @Test
    fun livesNeverGoBelowZero() {
        val game = changeLives(newGame("Ana", "Bruno", "Carla", lives = 2), "Ana", -10, END)
        assertEquals(0, game.lives["Ana"])
        assertNull("ainda há dois jogadores com vida", game.finishedAt)
    }

    @Test
    fun aPlayerOutsideTheTableIsIgnored() {
        val game = newGame("Ana", "Bruno")
        assertSame(game, changeLives(game, "fantasma", -1, END))
    }

    @Test
    fun oneLifeMeansTheFirstLossEliminatesThatPlayer() {
        val game = changeLives(newGame("Ana", "Bruno", "Carla", lives = 1), "Bruno", -1, END)
        assertEquals(0, game.lives["Bruno"])
        assertEquals(listOf("Ana", "Carla"), alivePlayers(game))
    }

    // ---------- Fim da partida ----------

    @Test
    fun twoPlayersEndTheGameWhenOneLosesAllLives() {
        val game = changeLives(newGame("Ana", "Bruno", lives = 1), "Ana", -1, END)
        assertEquals(END, game.finishedAt)
        assertEquals(listOf("Bruno"), game.winnerIds)
    }

    @Test
    fun threePlayersKeepPlayingAfterTheFirstElimination() {
        val game = changeLives(newGame("Ana", "Bruno", "Carla", lives = 1), "Ana", -1, END)
        assertNull(game.finishedAt)
        assertTrue(game.winnerIds.isEmpty())
    }

    @Test
    fun theGameEndsWhenTheSecondToLastPlayerIsEliminated() {
        var game = newGame("Ana", "Bruno", "Carla", lives = 1)
        game = changeLives(game, "Ana", -1, END)
        assertNull(game.finishedAt)
        game = changeLives(game, "Bruno", -1, END)
        assertEquals(END, game.finishedAt)
        assertEquals(listOf("Carla"), game.winnerIds)
    }

    @Test
    fun theWinnerKeepsTheLivesHeHas() {
        var game = newGame("Ana", "Bruno", lives = 3)
        game = changeLives(game, "Bruno", -3, END)
        assertEquals(listOf("Ana"), game.winnerIds)
        assertEquals(3, game.lives["Ana"])
    }

    @Test
    fun aRevivedPlayerKeepsTheGameGoingAndCanWin() {
        var game = newGame("Ana", "Bruno", "Carla", lives = 1)
        game = changeLives(game, "Ana", -1, END)
        game = changeLives(game, "Ana", +1, END)
        assertNull(game.finishedAt)
        assertEquals(listOf("Ana", "Bruno", "Carla"), alivePlayers(game))

        game = changeLives(game, "Bruno", -1, END)
        game = changeLives(game, "Carla", -1, END)
        assertEquals(listOf("Ana"), game.winnerIds)
    }

    @Test
    fun finishingUsesTheGivenTimestamp() {
        val game = changeLives(newGame("Ana", "Bruno", lives = 1), "Bruno", -1, "2026-05-05T05:05:00Z")
        assertEquals("2026-05-05T05:05:00Z", game.finishedAt)
    }

    @Test
    fun aFinishedGameIgnoresAllChanges() {
        val finished = changeLives(newGame("Ana", "Bruno", lives = 1), "Bruno", -1, END)
        assertSame(finished, changeLives(finished, "Ana", +5, END))
        assertSame(finished, changeLives(finished, "Bruno", +1, END))
    }

    @Test
    fun twentyLivesTakeTwentyLossesToEndATwoPlayerGame() {
        var game = newGame("Ana", "Bruno", lives = 20)
        repeat(19) { game = changeLives(game, "Bruno", -1, END) }
        assertNull(game.finishedAt)
        game = changeLives(game, "Bruno", -1, END)
        assertEquals(listOf("Ana"), game.winnerIds)
    }

    // ---------- Partidas antigas: vencedor, primeiro a morrer e empate ----------

    /** Partida encerrada no formato antigo: pode faltar o vencedor, e a ordem das mortes não é conhecida. */
    private fun oldGame(
        lives: Map<String, Int>,
        winners: List<String> = emptyList(),
        eliminated: List<String> = emptyList(),
        finished: Boolean = true,
        firstDeathTogether: Boolean = false,
    ): Game = Game(
        id = "antiga",
        startedAt = START,
        finishedAt = if (finished) END else null,
        playerIds = lives.keys.toList(),
        settings = GameSettings(),
        lives = lives,
        winnerIds = winners,
        eliminated = eliminated,
        firstDeathTogether = firstDeathTogether,
    )

    @Test
    fun aFinishedGameWithoutAWinnerNeedsAWinner() {
        assertTrue(needsWinner(oldGame(mapOf("Ana" to 0, "Bruno" to 0))))
        assertFalse(needsWinner(oldGame(mapOf("Ana" to 1, "Bruno" to 0), winners = listOf("Ana"))))
    }

    @Test
    fun aRunningGameNeverAsksForAWinnerOrAFirstDeath() {
        val running = oldGame(mapOf("Ana" to 1, "Bruno" to 0), finished = false)
        assertFalse(needsWinner(running))
        assertFalse(needsFirstOut(running))
        assertFalse(isIncomplete(running))
    }

    @Test
    fun aFinishedGameWhoseLosersHaveNoKnownOrderNeedsTheFirstDeath() {
        assertTrue(needsFirstOut(oldGame(mapOf("Ana" to 1, "Bruno" to 0), winners = listOf("Ana"))))
    }

    @Test
    fun noFirstDeathIsNeededWhenNobodyLostLives() {
        val game = oldGame(mapOf("Ana" to 1, "Bruno" to 1), winners = listOf("Ana"))
        assertFalse(needsFirstOut(game))
        assertFalse(isIncomplete(game))
    }

    @Test
    fun noFirstDeathIsNeededWhenTheOrderIsKnown() {
        val game = oldGame(mapOf("Ana" to 1, "Bruno" to 0), winners = listOf("Ana"), eliminated = listOf("Bruno"))
        assertFalse(needsFirstOut(game))
    }

    @Test
    fun theFirstDeathIsStillNeededWhenOnlySomeDeathsAreKnown() {
        val game = oldGame(
            mapOf("Ana" to 1, "Bruno" to 0, "Carla" to 0),
            winners = listOf("Ana"),
            eliminated = listOf("Carla"),
        )
        assertTrue(needsFirstOut(game))
    }

    @Test
    fun firstDeathTogetherAnswersTheFirstDeathQuestion() {
        val game = oldGame(mapOf("Ana" to 0, "Bruno" to 0), winners = listOf("Ana"), firstDeathTogether = true)
        assertFalse(needsFirstOut(game))
        assertFalse(isIncomplete(game))
    }

    @Test
    fun aGameIsIncompleteWhenTheWinnerOrTheFirstDeathIsMissing() {
        assertTrue(isIncomplete(oldGame(mapOf("Ana" to 0, "Bruno" to 0))))
        assertTrue(isIncomplete(oldGame(mapOf("Ana" to 1, "Bruno" to 0), winners = listOf("Ana"))))
        assertFalse(isIncomplete(oldGame(mapOf("Ana" to 1, "Bruno" to 1), winners = listOf("Ana"))))
    }

    @Test
    fun aTieWithoutRecordedPlayersIsOnlyDetectedWhenEveryoneIsOut() {
        assertTrue(isTieWithUnknownPlayers(oldGame(mapOf("Ana" to 0, "Bruno" to 0, "Carla" to 0))))
        assertFalse(
            "com vencedor, o empate já tem nomes",
            isTieWithUnknownPlayers(oldGame(mapOf("Ana" to 0, "Bruno" to 0), winners = listOf("Ana", "Bruno"))),
        )
        assertFalse("alguém ainda tem vida", isTieWithUnknownPlayers(oldGame(mapOf("Ana" to 1, "Bruno" to 0))))
        assertFalse(
            "partida em andamento",
            isTieWithUnknownPlayers(oldGame(mapOf("Ana" to 0, "Bruno" to 0), finished = false)),
        )
    }

    // ---------- Oferta de empate ----------

    @Test
    fun tieOfferAppearsWhenEveryAlivePlayerHasOneLife() {
        val game = changeLives(newGame("Ana", "Bruno", "Carla", lives = 1), "Ana", -1, END)
        assertEquals(listOf("Bruno", "Carla"), tieOfferPlayers(game))
    }

    @Test
    fun tieOfferWorksWithTwoPlayers() {
        assertEquals(listOf("Ana", "Bruno"), tieOfferPlayers(newGame("Ana", "Bruno", lives = 1)))
    }

    @Test
    fun tieOfferIsNullWhileAnAlivePlayerHasMoreThanOneLife() {
        val game = changeLives(newGame("Ana", "Bruno", "Carla", lives = 2), "Ana", -1, END)
        assertNull(tieOfferPlayers(game))
    }

    @Test
    fun tieOfferNeedsAtLeastTwoAlivePlayers() {
        val game = newGame("Ana", "Bruno").copy(lives = mapOf("Ana" to 1, "Bruno" to 0))
        assertNull(tieOfferPlayers(game))
    }

    @Test
    fun tieOfferIsNullForFinishedGames() {
        val finished = changeLives(newGame("Ana", "Bruno", lives = 1), "Ana", -1, END)
        assertNull(tieOfferPlayers(finished))
    }
}
