package com.threedreport.app.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {

    @Test
    fun durationTextUsesHoursAndMinutesTheWayPeopleSayIt() {
        assertEquals("6 h 30 min", 390.0.minutesToDurationText())
        assertEquals("45 min", 45.0.minutesToDurationText())
        assertEquals("3 h", 180.0.minutesToDurationText())
        assertEquals("0 min", 0.0.minutesToDurationText())
    }

    @Test
    fun durationTextRoundsToTheNearestMinute() {
        assertEquals("1 h 1 min", 60.6.minutesToDurationText())
        assertEquals("1 h", 60.4.minutesToDurationText())
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
