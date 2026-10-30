package com.rankfodinha.app.data

import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * Formato salvo pelas versões anteriores, com palpites e vazas.
 * Serve só para converter os dados que já estão no aparelho.
 */
@Serializable
internal data class LegacyFodinhaData(
    val players: List<Player> = emptyList(),
    val games: List<LegacyGame> = emptyList(),
)

@Serializable
internal data class LegacyGame(
    val id: String,
    val startedAt: String,
    val finishedAt: String? = null,
    val playerIds: List<String>,
    val settings: LegacySettings = LegacySettings(),
    val rounds: List<LegacyRound> = emptyList(),
    /** Rodada em andamento. Nulo quando a partida terminou. */
    val current: LegacyRound? = null,
    val winnerIds: List<String> = emptyList(),
)

@Serializable
internal data class LegacySettings(
    val startingLives: Int = DEFAULT_STARTING_LIVES,
    /** "DIFERENCA" ou "ERRO", como foram gravados. */
    val penaltyMode: String = "DIFERENCA",
)

/** Só os campos de rodada que mexem nas vidas. Palpites e vazas entram no cálculo da penalidade. */
@Serializable
internal data class LegacyRound(
    val bids: Map<String, Int> = emptyMap(),
    val tricks: Map<String, Int> = emptyMap(),
    val adjustments: Map<String, Int> = emptyMap(),
)

/**
 * Converte os dados do formato antigo. Palpites e vazas são descartados.
 * O que se mantém é quantas vidas cada jogador tem na tela: vidas iniciais, penalidades
 * das rodadas encerradas e ajustes manuais, inclusive os da rodada em andamento.
 */
internal fun migrateLegacyData(legacy: LegacyFodinhaData, migratedAt: String): FodinhaData =
    FodinhaData(
        players = legacy.players,
        games = legacy.games.map { migrateLegacyGame(it, migratedAt) },
    )

/**
 * Partida que ainda estava em andamento e ficou com no máximo um jogador com vida
 * termina agora, em [migratedAt], com esse jogador como vencedor (ou sem vencedor, se ninguém sobrou).
 */
internal fun migrateLegacyGame(game: LegacyGame, migratedAt: String): Game {
    val lives = game.playerIds.associateWith { game.settings.startingLives }.toMutableMap()

    for (round in game.rounds) {
        for ((id, bid) in round.bids) {
            if (id !in lives) continue
            val lost = legacyLivesLost(bid, round.tricks[id] ?: 0, game.settings.penaltyMode)
            val adjust = round.adjustments[id] ?: 0
            lives[id] = maxOf(0, lives.getValue(id) + adjust - lost)
        }
    }

    val current = game.current
    if (current != null && game.finishedAt == null) {
        for ((id, adjust) in current.adjustments) {
            if (id in current.bids && id in lives) {
                lives[id] = maxOf(0, lives.getValue(id) + adjust)
            }
        }
    }

    val alive = game.playerIds.filter { lives.getValue(it) > 0 }
    val endsNow = game.finishedAt == null && alive.size <= 1
    return Game(
        id = game.id,
        startedAt = game.startedAt,
        finishedAt = if (endsNow) migratedAt else game.finishedAt,
        playerIds = game.playerIds,
        settings = GameSettings(startingLives = game.settings.startingLives),
        lives = lives.toMap(),
        winnerIds = if (endsNow) alive else game.winnerIds,
    )
}

private fun legacyLivesLost(bid: Int, tricks: Int, mode: String): Int =
    if (mode == "ERRO") {
        if (bid == tricks) 0 else 1
    } else {
        abs(bid - tricks)
    }
