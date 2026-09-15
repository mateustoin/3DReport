package com.threedreport.app.data

import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** Valida que os perfis de impressora sobrevivem a uma nova instância do repositório (persistência em disco). */
class PrinterRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsWithDefaultPrinter() {
        val printers = PrinterRepository().printers.value
        assertTrue(printers.isNotEmpty())
    }

    @Test
    fun addedPrinterSurvivesNewRepositoryInstance() {
        val original = PrinterRepository()
        val printer = PrinterProfile(
            id = "bambu-a1",
            name = "Bambu A1",
            printerPowerWatts = 220.0,
            maintenanceCostPerHour = 0.10,
            machineInvestment = MachineInvestment(
                machinePrice = 2200.0,
                paybackMonths = 10,
                printingDaysPerMonth = 20,
                printingHoursPerDay = 10.0,
            ),
        )
        original.add(printer)

        val reloaded = PrinterRepository().printers.value
        assertTrue(reloaded.any { it.id == "bambu-a1" && it.name == "Bambu A1" })
    }

    @Test
    fun deleteRemovesFromDisk() {
        val repository = PrinterRepository()
        val printer = repository.printers.value.first()

        repository.delete(printer.id)

        val reloaded = PrinterRepository().printers.value
        assertTrue(reloaded.none { it.id == printer.id })
    }
}
