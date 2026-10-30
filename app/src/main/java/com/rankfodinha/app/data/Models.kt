package com.rankfodinha.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    /** Foto da equipe sorteada no cadastro, de 0 a AVATAR_COUNT - 1. Nulo só em cadastros antigos: eles usam a foto fixa pelo id. */
    val avatar: Int? = null,
    /**
     * Quando a foto do avatar foi tirada (texto ISO). Todo cadastro novo tem essa data. Nulo só em cadastros antigos,
     * que usam a foto da equipe.
     * A foto fica só no aparelho. Cada foto nova tem um horário novo, e por isso a tela carrega a imagem de novo.
     */
    val photoSavedAt: String? = null,
)

@Serializable
data class GameSettings(
    val startingLives: Int = DEFAULT_STARTING_LIVES,
)

@Serializable
data class Game(
    val id: String,
    val startedAt: String,
    /** Nulo enquanto a partida está em andamento. */
    val finishedAt: String? = null,
    /** Ordem da mesa. É a mesma ordem em que os jogadores aparecem na partida. */
    val playerIds: List<String>,
    val settings: GameSettings,
    /** Vidas atuais de cada jogador da partida. Quem chega a 0 está eliminado. */
    val lives: Map<String, Int>,
    /** Vazio enquanto a partida não termina. */
    val winnerIds: List<String> = emptyList(),
    /**
     * Quem perdeu a última vida, na ordem em que foi eliminado. O primeiro da lista morreu primeiro.
     * Quem volta à vida por correção sai da lista. Partidas antigas não guardam essa ordem e ficam vazias.
     */
    val eliminated: List<String> = emptyList(),
)

@Serializable
data class FodinhaData(
    val players: List<Player> = emptyList(),
    val games: List<Game> = emptyList(),
)
