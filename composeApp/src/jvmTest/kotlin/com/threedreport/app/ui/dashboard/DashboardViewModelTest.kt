package com.threedreport.app.ui.dashboard

import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.appDataDir
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteSummary
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.report.CatalogReport
import com.threedreport.core.report.QuoteReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O estado único do Dashboard: o modo (com venda, só orçamento em aberto, nada salvo), o catálogo de quem ainda não
 * vende, a troca de período e o acompanhamento dos repositórios. Os números são os do `core`, sem conta no ViewModel.
 */
class DashboardViewModelTest {

    // Unconfined: o estado acompanha os repositórios na hora, sem esperar relógio nenhum.
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    /** O "agora" dos repositórios; os testes de período o recuam pra gravar uma venda antiga. */
    private var nowMillis = System.currentTimeMillis()

    private lateinit var history: QuoteHistoryRepository
    private lateinit var settings: SettingsRepository

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toFile().path)
        // Só depois de definir a pasta: o repositório lê a pasta ao ser criado.
        val storage = LocalStorage(appDataDir(), Clock { nowMillis })
        history = storage.quoteHistory()
        settings = storage.settings()
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

    private fun viewModel() = DashboardViewModel(history, settings, scope)

    private fun saveOrder(name: String, status: OrderStatus = OrderStatus.ORCADO) {
        val saved = history.save(name = name, quote = quote, services = emptyList(), photo = null, sourceLink = null)
        if (status != OrderStatus.ORCADO) history.updateStatus(saved.id, status)
    }

    private fun saveProduct(name: String) {
        history.save(name = name, quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
    }

    // ---- estado vazio de cada tipo

    @Test
    fun withNothingSavedTheModeIsNothingSavedAndThereIsNoCatalog() {
        val state = viewModel().uiState.value

        assertEquals(DashboardMode.NOTHING_SAVED, state.mode)
        assertEquals(QuoteSummary.EMPTY, state.summary)
        assertNull(state.catalog)
        assertEquals(PeriodPreset.ALL, state.period, "começa em Tudo")
    }

    @Test
    fun anOrderStillQuotedIsOpenNotASale() {
        saveOrder("Chaveiro")

        val state = viewModel().uiState.value

        assertEquals(DashboardMode.NO_SALES, state.mode)
        assertEquals(0, state.summary.quoteCount)
        assertEquals(1, state.summary.openQuoteCount)
        assertEquals(quote.salePrice, state.summary.openQuoteTotal, 1e-9)
    }

    @Test
    fun anApprovedOrderIsASaleAndTheNumbersAreTheCoreOnes() {
        saveOrder("Chaveiro", OrderStatus.APROVADO)

        val state = viewModel().uiState.value

        assertEquals(DashboardMode.SALES, state.mode)
        assertEquals(QuoteReport.summarize(history.savedQuotes.value), state.summary)
        assertEquals(1, state.summary.quoteCount)
        assertEquals(quote.salePrice, state.summary.totalSalePrice, 1e-9)
        assertEquals(listOf("Chaveiro"), state.summary.topProducts.map { it.name })
        assertNotNull(state.summary.profitPerPrintHour)
    }

    @Test
    fun aCancelledOrderIsNeitherASaleNorOpen() {
        saveOrder("Chaveiro", OrderStatus.CANCELADO)

        assertEquals(DashboardMode.NOTHING_SAVED, viewModel().uiState.value.mode)
    }

    // ---- catálogo de quem ainda não vende (decisão 103)

    @Test
    fun productsOnlyShowTheCatalogInTheNothingSavedMode() {
        saveProduct("Porta-celular")
        saveProduct("Vaso")

        val state = viewModel().uiState.value

        assertEquals(DashboardMode.NOTHING_SAVED, state.mode, "produto não é pedido: não conta como orçamento em aberto")
        val catalog = assertNotNull(state.catalog)
        assertEquals(2, catalog.productCount)
        assertEquals(CatalogReport.summarize(history.savedQuotes.value), catalog)
    }

    @Test
    fun theCatalogAlsoShowsWhenThereAreOnlyOpenQuotes() {
        saveProduct("Porta-celular")
        saveOrder("Chaveiro")

        val state = viewModel().uiState.value

        assertEquals(DashboardMode.NO_SALES, state.mode)
        assertEquals(1, state.catalog?.productCount)
    }

    @Test
    fun theCatalogLeavesTheScreenOnceThereIsASale() {
        saveProduct("Porta-celular")
        saveOrder("Chaveiro", OrderStatus.APROVADO)

        val state = viewModel().uiState.value

        assertEquals(DashboardMode.SALES, state.mode)
        assertNull(state.catalog, "o catálogo é do Dashboard sem vendas")
    }

    // ---- período

    @Test
    fun changingThePeriodRecutsTheSummary() {
        saveOrder("Chaveiro", OrderStatus.APROVADO)
        val viewModel = viewModel()

        viewModel.setPeriod(PeriodPreset.LAST_MONTH)
        assertEquals(PeriodPreset.LAST_MONTH, viewModel.uiState.value.period)
        assertEquals(DashboardMode.NOTHING_SAVED, viewModel.uiState.value.mode, "a venda de agora não é do mês passado")
        assertEquals(QuoteSummary.EMPTY, viewModel.uiState.value.summary)

        viewModel.setPeriod(PeriodPreset.LAST_7_DAYS)
        assertEquals(DashboardMode.SALES, viewModel.uiState.value.mode)
        assertEquals(1, viewModel.uiState.value.summary.quoteCount)

        viewModel.setPeriod(PeriodPreset.ALL)
        assertEquals(PeriodPreset.ALL, viewModel.uiState.value.period)
    }

    @Test
    fun anOldSaleCountsOnlyWhereItsDateFalls() {
        val now = nowMillis
        nowMillis = now - 400L * DAY_MILLIS
        saveOrder("Antigo", OrderStatus.APROVADO)
        nowMillis = now
        saveOrder("Recente", OrderStatus.APROVADO)
        val viewModel = viewModel()

        assertEquals(2, viewModel.uiState.value.summary.quoteCount)

        viewModel.setPeriod(PeriodPreset.THIS_YEAR)
        assertTrue(viewModel.uiState.value.summary.quoteCount <= 1, "a venda de mais de um ano atrás sai do período")

        viewModel.setPeriod(PeriodPreset.LAST_30_DAYS)
        assertEquals(listOf("Recente"), viewModel.uiState.value.summary.topProducts.map { it.name })
    }

    @Test
    fun theCatalogDoesNotDependOnThePeriod() {
        saveProduct("Porta-celular")
        val viewModel = viewModel()

        viewModel.setPeriod(PeriodPreset.LAST_MONTH)

        assertEquals(1, viewModel.uiState.value.catalog?.productCount)
    }

    // ---- acompanha os repositórios

    @Test
    fun theStateFollowsTheQuotesAndTheLaborRate() {
        val viewModel = viewModel()
        assertEquals(DashboardMode.NOTHING_SAVED, viewModel.uiState.value.mode)
        assertEquals(0.0, viewModel.uiState.value.laborRatePerHour)

        saveOrder("Chaveiro", OrderStatus.APROVADO)
        assertEquals(DashboardMode.SALES, viewModel.uiState.value.mode)

        settings.update(settings.settings.value.copy(laborRatePerHour = 50.0))
        assertEquals(50.0, viewModel.uiState.value.laborRatePerHour)
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
