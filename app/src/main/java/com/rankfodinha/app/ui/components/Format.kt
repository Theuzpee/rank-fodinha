package com.rankfodinha.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TIME: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm")
    .withZone(ZoneId.systemDefault())

/** Formata um instante ISO 8601 como dd/mm/aaaa hh:mm, no fuso do aparelho. */
fun formatDateTime(iso: String): String = DATE_TIME.format(Instant.parse(iso))

/** Porcentagem com uma casa decimal, no padrão brasileiro: 66,7% */
fun formatPercent(value: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", value)

/** Junta nomes em português: "Ana", "Ana e Bruno", "Ana, Bruno e Carla". */
fun formatNames(names: List<String>): String = when (names.size) {
    0 -> "—"
    1 -> names.first()
    else -> names.dropLast(1).joinToString(", ") + " e " + names.last()
}

/** Escolhe singular ou plural conforme a quantidade: "1 vida", "3 vidas". */
fun plural(count: Int, singular: String, pluralForm: String): String =
    "$count ${if (count == 1) singular else pluralForm}"
