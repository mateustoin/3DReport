package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_undo
import _3dreport.composeapp.generated.resources.export_open_folder
import _3dreport.composeapp.generated.resources.history_error_photo_unreadable
import _3dreport.composeapp.generated.resources.history_notice_see_all
import _3dreport.composeapp.generated.resources.history_notice_see_orders
import _3dreport.composeapp.generated.resources.history_notice_status_moved
import _3dreport.composeapp.generated.resources.orders_deadline_late
import _3dreport.composeapp.generated.resources.orders_deadline_today
import _3dreport.composeapp.generated.resources.orders_status_approved
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OrdersView
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StatusChange
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.pricing.RepriceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * O estado único de Pedidos e do Catálogo: filtros, linhas, kanban, blocos de resumo, seleção, avisos de prazo
 * vencido, atualizar preço, detalhes e a visualização lembrada.
 */
class QuoteHistoryViewModelTest {

    private lateinit var dataDir: File

    // Unconfined: o estado acompanha os repositórios na hora, sem esperar relógio nenhum.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
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

    private fun viewModelOf(
        repository: QuoteHistoryRepository,
        platform: FakePlatform = FakePlatform(),
        clients: ClientRepository? = null,
        now: Long = 0L,
        kind: QuoteKind = QuoteKind.ORDER,
        filaments: FilamentRepository = FilamentRepository(),
        preferences: PreferencesRepository = PreferencesRepository(),
        today: () -> Long = { TODAY },
        currency: StateFlow<Currency> = MutableStateFlow(Currency.BRL),
    ) = QuoteHistoryViewModel(
        repository, BrandingRepository(), filaments, PrinterRepository(), SettingsRepository(), SalesChannelRepository(), preferences,
        today = today, clientRepository = clients, clock = { now }, platform = platform, scope = scope, kind = kind, currency = currency,
    )

    private fun QuoteHistoryRepository.saveOrder(
        name: String,
        deadline: Long? = null,
        status: OrderStatus = OrderStatus.ORCADO,
        currency: Currency = Currency.BRL,
    ): SavedQuote {
        val saved = save(name = name, quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = deadline, currency = currency)
        if (status != OrderStatus.ORCADO) updateStatus(saved.id, status)
        return savedQuotes.value.first { it.id == saved.id }
    }

    private fun QuoteHistoryRepository.saveProduct(name: String, category: String? = null, quote: Quote = this@QuoteHistoryViewModelTest.quote) =
        save(name = name, quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = category)

    /** O aviso emitido depois da última ação. */
    private fun QuoteHistoryViewModel.awaitNotice(): UiNotice = runBlocking { withTimeout(AWAIT_MILLIS) { notices.first() } }

    // ---- seleção

    @Test
    fun toggleSelectionAddsAndRemoves() {
        val viewModel = viewModelOf(QuoteHistoryRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        assertEquals(setOf("a", "b"), viewModel.uiState.value.selectedIds)

        viewModel.toggleSelection("a")
        assertEquals(setOf("b"), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun clearSelectionEmptiesItAndLeavesSelectionMode() {
        val viewModel = viewModelOf(QuoteHistoryRepository())

        viewModel.startSelection("a")
        assertTrue(viewModel.uiState.value.selectionMode)
        viewModel.toggleSelection("b")
        viewModel.clearSelection()

        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertFalse(viewModel.uiState.value.selectionMode)
    }

    @Test
    fun startingSelectionFromARowMarksIt() {
        val viewModel = viewModelOf(QuoteHistoryRepository())

        viewModel.startSelection("a")

        assertEquals(setOf("a"), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun deletingASelectedQuoteRemovesItFromSelection() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")

        viewModel.toggleSelection(saved.id)
        assertEquals(setOf(saved.id), viewModel.uiState.value.selectedIds)

        viewModel.delete(saved.id)

        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    // ---- andamento

    @Test
    fun updateStatusChangesTheSavedQuoteStatus() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")

        viewModel.updateStatus(saved.id, OrderStatus.PRONTO)

        assertEquals(OrderStatus.PRONTO, repository.savedQuotes.value.first { it.id == saved.id }.status)
        assertEquals(OrderStatus.PRONTO, viewModel.uiState.value.rows.single().quote.status)
    }

    // ---- aviso de prazo vencido

    @Test
    fun sendingAQuoteWithAnOverdueDeadlineIsHeldForConfirmation() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val overdue = repository.saveOrder("Vencido", deadline = TODAY - 1)

        viewModel.copyQuoteToClipboard(overdue)

        val pending = viewModel.uiState.value.pendingExport!!
        assertEquals(ClientExport.COPY, pending.export)
        assertEquals(listOf(overdue), pending.quotes)
        assertEquals(listOf("Vencido"), pending.overdue.map { it.name })
        assertNull(viewModel.uiState.value.copiedId, "segurado: nada foi copiado ainda")
    }

    @Test
    fun changingTheDateFromTheWarningOpensTheDeliveryDialogInsteadOfSending() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val overdue = repository.saveOrder("Vencido", deadline = TODAY - 1)
        viewModel.exportPdf(overdue)

        viewModel.changeDateOfPendingExport()
        assertEquals(overdue, viewModel.uiState.value.deliveryDateEditing)
        viewModel.saveDeliveryDate(107L)

        assertNull(viewModel.uiState.value.pendingExport)
        assertNull(viewModel.uiState.value.deliveryDateEditing)
        assertEquals(107L, repository.savedQuotes.value.first { it.id == overdue.id }.deliveryDateEpochDay)
    }

    @Test
    fun confirmingTheWarningCopiesTheHeldText() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform()
        val viewModel = viewModelOf(repository, platform)
        val overdue = repository.saveOrder("Vencido", deadline = TODAY - 1)
        viewModel.copyQuoteToClipboard(overdue)

        viewModel.confirmPendingExport()

        assertNull(viewModel.uiState.value.pendingExport)
        assertEquals(overdue.id, viewModel.uiState.value.copiedId)
        assertTrue(platform.copied.single().startsWith("Vencido"))
    }

    @Test
    fun batchExportIsHeldWhenAnySelectedQuoteIsOverdue() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val onTime = repository.saveOrder("No prazo", deadline = TODAY + 20)
        val overdue = repository.saveOrder("Vencido", deadline = TODAY - 1)
        viewModel.toggleSelection(onTime.id)
        viewModel.toggleSelection(overdue.id)

        viewModel.exportSelectedPdf()

        val pending = viewModel.uiState.value.pendingExport!!
        assertEquals(ClientExport.SELECTED_PDF, pending.export)
        assertEquals(listOf("Vencido"), pending.overdue.map { it.name })
        assertEquals(2, viewModel.uiState.value.selectedIds.size, "a seleção continua, pra poder cancelar e ajustar")

        viewModel.dismissPendingExport()
        assertNull(viewModel.uiState.value.pendingExport)
    }

