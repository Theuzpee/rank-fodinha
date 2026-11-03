package com.rankfodinha.app.data

/** Vidas que cada jogador recebe quando a partida começa, se nada for escolhido. */
const val DEFAULT_STARTING_LIVES = 5
const val MIN_PLAYERS = 2
const val MIN_STARTING_LIVES = 1
const val MAX_STARTING_LIVES = 20

/** Quantidade de desenhos de avatar. O desenho de cada jogador é um número de 0 a AVATAR_COUNT - 1. */
const val AVATAR_COUNT = 7

fun isValidStartingLives(lives: Int): Boolean = lives in MIN_STARTING_LIVES..MAX_STARTING_LIVES

/** Partida nova: todos começam com as vidas iniciais, na ordem da mesa. */
fun createGame(id: String, startedAt: String, playerIds: List<String>, settings: GameSettings): Game =
    Game(
        id = id,
        startedAt = startedAt,
        playerIds = playerIds,
        settings = settings,
        lives = playerIds.associateWith { settings.startingLives },
    )

/** Jogadores que ainda têm vida, na ordem da mesa. */
fun alivePlayers(game: Game): List<String> =
    game.playerIds.filter { (game.lives[it] ?: 0) > 0 }

/**
 * Jogadores que podem empatar agora: dois ou mais com vida, e todos com uma vida só.
 * Nulo nos outros casos e em partidas já encerradas. A ordem é a da mesa.
 */
fun tieOfferPlayers(game: Game): List<String>? {
    if (game.finishedAt != null) return null
    val alive = alivePlayers(game)
    if (alive.size < 2) return null
    return if (alive.all { game.lives[it] == 1 }) alive else null
}

/**
 * Soma [delta] às vidas de um jogador: -1 quando ele perde uma vida, +1 para corrigir.
 * As vidas não ficam abaixo de zero. Quando sobra no máximo um jogador com vida,
 * a partida termina em [finishedAt] e esse jogador vence.
 * Mudança em partida encerrada, ou de quem não está na mesa, não faz nada.
 */
fun changeLives(game: Game, playerId: String, delta: Int, finishedAt: String): Game {
    if (game.finishedAt != null || playerId !in game.lives) return game
    val current = game.lives.getValue(playerId)
    val next = maxOf(0, current + delta)
    // Quem perde a última vida entra na lista de eliminados. Uma correção que devolve a vida tira o nome da lista.
    val eliminated = when {
        current > 0 && next == 0 -> game.eliminated + playerId
        current == 0 && next > 0 -> game.eliminated - playerId
        else -> game.eliminated
    }
    val updated = game.copy(lives = game.lives + (playerId to next), eliminated = eliminated)
    val alive = alivePlayers(updated)
    return if (alive.size <= 1) {
        updated.copy(finishedAt = finishedAt, winnerIds = alive)
    } else {
        updated
    }
}

/**
 * Partida encerrada sem vencedor registrado. Acontece em partidas antigas, em que todos ficaram sem vida ao mesmo tempo.
 */
fun needsWinner(game: Game): Boolean = game.finishedAt != null && game.winnerIds.isEmpty()

/**
 * Partida encerrada em que nem todos os jogadores sem vida estão na lista de eliminados.
 * Partidas antigas não guardam essa lista, então falta saber quem morreu primeiro.
 * Quando as primeiras mortes foram juntas, a pergunta já foi respondida e não volta.
 */
fun needsFirstOut(game: Game): Boolean =
    game.finishedAt != null && !game.firstDeathTogether && game.eliminated.size < game.lives.count { it.value == 0 }

/** Partida encerrada que ainda precisa de vencedor, ou de quem morreu primeiro. */
fun isIncomplete(game: Game): Boolean = needsWinner(game) || needsFirstOut(game)

/**
 * Partida encerrada sem vencedor e com todos sem vida. Acontece em partidas antigas, em que todos perderam a última
 * vida ao mesmo tempo. Pela regra do app, isso é empate, mas os jogadores que empataram não foram guardados.
 */
fun isTieWithUnknownPlayers(game: Game): Boolean =
    game.finishedAt != null && game.winnerIds.isEmpty() && game.lives.isNotEmpty() && game.lives.values.all { it == 0 }
