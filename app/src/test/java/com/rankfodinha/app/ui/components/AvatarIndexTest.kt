package com.rankfodinha.app.ui.components

import com.rankfodinha.app.data.AVATAR_COUNT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarIndexTest {

    @Test
    fun everyIndexIsInsideTheListOfAvatars() {
        for (i in 0..5_000) {
            val index = avatarIndexFor("id-$i")
            assertTrue("índice $index para id-$i", index in 0 until AVATAR_COUNT)
        }
    }

    @Test
    fun thesamePlayerAlwaysGetsTheSameAvatar() {
        assertEquals(avatarIndexFor("jogador-abc"), avatarIndexFor("jogador-abc"))
    }

    @Test
    fun differentPlayersUseAllTheAvatars() {
        val used = (1..200).map { avatarIndexFor("jogador-$it") }.toSet()
        assertEquals(AVATAR_COUNT, used.size)
    }

    @Test
    fun anEmptyIdStillGetsAValidAvatar() {
        assertTrue(avatarIndexFor("") in 0 until AVATAR_COUNT)
    }
}
