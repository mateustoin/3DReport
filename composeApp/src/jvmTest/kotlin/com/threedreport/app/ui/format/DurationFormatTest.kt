package com.threedreport.app.ui.format

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {

    private val texts = runBlocking { loadDurationTexts() }

    @Test
    fun durationTextUsesHoursAndMinutesTheWayPeopleSayIt() {
        assertEquals("6 h 30 min", texts.duration(390.0))
        assertEquals("45 min", texts.duration(45.0))
        assertEquals("3 h", texts.duration(180.0))
        assertEquals("0 min", texts.duration(0.0))
    }

    @Test
    fun durationTextRoundsToTheNearestMinute() {
        assertEquals("1 h 1 min", texts.duration(60.6))
        assertEquals("1 h", texts.duration(60.4))
    }

    /** As horas e os minutos sem texto: as palavras ("h", "min") vêm de strings.xml. */
    @Test
    fun durationPartsAreWholeHoursAndMinutes() {
        assertEquals(DurationParts(hours = 6, minutes = 30), 390.0.toDurationParts())
        assertEquals(DurationParts(hours = 1, minutes = 1), 60.6.toDurationParts())
        assertEquals(DurationParts(hours = 0, minutes = 0), (-5.0).toDurationParts())
    }

    @Test
    fun decimalHoursKeepOneDecimalWithTheCommaOfTheLanguage() {
        assertEquals("12,5 h", texts.decimalHours(750.0))
        assertEquals("3 h", texts.decimalHours(180.0))
        assertEquals("-1,5 h", texts.decimalHours(-90.0))
    }

    @Test
    fun timeIsTypedTheWayTheSlicerShowsIt() {
        assertEquals(200.0, parseDurationMinutes("200"))
        assertEquals(200.0, parseDurationMinutes("3h20"))
        assertEquals(200.0, parseDurationMinutes("3:20"))
        assertEquals(200.0, parseDurationMinutes("3 h 20 min"))
        assertEquals(45.0, parseDurationMinutes("45min"))
        assertEquals(1560.0, parseDurationMinutes("1d2h"))
        assertEquals(200.5, parseDurationMinutes("3h20,5"))
    }

    @Test
    fun whatIsNotATimeIsRefused() {
        assertEquals(null, parseDurationMinutes("abc"))
        assertEquals(null, parseDurationMinutes("-10"))
        assertEquals(null, parseDurationMinutes(""))
    }

    @Test
    fun theFieldShowsHoursAndMinutesBack() {
        assertEquals("3h20", 200.0.toDurationInputText())
        assertEquals("45", 45.0.toDurationInputText())
        assertEquals("2h", 120.0.toDurationInputText())
    }
}
