package com.rankfodinha.app.data

data class RankingRow(
    val playerId: String,
    val name: String,
    /**
     * Posição entre quem já jogou. Empates no número de vitórias e no aproveitamento dividem a posição.
     * Nulo para quem ainda não jogou nenhuma partida encerrada.
     */
    val position: Int?,
    /** Partidas encerradas em que o jogador esteve na mesa. */
    val played: Int,
    /** Partidas que o jogador venceu sozinho. Empate não conta como vitória. */
    val wins: Int,
    /** Partidas encerradas em que o jogador empatou com pelo menos outro jogador. */
    val ties: Int,
    /** Partidas encerradas em que o jogador não venceu nem empatou. */
    val losses: Int,
    /** Porcentagem de partidas vencidas, com uma casa decimal. Nulo quando o jogador ainda não jogou. */
    val winRate: Double?,
)

/** Contagens de cada jogador, sem posição. Só entram partidas encerradas. */
private fun tallies(players: List<Player>, games: List<Game>): List<RankingRow> {
    val played = mutableMapOf<String, Int>()
    val wins = mutableMapOf<String, Int>()
    val ties = mutableMapOf<String, Int>()
    for (game in games) {
        if (game.finishedAt == null) continue
        val tie = game.winnerIds.size > 1
        for (id in game.playerIds) {
            played[id] = (played[id] ?: 0) + 1
            if (id in game.winnerIds) {
                val counts = if (tie) ties else wins
                counts[id] = (counts[id] ?: 0) + 1
            }
        }
    }
    return players.map { player ->
        val gamesPlayed = played[player.id] ?: 0
        val gamesWon = wins[player.id] ?: 0
        val gamesTied = ties[player.id] ?: 0
        RankingRow(
            playerId = player.id,
            name = player.name,
            position = null,
            played = gamesPlayed,
            wins = gamesWon,
            ties = gamesTied,
            losses = gamesPlayed - gamesWon - gamesTied,
            winRate = if (gamesPlayed == 0) null else Math.round(gamesWon * 1000.0 / gamesPlayed) / 10.0,
        )
    }
}

/**
 * Ranking com base nas partidas encerradas. Partidas em andamento não entram.
 * Com um único vencedor, ele ganha a partida e os outros perdem. Com mais de um vencedor, é empate:
 * os empatados ganham um empate, e os demais perdem.
 */
fun buildRanking(players: List<Player>, games: List<Game>): List<RankingRow> {
    // Quem jogou vem primeiro. Entre eles: mais vitórias, depois maior aproveitamento, depois mais jogos e nome.
    val rows = tallies(players, games)
        .sortedWith(
            compareByDescending<RankingRow> { it.played > 0 }
                .thenByDescending { it.wins }
                .thenByDescending { it.winRate ?: -1.0 }
                .thenByDescending { it.played }
                .thenBy { it.name.lowercase() }
        )

    val result = ArrayList<RankingRow>(rows.size)
    var position = 0
    var previous: RankingRow? = null
    for ((index, row) in rows.withIndex()) {
        if (row.played == 0) {
            result += row
            continue
        }
        val prev = previous
        val tied = prev != null && prev.wins == row.wins && prev.winRate == row.winRate
        if (!tied) position = index + 1
        val ranked = row.copy(position = position)
        result += ranked
        previous = ranked
    }
    return result
}

/**
 * Rank de derrotas: quantas partidas encerradas cada jogador perdeu, ou seja, não venceu nem empatou.
 * Quem mais perdeu fica em primeiro. Empate não é derrota.
 */
fun buildDefeatRanking(players: List<Player>, games: List<Game>): List<RankingRow> {
    // Quem jogou vem primeiro. Entre eles: mais derrotas, depois mais jogos e nome.
    val rows = tallies(players, games)
        .sortedWith(
            compareByDescending<RankingRow> { it.played > 0 }
                .thenByDescending { it.losses }
                .thenByDescending { it.played }
                .thenBy { it.name.lowercase() }
        )

    val result = ArrayList<RankingRow>(rows.size)
    var position = 0
    var previous: RankingRow? = null
    for ((index, row) in rows.withIndex()) {
        if (row.played == 0) {
            result += row
            continue
        }
        val prev = previous
        val tied = prev != null && prev.losses == row.losses
        if (!tied) position = index + 1
        val ranked = row.copy(position = position)
        result += ranked
        previous = ranked
    }
    return result
}

/** Linha do rank de quem mais morreu primeiro. */
data class FirstDeathRow(
    val playerId: String,
    val name: String,
    /**
     * Posição entre quem já jogou. Empates no número de vezes dividem a posição.
     * Nulo para quem ainda não jogou nenhuma partida encerrada.
     */
    val position: Int?,
    /** Vezes em que o jogador foi o primeiro a perder todas as vidas, em partidas encerradas. */
    val firstDeaths: Int,
    /** Partidas encerradas em que o jogador esteve na mesa. */
    val played: Int,
)

/**
 * Rank de quem mais morreu primeiro: quantas vezes cada jogador foi o primeiro a perder todas as vidas.
 * Só entram partidas encerradas. Partidas feitas antes de a ordem das mortes ser guardada não contam.
 */
fun buildFirstDeathRanking(players: List<Player>, games: List<Game>): List<FirstDeathRow> {
    val played = mutableMapOf<String, Int>()
    val firstDeaths = mutableMapOf<String, Int>()
    for (game in games) {
        if (game.finishedAt == null) continue
        for (id in game.playerIds) {
            played[id] = (played[id] ?: 0) + 1
        }
        game.eliminated.firstOrNull()?.let { first ->
            firstDeaths[first] = (firstDeaths[first] ?: 0) + 1
        }
    }

    // Quem jogou vem primeiro. Entre eles: mais vezes morreu primeiro, depois mais jogos e nome.
    val rows = players
        .map { player ->
            FirstDeathRow(
                playerId = player.id,
                name = player.name,
                position = null,
                firstDeaths = firstDeaths[player.id] ?: 0,
                played = played[player.id] ?: 0,
            )
        }
        .sortedWith(
            compareByDescending<FirstDeathRow> { it.played > 0 }
                .thenByDescending { it.firstDeaths }
                .thenByDescending { it.played }
                .thenBy { it.name.lowercase() }
        )

    val result = ArrayList<FirstDeathRow>(rows.size)
    var position = 0
    var previous: FirstDeathRow? = null
    for ((index, row) in rows.withIndex()) {
        if (row.played == 0) {
            result += row
            continue
        }
        val prev = previous
        val tied = prev != null && prev.firstDeaths == row.firstDeaths
        if (!tied) position = index + 1
        val ranked = row.copy(position = position)
        result += ranked
        previous = ranked
    }
    return result
}
