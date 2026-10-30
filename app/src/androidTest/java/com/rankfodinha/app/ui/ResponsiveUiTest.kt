package com.rankfodinha.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.ui.components.GameBoard
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.screens.JogadoresScreen
import com.rankfodinha.app.ui.screens.NovaPartidaScreen
import com.rankfodinha.app.ui.screens.RankingScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import com.rankfodinha.app.viewmodel.FakeFodinhaRepository
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Layout em telas estreitas (320 dp) com a fonte 30% maior, e em telas largas.
 * Os controles precisam continuar visíveis e dentro da largura disponível.
 */
class ResponsiveUiTest {

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

    private fun FodinhaViewModel.startWith(vararg names: String, lives: Int = 5) {
        check(startGame(names.map { id(it) }, GameSettings(startingLives = lives)) == null) {
            "não foi possível começar a partida"
        }
    }

    /** Coloca o conteúdo numa caixa de [widthDp] dp, com a fonte 30% maior, como num aparelho pequeno. */
    private fun renderNarrow(widthDp: Int, content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.3f)) {
                RankFodinhaTheme {
                    Box(Modifier.testTag("box").width(widthDp.dp).fillMaxHeight()) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    /** O elemento está visível e dentro da caixa, sem sair pelas laterais. */
    private fun assertInsideBox(label: String, node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val box = compose.onNodeWithTag("box").fetchSemanticsNode().boundsInRoot
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue("$label sai da largura da tela", bounds.left >= box.left - 1f && bounds.right <= box.right + 1f)
    }

    @Composable
    private fun LivesHarness(vm: FodinhaViewModel) {
        val data by vm.data.collectAsState()
        val names = data.players.associate { it.id to it.name }
        ScreenColumn {
            GameBoard(game = data.games.last(), names = names, viewModel = vm)
        }
    }

    @Test
    fun lifeButtonsStayInsideANarrowScreenWithBiggerText() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        compose.runOnUiThread { vm.startWith("Ana", "Bruno", "Carla") }
        renderNarrow(320) { LivesHarness(vm) }

        for (name in listOf("Ana", "Bruno", "Carla")) {
            assertInsideBox("botão + de $name", compose.onNodeWithContentDescription("Aumentar vidas de $name").performScrollTo())
            assertInsideBox("botão − de $name", compose.onNodeWithContentDescription("Diminuir vidas de $name").performScrollTo())
        }
    }

    @Test
    fun startButtonIsReachableOnANarrowScreen() {
        val vm = viewModelWithPlayers("Ana", "Bruno", "Carla")
        renderNarrow(320) { NovaPartidaScreen(viewModel = vm, onClose = {}) }

        val start = compose.onNodeWithText("Começar partida").performScrollTo()
        assertInsideBox("botão Começar partida", start)
        start.assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun rankingSummaryWrapsInsteadOfOverflowing() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread {
            vm.startWith("Ana", "Bruno", lives = 1)
            vm.adjustLives(vm.id("Bruno"), -1)
        }
        renderNarrow(320) { RankingScreen(viewModel = vm) }

        assertInsideBox(
            "resumo do ranking",
            compose.onNodeWithText("1 vitória · 0 empates · 0 derrotas"),
        )
        assertInsideBox("botão Limpar ranking", compose.onNodeWithText("Limpar ranking"))
    }

    @Test
    fun addPlayerButtonFitsANarrowScreen() {
        renderNarrow(320) { JogadoresScreen(viewModel = viewModelWithPlayers()) }
        assertInsideBox("botão Adicionar", compose.onNodeWithText("Adicionar"))
    }

    @Test
    fun wideScreensKeepTheContentCappedAndCentred() {
        val vm = viewModelWithPlayers("Ana", "Bruno")
        compose.runOnUiThread { vm.startWith("Ana", "Bruno") }

        // Caixa de 900 dp com densidade menor, para caber na janela do teste.
        // O teto de 640 dp e o padding de 16 dp são medidos em dp, então valem em qualquer densidade.
        val scale = 0.5f
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(scale, fontScale = 1f)) {
                RankFodinhaTheme {
                    Box(Modifier.testTag("box").width(900.dp).fillMaxHeight()) { LivesHarness(vm) }
                }
            }
        }
        compose.waitForIdle()

        val plus = compose.onNodeWithContentDescription("Aumentar vidas de Ana").performScrollTo()
        val bounds = plus.fetchSemanticsNode().boundsInRoot
        // Com 900 dp de caixa, o conteúdo fica entre 146 dp e 754 dp (centralizado, com 640 dp de largura).
        assertTrue("o conteúdo começou fora do centro", bounds.left >= 146f * scale - 1f)
        assertTrue("o conteúdo passou do teto de 640 dp", bounds.right <= 754f * scale + 1f)
    }
}
