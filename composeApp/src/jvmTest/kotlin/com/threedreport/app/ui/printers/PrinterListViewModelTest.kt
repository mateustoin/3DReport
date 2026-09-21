package com.threedreport.app.ui.printers

import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val viewModel = PrinterListViewModel(PrinterRepository(), QuoteHistoryRepository())
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

        val viewModel = PrinterListViewModel(printerRepository, historyRepository)
        val queue = viewModel.printQueue(viewModel.printers.value, viewModel.savedQuotes.value)

        assertEquals(120.0, queue.single { it.printer.id == "printer-1" }.queuedMinutes)
    }
}
