package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_open_folder
import _3dreport.composeapp.generated.resources.export_saved_pdf
import _3dreport.composeapp.generated.resources.export_whatsapp_copied
import _3dreport.composeapp.generated.resources.quote_notice_print_removed
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.OrderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Correções da revisão de código do Orçamento: avisos, textos do exportador e o que a tela lê pronto do estado. */
class QuoteReviewFixesTest {
    private lateinit var folder: String
    private lateinit var history: QuoteHistoryRepository
    private lateinit var printers: PrinterRepository
    private lateinit var platform: FakePlatform

    @BeforeTest
    fun setUp() {
        folder = createTempDirectory("3dreport-test").toString()
        System.setProperty("threedreport.dataDir", folder)
        history = QuoteHistoryRepository()
        printers = PrinterRepository()
        platform = FakePlatform().apply { nextSaveLocation = "$folder/" }
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel() = QuoteViewModel(
        FilamentRepository(), printers, SettingsRepository(), ServiceRepository(), SalesChannelRepository(), history,
        platform = platform,
        brandingRepository = BrandingRepository(),
    ).apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
    }

    private fun QuoteViewModel.awaitNotice(): UiNotice = runBlocking { withTimeout(AWAIT_MILLIS) { notices.first() } }

    @Test
    fun aNoticeEmittedWhileNobodyWasWatchingArrivesOnceWhenTheScreenComesBack() {
        val viewModel = viewModel()
        val printId = viewModel.addPrint()
        viewModel.removePrint(printId)

        val first = viewModel.awaitNotice()
        assertEquals(uiText(Res.string.quote_notice_print_removed, 2), first.message)

        val again = runBlocking { withTimeoutOrNull(SILENCE_MILLIS) { viewModel.notices.first() } }
        assertNull(again, "o aviso já mostrado não volta")
    }

    @Test
    fun exportNoticesComeAsResourceTextAndKeepTheErrorFlag() {
        val viewModel = viewModel()
        viewModel.sendToClient()

        viewModel.sendPdf()
        val saved = viewModel.awaitNotice()
        assertEquals(uiText(Res.string.export_saved_pdf), saved.message)
        assertEquals(uiText(Res.string.export_open_folder), saved.actionLabel)
        assertFalse(saved.isError)

        platform.browserWorks = false
        viewModel.sendWhatsApp()
        val failed = viewModel.awaitNotice()
        assertEquals(uiText(Res.string.export_whatsapp_copied), failed.message)
        assertTrue(failed.isError, "o erro chega marcado, pra ficar mais tempo na tela")
    }

    @Test
    fun theStateCarriesTheComparisonAndTheQueueSoTheScreenDoesNotCompute() {
        val first = printers.printers.value.first()
        printers.add(first.copy(id = "outra", name = "Outra"))
        val viewModel = viewModel()
        viewModel.selectPrinter(first.id)

        val state = viewModel.uiState.value
        assertEquals(2, state.comparison.size)
        assertTrue(state.queueHints.isEmpty())

        // Um pedido aprovado na impressora entra na dica de fila.
        viewModel.saveCurrentQuote()
        history.updateStatus(history.savedQuotes.value.single().id, OrderStatus.APROVADO)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("120")
        viewModel.selectPrinter(first.id)

        val hint = assertNotNull(viewModel.uiState.value.queueHints.singleOrNull())
        assertEquals(first.name, hint.printerName)
        assertEquals(1, hint.queuedQuoteCount)
        assertEquals(120.0, hint.thisMinutes, 1e-9)
    }

    @Test
    fun typingGramsStillTurnsIntoMetersWithTheFilamentOfTheRow() {
        val viewModel = viewModel()
        viewModel.setWeightGrams("50")

        val row = viewModel.uiState.value.input.prints.single().filaments.single()
        assertEquals("50", row.weightText)
        assertTrue(row.lengthText.isNotBlank() && row.lengthText != "12")
        assertNotNull(viewModel.uiState.value.result.quote)
    }

    private companion object {
        const val AWAIT_MILLIS = 5_000L
        const val SILENCE_MILLIS = 200L
    }
}
