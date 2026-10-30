package com.rankfodinha.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rankfodinha.app.ui.components.Stepper
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import org.junit.Rule
import org.junit.Test

class StepperUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheCurrentValue() {
        compose.setContent {
            RankFodinhaTheme {
                Stepper(label = "x", value = 3, min = 0, max = 9, onChange = {})
            }
        }
        compose.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun plusAndMinusMoveTheValueByOne() {
        compose.setContent {
            RankFodinhaTheme {
                var value by remember { mutableStateOf(3) }
                Stepper(label = "x", value = value, min = 0, max = 9, onChange = { value = it })
            }
        }
        compose.onNodeWithContentDescription("Aumentar x").performClick()
        compose.onNodeWithText("4").assertIsDisplayed()
        compose.onNodeWithContentDescription("Diminuir x").performClick()
        compose.onNodeWithContentDescription("Diminuir x").performClick()
        compose.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun minusIsDisabledAtTheMinimum() {
        compose.setContent {
            RankFodinhaTheme {
                Stepper(label = "x", value = 0, min = 0, max = 9, onChange = {})
            }
        }
        compose.onNodeWithContentDescription("Diminuir x").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Aumentar x").assertIsEnabled()
    }

    @Test
    fun plusIsDisabledAtTheMaximum() {
        compose.setContent {
            RankFodinhaTheme {
                Stepper(label = "x", value = 9, min = 0, max = 9, onChange = {})
            }
        }
        compose.onNodeWithContentDescription("Aumentar x").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Diminuir x").assertIsEnabled()
    }

    @Test
    fun aStepperWithNoUpperLimitKeepsGoing() {
        compose.setContent {
            RankFodinhaTheme {
                var value by remember { mutableStateOf(40) }
                Stepper(label = "vidas", value = value, min = 0, max = Int.MAX_VALUE, onChange = { value = it })
            }
        }
        compose.onNodeWithContentDescription("Aumentar vidas").performClick()
        compose.onNodeWithText("41").assertIsDisplayed()
    }
}
