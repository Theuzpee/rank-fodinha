package com.rankfodinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.Game
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.isIncomplete
import com.rankfodinha.app.data.isTieWithUnknownPlayers
import com.rankfodinha.app.data.needsFirstOut
import com.rankfodinha.app.data.needsWinner
import com.rankfodinha.app.data.tieOfferPlayers
import com.rankfodinha.app.ui.components.AppButton
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AppOutlinedButton
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.GameBoard
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.components.SectionTitle
import com.rankfodinha.app.ui.components.formatDateTime
import com.rankfodinha.app.ui.components.formatNames
import com.rankfodinha.app.ui.theme.AmberContainer
import com.rankfodinha.app.ui.theme.OnAmberContainer
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
    // Dois ou mais jogadores vivos, todos com uma vida: a oferta de empate aparece só nessa situação.
    val tieOffer = active?.let { tieOfferPlayers(it) }
    // Quando a oferta aparece, a tela rola até o botão de empate e o botão recebe o foco.
    val tieScroll = remember { BringIntoViewRequester() }
    val tieFocus = remember { FocusRequester() }
    val tieShown = tieOffer != null
    LaunchedEffect(tieShown) {
        if (tieShown) {
            tieScroll.bringIntoView()
            tieFocus.requestFocus()
        }
    }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var chooseWinner by rememberSaveable { mutableStateOf(false) }
    var confirmTie by rememberSaveable { mutableStateOf(false) }
    // Partida antiga aberta para completar. Guarda o id, para a escolha sobreviver à rotação da tela.
    var completingId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        if (active != null) {
            // Os dois botões ficam no topo, na mesma linha, para a lista de jogadores caber sem rolar.
            // O texto do "Finalizar partida" tem pouco espaço interno, para caber em uma linha só.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppButton(
                    text = "Finalizar partida",
                    onClick = { chooseWinner = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
                DiscardButton(onClick = { confirmDiscard = true })
            }
            // Todos os jogadores vivos com uma vida: o empate entre eles é oferecido aqui, antes de as mortes acontecerem.
            if (tieOffer != null) {
                TieOfferButton(
                    requester = tieScroll,
                    focus = tieFocus,
                    onClick = { confirmTie = true },
                )
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
            // Partidas antigas sem vencedor, ou sem quem morreu primeiro, ficam aqui até serem completadas.
            val incomplete = finished.filter { isIncomplete(it) }
            if (incomplete.isNotEmpty()) {
                IncompleteGamesCard(games = incomplete, onComplete = { completingId = it.id })
            }
            if (finished.size > 1) {
                PastGamesCard(games = finished.drop(1), names = names)
            }
        }
    }

    if (chooseWinner && active != null) {
        WinnerDialog(
            players = active.playerIds.mapNotNull { id -> data.players.firstOrNull { it.id == id } },
            onConfirm = { winnerIds ->
                chooseWinner = false
                actionError = viewModel.finishGame(winnerIds)
            },
            onDismiss = { chooseWinner = false },
        )
    }

    if (confirmTie && tieOffer != null) {
        AlertDialog(
            onDismissRequest = { confirmTie = false },
            title = { Text("Encerrar em empate?") },
            text = {
                Text(
                    "A partida termina empatada entre " +
                        "${formatNames(tieOffer.map { names[it] ?: "Jogador removido" })}. Não dá para desfazer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmTie = false
                    actionError = viewModel.finishGame(tieOffer)
                }) {
                    Text("Empatar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTie = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    completingId?.let { id ->
        val game = finished.firstOrNull { it.id == id }
        if (game != null && isIncomplete(game)) {
            CompleteGameDialog(
                game = game,
                players = data.players,
                onConfirm = { winnerIds, firstOutId, diedTogether ->
                    completingId = null
                    actionError = viewModel.completeGame(game.id, winnerIds, firstOutId, diedTogether)
                },
                onDismiss = { completingId = null },
            )
        }
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Atenção") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { actionError = null }) {
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
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(text = "Descartar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/**
 * Botão de empate, em âmbar: aparece quando todos os jogadores vivos têm uma vida e leva a uma confirmação antes de encerrar.
 * Quando aparece, a tela rola até ele e ele recebe o foco (por isso tem [requester] e [focus]).
 */
@Composable
private fun TieOfferButton(
    requester: BringIntoViewRequester,
    focus: FocusRequester,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .bringIntoViewRequester(requester)
            .focusRequester(focus),
        colors = ButtonDefaults.buttonColors(
            containerColor = AmberContainer,
            contentColor = OnAmberContainer,
        ),
    ) {
        Text(
            text = "Finalizar partida em empate",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Pergunta quem ganhou antes de encerrar a partida. Um jogador marcado é vitória.
 * Mais de um marcado é empate. Só confirma depois que pelo menos um jogador é marcado.
 */
@Composable
private fun WinnerDialog(players: List<Player>, onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var selectedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val tie = selectedIds.size > 1
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
                Text(
                    text = "Marque um jogador para vitória, ou mais de um para empate.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                players.forEach { player ->
                    WinnerOption(
                        player = player,
                        selected = player.id in selectedIds,
                        onToggle = {
                            selectedIds = if (player.id in selectedIds) {
                                selectedIds - player.id
                            } else {
                                selectedIds + player.id
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text(if (tie) "Finalizar com empate" else "Finalizar")
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
private fun WinnerOption(player: Player, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
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
            val result = when {
                isTieWithUnknownPlayers(game) -> "Empate. Quem empatou não foi registrado."
                game.winnerIds.isEmpty() -> "Vencedor não registrado"
                game.winnerIds.size > 1 -> "Empate: $winners"
                else -> "Vencedor: $winners"
            }
            Text(
                text = formatDateTime(game.finishedAt ?: game.startedAt),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Partidas antigas que ainda precisam de vencedor, ou de quem morreu primeiro. Cada uma tem o botão Completar. */
@Composable
private fun IncompleteGamesCard(games: List<Game>, onComplete: (Game) -> Unit) {
    AppCard {
        SectionTitle("Partidas para completar")
        Text(
            text = "Sem vencedor, a partida fica fora do ranking de vitórias. " +
                "Sem o primeiro a morrer, ela não entra no rank de Derrotas.",
            style = MaterialTheme.typography.bodyMedium,
        )
        games.forEach { game ->
            Text(
                text = formatDateTime(game.finishedAt ?: game.startedAt),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Falta: ${missingPartsText(game)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            AppOutlinedButton(text = "Completar", onClick = { onComplete(game) })
        }
    }
}

/** O que falta na partida: vencedor, quem morreu primeiro, ou os dois. */
private fun missingPartsText(game: Game): String {
    val winnerPart = if (isTieWithUnknownPlayers(game)) "quem empatou" else "vencedor"
    return when {
        needsWinner(game) && needsFirstOut(game) -> "$winnerPart e quem morreu primeiro"
        needsWinner(game) -> winnerPart
        else -> "quem morreu primeiro"
    }
}

/**
 * Pergunta só o que falta na partida antiga. O que já estava registrado não aparece aqui e não muda.
 * Salvar fica desabilitado até preencher cada parte que falta.
 */
@Composable
private fun CompleteGameDialog(
    game: Game,
    players: List<Player>,
    onConfirm: (winnerIds: List<String>, firstOutId: String?, diedTogether: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val askWinner = needsWinner(game)
    val askFirstOut = needsFirstOut(game)
    val byId = remember(players) { players.associateBy { it.id } }
    val gamePlayers = game.playerIds.mapNotNull { byId[it] }
    val fallenPlayers = gamePlayers.filter { (game.lives[it.id] ?: 0) == 0 }
    var winnerIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var firstOutId by rememberSaveable { mutableStateOf<String?>(null) }
    var diedTogether by rememberSaveable { mutableStateOf(false) }
    val ready = (!askWinner || winnerIds.isNotEmpty()) && (!askFirstOut || firstOutId != null || diedTogether)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Completar partida") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Partida de ${formatDateTime(game.finishedAt ?: game.startedAt)}. Preencha só o que falta.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (askWinner) {
                    Text(
                        text = "Quem ganhou?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Marque um jogador para vitória, ou mais de um para empate.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    gamePlayers.forEach { player ->
                        WinnerOption(
                            player = player,
                            selected = player.id in winnerIds,
                            onToggle = {
                                winnerIds = if (player.id in winnerIds) {
                                    winnerIds - player.id
                                } else {
                                    winnerIds + player.id
                                }
                            },
                        )
                    }
                }
                if (askFirstOut) {
                    Text(
                        text = "Quem morreu primeiro?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Escolha um dos jogadores que ficaram sem vida nesta partida. " +
                            "Se nenhum morreu antes dos outros, marque Morreram juntos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    fallenPlayers.forEach { player ->
                        FirstOutOption(
                            player = player,
                            selected = firstOutId == player.id,
                            onSelect = {
                                firstOutId = player.id
                                diedTogether = false
                            },
                        )
                    }
                    TogetherOption(
                        selected = diedTogether,
                        onSelect = {
                            firstOutId = null
                            diedTogether = true
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(winnerIds, firstOutId, diedTogether) },
                enabled = ready,
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

/** Uma opção de quem morreu primeiro. Só uma pode ficar marcada. */
@Composable
private fun FirstOutOption(player: Player, selected: Boolean, onSelect: () -> Unit) {
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

/** Opção para quando nenhum jogador morreu antes dos outros. A partida não conta em Derrotas. */
@Composable
private fun TogetherOption(selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Morreram juntos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Nenhum morreu antes dos outros. Não conta em Derrotas.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
