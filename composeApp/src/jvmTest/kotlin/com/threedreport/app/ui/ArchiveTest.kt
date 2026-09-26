package com.threedreport.app.ui

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.services.ServiceListViewModel
import com.threedreport.core.model.Filament
import com.threedreport.core.model.Service
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Arquivar um cadastro em uso (decisão 115). */
class ArchiveTest {

    // Criados só depois de apontar a pasta de dados pra uma temporária (ver MultiPrintQuoteTest).
    private lateinit var filaments: FilamentRepository
    private lateinit var printers: PrinterRepository
    private lateinit var history: QuoteHistoryRepository

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        filaments = FilamentRepository()
        printers = PrinterRepository()
        history = QuoteHistoryRepository()
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun quoteViewModel() =
        QuoteViewModel(filaments, printers, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), history)

    private val petg = Filament(id = "petg", name = "PETG", pricePerKg = 130.0, densityGPerCm3 = 1.27)

    /** Salva um pedido com o primeiro filamento e a primeira impressora do cadastro. */
    private fun saveOrder() = quoteViewModel().apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
        assertTrue(saveCurrentQuote())
    }

    @Test
    fun usageCountsTheOrdersThatUseTheFilament() {
        saveOrder()
        saveOrder()
        val viewModel = FilamentListViewModel(filaments, history.savedQuotes)
        val used = filaments.filaments.value.first().id
        filaments.add(petg)

        assertEquals(2, viewModel.usageCount(used))
        assertEquals(0, viewModel.usageCount(petg.id))
    }

    @Test
    fun editingAnArchivedFilamentKeepsItArchived() {
        val viewModel = FilamentListViewModel(filaments, history.savedQuotes)
        filaments.add(petg)
        viewModel.setArchived(petg.id, true)

        viewModel.startEdit(filaments.filaments.value.single { it.id == petg.id })
        viewModel.updateForm { it.copy(pricePerKgText = "140") }
        viewModel.save()

        val saved = filaments.filaments.value.single { it.id == petg.id }
        assertTrue(saved.archived)
        assertEquals(140.0, saved.pricePerKg)

        viewModel.setArchived(petg.id, false)
        assertFalse(filaments.filaments.value.single { it.id == petg.id }.archived)
    }

    @Test
    fun anArchivedFilamentOrPrinterIsNotPickedByItselfInANewQuote() {
        val first = filaments.filaments.value.first()
        filaments.add(petg)
        FilamentListViewModel(filaments).setArchived(first.id, true)
        val firstPrinter = printers.printers.value.first()
        printers.add(firstPrinter.copy(id = "outra", name = "Outra"))
        printers.update(firstPrinter.copy(archived = true))

        val viewModel = quoteViewModel()
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        val result = viewModel.currentResult()
        val chosen = assertNotNull(result.prints.single().filaments.single().filament)
        assertTrue(chosen.id != first.id && !chosen.archived)
        assertEquals("outra", result.prints.single().printer?.id)
        assertTrue(viewModel.comparePrinters(filaments.filaments.value, printers.printers.value, SettingsRepository().settings.value, emptyList(), viewModel.input.value).isEmpty())
    }

    @Test
    fun reopeningAnOrderWithAnArchivedFilamentStillCalculates() {
        saveOrder()
        val saved = history.savedQuotes.value.single()
        val used = saved.quote.prints.single().job.filaments.single().filament.id
        FilamentListViewModel(filaments).setArchived(used, true)

        val viewModel = quoteViewModel()
        viewModel.loadForEditing(saved)
        viewModel.setLengthMeters("13")

        val result = viewModel.currentResult()
        assertNull(result.errorMessage, "arquivado não é \"saiu do cadastro\"")
        assertNotNull(result.quote)
        assertEquals(used, result.prints.single().filaments.single().filament?.id)
    }

    @Test
    fun servicesCountTheOrdersThatCharged() {
        val services = ServiceRepository()
        services.add(Service(id = "pintura", name = "Pintura", suggestedPrice = 20.0))
        val quote = QuoteViewModel(filaments, printers, SettingsRepository(), services, SalesChannelRepository(), history).apply {
            setLengthMeters("12")
            setPrintTimeMinutes("190")
            toggleService("pintura")
        }
        assertTrue(quote.saveCurrentQuote())

        assertEquals(1, ServiceListViewModel(services, history.savedQuotes).usageCount("pintura"))
    }
}
