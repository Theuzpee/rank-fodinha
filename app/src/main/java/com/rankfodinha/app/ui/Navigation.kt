package com.rankfodinha.app.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface RootRoute : NavKey {
    @Serializable
    data object Jogo : RootRoute

    @Serializable
    data object Ranking : RootRoute

    @Serializable
    data object Jogadores : RootRoute

    /** Tela de nova partida. Não é uma aba: fica empilhada sobre a aba Jogo. */
    @Serializable
    data object NovaPartida : RootRoute
}