    // ---- filtros

    @Test
    fun visibleQuotesFiltersByQueryMatchingNameOrClient() {
        val byName = SavedQuoteFixture.of(quote, name = "Suporte de celular")
        val byClient = SavedQuoteFixture.of(quote, name = "Vaso", client = Client(name = "João"))
        val neither = SavedQuoteFixture.of(quote, name = "Chaveiro")

        val visible = visibleQuotes(listOf(byName, byClient, neither), HistoryFilter(query = "joão"))

        assertEquals(listOf(byClient), visible)
    }

    @Test
    fun visibleQuotesFiltersByStatus() {
        val orcado = SavedQuoteFixture.of(quote, name = "A", status = OrderStatus.ORCADO)
        val entregue = SavedQuoteFixture.of(quote, name = "B", status = OrderStatus.ENTREGUE)

        val visible = visibleQuotes(listOf(orcado, entregue), HistoryFilter(status = OrderStatus.ENTREGUE))

        assertEquals(listOf(entregue), visible)
    }

    @Test
    fun visibleQuotesFiltersByPeriod() {
        val now = System.currentTimeMillis()
        val today = SavedQuoteFixture.of(quote, name = "Hoje", savedAtEpochMillis = now)
        val longAgo = SavedQuoteFixture.of(quote, name = "Antigo", savedAtEpochMillis = now - 60L * 24 * 60 * 60 * 1000)

        val visible = visibleQuotes(listOf(today, longAgo), HistoryFilter(period = PeriodPreset.LAST_30_DAYS))

        assertEquals(listOf(today), visible)
    }

    @Test
    fun visibleQuotesAreSortedByMostRecentFirst() {
        val older = SavedQuoteFixture.of(quote, name = "Mais antigo", savedAtEpochMillis = 1_000L)
        val newer = SavedQuoteFixture.of(quote, name = "Mais novo", savedAtEpochMillis = 2_000L)

        assertEquals(listOf(newer, older), visibleQuotes(listOf(older, newer), HistoryFilter()))
    }

    @Test
    fun theStateFollowsTheSearchAndTheStatusFilter() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        repository.saveOrder("Suporte")
        repository.saveOrder("Vaso", status = OrderStatus.PRONTO)

        viewModel.setSearchQuery("vaso")
        assertEquals(listOf("Vaso"), viewModel.uiState.value.rows.map { it.quote.name })

        viewModel.setSearchQuery("")
        viewModel.setStatusFilter(OrderStatus.PRONTO)
        assertEquals(listOf("Vaso"), viewModel.uiState.value.rows.map { it.quote.name })

