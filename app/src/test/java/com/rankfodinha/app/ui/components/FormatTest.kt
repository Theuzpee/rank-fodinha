package com.rankfodinha.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun percentUsesOneDecimalAndACommaLikeBrazil() {
        assertEquals("0,0%", formatPercent(0.0))
        assertEquals("50,0%", formatPercent(50.0))
        assertEquals("66,7%", formatPercent(66.666))
        assertEquals("100,0%", formatPercent(100.0))
        assertEquals("12,5%", formatPercent(12.5))
    }

    @Test
    fun percentRoundsHalfUpLikeTheRankingDoes() {
        assertEquals("6,3%", formatPercent(6.25))
        assertEquals("33,3%", formatPercent(33.3))
    }

    @Test
    fun percentNeverUsesAThousandsSeparator() {
        assertEquals("100,0%", formatPercent(100.0))
        assertTrue(!formatPercent(100.0).contains("."))
    }

    @Test
    fun namesAreJoinedWithAnEAndCommas() {
        assertEquals("—", formatNames(emptyList()))
        assertEquals("Ana", formatNames(listOf("Ana")))
        assertEquals("Ana e Bruno", formatNames(listOf("Ana", "Bruno")))
        assertEquals("Ana, Bruno e Carla", formatNames(listOf("Ana", "Bruno", "Carla")))
        assertEquals("A, B, C e D", formatNames(listOf("A", "B", "C", "D")))
    }

    @Test
    fun pluralUsesTheSingularOnlyForOne() {
        assertEquals("0 vidas", plural(0, "vida", "vidas"))
        assertEquals("1 vida", plural(1, "vida", "vidas"))
        assertEquals("2 vidas", plural(2, "vida", "vidas"))
        assertEquals("12 rodadas", plural(12, "rodada", "rodadas"))
    }

    @Test
    fun dateTimeIsDayMonthYearHourMinute() {
        val formatted = formatDateTime("2026-03-05T14:30:00Z")
        assertTrue("formato dd/MM/yyyy HH:mm, veio: $formatted", Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}").matches(formatted))
    }

    @Test
    fun dateTimeMatchesTheDeviceTimeZone() {
        val iso = "2026-03-05T14:30:00Z"
        val expected = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
        assertEquals(expected, formatDateTime(iso))
    }

    @Test
    fun dateTimePadsSingleDigitsWithZeros() {
        val formatted = formatDateTime("2026-01-02T03:04:00Z")
        assertTrue(formatted, formatted.matches(Regex("\\d{2}/\\d{2}/2026 \\d{2}:\\d{2}")))
    }

    @Test
    fun dateTimeAcceptsTimestampsWithNanoseconds() {
        val formatted = formatDateTime("2026-03-05T14:30:00.123456789Z")
        assertTrue(formatted, formatted.matches(Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}")))
    }
}
