package com.rankfodinha.app.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Partidas inteiras com trocas aleatórias de vidas. A semente é fixa, então cada caso se repete igual.
 * A cada passo conferem-se as regras que nunca podem quebrar.
 */
class FodinhaSimulationTest {

    private data class Outcome(
        val steps: Int,
        val final: Game,
        val trace: List<Map<String, Int>>,
    )

    private val start = "2026-01-01T00:00:00Z"
    private val end = "2026-01-02T00:00:00Z"

    /** Partida em andamento: sem vidas negativas, mesma lista de jogadores e pelo menos dois com vida. */
    private fun checkRunning(game: Game) {
        assertNull(game.finishedAt)
        assertTrue(game.winnerIds.isEmpty())
        assertEquals(game.playerIds.toSet(), game.lives.keys)
        assertTrue("vidas nunca são negativas", game.lives.values.all { it >= 0 })
        assertTrue("partida em andamento tem pelo menos dois jogadores com vida", alivePlayers(game).size >= 2)
    }

    /** Uma mudança só altera o jogador escolhido, pelo valor certo, e só encerra quando sobra no máximo um. */
    private fun checkChange(before: Game, after: Game, who: String, delta: Int) {
        if (before.finishedAt != null) {
            assertEquals("partida encerrada não muda", before, after)
            return
        }
        for (id in before.playerIds) {
            if (id != who) assertEquals("só $who muda", before.lives[id], after.lives[id])
        }
        assertEquals("vidas de $who", maxOf(0, before.lives.getValue(who) + delta), after.lives[who])

        if (after.finishedAt != null) {
            val alive = alivePlayers(after)
            assertTrue("partida terminou com no máximo um vivo", alive.size <= 1)
            assertEquals("o único vivo vence", alive, after.winnerIds)
            assertEquals(end, after.finishedAt)
        } else {
            checkRunning(after)
        }
    }

    /** Partida encerrada: exatamente um vencedor, com vida, e os outros sem vida. */
    private fun checkFinished(game: Game) {
        assertEquals("uma partida encerrada tem exatamente um vencedor", 1, game.winnerIds.size)
        val winner = game.winnerIds.single()
        assertTrue("o vencedor ainda tem vida", game.lives.getValue(winner) > 0)
        assertTrue("os outros estão sem vida", game.lives.filterKeys { it != winner }.values.all { it == 0 })
        assertEquals(end, game.finishedAt)
    }

    private fun simulate(
        seed: Int,
        players: Int,
        lives: Int,
        corrections: Int = 0,
        maxSteps: Int = 20_000,
    ): Outcome {
        val rnd = Random(seed)
        val ids = (1..players).map { "P$it" }
        var game = createGame("sim-$seed", start, ids, GameSettings(startingLives = lives))
        val trace = mutableListOf<Map<String, Int>>()
        var steps = 0

        while (game.finishedAt == null) {
            check(steps < maxSteps) { "a partida $seed não terminou em $maxSteps passos" }
            steps += 1
            checkRunning(game)

            val before = game
            val correction = corrections > 0 && rnd.nextInt(corrections) == 0
            val who = if (correction) ids.random(rnd) else alivePlayers(game).random(rnd)
            val delta = if (correction) 1 else -1
            game = changeLives(game, who, delta, end)

            checkChange(before, game, who, delta)
            trace += game.lives
        }

        checkFinished(game)
        return Outcome(steps, game, trace)
    }

    @Test
    fun simulatedGamesFinishWithOneWinnerWhoStillHasLives() {
        for (seed in 1..200) {
            val outcome = simulate(seed, players = 2 + seed % 11, lives = 1 + seed % 8)
            assertEquals("partida $seed", 1, outcome.final.winnerIds.size)
        }
    }

    @Test
    fun simulatedGamesWithCorrectionsStillFinish() {
        for (seed in 1..200) {
            val outcome = simulate(seed, players = 2 + seed % 7, lives = 2 + seed % 5, corrections = 8)
            assertEquals("partida $seed", 1, outcome.final.winnerIds.size)
        }
    }

    @Test
    fun manyParticipantsStillFinish() {
        for (seed in 1..30) {
            val outcome = simulate(seed, players = 20 + seed, lives = 1 + seed % 3, maxSteps = 200_000)
            assertEquals("partida $seed", 1, outcome.final.winnerIds.size)
        }
    }

    @Test
    fun twoPlayersWithOneLifeEachEndAtTheFirstLoss() {
        for (seed in 1..20) {
            assertEquals("partida $seed", 1, simulate(seed, players = 2, lives = 1).steps)
        }
    }

    @Test
    fun finishedGamesIgnoreLaterChanges() {
        for (seed in 1..50) {
            val final = simulate(seed, players = 3, lives = 2).final
            val anyone = final.playerIds.first()
            assertEquals("partida $seed", final, changeLives(final, anyone, -1, end))
            assertEquals("partida $seed", final, changeLives(final, anyone, +3, end))
        }
    }

    @Test
    fun sameSeedReplaysTheSameGame() {
        for (seed in listOf(7, 42, 1234)) {
            val first = simulate(seed, players = 5, lives = 4, corrections = 6)
            val second = simulate(seed, players = 5, lives = 4, corrections = 6)
            assertEquals("semente $seed", first.trace, second.trace)
            assertEquals("semente $seed", first.final, second.final)
        }
    }

    @Test
    fun differentSeedsUsuallyPlayDifferentGames() {
        val a = simulate(11, players = 4, lives = 5)
        val b = simulate(12, players = 4, lives = 5)
        assertTrue(a.trace != b.trace)
    }
}
