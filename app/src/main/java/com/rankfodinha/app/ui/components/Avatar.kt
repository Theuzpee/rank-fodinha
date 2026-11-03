package com.rankfodinha.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.rankfodinha.app.R
import com.rankfodinha.app.data.AVATAR_COUNT
import com.rankfodinha.app.data.AvatarPhotos
import com.rankfodinha.app.data.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fotos da equipe, sorteadas no cadastro. Para trocar uma foto, substitua o arquivo de mesmo nome em res/drawable-nodpi.
 */
private val AVATARS = intArrayOf(
    R.drawable.avatar_01,
    R.drawable.avatar_02,
    R.drawable.avatar_03,
    R.drawable.avatar_04,
    R.drawable.avatar_05,
    R.drawable.avatar_06,
    R.drawable.avatar_07,
)

/** Zoom da foto dentro do círculo: 1.2 deixa o rosto um pouco maior. Muda só a tela, o arquivo salvo fica igual. */
private const val AVATAR_ZOOM = 1.2f

/** Foto da equipe de índice [index]. Um índice fora da lista volta para o início, para a tela nunca quebrar. */
private fun teamPhoto(index: Int): Int = AVATARS[index.mod(AVATAR_COUNT)]

/** O que o avatar mostra: a foto da equipe, a foto que o jogador tirou, ou um fundo enquanto a foto é lida. */
private sealed interface AvatarImage {
    data object Loading : AvatarImage
    data object Team : AvatarImage
    class Own(val bitmap: ImageBitmap) : AvatarImage
}

/**
 * Avatar do jogador, cortado em círculo. Mostra a foto que ele tirou. Sem essa foto, mostra a foto da equipe
 * sorteada no cadastro, ou a foto fixa pelo id para cadastros antigos.
 */
@Composable
fun AvatarBadge(player: Player, size: Dp, modifier: Modifier = Modifier) {
    check(AVATARS.size == AVATAR_COUNT) { "Quantidade de avatares diferente de AVATAR_COUNT" }
    val image = rememberAvatarImage(player)
    val frame = modifier.size(size).clip(CircleShape)
    val description = "Avatar de ${player.name}"
    // A foto fica dentro da caixa cortada em círculo. O zoom aumenta a foto dentro dessa caixa.
    Box(modifier = frame) {
        when (val current = image) {
            is AvatarImage.Own -> Image(
                bitmap = current.bitmap,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(AVATAR_ZOOM),
            )
            AvatarImage.Team -> Image(
                painter = painterResource(teamPhoto(player.avatar ?: avatarIndexFor(player.id))),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(AVATAR_ZOOM),
            )
            AvatarImage.Loading -> Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

/**
 * Lê a foto do jogador fora da thread principal. O horário da foto faz parte da chave,
 * então uma foto nova é lida de novo.
 */
@Composable
private fun rememberAvatarImage(player: Player): AvatarImage {
    val context = LocalContext.current
    val hasOwnPhoto = player.photoSavedAt != null
    val image by produceState<AvatarImage>(
        initialValue = if (hasOwnPhoto) AvatarImage.Loading else AvatarImage.Team,
        player.id,
        player.photoSavedAt,
    ) {
        if (hasOwnPhoto) {
            value = AvatarImage.Loading
            val bitmap = withContext(Dispatchers.IO) { AvatarPhotos.load(context, player.id) }
            value = bitmap?.let { AvatarImage.Own(it.asImageBitmap()) } ?: AvatarImage.Team
        } else {
            value = AvatarImage.Team
        }
    }
    return image
}

/**
 * Foto do jogador inteira, sem o zoom do avatar e sem cantos arredondados, para a tela ampliada.
 * A foto ocupa o tamanho que o modifier der.
 */
@Composable
fun PlayerPhoto(player: Player, modifier: Modifier = Modifier) {
    val image = rememberAvatarImage(player)
    val description = "Foto de ${player.name}"
    Box(modifier = modifier) {
        when (val current = image) {
            is AvatarImage.Own -> Image(
                bitmap = current.bitmap,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            AvatarImage.Team -> Image(
                painter = painterResource(teamPhoto(player.avatar ?: avatarIndexFor(player.id))),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            AvatarImage.Loading -> Unit
        }
    }
}
