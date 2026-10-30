package com.rankfodinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.RankingRow
import com.rankfodinha.app.data.buildDefeatRanking
import com.rankfodinha.app.data.buildRanking
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AppOutlinedButton
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.components.plural
import com.rankfodinha.app.viewmodel.FodinhaViewModel

/** As duas seções do ranking, com a explicação que aparece embaixo do botão. */
private enum class RankingSection(val label: String, val explanation: String) {
    Vitoria("Vitória", "Quem mais ganhou partidas"),
    Derrotas("Derrotas", "Quem mais perdeu partidas. Empate não conta como derrota."),
}

/** Uma linha de qualquer seção, com o resumo já pronto. */
private data class RankingItem(
    val player: Player,
    val position: Int?,
    val stats: String,
    val highlight: Boolean,
)

/** Aba Ranking: vitórias ou derrotas, escolhidas no botão de duas seções. Só entram partidas encerradas. */
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
    val defeatItems = remember(data) {
        val players = data.players.associateBy { it.id }
        buildDefeatRanking(data.players, data.games).map { row ->
            RankingItem(
                player = players[row.playerId] ?: Player(id = row.playerId, name = row.name),
                position = row.position,
                stats = defeatStatsText(row),
                highlight = false,
            )
        }
    }
    val hasFinishedGames = data.games.any { it.finishedAt != null }
    // Guardado pelo nome da seção, para a escolha sobreviver à rotação da tela.
    var sectionName by rememberSaveable { mutableStateOf(RankingSection.Vitoria.name) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    // Um nome guardado de uma versão anterior do app cai na seção padrão, em vez de quebrar a tela.
    val section = RankingSection.entries.firstOrNull { it.name == sectionName } ?: RankingSection.Vitoria
    val rankingItems = if (section == RankingSection.Vitoria) winItems else defeatItems
    // A lista começa sempre do primeiro colocado: ao abrir a aba e ao trocar de seção.
    val listState = rememberLazyListState()
    LaunchedEffect(section) { listState.scrollToItem(0) }

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
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

/** Um jogador: posição, avatar e nome, com o resumo embaixo. Cartão compacto, para caber mais linhas na tela. */
@Composable
private fun RankingCard(item: RankingItem) {
    AppCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = item.position?.toString() ?: "–",
                modifier = Modifier.widthIn(min = 28.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (item.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AvatarBadge(player = item.player, size = 40.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.player.name,
                    style = MaterialTheme.typography.titleMedium,
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

/** Vitórias, empates e derrotas do jogador. A porcentagem não aparece. */
private fun winStatsText(row: RankingRow): String {
    if (row.played == 0) return "Ainda sem partidas encerradas"
    val wins = plural(row.wins, "vitória", "vitórias")
    val ties = plural(row.ties, "empate", "empates")
    val losses = plural(row.losses, "derrota", "derrotas")
    return "$wins · $ties · $losses"
}

/** Só a quantidade de derrotas do jogador. Empate não entra aqui. */
private fun defeatStatsText(row: RankingRow): String {
    if (row.played == 0) return "Ainda sem partidas encerradas"
    return plural(row.losses, "derrota", "derrotas")
}
