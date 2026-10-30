package com.rankfodinha.app.ui.components

import com.rankfodinha.app.data.AVATAR_COUNT

/** Avatar fixo para quem foi cadastrado antes de o app guardar o desenho: o mesmo id sempre mostra o mesmo desenho. */
fun avatarIndexFor(playerId: String): Int = (playerId.hashCode() and Int.MAX_VALUE) % AVATAR_COUNT
