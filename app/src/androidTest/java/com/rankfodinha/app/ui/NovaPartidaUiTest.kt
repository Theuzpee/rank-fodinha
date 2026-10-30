package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.ui.screens.NovaPartidaScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import com.rankfodinha.app.viewmodel.FakeFodinhaRepository
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NovaPartidaUiTest {

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

    private fun show(vm: FodinhaViewModel, onClose: () -> Unit = {}) {
        compose.setContent {
            RankFodinhaTheme { NovaPartidaScreen(viewModel = vm, onClose = onClose) }
        }
        compose.waitForIdle()
    }

    @Test
    fun withoutEnoughPlayersItAsksToRegisterThemFirst() {
        show(viewModelWithPlayers())
        compose.onNodeWithText("Cadastre pelo menos 2 jogadores na aba Jogadores antes de começar uma partida.")
            .assertIsDisplayed()
    }

    @Test
    fun everyRegisteredPlayerIsSelectedByDefault() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        show(vm)
        listOf("Ana", "Bruno", "Carla").forEach { compose.onNodeWithText(it).assertIsSelected() }
        compose.onNodeWithText("3 jogador(es) nesta partida. A mesa precisa de pelo menos 2.").assertIsDisplayed()
    }

    @Test
    fun untickingAPlayerLeavesThemOutOfTheGame() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        show(vm)
        compose.onNodeWithText("Carla").performClick()
        compose.onNodeWithText("Carla").assertIsNotSelected()

        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf(vm.id("Ana"), vm.id("Bruno")), vm.data.value.games.single().playerIds)
    }

    @Test
    fun untickingDownToOnePlayerShowsTheRule() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        show(vm)
        compose.onNodeWithText("Bruno").performClick()
        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.onNodeWithText("A mesa precisa de pelo menos 2 jogadores.").performScrollTo().assertIsDisplayed()
        assertTrue(vm.data.value.games.isEmpty())
    }

    @Test
    fun startingTheGameClosesTheScreen() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        var closed = false
        show(vm) { closed = true }
        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue("a tela pediu para fechar", closed)
        assertEquals(2, vm.data.value.games.single().playerIds.size)
    }

    @Test
    fun theDefaultStartingLivesAreFive() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        show(vm)
        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(GameSettings(), vm.data.value.games.single().settings)
    }

    @Test
    fun theStartingLivesStepperIsSavedWithTheGame() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        show(vm)
        compose.onNodeWithContentDescription("Aumentar vidas iniciais").performClick()
        compose.onNodeWithContentDescription("Aumentar vidas iniciais").performClick()
        compose.onNodeWithText("7").assertIsDisplayed()

        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.waitForIdle()
        val game = vm.data.value.games.single()
        assertEquals(7, game.settings.startingLives)
        assertEquals(mapOf(vm.id("Ana") to 7, vm.id("Bruno") to 7), game.lives)
    }

    @Test
    fun thereIsNoUpperLimitOnParticipants() {
        val names = (1..12).map { "Pessoa $it" }
        val vm = viewModelWithPlayers(*names.toTypedArray())
        show(vm)
        compose.onNodeWithText("Começar partida").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(12, vm.data.value.games.single().playerIds.size)
    }

    @Test
    fun theStartButtonIsBigEnoughToTouch() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        show(vm)
        compose.onNodeWithText("Começar partida").performScrollTo().assertHeightIsAtLeast(56.dp)
    }
}
