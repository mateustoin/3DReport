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
}
