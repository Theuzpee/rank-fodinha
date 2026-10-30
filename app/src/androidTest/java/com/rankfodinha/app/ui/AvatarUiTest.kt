package com.rankfodinha.app.ui

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.theme.RankFodinhaTheme
import org.junit.Rule
import org.junit.Test

class AvatarUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eachPlayerHasAnAvatarWithTheirName() {
        compose.setContent {
            RankFodinhaTheme {
                AvatarBadge(player = Player("ana", "Ana"), size = 56.dp)
            }
        }
        compose.onNodeWithContentDescription("Avatar de Ana").assertIsDisplayed()
    }

    @Test
    fun avatarIsSquareWithTheRequestedSize() {
        compose.setContent {
            RankFodinhaTheme {
                AvatarBadge(player = Player("ana", "Ana"), size = 56.dp)
            }
        }
        compose.onNodeWithContentDescription("Avatar de Ana")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }
}
