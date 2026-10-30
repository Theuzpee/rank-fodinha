package com.rankfodinha.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Altura mínima dos botões, para um alvo de toque confortável. */
private val BUTTON_MIN_HEIGHT = 56.dp

/**
 * Botão principal, ocupando a largura disponível. [contentPadding] reduz o espaço ao redor do texto,
 * para o rótulo caber em uma linha quando o botão divide a linha com outro.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = BUTTON_MIN_HEIGHT),
        contentPadding = contentPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/**
 * Botão com contorno. [contentColor] muda a cor do texto, por exemplo para uma ação que apaga dados.
 * [shape] muda o formato das bordas. Passe `ButtonDefaults.shape` para ficar igual ao botão principal.
 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
    shape: Shape = ButtonDefaults.outlinedShape,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = BUTTON_MIN_HEIGHT),
        shape = shape,
        colors = if (contentColor == Color.Unspecified) {
            ButtonDefaults.outlinedButtonColors()
        } else {
            ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        },
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
