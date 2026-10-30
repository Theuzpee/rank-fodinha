package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.ui.screens.RankingScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import com.rankfodinha.app.viewmodel.FakeFodinhaRepository
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RankingUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun viewModelWithPlayers(vararg names: String): FodinhaViewModel {
        var created: FodinhaViewModel? = null
        compose.runOnUiThread {
            val vm = FodinhaViewModel(FakeFodinhaRepository())
            names.forEach { vm.addPlayer(it, vm.newPlayerId()) }
            created = vm
        }
        return created!!
    }

    private fun FodinhaViewModel.id(name: String): String = data.value.players.first { it.name == name }.id

    /** Partida de dois jogadores com 1 vida: [loser] perde a vida e sai, e [winner] vence. */
    private fun finishQuickGame(vm: FodinhaViewModel, winner: String, loser: String) {
        vm.startGame(listOf(vm.id(winner), vm.id(loser)), GameSettings(startingLives = 1))
        vm.adjustLives(vm.id(loser), -1)
    }

    private fun show(vm: FodinhaViewModel) {
        compose.setContent { RankFodinhaTheme { RankingScreen(viewModel = vm) } }
        compose.waitForIdle()
    }

    @Test
    fun emptyRankingExplainsWhatToDo() {
        show(viewModelWithPlayers())
        compose.onNodeWithText("Cadastre jogadores e registre partidas para montar o ranking.").assertIsDisplayed()
    }

    @Test
    fun winnersAreShownWithTheirStats() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread { finishQuickGame(vm, "Ana", "Bruno") }
        show(vm)
        compose.onNodeWithText("1 vitória · 1 jogo · 100,0% de aproveitamento").assertIsDisplayed()
        compose.onNodeWithText("0 vitórias · 1 jogo · 0,0% de aproveitamento").assertIsDisplayed()
    }

    @Test
    fun playersWithoutFinishedGamesShowADashAndNoStats() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        compose.runOnUiThread { finishQuickGame(vm, "Ana", "Bruno") }
        show(vm)
        compose.onAllNodesWithText("–").assertCountEquals(1)
        compose.onNodeWithText("Ainda sem partidas encerradas").assertIsDisplayed()
    }

    @Test
    fun clearButtonIsDisabledWhileThereIsNothingToClear() {
        show(viewModelWithPlayers("Ana", "Bruno"))
        compose.onNodeWithText("Limpar ranking").assertIsNotEnabled()
    }

    @Test
    fun clearingAsksForConfirmationAndThenEmptiesTheStats() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread { finishQuickGame(vm, "Ana", "Bruno") }
        show(vm)

        compose.onNodeWithText("Limpar ranking").performClick()
        compose.onNodeWithText("Limpar o ranking?").assertIsDisplayed()
        assertEquals("nada foi apagado antes da confirmação", 1, vm.data.value.games.size)

        compose.onNode(hasText("Limpar") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        assertTrue(vm.data.value.games.none { it.finishedAt != null })
        compose.onAllNodesWithText("Ainda sem partidas encerradas").assertCountEquals(2)
        compose.onNodeWithText("Limpar ranking").assertIsNotEnabled()
        assertEquals("os jogadores continuam cadastrados", 2, vm.data.value.players.size)
    }

    @Test
    fun cancellingTheClearKeepsTheRanking() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread { finishQuickGame(vm, "Ana", "Bruno") }
        show(vm)

        compose.onNodeWithText("Limpar ranking").performClick()
        compose.onNode(hasText("Cancelar") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        compose.onNodeWithText("1 vitória · 1 jogo · 100,0% de aproveitamento").assertIsDisplayed()
        assertEquals(1, vm.data.value.games.size)
    }

    @Test
    fun clearingKeepsTheGameInProgress() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        compose.runOnUiThread {
            finishQuickGame(vm, "Ana", "Bruno")
            vm.startGame(listOf(vm.id("Ana"), vm.id("Carla")), GameSettings())
        }
        show(vm)

        compose.onNodeWithText("Limpar ranking").performClick()
        compose.onNode(hasText("Limpar") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        val remaining = vm.data.value.games
        assertEquals(1, remaining.size)
        assertTrue(remaining.single().finishedAt == null)
    }
}
