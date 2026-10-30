package com.rankfodinha.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ORDER_START = "2026-01-01T00:00:00Z"
private const val ORDER_END = "2026-01-02T00:00:00Z"

private fun partida(vararg players: String, lives: Int = 1): Game =
    createGame("g-ordem", ORDER_START, players.toList(), GameSettings(startingLives = lives))

/**
 * Ordem das eliminações: quem perde a última vida entra em [Game.eliminated], na ordem em que isso acontece.
 * Uma correção que devolve a vida tira o nome da lista. O vencedor nunca entra na lista.
 */
class EliminationOrderTest {

    @Test
    fun aPlayerWhoLosesTheirLastLifeIsRecordedAsEliminated() {
        val game = changeLives(partida("Ana", "Bruno", "Carla"), "Bruno", -1, ORDER_END)
        assertEquals(listOf("Bruno"), game.eliminated)
    }

    @Test
    fun losingLivesWithoutDyingRecordsNothing() {
        var game = partida("Ana", "Bruno", lives = 3)
        game = changeLives(game, "Ana", -1, ORDER_END)
        game = changeLives(game, "Ana", -1, ORDER_END)
        assertEquals(1, game.lives["Ana"])
        assertTrue(game.eliminated.isEmpty())
    }

    @Test
    fun losingMoreLivesThanAPlayerHasRecordsTheEliminationOnce() {
        var game = partida("Ana", "Bruno", "Carla", lives = 2)
        game = changeLives(game, "Ana", -5, ORDER_END)
        assertEquals(listOf("Ana"), game.eliminated)
        game = changeLives(game, "Ana", -1, ORDER_END)
        assertEquals(listOf("Ana"), game.eliminated)
    }

    @Test
    fun eliminationsAreKeptInTheOrderTheyHappen() {
        var game = partida("Ana", "Bruno", "Carla", "Diego")
        game = changeLives(game, "Carla", -1, ORDER_END)
        assertEquals(listOf("Carla"), game.eliminated)
        game = changeLives(game, "Ana", -1, ORDER_END)
        assertEquals(listOf("Carla", "Ana"), game.eliminated)
        game = changeLives(game, "Diego", -1, ORDER_END)
        assertEquals(listOf("Bruno"), game.winnerIds)
        assertEquals(listOf("Carla", "Ana", "Diego"), game.eliminated)
    }

    @Test
    fun aCorrectionThatReturnsTheLifeRemovesThePlayerFromTheList() {
        var game = partida("Ana", "Bruno", "Carla")
        game = changeLives(game, "Ana", -1, ORDER_END)
        assertEquals(listOf("Ana"), game.eliminated)
        game = changeLives(game, "Ana", +1, ORDER_END)
        assertEquals(1, game.lives["Ana"])
        assertTrue(game.eliminated.isEmpty())
    }

    @Test
    fun aPlayerWhoDiesAgainAfterACorrectionIsRecordedAgainAtTheNewPosition() {
        var game = partida("Ana", "Bruno", "Carla", "Diego")
        game = changeLives(game, "Ana", -1, ORDER_END)
        game = changeLives(game, "Bruno", -1, ORDER_END)
        assertEquals(listOf("Ana", "Bruno"), game.eliminated)
        game = changeLives(game, "Ana", +1, ORDER_END)
        assertEquals(listOf("Bruno"), game.eliminated)
        game = changeLives(game, "Ana", -1, ORDER_END)
        assertEquals(listOf("Bruno", "Ana"), game.eliminated)
    }

    @Test
    fun theWinnerIsNeverRecordedAsEliminated() {
        val game = changeLives(partida("Ana", "Bruno"), "Bruno", -1, ORDER_END)
        assertEquals(listOf("Ana"), game.winnerIds)
        assertEquals(listOf("Bruno"), game.eliminated)
    }

    @Test
    fun aFinishedGameIgnoresLaterChangesAndKeepsItsList() {
        val finished = changeLives(partida("Ana", "Bruno"), "Bruno", -1, ORDER_END)
        assertEquals(finished, changeLives(finished, "Bruno", +1, ORDER_END))
        assertEquals(listOf("Bruno"), finished.eliminated)
    }
}
