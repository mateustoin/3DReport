package com.threedreport.app.platform

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodJvmTest {

    @Test
    fun allHasNoLowerBound() {
        assertNull(periodStartEpochMillis(PeriodPreset.ALL))
    }

    @Test
    fun last7DaysStartsSixDaysBeforeToday() {
        val zone = ZoneId.systemDefault()
        val expected = LocalDate.now(zone).minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expected, periodStartEpochMillis(PeriodPreset.LAST_7_DAYS))
    }

    @Test
    fun thisMonthStartsOnTheFirstDay() {
        val zone = ZoneId.systemDefault()
        val expected = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expected, periodStartEpochMillis(PeriodPreset.THIS_MONTH))
    }

    @Test
    fun lastMonthIsTheWholePreviousMonth() {
        val zone = ZoneId.systemDefault()
        val firstOfThisMonth = LocalDate.now(zone).withDayOfMonth(1)

        assertEquals(firstOfThisMonth.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli(), periodStartEpochMillis(PeriodPreset.LAST_MONTH))
        assertEquals(firstOfThisMonth.atStartOfDay(zone).toInstant().toEpochMilli(), periodEndEpochMillis(PeriodPreset.LAST_MONTH))
        assertTrue(!PeriodPreset.LAST_MONTH.contains(System.currentTimeMillis()))
    }

    @Test
    fun onlyLastMonthHasAnUpperBound() {
        PeriodPreset.entries.filter { it != PeriodPreset.LAST_MONTH }.forEach { assertNull(periodEndEpochMillis(it)) }
        assertTrue(PeriodPreset.THIS_YEAR.contains(System.currentTimeMillis()))
    }

    @Test
    fun boundsAreOrderedAllTimeToShortestPeriod() {
        val last30 = periodStartEpochMillis(PeriodPreset.LAST_30_DAYS)!!
        val last7 = periodStartEpochMillis(PeriodPreset.LAST_7_DAYS)!!

        assertTrue(last30 <= last7)
    }
}
