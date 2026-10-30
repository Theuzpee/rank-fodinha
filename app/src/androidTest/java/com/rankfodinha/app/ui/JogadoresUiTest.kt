package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onFirst
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.ui.screens.JogadoresScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import com.rankfodinha.app.viewmodel.FakeFodinhaRepository
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class JogadoresUiTest {

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

    private fun show(vm: FodinhaViewModel) {
        compose.setContent { RankFodinhaTheme { JogadoresScreen(viewModel = vm) } }
        compose.waitForIdle()
    }

    private fun typeName(name: String) {
        compose.onNode(hasSetTextAction()).performTextInput(name)
    }

    // Adicionar abre a câmera, que este teste de instrumentação não controla. O cadastro com foto é conferido no aparelho.

    @Test
    fun duplicateNamesAreRefusedIgnoringCase() {
        val vm = viewModelWithPlayers("Ana")
        show(vm)
        typeName("ana")
        compose.onNodeWithText("Adicionar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Já existe um jogador com este nome.").assertIsDisplayed()
        assertEquals(1, vm.data.value.players.size)
    }

    @Test
    fun namesWithOneLetterAreRefused() {
        val vm = viewModelWithPlayers()
        show(vm)
        typeName("A")
        compose.onNodeWithText("Adicionar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Digite um nome com pelo menos 2 letras.").assertIsDisplayed()
        assertEquals(0, vm.data.value.players.size)
    }

    @Test
    fun removingAsksForConfirmationFirst() {
        val vm = viewModelWithPlayers("Ana")
        show(vm)
        compose.onNodeWithText("Remover").performClick()
        compose.onNodeWithText("Remover Ana da lista?").assertIsDisplayed()
        assertEquals("nada foi removido antes da confirmação", 1, vm.data.value.players.size)
    }

    @Test
    fun confirmingRemovalDeletesThePlayer() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        show(vm)
        // A lista está em ordem alfabética: o primeiro botão "Remover" é o de Ana.
        compose.onAllNodesWithText("Remover").onFirst().performClick()
        compose.onNode(hasText("Remover") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertEquals(listOf("Bruno"), vm.data.value.players.map { it.name })
    }

    @Test
    fun playersWhoAlreadyPlayedCannotBeRemoved() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread {
            vm.startGame(listOf(vm.id("Ana"), vm.id("Bruno")), GameSettings())
        }
        show(vm)
        compose.onAllNodesWithText("Remover").onFirst().performClick()
        compose.onNodeWithText("Ana já participou de partidas e não pode ser removido.").assertIsDisplayed()
        assertEquals(2, vm.data.value.players.size)
    }
}
