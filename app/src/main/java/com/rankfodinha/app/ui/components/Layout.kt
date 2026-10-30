package com.rankfodinha.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Largura máxima do conteúdo. No celular ocupa a tela toda; em tablet fica centralizado. */
val MAX_CONTENT_WIDTH = 640.dp

/**
 * Coluna de conteúdo de uma tela. Usa a largura disponível, com teto em [MAX_CONTENT_WIDTH].
 * Com [scrollable] = false, a tela pode ter uma LazyColumn com weight(1f), porque uma lista
 * rolável dentro de uma coluna rolável não funciona.
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                // A ordem importa: primeiro limita a largura, depois preenche. Na ordem inversa o teto é ignorado.
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
