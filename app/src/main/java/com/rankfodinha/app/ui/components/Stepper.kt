package com.rankfodinha.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Controle de número com botões grandes de − e +. Não abre o teclado no celular.
 * O número fica no centro e ocupa o espaço que sobra, então o controle cabe em telas estreitas.
 * [unit] aparece embaixo do número, por exemplo "vidas".
 */
@Composable
fun Stepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StepperButton(
            text = "−",
            description = "Diminuir $label",
            enabled = value > min,
            primary = false,
            onClick = { onChange(value - 1) },
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            unit?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        StepperButton(
            text = "+",
            description = "Aumentar $label",
            enabled = value < max,
            primary = true,
            onClick = { onChange(value + 1) },
        )
    }
}

/** "+" é o botão principal (vermelho). "−" é um botão tonal, mais discreto. */
@Composable
private fun StepperButton(
    text: String,
    description: String,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .defaultMinSize(minWidth = 72.dp, minHeight = 72.dp)
        .semantics { contentDescription = description }
    val shape = RoundedCornerShape(20.dp)
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(text = text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}
