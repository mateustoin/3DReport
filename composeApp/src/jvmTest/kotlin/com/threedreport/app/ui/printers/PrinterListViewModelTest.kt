package com.threedreport.app.ui.printers

import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.report.MaintenanceState
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrinterListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startAddFromPresetPrefillsNameAndPower() {
        val viewModel = PrinterListViewModel(PrinterRepository(), QuoteHistoryRepository(), MaintenanceRepository())
        val preset = PRINTER_PRESETS.first()

        viewModel.startAddFromPreset(preset)

        val form = viewModel.form.value!!
        assertEquals("${preset.brand} ${preset.model}", form.name)
        assertEquals(preset.ratedPowerWatts.toString(), form.printerPowerWattsText)
    }

    @Test
    fun printQueueReflectsQuotesCurrentlyPrintingOnEachPrinter() {
        val printerRepository = PrinterRepository()
        val historyRepository = QuoteHistoryRepository()
        val printer = PrinterProfile(
            id = "printer-1",
            name = "Ender 3",
            printerPowerWatts = 200.0,
            maintenanceCostPerHour = 0.1,
            machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
        )
        printerRepository.add(printer)

        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 120.0),
            printer = printer,
            settings = com.threedreport.core.model.PricingSettings(
                energyPricePerKwh = 1.0,
                failureRate = 0.1,
                finishingRate = 0.1,
                profitMargin = 1.0,
            ),
        )
        val saved = historyRepository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        historyRepository.updateStatus(saved.id, OrderStatus.EM_IMPRESSAO)

        val viewModel = PrinterListViewModel(printerRepository, historyRepository, MaintenanceRepository())
        val queue = viewModel.printQueue(viewModel.printers.value, viewModel.savedQuotes.value)

        assertEquals(120.0, queue.single { it.printer.id == "printer-1" }.queuedMinutes)
    }

    private fun printerWithFinishedOrder(printMinutes: Double): Triple<PrinterRepository, QuoteHistoryRepository, PrinterProfile> {
        val printerRepository = PrinterRepository()
        val historyRepository = QuoteHistoryRepository()
        val printer = PrinterProfile(
            id = "printer-1",
            name = "Ender 3",
            printerPowerWatts = 200.0,
            maintenanceCostPerHour = 0.1,
            machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
        )
        printerRepository.add(printer)
        val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = printMinutes),
            printer = printer,
            settings = com.threedreport.core.model.PricingSettings(energyPricePerKwh = 1.0, failureRate = 0.1, finishingRate = 0.1, profitMargin = 1.0),
        )
        val saved = historyRepository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        historyRepository.updateStatus(saved.id, OrderStatus.PRONTO)
        return Triple(printerRepository, historyRepository, printer)
    }

    @Test
    fun newComponentStartsCountingFromHoursAlreadyRunAndServiceRestartsIt() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 360.0)
        val viewModel = PrinterListViewModel(printers, history, MaintenanceRepository())

        // Bico trocado há 52 h, a cada 50 h: o contador começa em 52, apesar de o app só conhecer 6 h.
        assertNull(viewModel.addComponent(printer.id, "Bico", "50", "52"))
        val before = viewModel.componentStatuses(printer.id, viewModel.savedQuotes.value, viewModel.maintenance.value).single()
        assertEquals(MaintenanceState.OVERDUE, before.state)
        assertEquals(52.0, before.hoursSinceService, 1e-9)

        viewModel.markServiced(before.component)

        val after = viewModel.componentStatuses(printer.id, viewModel.savedQuotes.value, viewModel.maintenance.value).single()
        assertEquals(0.0, after.hoursSinceService, 1e-9)
        assertEquals("Bico", viewModel.maintenance.value.log.single().description)
    }

    @Test
    fun manualUsageAddsToPrinterHours() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val viewModel = PrinterListViewModel(printers, history, MaintenanceRepository())

        assertNull(viewModel.addManualUsage(printer.id, "2,5", "Calibração"))

        assertEquals(3.5, viewModel.printerHours(printer.id, viewModel.savedQuotes.value, viewModel.maintenance.value), 1e-9)
    }

    @Test
    fun invalidMaintenanceInputReturnsAMessageAndSavesNothing() {
        val viewModel = PrinterListViewModel(PrinterRepository(), QuoteHistoryRepository(), MaintenanceRepository())

        assertNotNull(viewModel.addComponent("p", "Bico", "0", ""))
        assertNotNull(viewModel.addComponent("p", " ", "50", ""))
        assertNotNull(viewModel.addManualUsage("p", "abc", "Teste"))
        assertNotNull(viewModel.logService("p", 20_000, " ", null))

        val maintenance = viewModel.maintenance.value
        assertTrue(maintenance.components.isEmpty() && maintenance.manualUsage.isEmpty() && maintenance.log.isEmpty())
    }

    @Test
    fun deletingPrinterDeletesItsMaintenance() {
        val (printers, history, printer) = printerWithFinishedOrder(printMinutes = 60.0)
        val viewModel = PrinterListViewModel(printers, history, MaintenanceRepository())
        viewModel.addComponent(printer.id, "Bico", "50", "")
        viewModel.addManualUsage(printer.id, "1", "Teste")

        viewModel.delete(printer.id)

        val reloaded = MaintenanceRepository().maintenance.value
        assertTrue(reloaded.components.isEmpty() && reloaded.manualUsage.isEmpty())
    }
}
