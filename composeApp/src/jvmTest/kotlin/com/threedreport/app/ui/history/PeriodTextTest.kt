package com.threedreport.app.ui.history

import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.ui.dashboard.dashboardLabelRes
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodTextTest {

    @Test
    fun theDashboardPeriodsReadAsBefore() = runBlocking {
        assertEquals(
            listOf("Tudo", "7 dias", "30 dias", "Este mês", "Mês passado", "Este ano"),
            PeriodPreset.entries.map { getString(it.dashboardLabelRes()) },
        )
    }
}
