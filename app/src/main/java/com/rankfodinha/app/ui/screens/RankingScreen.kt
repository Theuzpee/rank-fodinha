package com.rankfodinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.FirstDeathRow
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.RankingRow
import com.rankfodinha.app.data.buildFirstDeathRanking
import com.rankfodinha.app.data.buildRanking
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AppOutlinedButton
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.components.formatPercent
import com.rankfodinha.app.ui.components.plural
import com.rankfodinha.app.viewmodel.FodinhaViewModel

/** As duas seções do ranking, com a explicação que aparece embaixo do botão. */
private enum class RankingSection(val label: String, val explanation: String) {
    Vitoria("Vitória", "Quem mais ganhou partidas"),
    Morte("Morte", "Quem mais vezes morreu primeiro em uma partida"),
}

/** Uma linha de qualquer seção, com o resumo já pronto. */
private data class RankingItem(
    val player: Player,
    val position: Int?,
    val stats: String,
    val highlight: Boolean,
)

/** Aba Ranking: vitórias ou mortes, escolhidas no botão de duas seções. Só entram partidas encerradas. */
@Composable
fun RankingScreen(viewModel: FodinhaViewModel) {
    val data by viewModel.data.collectAsState()
    val winItems = remember(data) {
        val players = data.players.associateBy { it.id }
        buildRanking(data.players, data.games).map { row ->
            RankingItem(
                player = players[row.playerId] ?: Player(id = row.playerId, name = row.name),
                position = row.position,
                stats = winStatsText(row),
                highlight = row.position == 1 && row.played > 0,
            )
        }
    }
    val deathItems = remember(data) {
        val players = data.players.associateBy { it.id }
        buildFirstDeathRanking(data.players, data.games).map { row ->
            RankingItem(
                player = players[row.playerId] ?: Player(id = row.playerId, name = row.name),
                position = row.position,
                stats = deathStatsText(row),
                highlight = false,
            )
        }
    }
    val hasFinishedGames = data.games.any { it.finishedAt != null }
    // Guardado pelo nome da seção, para a escolha sobreviver à rotação da tela.
    var sectionName by rememberSaveable { mutableStateOf(RankingSection.Vitoria.name) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val section = RankingSection.valueOf(sectionName)
    val rankingItems = if (section == RankingSection.Vitoria) winItems else deathItems

    ScreenColumn(scrollable = false) {
        Text(
            text = "Ranking",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RankingSection.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == section,
                    onClick = { sectionName = option.name },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RankingSection.entries.size),
                    label = { Text(option.label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        Text(
            text = section.explanation,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (rankingItems.isEmpty()) {
            Text(
                text = "Cadastre jogadores e registre partidas para montar o ranking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rankingItems, key = { it.player.id }) { item ->
                    RankingCard(item)
                }
            }
        }
        AppOutlinedButton(
            text = "Limpar ranking",
            onClick = { confirmClear = true },
            enabled = hasFinishedGames,
            contentColor = MaterialTheme.colorScheme.error,
            shape = ButtonDefaults.shape,
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar o ranking?") },
            text = {
                Text(
                    "Todas as partidas encerradas serão apagadas. O ranking e a lista de partidas anteriores " +
                        "ficam vazios. Os jogadores continuam cadastrados, e a partida em andamento não é afetada. " +
                        "Não dá para desfazer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearRanking()
                    confirmClear = false
                }) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

/** Um jogador: posição, avatar e nome, com o resumo embaixo. O resumo quebra linha em telas estreitas. */
@Composable
private fun RankingCard(item: RankingItem) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = item.position?.toString() ?: "–",
                modifier = Modifier.widthIn(min = 36.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (item.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AvatarBadge(player = item.player, size = 52.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.player.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.stats,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun winStatsText(row: RankingRow): String {
    val rate = row.winRate ?: return "Ainda sem partidas encerradas"
    val wins = plural(row.wins, "vitória", "vitórias")
    val games = plural(row.played, "jogo", "jogos")
    return "$wins · $games · ${formatPercent(rate)} de aproveitamento"
}

private fun deathStatsText(row: FirstDeathRow): String {
    if (row.played == 0) return "Ainda sem partidas encerradas"
    val rate = Math.round(row.firstDeaths * 1000.0 / row.played) / 10.0
    val times = plural(row.firstDeaths, "vez", "vezes")
    val games = plural(row.played, "jogo", "jogos")
    return "$times morreu primeiro · $games · ${formatPercent(rate)} das partidas"
}
