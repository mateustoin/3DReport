package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
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
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        assertEquals(setOf("a", "b"), viewModel.selectedIds.value)

        viewModel.toggleSelection("a")
        assertEquals(setOf("b"), viewModel.selectedIds.value)
    }

    @Test
    fun clearSelectionEmptiesIt() {
        val viewModel = QuoteHistoryViewModel(QuoteHistoryRepository(), BrandingRepository())

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")
        viewModel.clearSelection()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun deletingASelectedQuoteRemovesItFromSelection() {
        val repository = QuoteHistoryRepository()
        val viewModel = QuoteHistoryViewModel(repository, BrandingRepository())
        val saved = repository.save(name = "Peça", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        viewModel.toggleSelection(saved.id)
        assertEquals(setOf(saved.id), viewModel.selectedIds.value)

        viewModel.delete(saved.id)

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }
}
