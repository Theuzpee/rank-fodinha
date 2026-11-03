package com.rankfodinha.app.viewmodel

import com.rankfodinha.app.data.FodinhaData
import com.rankfodinha.app.data.Game
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.isIncomplete
import com.rankfodinha.app.data.needsFirstOut
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FodinhaViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun settings(lives: Int = 5) = GameSettings(startingLives = lives)

    /** Uma tela do app com repositório em memória. */
    private class Screen(initial: FodinhaData = FodinhaData()) {
        val repo = FakeFodinhaRepository(initial)
        val vm = FodinhaViewModel(repo)

        val state: FodinhaData
            get() = vm.data.value

        val game: Game
            get() = state.games.single()

        fun id(name: String): String = state.players.first { it.name == name }.id

        fun addAll(vararg names: String) {
            names.forEach { name -> assertNull("cadastro de $name", vm.addPlayer(name, vm.newPlayerId())) }
        }
    }

    /** Partida de dois jogadores com 1 vida: [loser] perde a vida e sai, e [winner] vence. */
    private fun Screen.finishQuickGame(winner: String, loser: String) {
        assertNull(vm.startGame(listOf(id(winner), id(loser)), settings(lives = 1)))
        vm.adjustLives(id(loser), -1)
    }

    private fun Screen.startThree(lives: Int = 5): List<String> {
        addAll("Ana", "Bruno", "Carla")
        val ids = listOf(id("Ana"), id("Bruno"), id("Carla"))
        assertNull(vm.startGame(ids, settings(lives)))
        return ids
    }

    // ---------- Carregamento ----------

    @Test
    fun loadsTheSavedDataWhenTheAppOpens() {
        val saved = FodinhaData(players = listOf(Player("ana", "Ana")))
        val screen = Screen(saved)
        assertTrue(screen.vm.loaded.value)
        assertEquals(saved, screen.state)
    }

    @Test
    fun aFailedLoadKeepsTheAppBlockedSoNothingIsOverwritten() {
        val repo = FakeFodinhaRepository(FodinhaData(players = listOf(Player("ana", "Ana"))))
        repo.loadFailure = IOException("arquivo corrompido")
        val vm = FodinhaViewModel(repo)

        assertFalse(vm.loaded.value)
        assertEquals("Os dados ainda não foram carregados.", vm.addPlayer("Bruno", vm.newPlayerId()))
        assertEquals("Os dados ainda não foram carregados.", vm.startGame(listOf("a", "b"), settings()))
        assertEquals("Os dados ainda não foram carregados.", vm.removePlayer("a"))
        assertEquals("Os dados ainda não foram carregados.", vm.completeGame("a", emptyList(), null, false))
        vm.adjustLives("a", -1)
        vm.clearRanking()
        vm.cancelGame()

        assertTrue("nada foi gravado por cima dos dados salvos", repo.saves.isEmpty())
        assertTrue("nenhum dado foi inventado", vm.data.value.players.isEmpty())
    }

    // ---------- Jogadores ----------

    @Test
    fun addPlayerTrimsAndCollapsesSpaces() {
        val screen = Screen()
        assertNull(screen.vm.addPlayer("   Ana     Maria  ", screen.vm.newPlayerId()))
        assertEquals("Ana Maria", screen.state.players.single().name)
    }

    @Test
    fun addPlayerRejectsNamesShorterThanTwoLetters() {
        val screen = Screen()
        listOf("", " ", "A", "  b  ").forEach { name ->
            assertEquals(
                "nome '$name'",
                "Digite um nome com pelo menos 2 letras.",
                screen.vm.addPlayer(name, screen.vm.newPlayerId()),
            )
        }
        assertTrue(screen.state.players.isEmpty())
    }

    @Test
    fun addPlayerRejectsDuplicatesIgnoringCaseAndSpaces() {
        val screen = Screen()
        screen.addAll("Ana")
        listOf("Ana", "ana", "  ANA  ", "aNa").forEach { name ->
            assertEquals(
                "duplicado '$name'",
                "Já existe um jogador com este nome.",
                screen.vm.addPlayer(name, screen.vm.newPlayerId()),
            )
        }
        assertEquals(1, screen.state.players.size)
    }

    @Test
    fun thereIsNoLimitOnTheNumberOfParticipants() {
        val screen = Screen()
        for (i in 1..120) {
            assertNull(screen.vm.addPlayer("Pessoa $i", screen.vm.newPlayerId()))
        }
        assertEquals(120, screen.state.players.size)
    }

    @Test
    fun everyPlayerChangeIsSavedToTheRepository() {
        val screen = Screen()
        screen.addAll("Ana")
        assertEquals(screen.state, screen.repo.stored)
    }

    @Test
    fun removePlayerDeletesAPlayerWithoutGames() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        assertNull(screen.vm.removePlayer(screen.id("Ana")))
        assertEquals(listOf("Bruno"), screen.state.players.map { it.name })
    }

    @Test
    fun removePlayerIsBlockedOnceThePlayerHasPlayed() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        assertNull(screen.vm.startGame(listOf(screen.id("Ana"), screen.id("Bruno")), settings()))
        assertEquals(
            "Este jogador já participou de partidas e não pode ser removido.",
            screen.vm.removePlayer(screen.id("Ana")),
        )
        assertEquals(2, screen.state.players.size)
    }

    @Test
    fun playerHasGamesReflectsTheHistory() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno", "Carla")
        assertFalse(screen.vm.playerHasGames(screen.id("Ana")))
        assertNull(screen.vm.startGame(listOf(screen.id("Ana"), screen.id("Bruno")), settings()))
        assertTrue(screen.vm.playerHasGames(screen.id("Ana")))
        assertFalse(screen.vm.playerHasGames(screen.id("Carla")))
    }

    // ---------- Começar partida ----------

    @Test
    fun startGameNeedsAtLeastTwoPlayers() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        assertEquals("A mesa precisa de pelo menos 2 jogadores.", screen.vm.startGame(listOf(screen.id("Ana")), settings()))
        assertEquals("A mesa precisa de pelo menos 2 jogadores.", screen.vm.startGame(emptyList(), settings()))
        assertTrue(screen.state.games.isEmpty())
    }

    @Test
    fun startGameRejectsRepeatedPlayers() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        val ana = screen.id("Ana")
        assertEquals("Há jogadores repetidos na mesa.", screen.vm.startGame(listOf(ana, ana), settings()))
        assertTrue(screen.state.games.isEmpty())
    }

    @Test
    fun startGameRejectsPlayersThatNoLongerExist() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        assertEquals(
            "Algum jogador da mesa não existe mais.",
            screen.vm.startGame(listOf(screen.id("Ana"), "fantasma"), settings()),
        )
    }

    @Test
    fun startGameValidatesStartingLives() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        val ids = listOf(screen.id("Ana"), screen.id("Bruno"))
        listOf(0, 21, -1).forEach { lives ->
            assertEquals("vidas $lives", "As vidas iniciais precisam ser de 1 a 20.", screen.vm.startGame(ids, settings(lives)))
        }
        assertNull("aceita o mínimo", screen.vm.startGame(ids, settings(1)))
        screen.vm.cancelGame()
        assertNull("aceita o máximo", screen.vm.startGame(ids, settings(20)))
    }

    @Test
    fun startGameKeepsTheOrderGivenAndEveryPlayerStartsWithTheChosenLives() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno", "Carla")
        val ids = listOf(screen.id("Carla"), screen.id("Ana"), screen.id("Bruno"))
        assertNull(screen.vm.startGame(ids, settings(lives = 7)))

        assertEquals(ids, screen.game.playerIds)
        assertEquals(7, screen.game.settings.startingLives)
        assertEquals(ids.associateWith { 7 }, screen.game.lives)
    }

    @Test
    fun aSecondGameCannotStartWhileOneIsRunning() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        val ids = listOf(screen.id("Ana"), screen.id("Bruno"))
        assertNull(screen.vm.startGame(ids, settings()))
        assertEquals(
            "Já existe uma partida em andamento. Encerre ou cancele a atual antes de começar outra.",
            screen.vm.startGame(ids, settings()),
        )
        assertEquals(1, screen.state.games.size)
    }

    @Test
    fun aNewGameCanStartOnceTheLastOneHasEnded() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        screen.finishQuickGame("Ana", "Bruno")
        assertNull(screen.vm.startGame(listOf(screen.id("Ana"), screen.id("Bruno")), settings()))
        assertEquals(2, screen.state.games.size)
        assertEquals(1, screen.state.games.count { it.finishedAt != null })
    }

    // ---------- Vidas ----------

    @Test
    fun adjustLivesDecreasesAndIncreasesTheRunningGame() {
        val screen = Screen()
        val ids = screen.startThree()
        screen.vm.adjustLives(ids[1], -1)
        assertEquals(4, screen.game.lives[ids[1]])
        screen.vm.adjustLives(ids[1], +2)
        assertEquals(6, screen.game.lives[ids[1]])
    }

    @Test
    fun adjustLivesEndsTheGameWhenOneSurvives() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        screen.finishQuickGame("Ana", "Bruno")

        assertNotNull(screen.game.finishedAt)
        assertEquals(listOf(screen.id("Ana")), screen.game.winnerIds)
        assertEquals(0, screen.game.lives[screen.id("Bruno")])
    }

    @Test
    fun adjustLivesWithoutARunningGameChangesNothingAndSavesNothing() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        val savesBefore = screen.repo.saves.size
        screen.vm.adjustLives(screen.id("Ana"), -1)
        assertEquals(savesBefore, screen.repo.saves.size)
        assertTrue(screen.state.games.isEmpty())
    }

    @Test
    fun adjustLivesAfterTheGameEndedChangesNothing() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        screen.finishQuickGame("Ana", "Bruno")
        val finished = screen.game
        val savesBefore = screen.repo.saves.size

        screen.vm.adjustLives(screen.id("Ana"), -1)
        screen.vm.adjustLives(screen.id("Bruno"), +3)

        assertEquals(finished, screen.game)
        assertEquals(savesBefore, screen.repo.saves.size)
    }

    @Test
    fun cancelGameDiscardsOnlyTheRunningGame() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno", "Carla")
        screen.finishQuickGame("Ana", "Bruno")
        assertNull(screen.vm.startGame(listOf(screen.id("Ana"), screen.id("Carla")), settings()))

        screen.vm.cancelGame()

        assertEquals(1, screen.state.games.size)
        assertNotNull(screen.state.games.single().finishedAt)
    }

    // ---------- Limpar ranking ----------

    @Test
    fun clearRankingRemovesOnlyTheFinishedGames() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno", "Carla")
        screen.finishQuickGame("Ana", "Bruno")
        assertNull(screen.vm.startGame(listOf(screen.id("Ana"), screen.id("Carla")), settings()))
        val running = screen.state.games.single { it.finishedAt == null }

        screen.vm.clearRanking()

        assertEquals(listOf(running), screen.state.games)
        assertNull(running.finishedAt)
    }

    @Test
    fun clearRankingKeepsEveryRegisteredPlayer() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno", "Carla")
        screen.finishQuickGame("Ana", "Bruno")

        screen.vm.clearRanking()

        assertEquals(3, screen.state.players.size)
    }

    @Test
    fun clearRankingWithoutFinishedGamesSavesNothing() {
        val screen = Screen()
        screen.startThree()
        val savesBefore = screen.repo.saves.size

        screen.vm.clearRanking()

        assertEquals(savesBefore, screen.repo.saves.size)
        assertEquals(1, screen.state.games.size)
    }

    @Test
    fun afterClearingARankingPlayersWhoOnlyPlayedFinishedGamesCanBeRemoved() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        screen.finishQuickGame("Ana", "Bruno")
        assertTrue(screen.vm.playerHasGames(screen.id("Ana")))

        screen.vm.clearRanking()

        assertFalse(screen.vm.playerHasGames(screen.id("Ana")))
        assertNull(screen.vm.removePlayer(screen.id("Ana")))
        assertEquals(listOf("Bruno"), screen.state.players.map { it.name })
    }

    // ---------- Gravação ----------

    @Test
    fun everyRealChangeIsSavedAndNoOpChangesAreNot() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        assertEquals(2, screen.repo.saves.size)

        val ids = listOf(screen.id("Ana"), screen.id("Bruno"))
        assertNull(screen.vm.startGame(ids, settings()))
        assertEquals(3, screen.repo.saves.size)

        screen.vm.adjustLives(ids[0], 0)
        assertEquals("ajuste zero não grava de novo", 3, screen.repo.saves.size)

        screen.vm.adjustLives(ids[0], -1)
        assertEquals(4, screen.repo.saves.size)
        assertEquals(screen.state, screen.repo.stored)
    }

    @Test
    fun actionsWithoutARunningGameAreHarmless() {
        val screen = Screen()
        screen.addAll("Ana", "Bruno")
        val before = screen.state

        screen.vm.cancelGame()
        screen.vm.clearRanking()
        screen.vm.adjustLives(screen.id("Ana"), 1)

        assertEquals(before, screen.state)
    }

    // ---------- Completar partida antiga ----------

    private val ana = Player("ana", "Ana")
    private val bruno = Player("bruno", "Bruno")
    private val carla = Player("carla", "Carla")

    /** Partida antiga encerrada. Sem vencedor, a menos que [winners] diga; sem ordem das mortes, a menos que [eliminated] diga. */
    private fun oldGame(
        lives: Map<String, Int>,
        winners: List<String> = emptyList(),
        eliminated: List<String> = emptyList(),
        finished: Boolean = true,
    ): Game = Game(
        id = "antiga",
        startedAt = "2026-01-01T10:00:00Z",
        finishedAt = if (finished) "2026-01-01T11:00:00Z" else null,
        playerIds = lives.keys.toList(),
        settings = GameSettings(),
        lives = lives,
        winnerIds = winners,
        eliminated = eliminated,
    )

    /** Tela com a partida antiga já carregada, e os três jogadores cadastrados. */
    private fun oldScreen(game: Game) = Screen(FodinhaData(players = listOf(ana, bruno, carla), games = listOf(game)))

    @Test
    fun completingAGameWithoutAWinnerRecordsTheWinnersAndTheFirstDeath() {
        val screen = oldScreen(oldGame(mapOf("ana" to 0, "bruno" to 0, "carla" to 0)))

        assertNull(screen.vm.completeGame("antiga", listOf("carla", "ana"), "bruno", diedTogether = false))

        val game = screen.game
        assertEquals("os vencedores ficam na ordem da mesa", listOf("ana", "carla"), game.winnerIds)
        assertEquals(listOf("bruno", "ana", "carla"), game.eliminated)
        assertFalse(game.firstDeathTogether)
        assertFalse(isIncomplete(game))
        assertEquals(screen.state, screen.repo.stored)
    }

    @Test
    fun completingNeedsTheMissingWinnerAndFirstDeathAndChecksThePlayers() {
        val screen = oldScreen(oldGame(mapOf("ana" to 0, "bruno" to 0, "carla" to 0)))

        assertEquals("Escolha pelo menos um jogador.", screen.vm.completeGame("antiga", emptyList(), "bruno", false))
        assertEquals("Escolha jogadores da partida.", screen.vm.completeGame("antiga", listOf("fantasma"), "bruno", false))
        assertEquals("Escolha quem morreu primeiro.", screen.vm.completeGame("antiga", listOf("ana"), null, false))

        assertTrue("nada mudou", screen.game.winnerIds.isEmpty() && screen.game.eliminated.isEmpty())
        assertTrue(screen.repo.saves.isEmpty())
    }

    @Test
    fun aPlayerWhoStillHadLivesCannotBeTheFirstToDie() {
        val screen = oldScreen(oldGame(mapOf("ana" to 1, "bruno" to 0, "carla" to 0), winners = listOf("ana")))
        assertEquals("Escolha quem morreu primeiro.", screen.vm.completeGame("antiga", emptyList(), "ana", false))
    }

    @Test
    fun completingOnlyFillsWhatIsMissing() {
        val screen = oldScreen(oldGame(mapOf("ana" to 1, "bruno" to 0, "carla" to 0), winners = listOf("ana")))

        assertNull(screen.vm.completeGame("antiga", listOf("carla"), "bruno", diedTogether = false))

        assertEquals("o vencedor já registrado não muda", listOf("ana"), screen.game.winnerIds)
        assertEquals(listOf("bruno", "carla"), screen.game.eliminated)
    }

    @Test
    fun diedTogetherKeepsTheGameOutOfFirstDeaths() {
        val screen = oldScreen(oldGame(mapOf("ana" to 0, "bruno" to 0, "carla" to 0)))

        assertNull(screen.vm.completeGame("antiga", listOf("ana", "bruno", "carla"), null, diedTogether = true))

        val game = screen.game
        assertTrue(game.firstDeathTogether)
        assertTrue("ninguém entra como primeiro a morrer", game.eliminated.isEmpty())
        assertFalse(needsFirstOut(game))
        assertFalse(isIncomplete(game))
    }

    @Test
    fun diedTogetherCannotBeMixedWithAPlayerWhoDiedFirst() {
        val screen = oldScreen(oldGame(mapOf("ana" to 0, "bruno" to 0, "carla" to 0)))

        assertEquals(
            "Escolha quem morreu primeiro, ou marque que morreram juntos.",
            screen.vm.completeGame("antiga", listOf("ana"), "bruno", diedTogether = true),
        )
        assertFalse(screen.game.firstDeathTogether)
        assertTrue(screen.game.winnerIds.isEmpty())
    }

    @Test
    fun aCompletedGameCannotBeCompletedAgain() {
        val screen = oldScreen(
            oldGame(mapOf("ana" to 1, "bruno" to 0), winners = listOf("ana"), eliminated = listOf("bruno")),
        )
        assertEquals(
            "Esta partida já está completa.",
            screen.vm.completeGame("antiga", emptyList(), null, diedTogether = false),
        )
    }

    @Test
    fun aRunningGameOrAnUnknownGameCannotBeCompleted() {
        val running = oldScreen(oldGame(mapOf("ana" to 1, "bruno" to 0), finished = false))
        assertEquals(
            "Esta partida ainda está em andamento.",
            running.vm.completeGame("antiga", listOf("ana"), "bruno", diedTogether = false),
        )

        val screen = oldScreen(oldGame(mapOf("ana" to 0, "bruno" to 0)))
        assertEquals(
            "Esta partida não existe mais.",
            screen.vm.completeGame("nao-existe", listOf("ana"), "bruno", diedTogether = false),
        )
    }
}
