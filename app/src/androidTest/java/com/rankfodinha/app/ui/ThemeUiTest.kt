package com.rankfodinha.app.ui

import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** O app é sempre claro, mesmo quando o aparelho está no modo escuro. */
class ThemeUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun colorSchemeWithSystemDarkMode(dark: Boolean): ColorScheme {
        var captured: ColorScheme? = null
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                RankFodinhaTheme {
                    val scheme = MaterialTheme.colorScheme
                    SideEffect { captured = scheme }
                }
            }
        }
        compose.waitForIdle()
        return checkNotNull(captured)
    }

    @Test
    fun usesTheGreenBrandColorsOnAPaperBackground() {
        val scheme = colorSchemeWithSystemDarkMode(dark = false)
        assertEquals(Color(0xFF2E7D32), scheme.primary)
        assertEquals(Color(0xFFFFF8F5), scheme.background)
        assertEquals(Color(0xFF2B1B1A), scheme.onBackground)
    }

    @Test
    fun staysLightWhenTheDeviceIsInDarkMode() {
        val scheme = colorSchemeWithSystemDarkMode(dark = true)
        assertEquals(Color(0xFFFFF8F5), scheme.background)
        assertEquals(Color(0xFF2B1B1A), scheme.onBackground)
    }
}