        viewModel.setStatusFilter(null)
        viewModel.setPeriodFilter(PeriodPreset.LAST_7_DAYS)
        assertEquals(PeriodPreset.LAST_7_DAYS, viewModel.uiState.value.filter.period)
    }

    @Test
    fun eachScreenShowsOnlyItsKind() {
        val repository = QuoteHistoryRepository()
        val order = repository.saveOrder("Pedido")
        val product = repository.saveProduct("Chaveiro")
        val orders = viewModelOf(repository)
        val catalog = viewModelOf(repository, kind = QuoteKind.PRODUCT)

        assertEquals(listOf(order.id), orders.uiState.value.rows.map { it.quote.id })
        assertEquals(listOf(product.id), catalog.uiState.value.rows.map { it.quote.id })
    }

    @Test
    fun theHistoryOpensOnOrders() {
        assertEquals(QuoteKind.ORDER, viewModelOf(QuoteHistoryRepository()).uiState.value.filter.kind)
    }

    @Test
    fun theFirstTimeScreenIsToldApartFromAFilterWithNoMatches() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        assertFalse(viewModel.uiState.value.hasItems)

        repository.saveOrder("Suporte")
        viewModel.setSearchQuery("nada disso")

        assertTrue(viewModel.uiState.value.hasItems)
        assertTrue(viewModel.uiState.value.rows.isEmpty())
    }

    // ---- Catálogo

    @Test
    fun convertToOrderIsOnlyOfferedForAProductThatWasNeverSold() {
        val repository = QuoteHistoryRepository()
        val product = repository.saveProduct("Chaveiro")
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)

        assertTrue(viewModel.uiState.value.rows.single().canConvertToOrder)

        repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, sourceProductId = product.id)

        assertFalse(viewModel.uiState.value.rows.single().canConvertToOrder)
    }

    @Test
    fun onlyProductsThatWereNeverSoldCanBecomeOrders() {
        val repository = QuoteHistoryRepository()
        var shownOrders = 0
        val viewModel = QuoteHistoryViewModel(
            repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(),
            PreferencesRepository(), scope = scope, kind = QuoteKind.PRODUCT, showOrders = { shownOrders++ },
        )
        val product = repository.saveProduct("Chaveiro")

        viewModel.convertToOrder(product.id)

        assertTrue(repository.savedQuotes.value.single().isOrder)
        val notice = viewModel.awaitNotice()
        assertEquals(UiText.Res(Res.string.history_notice_see_orders), notice.actionLabel)
        notice.action!!.invoke()
        assertEquals(1, shownOrders)
    }

    @Test
    fun categoryFilterNarrowsProducts() {
        val repository = QuoteHistoryRepository()
        val keychain = repository.saveProduct("Chaveiro", category = "Chaveiros")
        val vase = repository.saveProduct("Vaso")
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)

        assertEquals(listOf("Chaveiros"), viewModel.uiState.value.categories)
        assertTrue(viewModel.uiState.value.hasUncategorized)

        viewModel.setCategoryFilter(CategoryFilter.Named("chaveiros"))
        assertEquals(listOf(keychain.id), viewModel.uiState.value.rows.map { it.quote.id })

        viewModel.setCategoryFilter(CategoryFilter.None)
        assertEquals(listOf(vase.id), viewModel.uiState.value.rows.map { it.quote.id })
    }

    @Test
    fun aCategoryThatNoLongerExistsFallsBackToAll() {
        val repository = QuoteHistoryRepository()
        val keychain = repository.saveProduct("Chaveiro", category = "Chaveiros")
        repository.saveProduct("Vaso", category = "Decoração")
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)
        viewModel.setCategoryFilter(CategoryFilter.Named("Chaveiros"))

        repository.delete(keychain.id)

        assertEquals(CategoryFilter.All, viewModel.uiState.value.filter.category)
        assertEquals(1, viewModel.uiState.value.rows.size)
    }

    /** Produto calculado com o filamento e a impressora padrão que o app cria na primeira execução. */
    private fun calculatedQuote(): Quote = PricingCalculator.calculate(
        job = PrintJob(filament = FilamentRepository().filaments.value.first(), filamentLengthMeters = 12.0, printTimeMinutes = 190.0),
        printer = PrinterRepository().printers.value.first(),
        settings = SettingsRepository().settings.value,
    )

    @Test
    fun productWithUnchangedCostsNeedsNoRepricing() {
        val repository = QuoteHistoryRepository()
        val product = repository.saveProduct("Chaveiro", quote = calculatedQuote())
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)

        assertFalse((viewModel.repriceFor(product) as RepriceResult.Repriced).changed)
        assertNull(viewModel.uiState.value.rows.single().reprice)
        assertFalse(viewModel.uiState.value.rows.single().canReprice)
    }

    @Test
    fun repricingShowsTheNewPriceAndOnlySavesOnConfirm() {
        val repository = QuoteHistoryRepository()
        val product = repository.saveProduct("Chaveiro", quote = calculatedQuote())
        val filamentRepository = FilamentRepository()
        val filament = filamentRepository.filaments.value.first()
        filamentRepository.update(filament.copy(pricePerKg = filament.pricePerKg * 2))
        val viewModel = viewModelOf(repository, filaments = filamentRepository, kind = QuoteKind.PRODUCT)

        val row = viewModel.uiState.value.rows.single()
        assertTrue(row.canReprice)
        assertIs<RepriceNotice.CostsChanged>(row.reprice)

        viewModel.startRepricing(product)
        val pending = viewModel.uiState.value.repricing!!
        assertTrue(pending.newQuote.salePrice > product.quote.salePrice)
        assertEquals(product.quote.salePrice, pending.priceBefore)
        assertTrue(pending.priceAfter > pending.priceBefore)

        viewModel.cancelRepricing()
        assertEquals(product.quote, repository.savedQuotes.value.single().quote)

        viewModel.startRepricing(product)
        viewModel.confirmRepricing()
        val updated = repository.savedQuotes.value.single()
        assertEquals(pending.newQuote, updated.quote)
        assertTrue(updated.lastEditedEpochMillis != null)
        assertNull(viewModel.uiState.value.repricing)
    }

    @Test
    fun aProductWhoseFilamentWasDeletedSaysItCannotBeRecalculated() {
        val repository = QuoteHistoryRepository()
        repository.saveProduct("Chaveiro", quote = calculatedQuote())
        val filamentRepository = FilamentRepository()
        filamentRepository.filaments.value.forEach { filamentRepository.delete(it.id) }
        val viewModel = viewModelOf(repository, filaments = filamentRepository, kind = QuoteKind.PRODUCT)

        val row = viewModel.uiState.value.rows.single()

        assertIs<RepriceNotice.Unavailable>(row.reprice)
        assertFalse(row.canReprice)
    }

    // ---- avisos

    @Test
    fun deleteOffersUndoThatBringsTheQuoteBack() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")

        viewModel.delete(saved.id)
        val notice = viewModel.awaitNotice()
        assertEquals(UiText.Res(Res.string.common_undo), notice.actionLabel)
        assertTrue(repository.savedQuotes.value.isEmpty())

        notice.action!!.invoke()
        assertEquals(listOf(saved.id), repository.savedQuotes.value.map { it.id })
    }

    @Test
    fun aStatusChangeThatLeavesTheFilteredListSaysWhereTheOrderWent() {
        val repository = QuoteHistoryRepository()
        val preferences = PreferencesRepository().also { it.setOrdersView(OrdersView.LIST) }
        val viewModel = viewModelOf(repository, preferences = preferences)
        val saved = repository.saveOrder("Peça")
        viewModel.setStatusFilter(OrderStatus.ORCADO)

        viewModel.updateStatus(saved.id, OrderStatus.APROVADO)

        val notice = viewModel.awaitNotice()
        assertEquals(
            UiText.Res(Res.string.history_notice_status_moved, listOf("Peça", UiText.Res(Res.string.orders_status_approved))),
            notice.message,
        )
        assertEquals(UiText.Res(Res.string.history_notice_see_all), notice.actionLabel)
        notice.action!!.invoke()
        assertNull(viewModel.uiState.value.filter.status, "Ver todos tira o filtro")
    }

    // ---- exportar

    @Test
    fun exportingSelectedOnlyTakesWhatTheListShowsAndCancellingKeepsTheSelection() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform()
        val viewModel = viewModelOf(repository, platform)
        val shown = repository.saveOrder("Suporte")
        val hidden = repository.saveOrder("Vaso")
        viewModel.toggleSelection(shown.id)
        viewModel.toggleSelection(hidden.id)
        viewModel.setSearchQuery("Suporte")

        assertEquals(listOf(shown.id), viewModel.selectedVisible().map { it.id })
        assertEquals(1, viewModel.uiState.value.selectedVisibleCount)

        // Cancelar o "Salvar como" não joga fora o que foi marcado.
        viewModel.exportSelectedPdf()
        assertEquals(setOf(shown.id, hidden.id), viewModel.uiState.value.selectedIds)

        platform.nextSaveLocation = "/tmp/orcamentos.pdf"
        viewModel.exportSelectedPdf()
        assertTrue(platform.written.getValue("/tmp/orcamentos.pdf").isNotEmpty())
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertFalse(viewModel.uiState.value.selectionMode)
        assertEquals(UiText.Res(Res.string.export_open_folder), viewModel.awaitNotice().actionLabel)
    }

    @Test
    fun theCatalogExportTakesEverythingTheFilterShowsWhenNothingIsMarked() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform().apply { nextSaveLocation = "/tmp/" }
        val viewModel = viewModelOf(repository, platform, kind = QuoteKind.PRODUCT)
        repository.saveProduct("Chaveiro", category = "Chaveiros")
        repository.saveProduct("Vaso", category = "Decoração")
        viewModel.setCategoryFilter(CategoryFilter.Named("Chaveiros"))

        viewModel.exportCatalogPdf()

        assertEquals(listOf("/tmp/Catálogo _1 itens_.pdf"), platform.written.keys.toList())
    }

    @Test
    fun whenTheBrowserDoesNotOpenTheWhatsAppTextIsCopiedInstead() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform().apply { browserWorks = false }
        val viewModel = viewModelOf(repository, platform)
        val saved = repository.saveOrder("Peça")

        viewModel.openInWhatsApp(saved)

        assertTrue(platform.copied.single().startsWith("Peça"))
        assertTrue(viewModel.awaitNotice().isError)
    }

    @Test
    fun aFailedCopyIsAnErrorNoticeAndASuccessfulOneMarksTheRow() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform().apply { clipboardWorks = false }
        val viewModel = viewModelOf(repository, platform)
        val saved = repository.saveOrder("Peça")

        viewModel.copyQuoteToClipboard(saved)
        assertTrue(viewModel.awaitNotice().isError)
        assertNull(viewModel.uiState.value.copiedId)

        platform.clipboardWorks = true
        viewModel.copyQuoteToClipboard(saved)
        assertEquals(saved.id, viewModel.uiState.value.copiedId)
        viewModel.clearCopied()
        assertNull(viewModel.uiState.value.copiedId)
    }

    // ---- kanban, tabela e blocos de resumo

    @Test
    fun theKanbanShowsOrdersInProgressFromAnyDateButOnlyRecentDeliveries() {
        val day = 24L * 60 * 60 * 1000
        val old = SavedQuoteFixture.of(quote, name = "Aprovado há meses").copy(savedAtEpochMillis = 0L, status = OrderStatus.APROVADO)
        val recent = SavedQuoteFixture.of(quote, name = "Entregue ontem").copy(
            status = OrderStatus.ENTREGUE,
            statusHistory = listOf(StatusChange(OrderStatus.ENTREGUE, 99 * day)),
        )
        val longAgo = SavedQuoteFixture.of(quote, name = "Entregue faz tempo").copy(
            status = OrderStatus.ENTREGUE,
            statusHistory = listOf(StatusChange(OrderStatus.ENTREGUE, 10 * day)),
        )
        val cancelled = SavedQuoteFixture.of(quote, name = "Cancelado").copy(status = OrderStatus.CANCELADO)
        val all = listOf(old, recent, longAgo, cancelled)

        val board = kanbanQuotes(all, HistoryFilter(period = PeriodPreset.LAST_7_DAYS), showAllDelivered = false, nowEpochMillis = 100 * day)
        assertEquals(setOf("Aprovado há meses", "Entregue ontem"), board.map { it.name }.toSet())

        val everything = kanbanQuotes(all, HistoryFilter(), showAllDelivered = true, nowEpochMillis = 100 * day)
        assertEquals(3, everything.size, "cancelado fica fora do quadro")
    }

    @Test
    fun theKanbanColumnsComeInFlowOrderWithTheirTotalsAndTheHiddenDeliveries() {
        val day = 24L * 60 * 60 * 1000
        val repository = QuoteHistoryRepository()
        repository.saveOrder("A")
        repository.saveOrder("B")
        repository.saveOrder("C", status = OrderStatus.PRONTO)
        repository.saveOrder("D", status = OrderStatus.CANCELADO)
        val old = repository.saveOrder("E", status = OrderStatus.ENTREGUE)
        val viewModel = viewModelOf(repository, now = old.deliveredAtEpochMillis!! + 60 * day)

        val state = viewModel.uiState.value

        assertEquals(OrderStatus.PIPELINE, state.columns.map { it.status })
        assertEquals(listOf(2, 0, 0, 1, 0), state.columns.map { it.cards.size })
        assertEquals(2 * quote.salePrice, state.columns.first().total.single().amount, 0.001)
        assertEquals(1, state.cancelledCount)
        assertEquals(1, state.hiddenDelivered)

        viewModel.setShowAllDelivered(true)
        assertEquals(1, viewModel.uiState.value.columns.last().cards.size)
        assertEquals(0, viewModel.uiState.value.hiddenDelivered)
    }

    @Test
    fun theOrdersTableIsSortedByDeadlineWithTheOnesWithoutDeadlineLast() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Sem prazo")
        repository.saveOrder("Daqui a dez dias", deadline = TODAY + 10)
        repository.saveOrder("Atrasado", deadline = TODAY - 2)
        repository.saveOrder("Amanhã", deadline = TODAY + 1)
        val viewModel = viewModelOf(repository)

        assertEquals(
            listOf("Atrasado", "Amanhã", "Daqui a dez dias", "Sem prazo"),
            viewModel.uiState.value.rows.map { it.quote.name },
        )
    }

    @Test
    fun theSummaryCountsOnlyOrdersInProgressAndFollowsTheDeadlineBadges() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Orçado hoje", deadline = TODAY)
        repository.saveOrder("Orçado vencido", deadline = TODAY - 3)
        repository.saveOrder("Imprimindo hoje", deadline = TODAY, status = OrderStatus.EM_IMPRESSAO)
        repository.saveOrder("Pronto atrasado", deadline = TODAY - 2, status = OrderStatus.PRONTO)
        repository.saveOrder("Aprovado no prazo", deadline = TODAY + 5, status = OrderStatus.APROVADO)
        repository.saveOrder("Entregue atrasado", deadline = TODAY - 9, status = OrderStatus.ENTREGUE)
        repository.saveOrder("Cancelado", deadline = TODAY, status = OrderStatus.CANCELADO)
        repository.saveProduct("Produto")
        val viewModel = viewModelOf(repository)

        val summary = viewModel.uiState.value.summary

        assertEquals(3, summary.inProgress, "Orçado não conta como em andamento")
        assertEquals(Currency.BRL, summary.receivable.single().currency)
        assertEquals(3 * quote.salePrice, summary.receivable.single().amount, 0.001)
        assertEquals(2, summary.dueToday)
        assertEquals(1, summary.overdue, "orçamento parado com prazo vencido não é atraso")
    }

    @Test
    fun theSummaryIgnoresTheSearch() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Suporte", status = OrderStatus.APROVADO)
        repository.saveOrder("Vaso", status = OrderStatus.APROVADO)
        val viewModel = viewModelOf(repository)

        viewModel.setSearchQuery("vaso")

        assertEquals(2, viewModel.uiState.value.summary.inProgress)
    }

    @Test
    fun theDeadlineBadgeTellsEachSituationApart() {
        val onTime = SavedQuoteFixture.of(quote, name = "A").copy(deliveryDateEpochDay = TODAY + 3)
        val today = onTime.copy(deliveryDateEpochDay = TODAY)
        val late = onTime.copy(deliveryDateEpochDay = TODAY - 2, status = OrderStatus.PRONTO)
        val stale = onTime.copy(deliveryDateEpochDay = TODAY - 2, status = OrderStatus.ORCADO)
        val done = late.copy(status = OrderStatus.ENTREGUE)

        assertEquals(DeadlineTone.ON_TIME, onTime.deadlineBadge(TODAY)?.tone)
        assertEquals(DeadlineBadge(UiText.Res(Res.string.orders_deadline_today), DeadlineTone.TODAY), today.deadlineBadge(TODAY))
        assertEquals(
            DeadlineBadge(UiText.Plural(Res.plurals.orders_deadline_late, 2, listOf(2)), DeadlineTone.LATE),
            late.deadlineBadge(TODAY),
        )
        assertEquals(DeadlineTone.STALE, stale.deadlineBadge(TODAY)?.tone)
        assertEquals(DeadlineTone.DONE, done.deadlineBadge(TODAY)?.tone)
        assertNull(onTime.copy(deliveryDateEpochDay = null).deadlineBadge(TODAY))
    }

    // ---- visualização lembrada

    @Test
    fun ordersOpenInKanbanAndTheChosenViewIsRememberedByTheNextViewModel() {
        val repository = QuoteHistoryRepository()
        assertEquals(OrdersView.KANBAN, viewModelOf(repository).uiState.value.ordersView)

        viewModelOf(repository).setOrdersView(OrdersView.LIST)

        assertEquals(OrdersView.LIST, viewModelOf(repository).uiState.value.ordersView)
        assertEquals(OrdersView.LIST, PreferencesRepository().preferences.value.ordersView)
    }

    @Test
    fun switchingTheViewLeavesSelectionMode() {
        val viewModel = viewModelOf(QuoteHistoryRepository())
        viewModel.startSelection("a")

        viewModel.setOrdersView(OrdersView.KANBAN)

        assertFalse(viewModel.uiState.value.selectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun theCatalogOpensInGridAndTheChosenViewIsRemembered() {
        val repository = QuoteHistoryRepository()
        assertEquals(CatalogView.GRID, viewModelOf(repository, kind = QuoteKind.PRODUCT).uiState.value.catalogView)

        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)
        viewModel.setCatalogView(CatalogView.LIST)
        assertEquals(CatalogView.LIST, viewModel.uiState.value.catalogView)

        assertEquals(CatalogView.LIST, viewModelOf(repository, kind = QuoteKind.PRODUCT).uiState.value.catalogView)
        assertEquals(OrdersView.KANBAN, viewModelOf(repository).uiState.value.ordersView, "uma escolha não mexe na outra")
    }

    @Test
    fun aPreferencesFileFromBeforeOpensBothScreensInTheirDefaultViews() {
        File(dataDir, "preferences.json").writeText(
            """{"data":{"autoBackupEnabled":false,"checkForUpdates":true},"updatedAtEpochMillis":1}""",
        )

        val state = viewModelOf(QuoteHistoryRepository()).uiState.value

        assertEquals(OrdersView.KANBAN, state.ordersView)
        assertEquals(CatalogView.GRID, state.catalogView)
    }

    // ---- detalhes e prazo

    @Test
    fun editingDetailsKeepsThePriceAndLinksTheClient() {
        val repository = QuoteHistoryRepository()
        val clients = ClientRepository()
        val viewModel = viewModelOf(repository, clients = clients)
        val saved = repository.saveOrder("Peça")

        viewModel.startEditingDetails(saved)
        viewModel.updateDetails { it.copy(name = "Peça corrigida", clientName = "Ana", clientContact = "(11) 90000-0000") }
        viewModel.saveDetails()

        val updated = repository.savedQuotes.value.single()
        assertEquals("Peça corrigida", updated.name)
        assertEquals(saved.quote, updated.quote)
        assertEquals(clients.clients.value.single().id, updated.client?.id)
        assertNull(viewModel.uiState.value.details)
    }

    @Test
    fun theDetailsFormStartsFromTheQuoteAndLeavesAnAutoNameBlank() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.save(name = "Orçamento - 30/09/2026 10:15", quote = quote, services = emptyList(), photo = null, sourceLink = "https://x.test/modelo")

        viewModel.startEditingDetails(saved)

        val form = viewModel.uiState.value.details!!.form
        assertEquals("", form.name)
        assertEquals("https://x.test/modelo", form.sourceLink)

        viewModel.cancelEditingDetails()
        assertNull(viewModel.uiState.value.details)
    }

    @Test
    fun aPhotoThatDoesNotOpenShowsTheErrorInsideTheDialogAndKeepsTheOldOne() {
        val repository = QuoteHistoryRepository()
        val platform = FakePlatform().apply { nextPick = PickResult.Picked(PickedFile("quebrada.png", byteArrayOf(1, 2, 3))) }
        val viewModel = viewModelOf(repository, platform)
        viewModel.startEditingDetails(repository.saveOrder("Peça"))

        viewModel.chooseDetailsPhoto()

        val editing = viewModel.uiState.value.details!!
        assertNull(editing.form.photo)
        assertEquals(UiText.Res(Res.string.history_error_photo_unreadable, listOf("quebrada.png")), editing.photoError)

        viewModel.updateDetails { it.copy(name = "Outro") }
        assertNull(viewModel.uiState.value.details!!.photoError, "digitar de novo limpa o aviso")
    }

    @Test
    fun printSettingsOpenForOneQuoteAndOnlyOnePrintCanBeEdited() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")
        assertTrue(viewModel.uiState.value.rows.single().canEditPrintSettings)

        viewModel.startEditingPrintSettings(saved)
        assertEquals(saved, viewModel.uiState.value.printSettingsEditing)
        viewModel.savePrintSettings(com.threedreport.core.model.PrintSettings(layerHeightMm = 0.2))
        viewModel.cancelEditingPrintSettings()

        assertEquals(0.2, repository.savedQuotes.value.single().printSettings?.layerHeightMm)
        assertNull(viewModel.uiState.value.printSettingsEditing)
    }

    // ---- revisão: ordem, resumo, moedas, dia, avisos e filtros

    @Test
    fun theOrdersTableListsOpenOrdersByDeadlineThenOpenWithoutDeadlineThenDeliveredAndCancelled() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Entregue", deadline = TODAY - 20, status = OrderStatus.ENTREGUE)
        repository.saveOrder("Sem prazo")
        repository.saveOrder("Cancelado", deadline = TODAY + 1, status = OrderStatus.CANCELADO)
        repository.saveOrder("Próximo", deadline = TODAY + 4, status = OrderStatus.APROVADO)
        repository.saveOrder("Atrasado", deadline = TODAY - 2, status = OrderStatus.PRONTO)
        repository.saveOrder("Hoje", deadline = TODAY)
        val viewModel = viewModelOf(repository)

        val names = viewModel.uiState.value.rows.map { it.quote.name }

        assertEquals(listOf("Atrasado", "Hoje", "Próximo", "Sem prazo"), names.take(4))
        assertEquals(setOf("Entregue", "Cancelado"), names.drop(4).toSet())
    }

    @Test
    fun closedOrdersComeLastFromTheMostRecentToTheOldest() {
        val old = SavedQuoteFixture.of(quote, name = "Antigo", status = OrderStatus.ENTREGUE, savedAtEpochMillis = 1_000L)
            .copy(deliveryDateEpochDay = TODAY - 30)
        val recent = SavedQuoteFixture.of(quote, name = "Recente", status = OrderStatus.CANCELADO, savedAtEpochMillis = 9_000L)
            .copy(deliveryDateEpochDay = TODAY - 1)
        val open = SavedQuoteFixture.of(quote, name = "Aberto", savedAtEpochMillis = 5_000L).copy(deliveryDateEpochDay = TODAY + 50)

        assertEquals(listOf("Aberto", "Recente", "Antigo"), listOf(old, recent, open).sortedByDeadline().map { it.name })
    }

    @Test
    fun eachKanbanColumnFollowsTheDeadlineOrder() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Sem prazo", status = OrderStatus.APROVADO)
        repository.saveOrder("Depois", deadline = TODAY + 9, status = OrderStatus.APROVADO)
        repository.saveOrder("Antes", deadline = TODAY + 1, status = OrderStatus.APROVADO)
        val viewModel = viewModelOf(repository)

        val approved = viewModel.uiState.value.columns.first { it.status == OrderStatus.APROVADO }

        assertEquals(listOf("Antes", "Depois", "Sem prazo"), approved.cards.map { it.quote.name })
    }

    @Test
    fun theSummaryLeavesOutQuotesButStillCountsTheirDeadlines() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Orçado", deadline = TODAY)
        repository.saveOrder("Aprovado", status = OrderStatus.APROVADO)
        val viewModel = viewModelOf(repository)

        val summary = viewModel.uiState.value.summary

        assertEquals(1, summary.inProgress)
        assertEquals(quote.salePrice, summary.receivable.single().amount, 0.001)
        assertEquals(1, summary.dueToday)
    }

    @Test
    fun totalsAreSummedPerCurrencyWithTheAppCurrencyFirst() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Real", status = OrderStatus.APROVADO)
        repository.saveOrder("Real 2", status = OrderStatus.APROVADO)
        repository.saveOrder("Dólar", status = OrderStatus.APROVADO, currency = Currency.USD)
        val currency = MutableStateFlow(Currency.BRL)
        val viewModel = viewModelOf(repository, currency = currency)

        val receivable = viewModel.uiState.value.summary.receivable
        assertEquals(listOf(Currency.BRL, Currency.USD), receivable.map { it.currency })
        assertEquals(2 * quote.salePrice, receivable[0].amount, 0.001)
        assertEquals(quote.salePrice, receivable[1].amount, 0.001)
        val column = viewModel.uiState.value.columns.first { it.status == OrderStatus.APROVADO }
        assertEquals(listOf(Currency.BRL, Currency.USD), column.total.map { it.currency })

        currency.value = Currency.USD

        assertEquals(listOf(Currency.USD, Currency.BRL), viewModel.uiState.value.summary.receivable.map { it.currency })
        assertEquals(Currency.USD, viewModel.uiState.value.currency)
    }

    @Test
    fun anEmptyColumnTotalsZeroInTheAppCurrency() {
        val viewModel = viewModelOf(QuoteHistoryRepository(), currency = MutableStateFlow(Currency.EUR))

        assertEquals(listOf(CurrencyTotal(Currency.EUR, 0.0)), viewModel.uiState.value.columns.first().total)
    }

    @Test
    fun theDayIsRefreshedWhenTheScreenIsShownAfterMidnight() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Peça", deadline = TODAY + 1, status = OrderStatus.APROVADO)
        var day = TODAY
        val viewModel = viewModelOf(repository, today = { day })
        assertEquals(DeadlineTone.ON_TIME, viewModel.uiState.value.rows.single().deadline?.tone)
        assertEquals(0, viewModel.uiState.value.summary.dueToday)

        day = TODAY + 1
        assertEquals(TODAY, viewModel.uiState.value.today, "sem a tela avisar, o estado fica no dia antigo")
        viewModel.onShown()

        assertEquals(TODAY + 1, viewModel.uiState.value.today)
        assertEquals(DeadlineTone.TODAY, viewModel.uiState.value.rows.single().deadline?.tone)
        assertEquals(1, viewModel.uiState.value.summary.dueToday)
    }

    @Test
    fun showingTheScreenOnTheSameDayDoesNotRebuildTheState() {
        val repository = QuoteHistoryRepository()
        repository.saveOrder("Peça")
        val viewModel = viewModelOf(repository)
        val before = viewModel.uiState.value

        viewModel.onShown()

        assertSame(before, viewModel.uiState.value)
    }

    @Test
    fun movingACardOnTheKanbanNeverSaysItLeftAFilteredList() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")
        viewModel.setStatusFilter(OrderStatus.ORCADO)

        viewModel.updateStatus(saved.id, OrderStatus.APROVADO)

        assertNull(runBlocking { withTimeoutOrNull(200) { viewModel.notices.first() } })
    }

    @Test
    fun cancellingOnTheKanbanOffersUndoEvenWithAStatusFilterSet() {
        val repository = QuoteHistoryRepository()
        val viewModel = viewModelOf(repository)
        val saved = repository.saveOrder("Peça")
        viewModel.setStatusFilter(OrderStatus.ORCADO)

        viewModel.updateStatus(saved.id, OrderStatus.CANCELADO)

        val notice = viewModel.awaitNotice()
        assertEquals(UiText.Res(Res.string.common_undo), notice.actionLabel)
        notice.action!!.invoke()
        assertEquals(OrderStatus.ORCADO, repository.savedQuotes.value.single().status)
    }

    @Test
    fun theUncategorizedFilterFallsBackToAllWhenNoProductIsLeftWithoutCategory() {
        val repository = QuoteHistoryRepository()
        repository.saveProduct("Chaveiro", category = "Chaveiros")
        val vase = repository.saveProduct("Vaso")
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)
        viewModel.setCategoryFilter(CategoryFilter.None)
        assertEquals(listOf(vase.id), viewModel.uiState.value.rows.map { it.quote.id })

        repository.delete(vase.id)

        assertEquals(CategoryFilter.All, viewModel.uiState.value.filter.category)
        assertEquals(1, viewModel.uiState.value.rows.size)
    }

    @Test
    fun changesThatOnlyTouchADialogKeepTheRowsAsTheyWere() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder("Peça")
        val viewModel = viewModelOf(repository)
        val before = viewModel.uiState.value.rows

        viewModel.startEditingDeliveryDate(saved)
        assertSame(before, viewModel.uiState.value.rows)
        viewModel.cancelEditingDeliveryDate()
        viewModel.startEditingDetails(saved)
        viewModel.updateDetails { it.copy(name = "Outro") }
        assertSame(before, viewModel.uiState.value.rows)

        viewModel.setSearchQuery("pe")
        assertNotSame(before, viewModel.uiState.value.rows)
    }

    @Test
    fun clearFiltersKeepsTheKindAndDropsEverythingElse() {
        val repository = QuoteHistoryRepository()
        repository.saveProduct("Chaveiro", category = "Chaveiros")
        val viewModel = viewModelOf(repository, kind = QuoteKind.PRODUCT)
        viewModel.setSearchQuery("xyz")
        viewModel.setCategoryFilter(CategoryFilter.Named("Chaveiros"))
        viewModel.setPeriodFilter(PeriodPreset.LAST_7_DAYS)
        assertTrue(viewModel.uiState.value.rows.isEmpty())

        viewModel.clearFilters()

        assertEquals(HistoryFilter(kind = QuoteKind.PRODUCT), viewModel.uiState.value.filter)
        assertEquals(1, viewModel.uiState.value.rows.size)
    }

    @Test
    fun selectAllVisibleMarksOnlyWhatTheFilterShows() {
        val repository = QuoteHistoryRepository()
        val shown = repository.saveOrder("Suporte")
        repository.saveOrder("Vaso")
        val viewModel = viewModelOf(repository)
        viewModel.setSearchQuery("sup")
        viewModel.startSelection()

        viewModel.selectAllVisible()

        assertEquals(setOf(shown.id), viewModel.uiState.value.selectedIds)
        assertEquals(1, viewModel.uiState.value.selectedVisibleCount)
    }
}

private const val AWAIT_MILLIS = 5_000L
private const val TODAY = 100L

private object SavedQuoteFixture {
    fun of(
        quote: Quote,
        name: String,
        client: Client? = null,
        status: OrderStatus = OrderStatus.ORCADO,
        savedAtEpochMillis: Long = 0L,
    ) = SavedQuote(
        id = name,
        name = name,
        quote = quote,
        client = client,
        status = status,
        savedAtEpochMillis = savedAtEpochMillis,
    )
}
