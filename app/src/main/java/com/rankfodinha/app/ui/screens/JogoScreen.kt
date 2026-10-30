package com.rankfodinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.Game
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.ui.components.AppButton
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.GameBoard
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.components.SectionTitle
import com.rankfodinha.app.ui.components.formatDateTime
import com.rankfodinha.app.ui.components.formatNames
import com.rankfodinha.app.viewmodel.FodinhaViewModel

/** Aba Jogo: a partida em andamento, ou a última partida encerrada e o botão de nova partida. */
@Composable
fun JogoScreen(viewModel: FodinhaViewModel, onNewGame: () -> Unit) {
    val data by viewModel.data.collectAsState()
    val names = remember(data.players) { data.players.associate { it.id to it.name } }
    val active = data.games.firstOrNull { it.finishedAt == null }
    val finished = data.games
        .filter { it.finishedAt != null }
        .sortedByDescending { it.finishedAt ?: "" }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var chooseWinner by rememberSaveable { mutableStateOf(false) }
    var finishError by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        if (active != null) {
            // Os dois botões ficam no topo, na mesma linha, para a lista de jogadores caber sem rolar.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppButton(
                    text = "Finalizar partida",
                    onClick = { chooseWinner = true },
                    modifier = Modifier.weight(1f),
                )
                DiscardButton(onClick = { confirmDiscard = true })
            }
            GameBoard(game = active, names = names, viewModel = viewModel)
        } else {
            val latest = finished.firstOrNull()
            if (latest != null) {
                GameBoard(game = latest, names = names, viewModel = viewModel)
            } else {
                RulesCard()
            }
            AppButton(text = "Nova partida", onClick = onNewGame)
            if (finished.size > 1) {
                PastGamesCard(games = finished.drop(1), names = names)
            }
        }
    }

    if (chooseWinner && active != null) {
        WinnerDialog(
            players = active.playerIds.mapNotNull { id -> data.players.firstOrNull { it.id == id } },
            onConfirm = { winnerId ->
                chooseWinner = false
                finishError = viewModel.finishGame(winnerId)
            },
            onDismiss = { chooseWinner = false },
        )
    }

    finishError?.let { message ->
        AlertDialog(
            onDismissRequest = { finishError = null },
            title = { Text("Atenção") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { finishError = null }) {
                    Text("OK")
                }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar partida") },
            text = { Text("A partida em andamento será apagada e não entra no ranking.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelGame()
                    confirmDiscard = false
                }) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

/** Descarta a partida em andamento. Mesma altura do botão Finalizar partida, para ficarem alinhados. */
@Composable
private fun DiscardButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 56.dp),
        shape = ButtonDefaults.shape,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text = "Descartar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Pergunta quem ganhou antes de encerrar a partida. Só confirma depois que alguém é escolhido. */
@Composable
private fun WinnerDialog(players: List<Player>, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var winnerId by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quem ganhou a partida?") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                players.forEach { player ->
                    WinnerOption(
                        player = player,
                        selected = winnerId == player.id,
                        onSelect = { winnerId = player.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { winnerId?.let(onConfirm) },
                enabled = winnerId != null,
            ) {
                Text("Finalizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun WinnerOption(player: Player, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        AvatarBadge(player = player, size = 40.dp)
        Text(
            text = player.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RulesCard() {
    AppCard {
        SectionTitle("Como funciona")
        RuleLine("• Cada jogador começa com a quantidade de vidas escolhida.")
        RuleLine("• Quando alguém perde uma vida, toque em − ao lado do nome dele.")
        RuleLine("• Se errar um toque, use + para corrigir.")
        RuleLine("• Quem zerar as vidas sai da partida. O último com vida vence.")
    }
}

@Composable
private fun RuleLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun PastGamesCard(games: List<Game>, names: Map<String, String>) {
    AppCard {
        SectionTitle("Partidas anteriores")
        games.forEach { game ->
            val winners = formatNames(game.winnerIds.map { names[it] ?: "Jogador removido" })
            Text(
                text = formatDateTime(game.finishedAt ?: game.startedAt),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Vencedor: $winners",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
