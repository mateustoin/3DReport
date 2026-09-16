package com.threedreport.app.platform

import java.time.LocalDate
import java.time.ZoneId

actual fun periodStartEpochMillis(preset: PeriodPreset): Long? {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val startDate = when (preset) {
        PeriodPreset.ALL -> return null
        PeriodPreset.LAST_7_DAYS -> today.minusDays(6)
        PeriodPreset.LAST_30_DAYS -> today.minusDays(29)
        PeriodPreset.THIS_MONTH -> today.withDayOfMonth(1)
    }
    return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
}
