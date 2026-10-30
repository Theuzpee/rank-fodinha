package com.rankfodinha.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.ui.components.GameBoard
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import com.rankfodinha.app.viewmodel.FakeFodinhaRepository
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GameBoardUiTest {

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

    private fun FodinhaViewModel.start(vararg names: String, lives: Int = 5) {
        val ids = names.map { id(it) }
        check(startGame(ids, GameSettings(startingLives = lives)) == null) { "não foi possível começar a partida" }
    }

    private fun runOnUi(block: () -> Unit) = compose.runOnUiThread(block)

    /** Mostra a partida (ou a última) como a aba Jogo mostra, dentro da mesma coluna rolável. */
    @Composable
    private fun BoardHarness(vm: FodinhaViewModel) {
        val data by vm.data.collectAsState()
        val names = data.players.associate { it.id to it.name }
        val game = data.games.lastOrNull()
        if (game != null) {
            ScreenColumn {
                GameBoard(game = game, names = names, viewModel = vm)
            }
        }
    }

    private fun show(vm: FodinhaViewModel) {
        compose.setContent { RankFodinhaTheme { BoardHarness(vm) } }
        compose.waitForIdle()
    }

    @Test
    fun everyPlayerHasBigMinusAndPlusButtons() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno") }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Aumentar vidas de Bruno").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun tappingMinusLowersTheLivesOnScreenAndInTheGame() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno") }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("4").assertIsDisplayed()
        assertEquals(4, vm.data.value.games.single().lives[vm.id("Ana")])
    }

    @Test
    fun tappingPlusCorrectsAMistake() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno") }
        show(vm)
        compose.onNodeWithContentDescription("Aumentar vidas de Bruno").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("6").assertIsDisplayed()
    }

    @Test
    fun aPlayerWithNoLivesIsMarkedEliminatedAndCannotGoLower() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        runOnUi { vm.start("Ana", "Bruno", "Carla", lives = 1) }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Eliminado").assertIsDisplayed()
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").assertIsNotEnabled()
    }

    @Test
    fun plusBringsAnEliminatedPlayerBack() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        runOnUi { vm.start("Ana", "Bruno", "Carla", lives = 1) }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Aumentar vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Eliminado").assertCountEquals(0)
    }

    @Test
    fun losingTheLastRivalsLifeEndsTheGameAndShowsTheWinner() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno", lives = 1) }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Partida encerrada").assertIsDisplayed()
        compose.onNodeWithText("Vencedor: Bruno").assertIsDisplayed()
    }

    @Test
    fun aFinishedGameHasNoLifeButtons() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno", lives = 1) }
        show(vm)
        compose.onNodeWithContentDescription("Diminuir vidas de Ana").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Aumentar vidas de Bruno").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Diminuir vidas de Bruno").assertCountEquals(0)
    }

    @Test
    fun aRunningGameHasNoHeaderCard() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        runOnUi { vm.start("Ana", "Bruno", "Carla") }
        show(vm)
        compose.onAllNodesWithText("Partida em andamento").assertCountEquals(0)
        compose.onAllNodesWithText("Ainda na partida", substring = true).assertCountEquals(0)
    }

    @Test
    fun theSingularWordIsUsedForOneLife() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        runOnUi { vm.start("Ana", "Bruno", lives = 1) }
        show(vm)
        compose.onAllNodesWithText("vida").assertCountEquals(2)
        compose.onAllNodesWithText("vidas").assertCountEquals(0)
    }
}
