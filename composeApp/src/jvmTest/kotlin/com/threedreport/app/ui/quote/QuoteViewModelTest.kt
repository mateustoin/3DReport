package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.SalesChannel
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
            SalesChannelRepository(),
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
            SalesChannelRepository(),
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
            SalesChannelRepository(),
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
            SalesChannelRepository(),
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
            SalesChannelRepository(),
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
    fun salesChannelFeeOnlyAppliesToTheQuoteThatSelectsIt() {
        val channelRepository = SalesChannelRepository()
        channelRepository.add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.15))
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            SettingsRepository(),
            ServiceRepository(),
            channelRepository,
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
            viewModel.salesChannels.value,
        ).quote!!

        // Venda direta é o padrão: cadastrar um canal não cobra taxa de ninguém sozinho.
        val direct = currentQuote()
        assertEquals(0.0, direct.marketplaceFeeRate)

        viewModel.selectSalesChannel("shopee")
        val viaShopee = currentQuote()

        assertEquals(0.15, viaShopee.marketplaceFeeRate)
        assertEquals("Shopee", viaShopee.channelName)
        assertTrue(viaShopee.salePrice > direct.salePrice)
        // Margem real (lucro) não muda mesmo com o preço de tabela maior.
        assertEquals(direct.profit, viaShopee.profit, 1e-9)
    }

    @Test
    fun calculateResolvesFirstAvailableColorWhenNoneSelected() {
        val filamentRepository = FilamentRepository()
        val filament = Filament(
            id = "multi-color",
            name = "PLA Voolt",
            pricePerKg = 100.0,
            densityGPerCm3 = 1.24,
            colors = listOf(FilamentColor(id = "red", name = "Vermelho"), FilamentColor(id = "blue", name = "Azul")),
        )
        filamentRepository.add(filament)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("multi-color")

        val result = viewModel.calculate(viewModel.filaments.value, viewModel.printers.value, viewModel.settings.value, viewModel.services.value, viewModel.input.value)

        assertEquals("red", result.filamentColor?.id)
    }

    @Test
    fun calculateUsesTheExplicitlySelectedColor() {
        val filamentRepository = FilamentRepository()
        val filament = Filament(
            id = "multi-color",
            name = "PLA Voolt",
            pricePerKg = 100.0,
            densityGPerCm3 = 1.24,
            colors = listOf(FilamentColor(id = "red", name = "Vermelho"), FilamentColor(id = "blue", name = "Azul")),
        )
        filamentRepository.add(filament)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("multi-color")
        viewModel.selectFilamentColor("blue")
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        val result = viewModel.calculate(viewModel.filaments.value, viewModel.printers.value, viewModel.settings.value, viewModel.services.value, viewModel.input.value)

        assertEquals("blue", result.filamentColor?.id)
        assertEquals("blue", result.quote?.job?.filamentColor?.id)
    }

    @Test
    fun calculateSkipsOutOfStockColorsWhenFallingBack() {
        val filamentRepository = FilamentRepository()
        val filament = Filament(
            id = "multi-color",
            name = "PLA Voolt",
            pricePerKg = 100.0,
            densityGPerCm3 = 1.24,
            colors = listOf(
                FilamentColor(id = "red", name = "Vermelho", inStock = false),
                FilamentColor(id = "blue", name = "Azul", inStock = true),
            ),
        )
        filamentRepository.add(filament)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("multi-color")

        val result = viewModel.calculate(viewModel.filaments.value, viewModel.printers.value, viewModel.settings.value, viewModel.services.value, viewModel.input.value)

        assertEquals("blue", result.filamentColor?.id)
    }

    @Test
    fun selectingANewFilamentResetsTheChosenColor() {
        val filamentRepository = FilamentRepository()
        val first = Filament(id = "f1", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, colors = listOf(FilamentColor(id = "red")))
        val second = Filament(id = "f2", name = "ABS", pricePerKg = 90.0, densityGPerCm3 = 1.04, colors = listOf(FilamentColor(id = "blue")))
        filamentRepository.add(first)
        filamentRepository.add(second)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("f1")
        viewModel.selectFilamentColor("red")

        viewModel.selectFilament("f2")

        assertEquals(null, viewModel.input.value.filamentColorId)
    }

    @Test
    fun saveCurrentQuoteSavesWhenThereIsAValidQuote() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        viewModel.saveCurrentQuote()

        assertEquals(1, historyRepository.savedQuotes.value.size)
    }

    @Test
    fun saveCurrentQuoteDoesNothingWithoutAValidQuote() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        // comprimento/tempo em branco: sem orçamento calculado

        viewModel.saveCurrentQuote()

        assertTrue(historyRepository.savedQuotes.value.isEmpty())
    }

    @Test
    fun loadForEditingRepopulatesFormAndUpdatesInsteadOfDuplicating() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setSaveName("Peça original")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.first()

        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        assertEquals("12", viewModel.input.value.lengthMetersText)
        assertEquals("190", viewModel.input.value.printTimeMinutesText)
        assertEquals(saved.id, viewModel.saveForm.value.editingQuoteId)
        assertEquals("Peça original", viewModel.saveForm.value.name)

        viewModel.setSaveName("Peça corrigida")
        viewModel.saveCurrentQuote()

        assertEquals(1, historyRepository.savedQuotes.value.size)
        val updated = historyRepository.savedQuotes.value.first()
        assertEquals("Peça corrigida", updated.name)
        assertEquals(saved.savedAtEpochMillis, updated.savedAtEpochMillis)
        assertTrue(updated.lastEditedEpochMillis != null)
    }

    @Test
    fun editingANegotiatedQuoteKeepsTheNegotiatedPrice() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setTargetTotal("30")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.first()
        assertTrue(saved.quote.isNegotiated)

        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        // Regressão: o preço fechado não voltava pro campo, e salvar de novo trocava em silêncio
        // o valor cobrado pelo de tabela.
        assertEquals("30", viewModel.input.value.targetTotalText)
        viewModel.saveCurrentQuote()
        val updated = historyRepository.savedQuotes.value.first()
        assertEquals(30.0, updated.quote.salePrice, 1e-9)
        assertEquals(saved.quote.tableSalePrice, updated.quote.tableSalePrice)
    }

    @Test
    fun duplicateForNewQuoteCreatesANewEntryInsteadOfUpdatingTheOriginal() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setSaveName("Peça original")
        viewModel.saveCurrentQuote()
        val original = historyRepository.savedQuotes.value.first()

        viewModel.duplicateForNewQuote(original)

        assertEquals(null, viewModel.saveForm.value.editingQuoteId)
        assertEquals("Peça original", viewModel.saveForm.value.duplicatedFromName)
        assertEquals("Peça original", viewModel.saveForm.value.name)
        assertEquals("12", viewModel.input.value.lengthMetersText)

        viewModel.setSaveName("Peça pro cliente novo")
        viewModel.saveCurrentQuote()

        assertEquals(2, historyRepository.savedQuotes.value.size)
        val duplicate = historyRepository.savedQuotes.value.first { it.id != original.id }
        assertEquals("Peça pro cliente novo", duplicate.name)
        assertEquals(com.threedreport.core.model.OrderStatus.ORCADO, duplicate.status)
        assertTrue(duplicate.savedAtEpochMillis >= original.savedAtEpochMillis)
    }

    @Test
    fun setPrintSettingsIsPersistedOnSaveAndRestoredOnEdit() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        val settings = PrintSettings(layerHeightMm = 0.2, infillPercentage = 15.0, infillPattern = "gyroid", supportsEnabled = true)
        viewModel.setPrintSettings(settings)

        viewModel.saveCurrentQuote()

        val saved = historyRepository.savedQuotes.value.first()
        assertEquals(settings, saved.printSettings)

        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        assertEquals(settings, viewModel.saveForm.value.printSettings)
    }

    @Test
    fun emptyPrintSettingsAreSavedAsNull() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        viewModel.saveCurrentQuote()

        assertEquals(null, historyRepository.savedQuotes.value.first().printSettings)
    }

    @Test
    fun resetFormClearsInputAndSaveForm() {
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setSaveName("Peça de teste")
        viewModel.setClientName("Maria")

        viewModel.resetForm()

        assertEquals(QuoteInputState(), viewModel.input.value)
        assertEquals(SaveQuoteFormState(), viewModel.saveForm.value)
    }

    @Test
    fun deliveryDateIsSavedAndRestoredOnEditButNotCopiedOnDuplicate() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setDeliveryDate(20_700L)

        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.first()
        assertEquals(20_700L, saved.deliveryDateEpochDay)

        viewModel.loadForEditing(saved)
        assertEquals(20_700L, viewModel.saveForm.value.deliveryDateEpochDay)

        // A data de outro pedido, provavelmente já vencida, não pode ir parar num orçamento novo.
        viewModel.duplicateForNewQuote(saved)
        assertEquals(null, viewModel.saveForm.value.deliveryDateEpochDay)
    }

    @Test
    fun queueHintCountsApprovedAndPrintingButNotTheQuoteBeingEdited() {
        val historyRepository = QuoteHistoryRepository()
        val printerRepository = PrinterRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        val printer = printerRepository.printers.value.first()
        viewModel.selectPrinter(printer.id)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("120")
        viewModel.saveCurrentQuote()
        viewModel.saveCurrentQuote()
        val (approved, editing) = historyRepository.savedQuotes.value
        historyRepository.updateStatus(approved.id, com.threedreport.core.model.OrderStatus.APROVADO)
        historyRepository.updateStatus(editing.id, com.threedreport.core.model.OrderStatus.EM_IMPRESSAO)

        val ahead = viewModel.queueAheadOf(printer, historyRepository.savedQuotes.value, editingQuoteId = editing.id)

        assertEquals(1, ahead?.queuedQuoteCount)
        assertEquals(120.0, ahead?.queuedMinutes)
        assertEquals(null, viewModel.queueAheadOf(printer, emptyList(), editingQuoteId = null), "fila vazia não mostra dica")
    }
}
