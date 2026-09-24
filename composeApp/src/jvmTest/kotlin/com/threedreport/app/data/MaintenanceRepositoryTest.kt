package com.threedreport.app.data

import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.MaintenanceLogEntry
import com.threedreport.core.model.ManualUsageEntry
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que a manutenção sobrevive a uma nova instância do repositório e que registrar zera o componente certo. */
class MaintenanceRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun component(id: String, printerId: String = "p1") =
        MaintenanceComponent(id = id, printerId = printerId, name = "Bico", intervalHours = 50.0, hoursAtLastService = 0.0)

    @Test
    fun startsEmpty() {
        val maintenance = MaintenanceRepository().maintenance.value
        assertTrue(maintenance.components.isEmpty() && maintenance.log.isEmpty() && maintenance.manualUsage.isEmpty())
    }

    @Test
    fun everythingSurvivesNewRepositoryInstance() {
        val original = MaintenanceRepository()
        original.addComponent(component("bico"))
        original.logService(MaintenanceLogEntry(id = "l1", printerId = "p1", dateEpochDay = 20_000, description = "Nivelada a mesa"), 0.0)
        original.addManualUsage(ManualUsageEntry(id = "m1", printerId = "p1", dateEpochDay = 20_000, hours = 2.0, reason = "Calibração"))

        val reloaded = MaintenanceRepository().maintenance.value
        assertEquals(listOf("bico"), reloaded.components.map { it.id })
        assertEquals(listOf("l1"), reloaded.log.map { it.id })
        assertEquals(listOf("m1"), reloaded.manualUsage.map { it.id })
    }

    @Test
    fun loggingWithComponentRestartsOnlyThatCounter() {
        val repository = MaintenanceRepository()
        repository.addComponent(component("bico"))
        repository.addComponent(component("eixos"))

        repository.logService(
            MaintenanceLogEntry(id = "l1", printerId = "p1", dateEpochDay = 20_000, description = "Bico", componentId = "bico"),
            printerHoursNow = 73.0,
        )

        val byId = repository.maintenance.value.components.associateBy { it.id }
        assertEquals(73.0, byId.getValue("bico").hoursAtLastService)
        assertEquals(0.0, byId.getValue("eixos").hoursAtLastService)
    }

    @Test
    fun deleteAllForRemovesOnlyThatPrinter() {
        val repository = MaintenanceRepository()
        repository.addComponent(component("a", printerId = "p1"))
        repository.addComponent(component("b", printerId = "p2"))
        repository.addManualUsage(ManualUsageEntry(id = "m1", printerId = "p1", dateEpochDay = 20_000, hours = 2.0, reason = "Teste"))

        repository.deleteAllFor("p1")

        val reloaded = MaintenanceRepository().maintenance.value
        assertEquals(listOf("b"), reloaded.components.map { it.id })
        assertTrue(reloaded.manualUsage.isEmpty())
    }
}
