package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rankfodinha.app.MainActivity
import org.junit.Assume
import org.junit.Rule
import org.junit.Test

/**
 * Navegação do app inteiro, com a MainActivity de verdade.
 * Os testes só leem e navegam: não começam, não descartam e não alteram partidas do aparelho.
 */
class NavigationUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForTabs() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasText("Jogo") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tab(label: String) = compose.onNode(hasText(label) and hasClickAction())

    @Test
    fun bottomBarHasTheThreeTabs() {
        waitForTabs()
        tab("Jogo").assertIsDisplayed()
        tab("Ranking").assertIsDisplayed()
        tab("Jogadores").assertIsDisplayed()
    }

    @Test
    fun eachTabOpensItsScreen() {
        waitForTabs()
        tab("Ranking").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quem mais ganhou partidas").assertIsDisplayed()

        tab("Jogadores").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Adicionar").assertIsDisplayed()

        tab("Jogo").performClick()
        compose.waitForIdle()
    }

    @Test
    fun novaPartidaOpensAsAFullScreenRouteWithoutTheTabBar() {
        waitForTabs()
        tab("Jogo").performClick()
        compose.waitForIdle()

        val newGame = compose.onAllNodes(hasText("Nova partida") and hasClickAction())
        if (newGame.fetchSemanticsNodes().isEmpty()) {
            Assume.assumeTrue("o aparelho já tem uma partida em andamento", false)
        }
        newGame.onFirst().performScrollTo().performClick()
        compose.waitForIdle()

        compose.onAllNodes(hasText("Ranking") and hasClickAction()).assertCountEquals(0)
        // Com jogadores cadastrados, a tela mostra o título. Sem jogadores, mostra o aviso para cadastrá-los.
        // O teste não cadastra jogadores, para não mexer nos dados do aparelho.
        compose.onAllNodes(
            (hasText("Nova partida") and !hasClickAction()) or
                hasText("Cadastre pelo menos 2 jogadores", substring = true)
        ).onFirst().assertIsDisplayed()
    }
}
