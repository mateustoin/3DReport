package com.threedreport.app.ui.quote

import kotlin.test.assertNull
import com.threedreport.core.model.QuoteKind
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
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

        assertEquals(edited.printerPowerWatts, result.prints.first().printer?.printerPowerWatts)
    }

    @Test
    fun deletedSelectedPrinterAsksToChooseAgain() {
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

        // A impressora escolhida saiu do cadastro: a tela pede pra escolher de novo, em vez de trocar pela
        // primeira da lista em silêncio (decisão 108).
        assertNull(result.prints.first().printer)
        assertNull(result.quote)
        assertTrue(result.errorMessage!!.contains("impressora"))
    }

    private fun viewModelWith(
        vararg services: Service,
        historyRepository: QuoteHistoryRepository = QuoteHistoryRepository(),
    ): QuoteViewModel {
        val serviceRepository = ServiceRepository()
        services.forEach(serviceRepository::add)
        return QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            SettingsRepository(),
            serviceRepository,
            SalesChannelRepository(),
            historyRepository,
        ).apply {
            setLengthMeters("12")
            setPrintTimeMinutes("190")
        }
    }

    private fun QuoteViewModel.currentResult() =
        calculate(filaments.value, printers.value, settings.value, services.value, input.value)

    @Test
    fun toggleServiceAddsAndRemovesFromSelection() {
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura", suggestedPrice = 20.0))

        viewModel.toggleService("paint")
        assertEquals(setOf("paint"), viewModel.input.value.selectedServices.keys)

        viewModel.toggleService("paint")
        assertTrue(viewModel.input.value.selectedServices.isEmpty())
    }

    @Test
    fun checkingAServicePrefillsTheSuggestedPriceAndChargeMode() {
        val viewModel = viewModelWith(
            Service(id = "paint", name = "Pintura", suggestedPrice = 20.0),
            Service(id = "delivery", name = "Entrega", chargedPerOrder = true),
        )

        viewModel.toggleService("paint")
        viewModel.toggleService("delivery")

        val selected = viewModel.input.value.selectedServices
        assertEquals(ServiceInput(name = "Pintura", priceText = "20", chargedPerOrder = false), selected["paint"])
        assertEquals(ServiceInput(name = "Entrega", priceText = "", chargedPerOrder = true), selected["delivery"])
    }

    @Test
    fun calculateIncludesSelectedServicesInResultButNotInProfit() {
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura", suggestedPrice = 20.0))
        viewModel.toggleService("paint")

        val result = viewModel.currentResult()

        val quote = result.quote!!
        assertEquals(listOf(QuoteService(id = "paint", name = "Pintura", price = 20.0, chargedPerOrder = false)), result.selectedServices)
        assertEquals(20.0, result.servicesTotal)
        assertEquals(quote.salePrice + 20.0, result.grandTotal)
        // Lucro não deve mudar por causa de serviços (decisão: só entram no total, não no lucro).
        assertEquals(quote.salePrice - quote.productionCost, quote.profit, 1e-9)
    }

    @Test
    fun priceTypedInTheQuoteOverridesTheSuggestion() {
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura", suggestedPrice = 20.0))
        viewModel.toggleService("paint")

        viewModel.setServicePrice("paint", "35,50")

        assertEquals(35.5, viewModel.currentResult().servicesTotal, 1e-9)
    }

    @Test
    fun perOrderServiceIsNotMultipliedByQuantity() {
        val viewModel = viewModelWith(
            Service(id = "paint", name = "Pintura", suggestedPrice = 2.0),
            Service(id = "delivery", name = "Entrega", suggestedPrice = 15.0, chargedPerOrder = true),
        )
        viewModel.setQuantity("10")
        viewModel.toggleService("paint")
        viewModel.toggleService("delivery")

        val result = viewModel.currentResult()

        assertEquals(2.0 * 10 + 15.0, result.servicesTotal, 1e-9)
        assertEquals(result.quote!!.salePrice + 35.0, result.grandTotal!!, 1e-9)
    }

    @Test
    fun chargeModeCanBeChangedInTheQuote() {
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura", suggestedPrice = 30.0))
        viewModel.setQuantity("10")
        viewModel.toggleService("paint")

        viewModel.setServiceChargedPerOrder("paint", true)

        assertEquals(30.0, viewModel.currentResult().servicesTotal, 1e-9)
    }

    @Test
    fun targetTotalSubtractsPerOrderServiceOnlyOnce() {
        val viewModel = viewModelWith(Service(id = "delivery", name = "Entrega", suggestedPrice = 15.0, chargedPerOrder = true))
        viewModel.setQuantity("10")
        viewModel.toggleService("delivery")
        viewModel.setTargetTotal("100")

        val result = viewModel.currentResult()

        assertEquals(85.0, result.quote!!.salePrice, 1e-9)
        assertEquals(100.0, result.grandTotal!!, 1e-9)
    }

    @Test
    fun serviceWithoutPriceBlocksSavingInsteadOfCountingAsZero() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura"), historyRepository = historyRepository)
        viewModel.toggleService("paint")

        val result = viewModel.currentResult()
        assertTrue(result.missingServicePrice)
        assertTrue(result.selectedServices.isEmpty())

        viewModel.saveCurrentQuote()
        assertTrue(historyRepository.savedQuotes.value.isEmpty())

        viewModel.setServicePrice("paint", "25")
        viewModel.saveCurrentQuote()
        assertEquals(25.0, historyRepository.savedQuotes.value.single().services.single().price)
    }

    @Test
    fun reopeningAQuoteUsesTheSavedServicePriceNotTheCurrentCatalog() {
        val historyRepository = QuoteHistoryRepository()
        val serviceRepository = ServiceRepository()
        serviceRepository.add(Service(id = "paint", name = "Pintura", suggestedPrice = 20.0))
        val viewModel = QuoteViewModel(
            FilamentRepository(), PrinterRepository(), SettingsRepository(), serviceRepository,
            SalesChannelRepository(), historyRepository,
        )
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setQuantity("3")
        viewModel.toggleService("paint")
        viewModel.setServicePrice("paint", "40")
        viewModel.setServiceChargedPerOrder("paint", true)
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        serviceRepository.update(Service(id = "paint", name = "Pintura", suggestedPrice = 99.0))
        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        assertEquals(ServiceInput(name = "Pintura", priceText = "40", chargedPerOrder = true), viewModel.input.value.selectedServices["paint"])
        viewModel.saveCurrentQuote()
        assertEquals(saved.totalWithServices, historyRepository.savedQuotes.value.single().totalWithServices, 1e-9)
    }

    @Test
    fun reopeningKeepsServicePriceWithMoreThanTwoDecimals() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(Service(id = "paint", name = "Pintura"), historyRepository = historyRepository)
        viewModel.setQuantity("1000")
        viewModel.toggleService("paint")
        viewModel.setServicePrice("paint", "2,345")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        viewModel.loadForEditing(saved)
        viewModel.saveCurrentQuote()

        assertEquals(2.345, historyRepository.savedQuotes.value.single().services.single().price)
    }

    @Test
    fun reopeningKeepsShippingAndPartDataWithoutRounding() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setLengthMeters("12,3456")
        viewModel.setShippingCost("19,999")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        assertEquals("12,3456", viewModel.input.value.prints.first().filaments.first().lengthText)
        assertEquals("19,999", viewModel.input.value.shippingCostText)
        viewModel.saveCurrentQuote()
        assertEquals(saved.totalWithServices, historyRepository.savedQuotes.value.single().totalWithServices)
    }

    @Test
    fun serviceRemovedFromCatalogStaysInAReopenedQuote() {
        val historyRepository = QuoteHistoryRepository()
        val serviceRepository = ServiceRepository()
        serviceRepository.add(Service(id = "paint", name = "Pintura", suggestedPrice = 20.0))
        val viewModel = QuoteViewModel(
            FilamentRepository(), PrinterRepository(), SettingsRepository(), serviceRepository,
            SalesChannelRepository(), historyRepository,
        )
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.toggleService("paint")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        serviceRepository.delete("paint")
        viewModel.loadForEditing(saved)
        viewModel.saveCurrentQuote()

        assertEquals(saved.services, historyRepository.savedQuotes.value.single().services)
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
        assertEquals(0.0, direct.channelFeeRate)

        viewModel.selectSalesChannel("shopee")
        val viaShopee = currentQuote()

        assertEquals(0.15, viaShopee.channelFeeRate)
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

        assertEquals("red", result.prints.first().filaments.first().color?.id)
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

        assertEquals("blue", result.prints.first().filaments.first().color?.id)
        assertEquals("blue", result.quote?.prints?.first()?.job?.filaments?.first()?.color?.id)
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

        assertEquals("blue", result.prints.first().filaments.first().color?.id)
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

        assertEquals(null, viewModel.input.value.prints.first().filaments.first().colorId)
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

        assertEquals("12", viewModel.input.value.prints.first().filaments.first().lengthText)
        assertEquals("3h10", viewModel.input.value.prints.first().printTimeText)
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
    fun editingInTheQuoteTabLeavesTheTabDraftAlone() {
        // O App usa um ViewModel pra aba e outro pra edição (decisões 108 e 113): editar um pedido salvo
        // no Orçamento não pode apagar o rascunho que estava lá.
        val historyRepository = QuoteHistoryRepository()
        val tab = viewModelWith(historyRepository = historyRepository)
        tab.setSaveName("Pedido salvo")
        tab.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()
        tab.setLengthMeters("30")
        tab.setSaveName("Rascunho")
        val draft = tab.input.value

        val editing = viewModelWith(historyRepository = historyRepository)
        editing.loadForEditing(saved)
        editing.setSaveName("Pedido corrigido")
        assertTrue(editing.hasUnsavedEdits)
        assertTrue(editing.saveCurrentQuote())
        assertEquals(QuoteKind.ORDER, editing.saveForm.value.operation?.originKind)
        editing.resetForm()

        assertEquals("Pedido corrigido", historyRepository.savedQuotes.value.single().name)
        assertEquals(draft, tab.input.value)
        assertEquals("Rascunho", tab.saveForm.value.name)
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
        assertEquals("12", viewModel.input.value.prints.first().filaments.first().lengthText)

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

        assertEquals(settings, viewModel.input.value.prints.single().settings)
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
        // Salvar limpa o formulário (clicar duas vezes não cria dois pedidos): o segundo é preenchido de novo.
        repeat(2) {
            viewModel.selectPrinter(printer.id)
            viewModel.setLengthMeters("12")
            viewModel.setPrintTimeMinutes("120")
            assertTrue(viewModel.saveCurrentQuote())
        }
        val (approved, editing) = historyRepository.savedQuotes.value
        historyRepository.updateStatus(approved.id, com.threedreport.core.model.OrderStatus.APROVADO)
        historyRepository.updateStatus(editing.id, com.threedreport.core.model.OrderStatus.EM_IMPRESSAO)

        val ahead = viewModel.queueAheadOf(printer, historyRepository.savedQuotes.value, editingQuoteId = editing.id)

        assertEquals(1, ahead?.queuedQuoteCount)
        assertEquals(120.0, ahead?.queuedMinutes)
        assertEquals(null, viewModel.queueAheadOf(printer, emptyList(), editingQuoteId = null), "fila vazia não mostra dica")
    }

    /** Linhas de um G-code real do Bambu Studio 02.03 (ver `RealGCodeFixtures` no módulo core). */
    private val bambuX1cGCode = """
        ; BambuStudio 02.03.01.51
        ; model printing time: 4m 48s; total estimated time: 11m 36s
        ; total filament length [mm] : 325.49
        ; filament_colour = #00AE42
        ; filament_type = PLA
        ; filament_vendor = (Undefined)
        ; printer_model = Bambu Lab X1 Carbon
        ; printer_settings_id = Bambu Lab X1 Carbon 0.4 nozzle
    """.trimIndent()

    private fun gcodeFile(text: String, name: String = "peca.gcode") = com.threedreport.app.platform.PickedFile(name, text.encodeToByteArray())

    @Test
    fun importingAGCodePicksTheMatchingPrinterAndFilamentAndUndoGivesThemBack() {
        val printerRepository = PrinterRepository()
        val filamentRepository = FilamentRepository()
        val originalPrinter = printerRepository.printers.value.first()
        val x1c = originalPrinter.copy(id = "x1c", name = "Bambu Lab X1 Carbon")
        printerRepository.add(x1c)
        val green = FilamentColor(id = "verde", name = "Verde", hex = "#00AA40")
        val pla = Filament(id = "pla-verde", name = "PLA Verde", pricePerKg = 120.0, densityGPerCm3 = 1.24, materialType = "PLA", colors = listOf(FilamentColor(id = "preto", hex = "#000000"), green))
        filamentRepository.filaments.value.forEach { filamentRepository.delete(it.id) }
        filamentRepository.add(pla)
        val viewModel = QuoteViewModel(filamentRepository, printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectPrinter(originalPrinter.id)

        viewModel.importGCode(gcodeFile(bambuX1cGCode))

        val input = viewModel.input.value
        assertEquals("x1c", input.prints.first().printerId)
        assertEquals("pla-verde", input.prints.first().filaments.first().filamentId)
        assertEquals("verde", input.prints.first().filaments.first().colorId)
        // 11 min 36 s, arredondado pro minuto como o campo de tempo aceita.
        assertEquals("12", input.prints.first().printTimeText)
        assertTrue(input.prints.first().gcodeImportMessage!!.contains("Impressora: Bambu Lab X1 Carbon"))

        viewModel.undoGCodeImport()

        assertEquals(originalPrinter.id, viewModel.input.value.prints.first().printerId)
        assertEquals("", viewModel.input.value.prints.first().printTimeText)
    }

    @Test
    fun aSimilarPrinterIsOnlyMentionedNotSelected() {
        val printerRepository = PrinterRepository()
        val current = printerRepository.printers.value.first()
        printerRepository.add(current.copy(id = "x1", name = "Bambu Lab X1"))
        val viewModel = QuoteViewModel(FilamentRepository(), printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectPrinter(current.id)

        viewModel.importGCode(gcodeFile(bambuX1cGCode))

        assertEquals(current.id, viewModel.input.value.prints.first().printerId)
        assertTrue(viewModel.input.value.prints.first().gcodeImportMessage!!.contains("a mais parecida cadastrada é \"Bambu Lab X1\""))
    }

    @Test
    fun binaryGCodeIsRejectedWithAWayOut() {
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())

        viewModel.importGCode(gcodeFile("GCDE binário", name = "peca.bgcode"))

        assertTrue(viewModel.input.value.prints.first().gcodeImportMessage!!.contains("binário"))
        assertEquals("", viewModel.input.value.prints.first().filaments.first().lengthText)
    }

    @Test
    fun laborTimeCountsAsMissingOnlyWhenNoMinutesWereInformed() {
        assertTrue(QuoteInputState().isLaborTimeMissing)
        assertTrue(QuoteInputState(laborMinutesText = "0").isLaborTimeMissing)
        assertFalse(QuoteInputState(laborMinutesText = "15").isLaborTimeMissing)
    }

    @Test
    fun laborTimeIsTheWholeOrderAndIsNotMultipliedByQuantity() {
        SettingsRepository().let { it.update(it.settings.value.copy(laborRatePerHour = 60.0)) }
        val viewModel = viewModelWith()
        viewModel.setQuantity("10")
        viewModel.setLaborMinutes("90")

        val quote = viewModel.currentResult().quote!!

        assertEquals(90.0, quote.costs.labor, 1e-9)
        assertEquals(90.0, quote.laborMinutes, 1e-9)
    }

    @Test
    fun productIgnoresShippingButKeepsTheAnnouncedPrice() {
        val viewModel = viewModelWith()
        viewModel.setShippingCost("15")
        viewModel.setTargetTotal("30")
        val asOrder = viewModel.currentResult()

        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setTargetTotal("30")
        val asProduct = viewModel.currentResult()

        assertEquals(15.0, asOrder.shippingCost)
        assertEquals(0.0, asProduct.shippingCost)
        // Em produto, o preço fechado é o anunciado no catálogo (decisão 102), sem o frete descontado.
        assertEquals(30.0, asProduct.quote!!.salePrice, 1e-9)
        assertEquals(15.0, asOrder.quote!!.salePrice, 1e-9)
    }

    @Test
    fun savingAsProductDropsClientShippingAndDeadlineAndKeepsTheKindForTheNextOne() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setClientName("Maria")
        viewModel.setShippingCost("15")
        viewModel.setDeliveryDate(20_700L)

        viewModel.saveCurrentQuote()

        val product = historyRepository.savedQuotes.value.single()
        assertEquals(QuoteKind.PRODUCT, product.kind)
        assertNull(product.client)
        assertEquals(0.0, product.shippingCost)
        assertNull(product.deliveryDateEpochDay)
        assertTrue(viewModel.saveForm.value.savedAsProduct)
        assertEquals(QuoteKind.PRODUCT, viewModel.input.value.kind)
    }

    @Test
    fun editingAProductReopensItAsAProduct() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()
        viewModel.resetForm()

        viewModel.loadForEditing(product)
        viewModel.saveCurrentQuote()

        assertEquals(QuoteKind.PRODUCT, viewModel.input.value.kind)
        assertEquals(QuoteKind.PRODUCT, historyRepository.savedQuotes.value.single().kind)
    }

    @Test
    fun sellingAProductCreatesAnOrderThatPointsBackAndLeavesTheProductAlone() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setSaveName("Chaveiro")
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()

        viewModel.sellFromProduct(product)

        assertEquals(QuoteKind.ORDER, viewModel.input.value.kind)
        assertEquals("Chaveiro", viewModel.saveForm.value.soldFromProductName)
        assertEquals("Chaveiro", viewModel.saveForm.value.name)
        assertNull(viewModel.saveForm.value.editingQuoteId)

        viewModel.setClientName("Maria")
        viewModel.saveCurrentQuote()

        val order = historyRepository.savedQuotes.value.first { it.id != product.id }
        assertTrue(order.isOrder)
        assertEquals(product.id, order.sourceProductId)
        assertEquals("Maria", order.client?.name)
        assertEquals(product, historyRepository.savedQuotes.value.first { it.id == product.id })
    }

    @Test
    fun copyingANegotiatedOrderToTheCatalogUsesTheTablePriceAndLeavesTheOrderAlone() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setTargetTotal("20")
        viewModel.setShippingCost("15")
        viewModel.setClientName("Maria")
        assertTrue(viewModel.saveCurrentQuote())
        val order = historyRepository.savedQuotes.value.single()
        assertTrue(order.quote.isNegotiated)

        viewModel.copyToCatalog(order)

        assertEquals(QuoteKind.PRODUCT, viewModel.input.value.kind)
        assertEquals("", viewModel.input.value.targetTotalText)
        assertEquals("", viewModel.saveForm.value.clientName)
        viewModel.saveCurrentQuote()

        val product = historyRepository.savedQuotes.value.first { it.id != order.id }
        assertEquals(QuoteKind.PRODUCT, product.kind)
        assertFalse(product.quote.isNegotiated)
        assertNull(product.client)
        assertEquals(order, historyRepository.savedQuotes.value.first { it.id == order.id })
    }

    @Test
    fun announcedPriceBecomesTheProductPriceAndSellingCarriesIt() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setTargetTotal("25")
        val result = viewModel.currentResult()
        assertEquals(25.0, result.quote!!.salePrice, 1e-9)
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()
        assertTrue(product.quote.isNegotiated)

        viewModel.sellFromProduct(product)

        assertEquals("", viewModel.input.value.targetTotalText)
        assertEquals(25.0, viewModel.input.value.announcedUnitPrice!!, 1e-9)
        viewModel.saveCurrentQuote()
        val order = historyRepository.savedQuotes.value.first { it.id != product.id }
        assertEquals(25.0, order.quote.salePrice, 1e-9)
    }

    @Test
    fun showcaseSuggestionIsTheNextValueEndingIn90() {
        val viewModel = viewModelWith()

        assertEquals(18.90, viewModel.showcasePriceSuggestion(18.37)!!, 1e-9)
        assertEquals(19.90, viewModel.showcasePriceSuggestion(18.95)!!, 1e-9)
        assertEquals(0.90, viewModel.showcasePriceSuggestion(0.10)!!, 1e-9)
        assertNull(viewModel.showcasePriceSuggestion(18.90))

        viewModel.applyShowcasePrice(18.90)
        assertEquals("18,9", viewModel.input.value.targetTotalText)
    }

    @Test
    fun productCategoryIsSavedRestoredOnEditAndSuggestedLater() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setCategory("Chaveiros")
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()

        viewModel.loadForEditing(product)

        assertEquals("Chaveiros", viewModel.saveForm.value.category)
        assertEquals(listOf("Chaveiros"), viewModel.knownCategories(historyRepository.savedQuotes.value))
    }

    @Test
    fun sellingAnAnnouncedProductAddsShippingOnTopInsteadOfTakingItFromThePiece() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setTargetTotal("50")
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()

        viewModel.sellFromProduct(product)
        viewModel.setShippingCost("15")
        viewModel.setQuantity("2")
        val result = viewModel.currentResult()

        assertEquals(100.0, result.quote!!.salePrice, 1e-9)
        assertEquals(115.0, result.grandTotal!!, 1e-9)

        viewModel.setTargetTotal("90")
        assertEquals(75.0, viewModel.currentResult().quote!!.salePrice, 1e-9, "preço fechado digitado vence o anunciado")
    }

    @Test
    fun switchingKindClearsTheClosedPrice() {
        val viewModel = viewModelWith()
        viewModel.setShippingCost("20")
        viewModel.setTargetTotal("100")

        viewModel.setKind(QuoteKind.PRODUCT)

        assertEquals("", viewModel.input.value.targetTotalText)
        assertFalse(viewModel.currentResult().quote!!.isNegotiated)

        viewModel.setTargetTotal("30")
        viewModel.setKind(QuoteKind.PRODUCT)
        assertEquals("30", viewModel.input.value.targetTotalText, "escolher o mesmo tipo de novo não apaga nada")
    }

    /** Decisão 110: sem perfil de uso, todo orçamento novo começa como pedido; produto se escolhe na tela. */
    @Test
    fun aNewQuoteStartsAsAnOrderAndStartingOverGoesBackToIt() {
        val viewModel = viewModelWith()
        assertEquals(QuoteKind.ORDER, viewModel.input.value.kind)

        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.resetForm()

        assertEquals(QuoteKind.ORDER, viewModel.input.value.kind)
    }

    @Test
    fun duplicatingASaleFromTheCatalogKeepsItsOrigin() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()
        viewModel.sellFromProduct(product)
        viewModel.saveCurrentQuote()
        val order = historyRepository.savedQuotes.value.first { it.isOrder }

        viewModel.duplicateForNewQuote(order)
        viewModel.saveCurrentQuote()

        val reprint = historyRepository.savedQuotes.value.first { it.isOrder && it.id != order.id }
        assertEquals(product.id, reprint.sourceProductId)
    }

    @Test
    fun sellingAtTheAnnouncedPriceIsMarkedAndSurvivesAnEdit() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setKind(QuoteKind.PRODUCT)
        viewModel.setTargetTotal("25")
        viewModel.saveCurrentQuote()
        val product = historyRepository.savedQuotes.value.single()

        viewModel.sellFromProduct(product)
        viewModel.saveCurrentQuote()
        val order = historyRepository.savedQuotes.value.first { it.isOrder }
        assertTrue(order.soldAtCatalogPrice)
        assertFalse(order.isNegotiatedWithClient)

        viewModel.loadForEditing(order)
        assertEquals("", viewModel.input.value.targetTotalText)
        viewModel.saveCurrentQuote()
        val edited = historyRepository.savedQuotes.value.first { it.id == order.id }
        assertTrue(edited.soldAtCatalogPrice)
        assertEquals(25.0, edited.quote.salePrice, 1e-9)

        viewModel.loadForEditing(edited)
        viewModel.setTargetTotal("22")
        viewModel.saveCurrentQuote()
        val negotiated = historyRepository.savedQuotes.value.first { it.id == order.id }
        assertFalse(negotiated.soldAtCatalogPrice, "outro preço digitado é negociação de verdade")
        assertTrue(negotiated.isNegotiatedWithClient)
    }

    // --- Multicolor (decisão 105) ---

    @Test
    fun addFilamentCopiesTheLastRowsFilamentAndRemoveFilamentRemovesARowButNeverTheLast() {
        val filamentRepository = FilamentRepository()
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, colors = listOf(FilamentColor(id = "unica")))
        filamentRepository.add(pla)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("pla")
        viewModel.selectFilamentColor("unica")

        viewModel.addFilament()

        val rows = viewModel.input.value.prints.first().filaments
        assertEquals(2, rows.size)
        assertEquals("pla", rows[1].filamentId)
        assertEquals(null, rows[1].colorId)
        assertEquals("", rows[1].lengthText)

        viewModel.removeFilament(rows[0].id)
        assertEquals(1, viewModel.input.value.prints.first().filaments.size)
        assertEquals("pla", viewModel.input.value.prints.first().filaments.single().filamentId)

        viewModel.removeFilament(viewModel.input.value.prints.first().filaments.single().id)
        assertEquals(1, viewModel.input.value.prints.first().filaments.size, "a última linha não pode sair")
    }

    /**
     * Encontrado rodando o app: sem nada escolhido, a linha única aparece como o primeiro filamento
     * em estoque. Ao adicionar a segunda, esse padrão não vale mais, e a linha 1 trocava sozinha pra
     * "Escolha o filamento". O que a tela mostrava tem que virar escolha de verdade.
     */
    @Test
    fun addingASecondRowKeepsTheFilamentTheFirstRowWasShowing() {
        val filamentRepository = FilamentRepository()
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        val shown = viewModel.calculate(filamentRepository.filaments.value.filter { it.hasStockAvailable }, PrinterRepository().printers.value, SettingsRepository().settings.value, emptyList(), viewModel.input.value)
            .prints.single().filaments.single().filament
        assertNotNull(shown)

        viewModel.addFilament()

        val rows = viewModel.input.value.prints.single().filaments
        assertEquals(listOf(shown.id, shown.id), rows.map { it.filamentId })
    }

    @Test
    fun calculateWithTwoFilamentRowsPricesMaterialPerFilament() {
        val filamentRepository = FilamentRepository()
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        val petg = Filament(id = "petg", name = "PETG", pricePerKg = 200.0, densityGPerCm3 = 1.27)
        filamentRepository.add(pla)
        filamentRepository.add(petg)
        val printerRepository = PrinterRepository()
        val printer = printerRepository.printers.value.first()
        val viewModel = QuoteViewModel(filamentRepository, printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        val input = QuoteInputState(
            prints = listOf(
                PrintInput(
                    printerId = printer.id,
                    filaments = listOf(FilamentInput(filamentId = "pla", lengthText = "12"), FilamentInput(filamentId = "petg", lengthText = "12")),
                    printTimeText = "190",
                ),
            ),
        )

        val result = viewModel.calculate(filamentRepository.filaments.value, printerRepository.printers.value, viewModel.settings.value, viewModel.services.value, input)

        val expected = PricingCalculator.calculate(
            job = PrintJob(filaments = listOf(FilamentUsage(pla, 12.0), FilamentUsage(petg, 12.0)), printTimeMinutes = 190.0),
            printer = printer,
            settings = viewModel.settings.value,
        )
        assertEquals(expected.costs.material, result.quote!!.costs.material, 1e-9)
    }

    @Test
    fun calculateWithTwoRowsAndOneWithoutAChosenFilamentYieldsNoQuote() {
        val filamentRepository = FilamentRepository()
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        filamentRepository.add(pla)
        val printerRepository = PrinterRepository()
        val printer = printerRepository.printers.value.first()
        val viewModel = QuoteViewModel(filamentRepository, printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        val input = QuoteInputState(
            prints = listOf(
                PrintInput(
                    printerId = printer.id,
                    filaments = listOf(FilamentInput(filamentId = "pla", lengthText = "12"), FilamentInput(filamentId = null, lengthText = "5")),
                    printTimeText = "190",
                ),
            ),
        )

        val result = viewModel.calculate(filamentRepository.filaments.value, printerRepository.printers.value, viewModel.settings.value, viewModel.services.value, input)

        assertEquals(null, result.quote, "linha sem filamento escolhido não pode virar preço em silêncio")
    }

    /** Cabeçalho real do Bambu Studio 01.10.01.50 (A1, dois PLA e torre de purga, decisão 105). */
    private val bambuA1TwoColorsGCode = """
        ; BambuStudio 01.10.01.50
        ; model printing time: 4h 6m 55s; total estimated time: 4h 12m 53s
        ; total filament length [mm] : 9035.47,11467.79
        ; total filament weight [g] : 27.38,34.75
        ; enable_prime_tower = 1
        ; filament_colour = #00AE42;#FFFF00
        ; filament_type = PLA;PLA
        ; filament_vendor = "Bambu Lab";"Bambu Lab"
        ; printer_model = Bambu Lab A1
        ; printer_settings_id = Bambu Lab A1 0.4 nozzle
    """.trimIndent()

    @Test
    fun importingAMulticolorGCodeCreatesOneRowPerExtruderAndUndoRestoresTheSingleRow() {
        val printerRepository = PrinterRepository()
        printerRepository.add(printerRepository.printers.value.first().copy(id = "a1", name = "Bambu Lab A1"))
        val filamentRepository = FilamentRepository()
        filamentRepository.filaments.value.forEach { filamentRepository.delete(it.id) }
        val green = FilamentColor(id = "verde", hex = "#00AE42")
        val yellow = FilamentColor(id = "amarelo", hex = "#FFFF00")
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, brand = "Bambu Lab", materialType = "PLA", colors = listOf(green, yellow))
        filamentRepository.add(pla)
        val viewModel = QuoteViewModel(filamentRepository, printerRepository, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())

        viewModel.importGCode(gcodeFile(bambuA1TwoColorsGCode))

        val imported = viewModel.input.value.prints.first()
        assertEquals("a1", imported.printerId)
        assertEquals(2, imported.filaments.size)
        assertEquals("pla", imported.filaments[0].filamentId)
        assertEquals("verde", imported.filaments[0].colorId)
        assertEquals("9,04", imported.filaments[0].lengthText)
        assertEquals("pla", imported.filaments[1].filamentId)
        assertEquals("amarelo", imported.filaments[1].colorId)
        assertEquals("11,47", imported.filaments[1].lengthText)
        assertTrue(imported.gcodeImportMessage!!.startsWith("Preenchido a partir do G-code"))
        assertTrue(imported.gcodeImportMessage.contains("Um filamento por extrusor"))

        viewModel.undoGCodeImport()

        val restored = viewModel.input.value.prints.first()
        assertEquals(1, restored.filaments.size)
        assertEquals(null, restored.filaments.single().filamentId)
        assertEquals("", restored.filaments.single().lengthText)
        assertEquals(null, restored.printerId)
    }

    /** Revisão de código: o Ctrl+S calculava com os filamentos esgotados também, e salvava outro preço. */
    @Test
    fun ctrlSSavesWhatTheScreenShowsAndNotAnOutOfStockFilament() {
        val filamentRepository = FilamentRepository()
        filamentRepository.filaments.value.forEach { filamentRepository.delete(it.id) }
        val soldOut = Filament(id = "esgotado", name = "PLA caro", pricePerKg = 500.0, densityGPerCm3 = 1.24, colors = listOf(FilamentColor(id = "x", inStock = false)))
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
        filamentRepository.add(soldOut)
        filamentRepository.add(pla)
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        viewModel.saveCurrentQuote()

        assertEquals("pla", historyRepository.savedQuotes.value.single().quote.prints.single().job.filaments.single().filament.id)
    }

    /** Revisão de código: sem o consumo de cada extrusor, o import juntava as linhas que a pessoa separou. */
    @Test
    fun aGCodeWithoutPerExtruderLengthsKeepsTheRowsThePersonSplit() {
        val filamentRepository = FilamentRepository()
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        val first = filamentRepository.filaments.value.first().id
        viewModel.selectFilament(first)
        viewModel.setLengthMeters("3")
        viewModel.addFilament()
        viewModel.setLengthMeters("1", rowId = viewModel.input.value.prints.single().filaments[1].id)
        val before = viewModel.input.value.prints.single().filaments

        // Três comprimentos pra dois tipos: não dá pra saber de quem é cada um.
        viewModel.importGCode(gcodeFile("; filament used [mm] = 1000, 500, 250\n; filament_type = PLA;PETG\n; estimated printing time (normal mode) = 1h\n"))

        val print = viewModel.input.value.prints.single()
        assertEquals(before, print.filaments)
        assertEquals("1h", print.printTimeText)
        assertTrue(print.gcodeImportMessage!!.contains("mantive as suas linhas de filamento"))
    }

    /** Revisão de código: quatro slots do mesmo PLA, sem consumo por slot, ainda são um filamento só. */
    @Test
    fun severalSlotsOfTheSameFilamentWithoutLengthsStillChooseIt() {
        val filamentRepository = FilamentRepository()
        filamentRepository.filaments.value.forEach { filamentRepository.delete(it.id) }
        val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, materialType = "PLA")
        val petg = Filament(id = "petg", name = "PETG", pricePerKg = 100.0, densityGPerCm3 = 1.27, materialType = "PETG")
        filamentRepository.add(petg)
        filamentRepository.add(pla)
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        viewModel.selectFilament("petg")

        viewModel.importGCode(gcodeFile("; filament used [mm] = 1000, 500\n; filament_type = PLA;PLA;PLA;PLA\n"))

        val row = viewModel.input.value.prints.single().filaments.single()
        assertEquals("pla", row.filamentId)
        assertEquals("1,5", row.lengthText)
    }

    /** Revisão de código: canal excluído e recriado ganha id novo; reabrir perdia a taxa em silêncio. */
    @Test
    fun reopeningFindsTheChannelByNameWhenTheIdChangedAndWarnsWhenItIsGone() {
        val channelRepository = SalesChannelRepository()
        channelRepository.add(SalesChannel(id = "shopee-antigo", name = "Shopee", feeRate = 0.2))
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), channelRepository, historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.selectSalesChannel("shopee-antigo")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        channelRepository.delete("shopee-antigo")
        channelRepository.add(SalesChannel(id = "shopee-novo", name = "Shopee", feeRate = 0.2))
        viewModel.loadForEditing(saved)
        assertEquals("shopee-novo", viewModel.input.value.salesChannelId)
        assertEquals(null, viewModel.input.value.missingChannelName)

        channelRepository.delete("shopee-novo")
        viewModel.loadForEditing(saved)
        assertEquals(null, viewModel.input.value.salesChannelId)
        assertEquals("Shopee", viewModel.input.value.missingChannelName)

        viewModel.selectSalesChannel(null)
        assertEquals(null, viewModel.input.value.missingChannelName)
    }

    @Test
    fun savingAndReopeningATwoRowQuoteRestoresBothRows() {
        val filamentRepository = FilamentRepository()
        val pla = Filament(
            id = "pla",
            name = "PLA",
            pricePerKg = 100.0,
            densityGPerCm3 = 1.24,
            colors = listOf(FilamentColor(id = "verde", hex = "#00AE42"), FilamentColor(id = "amarelo", hex = "#FFFF00")),
        )
        filamentRepository.add(pla)
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(filamentRepository, PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.selectFilament("pla")
        viewModel.selectFilamentColor("verde")
        viewModel.setLengthMeters("9.04")
        viewModel.addFilament()
        val second = viewModel.input.value.prints.single().filaments[1].id
        viewModel.selectFilamentColor("amarelo", rowId = second)
        viewModel.setLengthMeters("11.47", rowId = second)
        viewModel.setPrintTimeMinutes("190")

        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        viewModel.resetForm()
        viewModel.loadForEditing(saved)

        val rows = viewModel.input.value.prints.first().filaments
        assertEquals(2, rows.size)
        assertEquals("pla", rows[0].filamentId)
        assertEquals("verde", rows[0].colorId)
        assertEquals("9,04", rows[0].lengthText)
        assertEquals("pla", rows[1].filamentId)
        assertEquals("amarelo", rows[1].colorId)
        assertEquals("11,47", rows[1].lengthText)
    }

    @Test
    fun reopeningAndSavingWithoutChangesKeepsTheSavedPriceEvenIfCostsChanged() {
        val historyRepository = QuoteHistoryRepository()
        val settingsRepository = SettingsRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), settingsRepository, ServiceRepository(), SalesChannelRepository(), historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        assertTrue(viewModel.saveCurrentQuote())
        val saved = historyRepository.savedQuotes.value.single()

        // A energia subiu depois do orçamento enviado.
        settingsRepository.update(settingsRepository.settings.value.copy(energyPricePerKwh = 5.0))
        viewModel.loadForEditing(saved)
        val reopened = viewModel.currentResult()
        assertTrue(reopened.keepsOriginalPrice)
        assertEquals(saved.quote, reopened.quote)
        assertTrue(reopened.todaysQuote!!.salePrice > saved.quote.salePrice, "a tela mostra quanto sairia hoje")

        viewModel.setClientContact("(11) 90000-0000")
        assertTrue(viewModel.hasUnsavedEdits)
        assertTrue(viewModel.saveCurrentQuote())

        assertEquals(saved.quote, historyRepository.savedQuotes.value.single().quote, "corrigir o contato não reprecifica")
    }

    @Test
    fun changingAPriceInputRecalculatesAndShowsTheOldPrice() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()

        viewModel.loadForEditing(saved)
        viewModel.setLengthMeters("24")

        val result = viewModel.currentResult()
        assertFalse(result.keepsOriginalPrice)
        assertEquals(saved.quote, result.originalQuote)
        assertTrue(result.quote!!.salePrice > saved.quote.salePrice)
    }

    @Test
    fun negativeShippingIsAFieldErrorAndBlocksSavingWithoutCrashing() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setShippingCost("-10")

        val result = viewModel.currentResult()
        assertEquals("Frete não pode ser negativo.", result.fieldErrors[QuoteFields.SHIPPING])
        assertNull(result.quote)

        assertFalse(viewModel.saveCurrentQuote())
        assertNotNull(viewModel.saveForm.value.blockedMessage)
        assertTrue(historyRepository.savedQuotes.value.isEmpty())
    }

    @Test
    fun aQuantityThatIsNotAWholeNumberIsAnErrorInsteadOfBecomingOne() {
        val viewModel = viewModelWith()
        viewModel.setQuantity("2,5")

        assertNotNull(viewModel.currentResult().fieldErrors[QuoteFields.QUANTITY])
    }

    @Test
    fun savingTwiceInARowCreatesOneOrderAndGivesItANumber() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)

        assertTrue(viewModel.saveCurrentQuote())
        assertFalse(viewModel.saveCurrentQuote(), "o formulário foi limpo: o segundo clique não salva nada")

        val saved = historyRepository.savedQuotes.value.single()
        assertEquals(1, saved.number)
        assertEquals("#0001", viewModel.saveForm.value.savedNumber)
    }

    @Test
    fun theQuoteIsSavedInTheCurrencyInUse() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(
            FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository,
            currency = { com.threedreport.core.model.Currency.USD },
        )
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.saveCurrentQuote()

        assertEquals(com.threedreport.core.model.Currency.USD, historyRepository.savedQuotes.value.single().currency)
    }

    @Test
    fun theClientGoesToTheRegistryAndDuplicatingDoesNotCopyIt() {
        val historyRepository = QuoteHistoryRepository()
        val clients = com.threedreport.app.data.ClientRepository()
        val viewModel = QuoteViewModel(
            FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), historyRepository,
            clientRepository = clients,
        )
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.setClientName("Maria")
        viewModel.saveCurrentQuote()

        val saved = historyRepository.savedQuotes.value.single()
        assertEquals(clients.clients.value.single().id, saved.client?.id)
        assertEquals(listOf("Maria"), viewModel.clientSuggestions("mar", clients.clients.value).map { it.name })

        viewModel.duplicateForNewQuote(saved)
        assertEquals("", viewModel.saveForm.value.clientName)
    }

    @Test
    fun typingTheWeightFillsTheLengthWithTheFilamentDensity() {
        val viewModel = viewModelWith()
        val filament = viewModel.currentResult().prints.first().filaments.first().filament!!

        viewModel.setWeightGrams("100")

        val meters = viewModel.input.value.prints.first().filaments.first().lengthText
        assertEquals(100.0, filament.weightGrams(com.threedreport.app.ui.format.parseDecimal(meters, com.threedreport.app.ui.format.NumberKind.MEASURE)!!), 0.01)
    }

    @Test
    fun aDraftIsDetectedSoOtherActionsCanAskBeforeReplacingIt() {
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository())
        assertFalse(viewModel.hasDraft)

        viewModel.setLengthMeters("12")
        assertTrue(viewModel.hasDraft)

        viewModel.resetForm()
        assertFalse(viewModel.hasDraft)
    }

    @Test
    fun droppingAFileThatIsNotGCodeExplainsInsteadOfReadingIt() {
        val viewModel = viewModelWith()

        viewModel.importDropped(com.threedreport.app.platform.PickResult.Picked(com.threedreport.app.platform.PickedFile("video.mp4", ByteArray(0))))

        assertNotNull(viewModel.input.value.prints.first().gcodeImportMessage)
        assertEquals("12", viewModel.input.value.prints.first().filaments.first().lengthText, "nada do orçamento mudou")
    }

    /** Revisão do PR #2: um preço fechado abaixo de serviços e frete salvava outro total, sem aviso. */
    @Test
    fun aClosedPriceBelowServicesAndShippingIsAFieldError() {
        val historyRepository = QuoteHistoryRepository()
        val viewModel = viewModelWith(historyRepository = historyRepository)
        viewModel.setShippingCost("25")
        viewModel.setTargetTotal("20")

        val result = viewModel.currentResult()
        assertNotNull(result.fieldErrors[QuoteFields.TARGET])
        assertNull(result.quote)
        assertFalse(viewModel.saveCurrentQuote())

        viewModel.setTargetTotal("40")
        assertEquals(40.0, viewModel.currentResult().quote!!.customerTotal, 1e-9)
    }

    /** Revisão do PR #2: com o canal excluído, mexer no preço recalculava sem a taxa, só com um aviso. */
    @Test
    fun aDeletedChannelBlocksRecalculatingUntilAnotherIsChosen() {
        val channelRepository = SalesChannelRepository()
        channelRepository.add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.2))
        val historyRepository = QuoteHistoryRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), channelRepository, historyRepository)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")
        viewModel.selectSalesChannel("shopee")
        viewModel.saveCurrentQuote()
        val saved = historyRepository.savedQuotes.value.single()
        channelRepository.delete("shopee")

        viewModel.loadForEditing(saved)
        assertEquals(saved.quote, viewModel.currentResult().quote, "sem mudança, o preço salvo continua valendo")

        viewModel.setLengthMeters("24")
        val changed = viewModel.currentResult()
        assertNull(changed.quote)
        assertTrue(changed.errorMessage!!.contains("Shopee"))
        assertFalse(viewModel.saveCurrentQuote())

        viewModel.selectSalesChannel(null)
        assertEquals(0.0, viewModel.currentResult().quote!!.channelFeeRate)
    }
}
