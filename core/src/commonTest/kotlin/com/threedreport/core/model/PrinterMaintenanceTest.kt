package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrinterMaintenanceTest {

    private val bico = MaintenanceComponent(id = "bico", printerId = "p1", name = "Bico", intervalHours = 50.0, hoursAtLastService = 10.0)
    private val eixos = MaintenanceComponent(id = "eixos", printerId = "p1", name = "Eixos", intervalHours = 100.0, hoursAtLastService = 5.0)

    private fun entry(id: String, componentId: String?) =
        MaintenanceLogEntry(id = id, printerId = "p1", dateEpochDay = 20_000, description = "Manutenção", componentId = componentId)

    private fun PrinterMaintenance.hoursOf(id: String) = components.single { it.id == id }.hoursAtLastService

    @Test
    fun serviceRestartsOnlyThatComponentAndRemembersWhereItWas() {
        val after = PrinterMaintenance(components = listOf(bico, eixos)).withService(entry("l1", "bico"), printerHoursNow = 55.0)

        assertEquals(55.0, after.hoursOf("bico"))
        assertEquals(5.0, after.hoursOf("eixos"))
        assertEquals(10.0, after.log.single().previousHoursAtLastService)
    }

    @Test
    fun deletingTheLatestServiceUndoesTheReset() {
        // "Feito hoje" clicado por engano: excluir a entrada devolve as horas acumuladas.
        val serviced = PrinterMaintenance(components = listOf(bico)).withService(entry("l1", "bico"), printerHoursNow = 55.0)

        val undone = serviced.withoutLogEntry("l1")

        assertEquals(10.0, undone.hoursOf("bico"))
        assertTrue(undone.log.isEmpty())
    }

    @Test
    fun deletingAnOlderServiceKeepsTheCurrentCounter() {
        val twice = PrinterMaintenance(components = listOf(bico))
            .withService(entry("l1", "bico"), printerHoursNow = 55.0)
            .withService(entry("l2", "bico"), printerHoursNow = 90.0)

        val withoutOlder = twice.withoutLogEntry("l1")

        assertEquals(90.0, withoutOlder.hoursOf("bico"))
        assertEquals(listOf("l2"), withoutOlder.log.map { it.id })
        // E desfazer a mais recente depois volta pra onde ela encontrou o contador.
        assertEquals(55.0, withoutOlder.withoutLogEntry("l2").hoursOf("bico"))
    }

    @Test
    fun entryWithoutComponentOnlyLeavesTheDiary() {
        val logged = PrinterMaintenance(components = listOf(bico)).withService(entry("l1", null), printerHoursNow = 55.0)

        assertNull(logged.log.single().previousHoursAtLastService)
        assertEquals(10.0, logged.withoutLogEntry("l1").hoursOf("bico"))
    }

    @Test
    fun entrySavedBeforeTheUndoFieldExistedOnlyLeavesTheDiary() {
        val old = PrinterMaintenance(components = listOf(bico.copy(hoursAtLastService = 55.0)), log = listOf(entry("l1", "bico")))

        val after = old.withoutLogEntry("l1")

        assertEquals(55.0, after.hoursOf("bico"))
        assertTrue(after.log.isEmpty())
    }
}
