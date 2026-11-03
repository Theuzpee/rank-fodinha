package com.rankfodinha.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rankfodinha.app.data.Game
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.isTieWithUnknownPlayers
import com.rankfodinha.app.viewmodel.FodinhaViewModel

/** Lado de cada botão − e +. 48 dp é o menor tamanho recomendado para tocar com o dedo. */
private val LIFE_BUTTON_SIZE = 48.dp

/**
 * Partida: um cartão por jogador, com as vidas e os botões − e +.
 * Em andamento, toque em − quando alguém perder uma vida, e em + para corrigir.
 * Quando a partida termina, mostra o vencedor e não tem mais botões.
 */
@Composable
fun GameBoard(game: Game, names: Map<String, String>, viewModel: FodinhaViewModel) {
    val data by viewModel.data.collectAsState()
    val players = remember(data.players) { data.players.associateBy { it.id } }
    val nameOf: (String) -> String = { id -> names[id] ?: "Jogador removido" }
    val editable = game.finishedAt == null
    // Jogador cuja foto está aberta em tela cheia. Nulo quando nenhuma foto está aberta.
    var enlargedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game.finishedAt != null) {
            GameHeaderCard(game, nameOf)
        }
        game.playerIds.forEach { id ->
            val lives = game.lives[id] ?: 0
            PlayerLivesCard(
                // Quem não está mais na lista de jogadores aparece só com o nome da partida, sem foto própria.
                player = players[id] ?: Player(id = id, name = nameOf(id)),
                lives = lives,
                editable = editable,
                onChange = { newValue -> viewModel.adjustLives(id, newValue - lives) },
                onAvatarClick = { enlargedId = id },
            )
        }
    }

    enlargedId?.let { id ->
        PhotoViewer(
            player = players[id] ?: Player(id = id, name = nameOf(id)),
            onClose = { enlargedId = null },
        )
    }
}

@Composable
private fun GameHeaderCard(game: Game, nameOf: (String) -> String) {
    AppCard {
        SectionTitle("Partida encerrada")
        Text(
            text = when {
                isTieWithUnknownPlayers(game) -> "Empate. Quem empatou não foi registrado."
                game.winnerIds.isEmpty() -> "Vencedor não registrado"
                game.winnerIds.size > 1 -> "Empate: ${formatNames(game.winnerIds.map(nameOf))}"
                else -> "Vencedor: ${formatNames(game.winnerIds.map(nameOf))}"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Cartão de um jogador, em uma linha: avatar, nome, vidas e, em andamento, os botões − e +.
 * Cartões baixos deixam os 8 jogadores visíveis sem rolar a tela.
 */
@Composable
private fun PlayerLivesCard(
    player: Player,
    lives: Int,
    editable: Boolean,
    onChange: (Int) -> Unit,
    onAvatarClick: () -> Unit,
) {
    AppCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // O avatar abre a foto ampliada. A área de toque tem 48 dp, mas o desenho continua com 40 dp.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Ver foto de ${player.name}",
                        onClick = onAvatarClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AvatarBadge(player = player, size = 40.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lives == 0) {
                    Text(
                        text = "Eliminado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                    )
                }
            }
            if (editable) {
                LifeButtons(name = player.name, lives = lives, onChange = onChange)
            } else {
                LivesCount(lives = lives)
            }
        }
    }
}

/** Botões − e + de um jogador, com a quantidade de vidas no meio. */
@Composable
private fun LifeButtons(name: String, lives: Int, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilledTonalButton(
            onClick = { onChange(lives - 1) },
            enabled = lives > 0,
            modifier = Modifier
                .size(LIFE_BUTTON_SIZE)
                .semantics { contentDescription = "Diminuir vidas de $name" },
            shape = shape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        LivesCount(lives = lives)
        Button(
            onClick = { onChange(lives + 1) },
            modifier = Modifier
                .size(LIFE_BUTTON_SIZE)
                .semantics { contentDescription = "Aumentar vidas de $name" },
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Quantidade de vidas, com a palavra "vida" ou "vidas" embaixo. */
@Composable
private fun LivesCount(lives: Int) {
    Column(
        modifier = Modifier.widthIn(min = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = lives.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = if (lives == 1) "vida" else "vidas",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Foto do jogador aberta em tela cheia, no formato da tela de foto aberta: quadrada, na largura da tela,
 * no meio, com um X branco no canto superior direito para fechar. O botão Voltar do aparelho também fecha.
 */
@Composable
private fun PhotoViewer(player: Player, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
                    // A foto ocupa a largura da tela. Em tela deitada, cabe na altura e não passa da tela.
                    val side = minOf(maxWidth, maxHeight)
                    PlayerPhoto(player = player, modifier = Modifier.size(side))
                }
                FilledIconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar foto")
                }
            }
        }
    }
}
