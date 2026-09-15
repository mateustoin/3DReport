package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Service
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regressão do bug relatado: cadastrar/editar um filamento ou impressora nas
 * telas de cadastro não atualizava a tela de Orçamento. Aqui simulamos o
 * mesmo cenário (repositório compartilhado sofre uma mudança "em outra
 * tela") sem depender de clique de UI.
 */
class QuoteViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun newlyAddedFilamentAppearsWithoutRecreatingTheViewModel() {
        val filamentRepository = FilamentRepository()
        val viewModel = QuoteViewModel(
            filamentRepository,
            PrinterRepository(),
            SettingsRepository(),
            ServiceRepository(),
            QuoteHistoryRepository(),
        )

        val newFilament = Filament(id = "nylon", name = "Nylon", pricePerKg = 150.0, densityGPerCm3 = 1.14)
        filamentRepository.add(newFilament) // equivalente a salvar na tela de Filamentos

        assertTrue(viewModel.filaments.value.any { it.id == "nylon" })
    }

    @Test
    fun editedPrinterIsReflectedInCalculation() {
        val printerRepository = PrinterRepository()
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            printerRepository,
            SettingsRepository(),
            ServiceRepository(),
            QuoteHistoryRepository(),
        )

        val printer = printerRepository.printers.value.first()
        viewModel.selectPrinter(printer.id)

        val edited = printer.copy(printerPowerWatts = printer.printerPowerWatts + 100.0)
        printerRepository.update(edited) // equivalente a editar na tela de Impressoras

        val result = viewModel.calculate(
            viewModel.filaments.value,
            viewModel.printers.value,
            viewModel.settings.value,
            viewModel.services.value,
            viewModel.input.value,
        )

        assertEquals(edited.printerPowerWatts, result.printer?.printerPowerWatts)
    }

    @Test
    fun deletedSelectedPrinterFallsBackToFirstRemaining() {
        val printerRepository = PrinterRepository()
        printerRepository.add(
            PrinterProfile(
                id = "second",
                name = "Segunda impressora",
                printerPowerWatts = 200.0,
                maintenanceCostPerHour = 0.05,
                machineInvestment = MachineInvestment(1000.0, 10, 20, 8.0),
            )
        )
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            printerRepository,
            SettingsRepository(),
            ServiceRepository(),
            QuoteHistoryRepository(),
        )
        val firstPrinter = printerRepository.printers.value.first()
        viewModel.selectPrinter(firstPrinter.id)

        printerRepository.delete(firstPrinter.id)

        val result = viewModel.calculate(
            viewModel.filaments.value,
            viewModel.printers.value,
            viewModel.settings.value,
            viewModel.services.value,
            viewModel.input.value,
        )

        assertEquals("second", result.printer?.id)
    }

    @Test
    fun toggleServiceAddsAndRemovesFromSelection() {
        val serviceRepository = ServiceRepository()
        serviceRepository.add(Service(id = "paint", name = "Pintura", price = 20.0))
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            SettingsRepository(),
            serviceRepository,
            QuoteHistoryRepository(),
        )

        viewModel.toggleService("paint")
        assertEquals(setOf("paint"), viewModel.input.value.selectedServiceIds)

        viewModel.toggleService("paint")
        assertTrue(viewModel.input.value.selectedServiceIds.isEmpty())
    }

    @Test
    fun calculateIncludesSelectedServicesInResultButNotInProfit() {
        val serviceRepository = ServiceRepository()
        val paint = Service(id = "paint", name = "Pintura", price = 20.0)
        serviceRepository.add(paint)
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            SettingsRepository(),
            serviceRepository,
            QuoteHistoryRepository(),
        )
        viewModel.toggleService("paint")
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        val result = viewModel.calculate(
            viewModel.filaments.value,
            viewModel.printers.value,
            viewModel.settings.value,
            viewModel.services.value,
            viewModel.input.value,
        )

        val quote = result.quote!!
        assertEquals(listOf(paint), result.selectedServices)
        assertEquals(20.0, result.servicesTotal)
        assertEquals(quote.salePrice + 20.0, result.grandTotal)
        // Lucro não deve mudar por causa de serviços (decisão: só entram no total, não no lucro).
        assertEquals(quote.profit, quote.salePrice - quote.productionCost)
    }

    @Test
    fun marketplaceFeeOnlyAppliesWhenToggled() {
        val settingsRepository = SettingsRepository()
        settingsRepository.update(settingsRepository.settings.value.copy(marketplaceFeeRate = 0.15))
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            settingsRepository,
            ServiceRepository(),
            QuoteHistoryRepository(),
        )
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        fun currentQuote() = viewModel.calculate(
            viewModel.filaments.value,
            viewModel.printers.value,
            viewModel.settings.value,
            viewModel.services.value,
            viewModel.input.value,
        ).quote!!

        val withoutFee = currentQuote()
        assertEquals(0.0, withoutFee.marketplaceFeeRate)

        viewModel.setAppliesMarketplaceFee(true)
        val withFee = currentQuote()

        assertEquals(0.15, withFee.marketplaceFeeRate)
        assertTrue(withFee.salePrice > withoutFee.salePrice)
        // Margem real (lucro) não deve mudar mesmo com o preço de tabela maior.
        assertEquals(withoutFee.profit, withFee.profit, 1e-9)
    }
}
