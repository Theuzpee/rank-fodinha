package com.rankfodinha.app.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.AvatarPhotos
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.ErrorText
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.viewmodel.FodinhaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Aba Jogadores: cadastro dos colegas. Todo jogador novo ganha uma foto, tirada na hora do cadastro.
 * Tocar no avatar de alguém permite trocar a foto dele.
 */
@Composable
fun JogadoresScreen(viewModel: FodinhaViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data by viewModel.data.collectAsState()
    val players = remember(data.players) { data.players.sortedBy { it.name.lowercase() } }
    var name by rememberSaveable { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var pendingRemoval by remember { mutableStateOf<Player?>(null) }
    var avatarTarget by remember { mutableStateOf<Player?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    // Foto em andamento: o id do jogador, que nomeia o arquivo, e se a foto é de um cadastro novo.
    // Guardados com rememberSaveable, para a foto voltar ao lugar certo se o celular girar com a câmera aberta.
    var photoPlayerId by rememberSaveable { mutableStateOf<String?>(null) }
    var photoForNewPlayer by rememberSaveable { mutableStateOf(false) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val playerId = photoPlayerId ?: return@rememberLauncherForActivityResult
        val forNewPlayer = photoForNewPlayer
        photoPlayerId = null
        photoForNewPlayer = false
        scope.launch {
            if (!taken) {
                withContext(Dispatchers.IO) { AvatarPhotos.discardCapture(context, playerId) }
                return@launch
            }
            val saved = withContext(Dispatchers.IO) { AvatarPhotos.saveCapture(context, playerId) }
            if (!saved) {
                dialogMessage = "Não foi possível usar a foto. Tente tirar outra."
                return@launch
            }
            if (forNewPlayer) {
                // O jogador só entra na lista com a foto salva. Se o cadastro falhar, a foto é apagada.
                val message = viewModel.addPlayer(name, playerId)
                if (message == null) {
                    name = ""
                    formError = null
                } else {
                    formError = message
                    withContext(Dispatchers.IO) { AvatarPhotos.delete(context, playerId) }
                }
            } else {
                val message = viewModel.setPlayerPhoto(playerId)
                // Se o jogador foi removido enquanto a câmera estava aberta, a foto recém-salva não tem dono.
                if (message != null) {
                    withContext(Dispatchers.IO) { AvatarPhotos.delete(context, playerId) }
                }
                dialogMessage = message
            }
        }
    }

    /** Abre a câmera para a foto de um jogador. Sem app de câmera, avisa e não muda nada. */
    fun openCamera(playerId: String, forNewPlayer: Boolean) {
        val uri = AvatarPhotos.prepareCapture(context, playerId)
        photoPlayerId = playerId
        photoForNewPlayer = forNewPlayer
        try {
            takePhoto.launch(uri)
        } catch (error: ActivityNotFoundException) {
            photoPlayerId = null
            photoForNewPlayer = false
            dialogMessage = "Nenhum aplicativo de câmera foi encontrado neste aparelho."
        }
    }

    /** Confere o nome e abre a câmera. O jogador só é cadastrado depois que a foto é tirada. */
    fun submit() {
        val message = viewModel.checkNewPlayerName(name)
        if (message != null) {
            formError = message
            return
        }
        formError = null
        openCamera(playerId = viewModel.newPlayerId(), forNewPlayer = true)
    }

    ScreenColumn(modifier = Modifier.imePadding(), scrollable = false) {
        Text(
            text = "Jogadores",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    formError = null
                },
                label = { Text("Nome", maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { submit() },
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text("Adicionar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "Ao tocar em Adicionar, a câmera abre para tirar a foto do jogador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        formError?.let { ErrorText(it) }

        if (players.isEmpty()) {
            Text(
                text = "Nenhum jogador cadastrado ainda.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(players, key = { it.id }) { player ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            AvatarBadge(
                                player = player,
                                size = 48.dp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(
                                        onClickLabel = "Trocar avatar",
                                        role = Role.Button,
                                        onClick = { avatarTarget = player },
                                    ),
                            )
                            Text(
                                text = player.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(
                                onClick = {
                                    if (viewModel.playerHasGames(player.id)) {
                                        dialogMessage = "${player.name} já participou de partidas e não pode ser removido."
                                    } else {
                                        pendingRemoval = player
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = "Remover",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    avatarTarget?.let { player ->
        AlertDialog(
            onDismissRequest = { avatarTarget = null },
            title = { Text("Foto de ${player.name}") },
            text = { Text("Tire uma foto para trocar o avatar. A foto fica salva só neste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    avatarTarget = null
                    openCamera(playerId = player.id, forNewPlayer = false)
                }) {
                    Text("Tirar foto")
                }
            },
            dismissButton = {
                TextButton(onClick = { avatarTarget = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    pendingRemoval?.let { player ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remover jogador") },
            text = { Text("Remover ${player.name} da lista?") },
            confirmButton = {
                TextButton(onClick = {
                    val message = viewModel.removePlayer(player.id)
                    if (message == null) {
                        scope.launch(Dispatchers.IO) { AvatarPhotos.delete(context, player.id) }
                    }
                    dialogMessage = message
                    pendingRemoval = null
                }) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Atenção") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { dialogMessage = null }) {
                    Text("OK")
                }
            },
        )
    }
}
