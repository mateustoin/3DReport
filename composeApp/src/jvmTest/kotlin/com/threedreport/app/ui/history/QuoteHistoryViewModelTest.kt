package com.threedreport.app.ui.history

import kotlin.test.assertFalse
import com.threedreport.core.model.QuoteKind
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Valida a seleção múltipla de orçamentos (usada pra exportar vários num PDF só). */
class QuoteHistoryViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = Quote(
        prints = listOf(
            QuotedPrint(
                job = PrintJob(
                    filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                    filamentLengthMeters = 12.0,
                    printTimeMinutes = 190.0,
                ),
                printerId = "printer",
                printerName = "Impressora",
                cost = PrintCost(material = 3.58, energy = 1.48, maintenance = 0.54, finishing = 0.36, investmentReturn = 1.78, fixedCost = 0.0),
            ),
        ),
        costs = CostBreakdown(
            material = 3.58, energy = 1.48, maintenance = 0.54, failures = 0.36, finishing = 0.36,
            investmentReturn = 1.78, administrative = 0.0, labor = 0.0, fixedCost = 0.0,
        ),
        salePrice = 16.19,
    )

    @Test
    fun toggleSelectionAddsAndRemoves() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        assertEquals(setOf("a", "b"), viewModel.selectedIds.value)

        viewModel.toggleSelection("a")
        assertEquals(setOf("b"), viewModel.selectedIds.value)
    }

    @Test
    fun clearSelectionEmptiesIt() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        viewModel.clearSelection()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun deletingASelectedQuoteRemovesItFromSelection() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.toggleSelection(saved.id)
        assertEquals(setOf(saved.id), viewModel.selectedIds.value)

        viewModel.delete(saved.id)

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun updateStatusChangesTheSavedQuoteStatus() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.updateStatus(saved.id, OrderStatus.PRONTO)

        assertEquals(OrderStatus.PRONTO, repository.savedQuotes.value.first { it.id == saved.id }.status)
    }

    @Test
    fun sendingAQuoteWithAnOverdueDeadlineIsHeldForConfirmation() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), today = { 100L })
        val overdue = repository.save(name = "Vencido", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 99L)

        viewModel.copyQuoteToClipboard(overdue)

        assertEquals(PendingExport(ClientExport.COPY, listOf(overdue)), viewModel.pendingExport.value)
        assertNull(viewModel.copiedId.value, "segurado: nada foi copiado ainda")
    }

    @Test
    fun changingTheDateFromTheWarningOpensTheDeliveryDialogInsteadOfSending() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), today = { 100L })
        val overdue = repository.save(name = "Vencido", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 99L)
        viewModel.exportPdf(overdue)

        viewModel.changeDateOfPendingExport()
        viewModel.saveDeliveryDate(107L)

        assertNull(viewModel.pendingExport.value)
        assertNull(viewModel.deliveryDateEditing.value)
        assertEquals(107L, repository.savedQuotes.value.first { it.id == overdue.id }.deliveryDateEpochDay)
    }

    @Test
    fun batchExportIsHeldWhenAnySelectedQuoteIsOverdue() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), today = { 100L })
        val onTime = repository.save(name = "No prazo", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 120L)
        val overdue = repository.save(name = "Vencido", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 99L)
        viewModel.toggleSelection(onTime.id)
        viewModel.toggleSelection(overdue.id)

        viewModel.exportSelectedPdf()

        assertEquals(ClientExport.SELECTED_PDF, viewModel.pendingExport.value?.export)
        assertEquals(2, viewModel.selectedIds.value.size, "a seleção continua, pra poder cancelar e ajustar")

        viewModel.dismissPendingExport()
        assertNull(viewModel.pendingExport.value)
    }

    @Test
    fun visibleQuotesFiltersByQueryMatchingNameOrClient() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val byName = quote.let { SavedQuoteFixture.of(it, name = "Suporte de celular") }
        val byClient = quote.let { SavedQuoteFixture.of(it, name = "Vaso", client = Client(name = "João")) }
        val neither = quote.let { SavedQuoteFixture.of(it, name = "Chaveiro") }

        val filter = HistoryFilter(query = "joão")
        val visible = viewModel.visibleQuotes(listOf(byName, byClient, neither), filter)

        assertEquals(listOf(byClient), visible)
    }

    @Test
    fun visibleQuotesFiltersByStatus() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val orcado = SavedQuoteFixture.of(quote, name = "A", status = OrderStatus.ORCADO)
        val entregue = SavedQuoteFixture.of(quote, name = "B", status = OrderStatus.ENTREGUE)

        val visible = viewModel.visibleQuotes(listOf(orcado, entregue), HistoryFilter(status = OrderStatus.ENTREGUE))

        assertEquals(listOf(entregue), visible)
    }

    @Test
    fun visibleQuotesFiltersByPeriod() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val now = System.currentTimeMillis()
        val today = SavedQuoteFixture.of(quote, name = "Hoje", savedAtEpochMillis = now)
        val longAgo = SavedQuoteFixture.of(quote, name = "Antigo", savedAtEpochMillis = now - 60L * 24 * 60 * 60 * 1000)

        val visible = viewModel.visibleQuotes(listOf(today, longAgo), HistoryFilter(period = PeriodPreset.LAST_30_DAYS))

        assertEquals(listOf(today), visible)
    }

    @Test
    fun visibleQuotesAreSortedByMostRecentFirst() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val older = SavedQuoteFixture.of(quote, name = "Mais antigo", savedAtEpochMillis = 1_000L)
        val newer = SavedQuoteFixture.of(quote, name = "Mais novo", savedAtEpochMillis = 2_000L)

        val visible = viewModel.visibleQuotes(listOf(older, newer), HistoryFilter())

        assertEquals(listOf(newer, older), visible)
    }
    @Test
    fun eachScreenShowsOnlyItsKind() {
        val repository = QuoteHistoryRepository()
        val order = repository.save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
        val orders = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())
        val catalog = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), kind = QuoteKind.PRODUCT)

        assertEquals(listOf(order.id), orders.visibleQuotes(repository.savedQuotes.value, orders.filter.value).map { it.id })
        assertEquals(listOf(product.id), catalog.visibleQuotes(repository.savedQuotes.value, catalog.filter.value).map { it.id })
    }

    @Test
    fun convertToOrderIsOnlyOfferedForAProductThatWasNeverSold() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository())

        assertTrue(viewModel.canConvertToOrder(product, viewModel.soldProductIds(repository.savedQuotes.value)))

        repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, sourceProductId = product.id)

        assertFalse(viewModel.canConvertToOrder(product, viewModel.soldProductIds(repository.savedQuotes.value)))
    }
    private fun historyViewModel(
        repository: QuoteHistoryRepository,
        filamentRepository: FilamentRepository = FilamentRepository(),
        kind: QuoteKind = QuoteKind.ORDER,
    ) = QuoteHistoryViewModel(repository, BrandingRepository(), filamentRepository, PrinterRepository(), SettingsRepository(), SalesChannelRepository(), kind = kind)

    /** Produto calculado com o filamento e a impressora padrão que o app cria na primeira execução. */
    private fun savedProduct(repository: QuoteHistoryRepository, category: String? = null): com.threedreport.core.model.SavedQuote {
        val filament = FilamentRepository().filaments.value.first()
        val printer = PrinterRepository().printers.value.first()
        val calculated = com.threedreport.core.pricing.PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 12.0, printTimeMinutes = 190.0),
            printer = printer,
            settings = SettingsRepository().settings.value,
        )
        return repository.save(name = "Chaveiro", quote = calculated, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = category)
    }

    @Test
    fun categoryFilterNarrowsProducts() {
        val repository = QuoteHistoryRepository()
        val keychain = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = "Chaveiros")
        val vase = repository.save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
        val viewModel = historyViewModel(repository, kind = QuoteKind.PRODUCT)

        assertEquals(listOf("Chaveiros"), viewModel.productCategories(repository.savedQuotes.value))

        viewModel.setCategoryFilter(CategoryFilter.Named("chaveiros"))
        assertEquals(listOf(keychain.id), viewModel.visibleQuotes(repository.savedQuotes.value, viewModel.filter.value).map { it.id })

        viewModel.setCategoryFilter(CategoryFilter.None)
        assertEquals(listOf(vase.id), viewModel.visibleQuotes(repository.savedQuotes.value, viewModel.filter.value).map { it.id })
    }

    @Test
    fun productWithUnchangedCostsNeedsNoRepricing() {
        val repository = QuoteHistoryRepository()
        val product = savedProduct(repository)

        val result = historyViewModel(repository).repriceFor(product)

        assertFalse((result as com.threedreport.core.pricing.RepriceResult.Repriced).changed)
    }

    @Test
    fun repricingShowsTheNewPriceAndOnlySavesOnConfirm() {
        val repository = QuoteHistoryRepository()
        val product = savedProduct(repository)
        val filamentRepository = FilamentRepository()
        val filament = filamentRepository.filaments.value.first()
        filamentRepository.update(filament.copy(pricePerKg = filament.pricePerKg * 2))
        val viewModel = historyViewModel(repository, filamentRepository)

        assertTrue((viewModel.repriceFor(product) as com.threedreport.core.pricing.RepriceResult.Repriced).changed)

        viewModel.startRepricing(product)
        val pending = viewModel.repricing.value!!
        assertTrue(pending.newQuote.salePrice > product.quote.salePrice)

        viewModel.cancelRepricing()
        assertEquals(product.quote, repository.savedQuotes.value.single().quote)

        viewModel.startRepricing(product)
        viewModel.confirmRepricing()
        val updated = repository.savedQuotes.value.single()
        assertEquals(pending.newQuote, updated.quote)
        assertTrue(updated.lastEditedEpochMillis != null)
        assertNull(viewModel.repricing.value)
    }

    @Test
    fun theHistoryOpensOnOrders() {
        val viewModel = viewModelWith(QuoteHistoryRepository())

        assertEquals(QuoteKind.ORDER, viewModel.filter.value.kind)
    }


    private fun viewModelWith(
        repository: QuoteHistoryRepository,
        platform: FakePlatform = FakePlatform(),
        clients: com.threedreport.app.data.ClientRepository? = null,
        now: Long = 0L,
    ) = QuoteHistoryViewModel(
        repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(),
        today = { 100L }, clientRepository = clients, clock = { now }, platform = platform,
    )

    @Test
    fun deleteOffersUndoThatBringsTheQuoteBack() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelWith(repository)
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.delete(saved.id)
        val notice = viewModel.notice.value!!
        assertEquals("Desfazer", notice.actionLabel)
        assertTrue(repository.savedQuotes.value.isEmpty())

        notice.action!!.invoke()
        assertEquals(listOf(saved.id), repository.savedQuotes.value.map { it.id })
    }

    @Test
    fun aStatusChangeThatLeavesTheFilteredListSaysWhereTheOrderWent() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelWith(repository)
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        viewModel.setStatusFilter(OrderStatus.ORCADO)

        viewModel.updateStatus(saved.id, OrderStatus.APROVADO)

        val notice = viewModel.notice.value!!
        assertTrue(notice.message.contains("Aprovado"))
        notice.action!!.invoke()
        assertNull(viewModel.filter.value.status, "\"Ver todos\" tira o filtro")
    }

    @Test
    fun exportingSelectedOnlyTakesWhatTheListShowsAndCancellingKeepsTheSelection() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform()
        val viewModel = viewModelWith(repository, platform)
        val shown = repository.save(name = "Suporte", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val hidden = repository.save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        viewModel.toggleSelection(shown.id)
        viewModel.toggleSelection(hidden.id)
        viewModel.setSearchQuery("Suporte")

        assertEquals(listOf(shown.id), viewModel.selectedVisible().map { it.id })

        // Cancelar o "Salvar como" não joga fora o que foi marcado.
        viewModel.exportSelectedPdf()
        assertEquals(setOf(shown.id, hidden.id), viewModel.selectedIds.value)

        platform.nextSaveLocation = "/tmp/orcamentos.pdf"
        viewModel.exportSelectedPdf()
        assertTrue(platform.written.getValue("/tmp/orcamentos.pdf").isNotEmpty())
        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertEquals("Abrir pasta", viewModel.notice.value?.actionLabel)
    }

    @Test
    fun whenTheBrowserDoesNotOpenTheWhatsAppTextIsCopiedInstead() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform().apply { browserWorks = false }
        val viewModel = viewModelWith(repository, platform)
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.openInWhatsApp(saved)

        assertTrue(platform.copied.single().startsWith("Peça"))
        assertTrue(viewModel.notice.value!!.isError)
    }

    @Test
    fun theKanbanShowsOrdersInProgressFromAnyDateButOnlyRecentDeliveries() {
        val repository = QuoteHistoryRepository()
        val day = 24L * 60 * 60 * 1000
        val viewModel = viewModelWith(repository, now = 100 * day)
        val old = SavedQuoteFixture.of(quote, name = "Aprovado há meses").copy(savedAtEpochMillis = 0L, status = OrderStatus.APROVADO)
        val recent = SavedQuoteFixture.of(quote, name = "Entregue ontem").copy(
            status = OrderStatus.ENTREGUE,
            statusHistory = listOf(com.threedreport.core.model.StatusChange(OrderStatus.ENTREGUE, 99 * day)),
        )
        val longAgo = SavedQuoteFixture.of(quote, name = "Entregue faz tempo").copy(
            status = OrderStatus.ENTREGUE,
            statusHistory = listOf(com.threedreport.core.model.StatusChange(OrderStatus.ENTREGUE, 10 * day)),
        )
        val cancelled = SavedQuoteFixture.of(quote, name = "Cancelado").copy(status = OrderStatus.CANCELADO)
        val all = listOf(old, recent, longAgo, cancelled)

        val board = viewModel.kanbanQuotes(all, HistoryFilter(period = PeriodPreset.LAST_7_DAYS), showAllDelivered = false, nowEpochMillis = 100 * day)
        assertEquals(setOf("Aprovado há meses", "Entregue ontem"), board.map { it.name }.toSet())

        val everything = viewModel.kanbanQuotes(all, HistoryFilter(), showAllDelivered = true, nowEpochMillis = 100 * day)
        assertEquals(3, everything.size, "cancelado fica fora do quadro")
    }

    @Test
    fun editingDetailsKeepsThePriceAndLinksTheClient() {
        val repository = QuoteHistoryRepository()
        val clients = com.threedreport.app.data.ClientRepository()
        val viewModel = viewModelWith(repository, clients = clients)
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.startEditingDetails(saved)
        viewModel.saveDetails(DetailsForm(name = "Peça corrigida", clientName = "Ana", clientContact = "(11) 90000-0000"))

        val updated = repository.savedQuotes.value.single()
        assertEquals("Peça corrigida", updated.name)
        assertEquals(saved.quote, updated.quote)
        assertEquals(clients.clients.value.single().id, updated.client?.id)
        assertNull(viewModel.detailsEditing.value)
    }

    @Test
    fun onlyProductsThatWereNeverSoldCanBecomeOrders() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelWith(repository)
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)

        viewModel.convertToOrder(product.id)

        val converted = repository.savedQuotes.value.single()
        assertTrue(converted.isOrder)
        assertEquals("Ver pedidos", viewModel.notice.value?.actionLabel)
    }
}

private object SavedQuoteFixture {
    fun of(
        quote: Quote,
        name: String,
        client: Client? = null,
        status: OrderStatus = OrderStatus.ORCADO,
        savedAtEpochMillis: Long = 0L,
    ) = com.threedreport.core.model.SavedQuote(
        id = name,
        name = name,
        quote = quote,
        client = client,
        status = status,
        savedAtEpochMillis = savedAtEpochMillis,
    )
}
