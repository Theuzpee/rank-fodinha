package com.rankfodinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rankfodinha.app.data.DEFAULT_STARTING_LIVES
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.data.MAX_STARTING_LIVES
import com.rankfodinha.app.data.MIN_PLAYERS
import com.rankfodinha.app.data.MIN_STARTING_LIVES
import com.rankfodinha.app.ui.components.AppButton
import com.rankfodinha.app.ui.components.AppCard
import com.rankfodinha.app.ui.components.AppOutlinedButton
import com.rankfodinha.app.ui.components.AvatarBadge
import com.rankfodinha.app.ui.components.ErrorText
import com.rankfodinha.app.ui.components.ScreenColumn
import com.rankfodinha.app.ui.components.SectionTitle
import com.rankfodinha.app.ui.components.Stepper
import com.rankfodinha.app.viewmodel.FodinhaViewModel

/** Nova partida. Todos os jogadores cadastrados entram; quem não vai jogar pode ser desmarcado. */
@Composable
fun NovaPartidaScreen(viewModel: FodinhaViewModel, onClose: () -> Unit) {
    val data by viewModel.data.collectAsState()
    val players = remember(data.players) { data.players.sortedBy { it.name.lowercase() } }
    // Guardado com rememberSaveable: a escolha não se perde se o celular girar.
    var excluded by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var lives by rememberSaveable { mutableIntStateOf(DEFAULT_STARTING_LIVES) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    if (players.size < MIN_PLAYERS) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Cadastre pelo menos $MIN_PLAYERS jogadores na aba Jogadores antes de começar uma partida.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = onClose) {
                Text("Voltar", style = MaterialTheme.typography.labelLarge)
            }
        }
        return
    }

    val selected = players.filter { it.id !in excluded }

    ScreenColumn {
        Text(
            text = "Nova partida",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )

        AppCard {
            SectionTitle("Jogadores")
            Text(
                text = "Todos os cadastrados já entram. Desmarque quem não vai jogar esta partida.",
                style = MaterialTheme.typography.bodyMedium,
            )
            players.forEach { player ->
                val checked = player.id !in excluded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .selectable(
                            selected = checked,
                            role = Role.Checkbox,
                            onClick = {
                                error = null
                                excluded = if (checked) excluded + player.id else excluded - player.id
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    AvatarBadge(player = player, size = 44.dp)
                    Text(
                        text = player.name,
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = "${selected.size} jogador(es) nesta partida. A mesa precisa de pelo menos $MIN_PLAYERS.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        AppCard {
            SectionTitle("Vidas")
            Text(
                text = "Quantas vidas cada jogador começa.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Stepper(
                label = "vidas iniciais",
                value = lives,
                min = MIN_STARTING_LIVES,
                max = MAX_STARTING_LIVES,
                onChange = { lives = it },
                unit = if (lives == 1) "vida" else "vidas",
            )
        }

        error?.let { ErrorText(it) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppOutlinedButton(
                text = "Cancelar",
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Começar partida",
                onClick = {
                    val message = viewModel.startGame(
                        playerIds = selected.map { it.id },
                        settings = GameSettings(startingLives = lives),
                    )
                    if (message == null) onClose() else error = message
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
