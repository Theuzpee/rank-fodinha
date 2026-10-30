package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rankfodinha.app.ui.components.LoadErrorScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoadErrorUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val message = "Não foi possível abrir os dados salvos no aparelho."

    @Test
    fun showsTheMessageAndTheRetryButton() {
        compose.setContent {
            RankFodinhaTheme {
                LoadErrorScreen(message = message, onRetry = {})
            }
        }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Tentar de novo").assertIsDisplayed()
    }

    @Test
    fun tappingRetryCallsOnRetryOnce() {
        var retries = 0
        compose.setContent {
            RankFodinhaTheme {
                LoadErrorScreen(message = message, onRetry = { retries++ })
            }
        }
        compose.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, retries)
    }
}
