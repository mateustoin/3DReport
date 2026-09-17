package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        job = PrintJob(
            filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = 190.0,
        ),
        filamentWeightGrams = 35.79,
        costs = CostBreakdown(3.58, 1.48, 0.54, 0.36, 0.36, 1.78, 0.0),
        productionCost = 8.09,
        salePrice = 16.19,
    )

    @Test
    fun toggleSelectionAddsAndRemoves() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        assertEquals(setOf("a", "b"), viewModel.selectedIds.value)

        viewModel.toggleSelection("a")
        assertEquals(setOf("b"), viewModel.selectedIds.value)
    }

    @Test
    fun clearSelectionEmptiesIt() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        viewModel.clearSelection()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun deletingASelectedQuoteRemovesItFromSelection() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), CurrencyRepository())
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.toggleSelection(saved.id)
        assertEquals(setOf(saved.id), viewModel.selectedIds.value)

        viewModel.delete(saved.id)

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun updateStatusChangesTheSavedQuoteStatus() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository(), CurrencyRepository())
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.updateStatus(saved.id, OrderStatus.PRONTO)

        assertEquals(OrderStatus.PRONTO, repository.savedQuotes.value.first { it.id == saved.id }.status)
    }

    @Test
    fun visibleQuotesFiltersByQueryMatchingNameOrClient() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())
        val byName = quote.let { SavedQuoteFixture.of(it, name = "Suporte de celular") }
        val byClient = quote.let { SavedQuoteFixture.of(it, name = "Vaso", client = Client(name = "João")) }
        val neither = quote.let { SavedQuoteFixture.of(it, name = "Chaveiro") }

        val filter = HistoryFilter(query = "joão")
        val visible = viewModel.visibleQuotes(listOf(byName, byClient, neither), filter)

        assertEquals(listOf(byClient), visible)
    }

    @Test
    fun visibleQuotesFiltersByStatus() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())
        val orcado = SavedQuoteFixture.of(quote, name = "A", status = OrderStatus.ORCADO)
        val entregue = SavedQuoteFixture.of(quote, name = "B", status = OrderStatus.ENTREGUE)

        val visible = viewModel.visibleQuotes(listOf(orcado, entregue), HistoryFilter(status = OrderStatus.ENTREGUE))

        assertEquals(listOf(entregue), visible)
    }

    @Test
    fun visibleQuotesFiltersByPeriod() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())
        val now = System.currentTimeMillis()
        val today = SavedQuoteFixture.of(quote, name = "Hoje", savedAtEpochMillis = now)
        val longAgo = SavedQuoteFixture.of(quote, name = "Antigo", savedAtEpochMillis = now - 60L * 24 * 60 * 60 * 1000)

        val visible = viewModel.visibleQuotes(listOf(today, longAgo), HistoryFilter(period = PeriodPreset.LAST_30_DAYS))

        assertEquals(listOf(today), visible)
    }

    @Test
    fun visibleQuotesAreSortedByMostRecentFirst() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository(), CurrencyRepository())
        val older = SavedQuoteFixture.of(quote, name = "Mais antigo", savedAtEpochMillis = 1_000L)
        val newer = SavedQuoteFixture.of(quote, name = "Mais novo", savedAtEpochMillis = 2_000L)

        val visible = viewModel.visibleQuotes(listOf(older, newer), HistoryFilter())

        assertEquals(listOf(newer, older), visible)
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
