package com.threedreport.core.report

import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.ManualUsageEntry
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaintenanceReportTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private fun printed(printerId: String?, minutes: Double, status: OrderStatus, quantity: Int = 1) = SavedQuote(
        id = "$printerId-$minutes-$status-$quantity",
        name = "Peça",
        status = status,
        quote = Quote(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = minutes),
            filamentWeightGrams = 5.0,
            costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            productionCost = 0.0,
            salePrice = 0.0,
            printerId = printerId,
            quantity = quantity,
        ),
        savedAtEpochMillis = 0L,
    )

    private fun component(interval: Double, atLastService: Double) =
        MaintenanceComponent(id = "c", printerId = "p1", name = "Bico", intervalHours = interval, hoursAtLastService = atLastService)

    @Test
    fun onlyFinishedOrdersOfThatPrinterCount() {
        val quotes = listOf(
            printed("p1", 120.0, OrderStatus.PRONTO),
            printed("p1", 60.0, OrderStatus.ENTREGUE),
            printed("p1", 600.0, OrderStatus.EM_IMPRESSAO),
            printed("p1", 600.0, OrderStatus.APROVADO),
            printed("p1", 600.0, OrderStatus.ORCADO),
            printed("p2", 600.0, OrderStatus.ENTREGUE),
            printed(null, 600.0, OrderStatus.ENTREGUE),
        )

        assertEquals(3.0, MaintenanceReport.printerHours("p1", quotes, emptyList()), 1e-9)
    }

    @Test
    fun quantityMultipliesMachineTime() {
        val quotes = listOf(printed("p1", 30.0, OrderStatus.PRONTO, quantity = 4))

        assertEquals(2.0, MaintenanceReport.printerHours("p1", quotes, emptyList()), 1e-9)
    }

    @Test
    fun manualUsageOfThatPrinterAdds() {
        val manual = listOf(
            ManualUsageEntry(id = "m1", printerId = "p1", dateEpochDay = 0, hours = 1.5, reason = "Calibração"),
            ManualUsageEntry(id = "m2", printerId = "p2", dateEpochDay = 0, hours = 9.0, reason = "Outra"),
        )

        assertEquals(2.5, MaintenanceReport.printerHours("p1", listOf(printed("p1", 60.0, OrderStatus.PRONTO)), manual), 1e-9)
    }

    @Test
    fun statesFollowRemainingHours() {
        assertEquals(MaintenanceState.OK, MaintenanceReport.componentStatus(component(100.0, 0.0), 50.0).state)
        assertEquals(MaintenanceState.DUE_SOON, MaintenanceReport.componentStatus(component(100.0, 0.0), 90.0).state)
        assertEquals(MaintenanceState.DUE_SOON, MaintenanceReport.componentStatus(component(100.0, 0.0), 100.0).state)
        val overdue = MaintenanceReport.componentStatus(component(100.0, 0.0), 108.0)
        assertEquals(MaintenanceState.OVERDUE, overdue.state)
        assertEquals(-8.0, overdue.remainingHours, 1e-9)
    }

    @Test
    fun counterNeverGoesNegativeWhenTheTotalShrinks() {
        // Pedido excluído depois da manutenção: o total caiu abaixo da fotografia.
        val status = MaintenanceReport.componentStatus(component(50.0, 20.0), 12.0)

        assertEquals(0.0, status.hoursSinceService)
        assertEquals(50.0, status.remainingHours)
    }

    @Test
    fun hoursAlreadyRunBeforeTheAppStartTheCounterAhead() {
        // Máquina com 10 h no app e bico trocado há 45 h: o contador começa em 45.
        val start = MaintenanceReport.hoursAtLastServiceFor(currentHours = 10.0, hoursSinceLastService = 45.0)
        val status = MaintenanceReport.componentStatus(component(50.0, start), currentHours = 16.0)

        assertEquals(51.0, status.hoursSinceService, 1e-9)
        assertEquals(MaintenanceState.OVERDUE, status.state)
    }

    @Test
    fun mostUrgentIsTheOneWithLeastHoursLeft() {
        val a = MaintenanceReport.componentStatus(component(100.0, 0.0).copy(id = "a"), 30.0)
        val b = MaintenanceReport.componentStatus(component(40.0, 0.0).copy(id = "b"), 30.0)

        assertEquals("b", MaintenanceReport.mostUrgent(listOf(a, b))?.component?.id)
        assertNull(MaintenanceReport.mostUrgent(emptyList()))
    }

    @Test
    fun catalogProductsDoNotAddMachineHours() {
        val quotes = listOf(
            printed("p1", 60.0, OrderStatus.PRONTO),
            printed("p1", 600.0, OrderStatus.ENTREGUE).copy(kind = QuoteKind.PRODUCT),
        )

        assertEquals(1.0, MaintenanceReport.printerHours("p1", quotes, emptyList()), 1e-9)
    }
}
