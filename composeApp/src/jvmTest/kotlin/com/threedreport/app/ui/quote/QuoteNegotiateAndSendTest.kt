package com.threedreport.app.ui.quote

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.QuoteKind
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "Negociar" e "Enviar pro cliente" do painel de resultado (decisão 131). */
class QuoteNegotiateAndSendTest {
    private lateinit var folder: String
    private lateinit var history: QuoteHistoryRepository
    private lateinit var platform: FakePlatform

    @BeforeTest
    fun setUp() {
        folder = createTempDirectory("3dreport-test").toString()
        System.setProperty("threedreport.dataDir", folder)
        history = QuoteHistoryRepository()
        platform = FakePlatform().apply { nextSaveLocation = "$folder/" }
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel(filled: Boolean = true) = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), history,
        platform = platform,
        brandingRepository = BrandingRepository(),
    ).apply {
        if (filled) {
            setLengthMeters("12")
            setPrintTimeMinutes("190")
        }
    }

    @Test
    fun negotiatingTypesIntoTheSameFieldAsTheForm() {
        val viewModel = viewModel()
        val calculated = viewModel.uiState.value.result.quote!!.salePrice

        viewModel.setTargetTotal("150")
        val negotiated = viewModel.uiState.value

        assertEquals("150", negotiated.input.targetTotalText)
        assertEquals(150.0, negotiated.result.grandTotal!!, 1e-9)
        assertTrue(negotiated.result.quote!!.isNegotiated)
        assertTrue(negotiated.result.quote.profit != calculated)

        // "Limpar" volta pro preço calculado.
        viewModel.setTargetTotal("")
        assertEquals(calculated, viewModel.uiState.value.result.quote!!.salePrice, 1e-9)
        assertFalse(viewModel.uiState.value.result.quote!!.isNegotiated)
    }

    @Test
    fun sendingSavesAnOrderInQuotedStatusAndLeavesTheScreenAlone() {
        val viewModel = viewModel()
        viewModel.setTargetTotal("150")
        val before = viewModel.uiState.value.input

        assertTrue(viewModel.sendToClient())

        val saved = history.savedQuotes.value.single()
        assertEquals(QuoteKind.ORDER, saved.kind)
        assertEquals(OrderStatus.ORCADO, saved.status)
        assertEquals(150.0, saved.totalWithServices, 1e-9)
        assertEquals(before, viewModel.uiState.value.input)
        assertEquals(saved, viewModel.uiState.value.form.sent?.savedQuote)
    }

    @Test
    fun sendingTwiceWithoutChangesSavesOnlyOnce() {
        val viewModel = viewModel()

        assertTrue(viewModel.sendToClient())
        assertTrue(viewModel.sendToClient())

        assertEquals(1, history.savedQuotes.value.size)
    }

    @Test
    fun savingAfterSendingDoesNotCreateASecondOrder() {
        val viewModel = viewModel()
        viewModel.sendToClient()

        assertTrue(viewModel.saveCurrentQuote())

        assertEquals(1, history.savedQuotes.value.size)
        // E o formulário recomeça, como em qualquer salvar.
        assertNull(viewModel.uiState.value.form.sent)
    }

    @Test
    fun changingAfterSendingUpdatesTheSameOrderInsteadOfCreatingAnother() {
        val viewModel = viewModel()
        viewModel.sendToClient()
        val id = history.savedQuotes.value.single().id

        viewModel.setTargetTotal("200")
        assertTrue(viewModel.sendToClient())

        val saved = history.savedQuotes.value.single()
        assertEquals(id, saved.id)
        assertEquals(200.0, saved.totalWithServices, 1e-9)
    }

    @Test
    fun sendingABlockedQuoteSavesNothingAndShowsTheSameBlock() {
        val viewModel = viewModel(filled = false)

        assertFalse(viewModel.sendToClient())

        assertTrue(history.savedQuotes.value.isEmpty())
        val sendBlock = viewModel.uiState.value.form.blockedMessage
        assertNotNull(sendBlock)

        viewModel.consumeBlockedMessage()
        viewModel.saveCurrentQuote()
        assertEquals(sendBlock, viewModel.uiState.value.form.blockedMessage)
    }

    @Test
    fun aCatalogProductIsNotSentToAClient() {
        val viewModel = viewModel()
        viewModel.setKind(QuoteKind.PRODUCT)

        assertFalse(viewModel.sendToClient())

        assertTrue(history.savedQuotes.value.isEmpty())
    }

    @Test
    fun theSendOptionsDeliverTheSavedOrderTheWayOrdersDoes() {
        val viewModel = viewModel()
        viewModel.sendToClient()

        viewModel.sendWhatsApp()
        assertTrue(platform.opened.single().startsWith("https://wa.me/"))

        viewModel.sendPdf()
        assertTrue(platform.awaitWritten().keys.single().endsWith(".pdf"))
        // Nada disso grava outro pedido.
        assertEquals(1, history.savedQuotes.value.size)
    }

    @Test
    fun switchingToProductAfterSendingLeavesTheSentOrderAlone() {
        val viewModel = viewModel()
        viewModel.sendToClient()
        val sent = history.savedQuotes.value.single()

        viewModel.setKind(QuoteKind.PRODUCT)
        assertNull(viewModel.uiState.value.form.sent)
        assertTrue(viewModel.saveCurrentQuote())

        val all = history.savedQuotes.value
        assertEquals(2, all.size)
        assertEquals(sent, all.single { it.id == sent.id }, "o pedido enviado fica como está")
        assertEquals(QuoteKind.PRODUCT, all.single { it.id != sent.id }.kind)
    }

    @Test
    fun anOrderDeletedAfterSendingIsSavedAgainInsteadOfBroughtBackOrTrusted() {
        val viewModel = viewModel()
        viewModel.sendToClient()
        val first = history.savedQuotes.value.single()

        history.delete(first.id)
        assertTrue(viewModel.sendToClient())

        val again = history.savedQuotes.value.single()
        assertTrue(again.id != first.id, "grava de novo, sem ressuscitar o excluído nem confiar nele")
        assertEquals(again, viewModel.uiState.value.form.sent?.savedQuote)

        // Salvar depois de excluir também grava de novo.
        history.delete(again.id)
        assertTrue(viewModel.saveCurrentQuote())
        assertEquals(1, history.savedQuotes.value.size)
    }

    @Test
    fun sendingWhileEditingKeepsTheEditInSyncWithWhatIsOnDisk() {
        val viewModel = viewModel()
        viewModel.saveCurrentQuote()
        viewModel.loadForEditing(history.savedQuotes.value.single())
        assertFalse(viewModel.hasUnsavedEdits)

        viewModel.setTargetTotal("200")
        assertTrue(viewModel.hasUnsavedEdits)
        assertTrue(viewModel.sendToClient())

        val saved = history.savedQuotes.value.single()
        assertEquals(200.0, saved.totalWithServices, 1e-9)
        val editing = viewModel.uiState.value.form.operation as QuoteOperation.Editing
        assertEquals(saved, editing.savedQuote)
        assertFalse(viewModel.hasUnsavedEdits, "o que está na tela é o que está em disco")
        assertEquals(200.0, viewModel.uiState.value.result.quote!!.salePrice, 1e-9)

        viewModel.setTargetTotal("210")
        assertTrue(viewModel.hasUnsavedEdits)
    }

    @Test
    fun anOrderSentAndNotChangedSinceIsNotADraft() {
        val viewModel = viewModel()
        assertTrue(viewModel.hasDraft)

        viewModel.sendToClient()
        assertFalse(viewModel.hasDraft)

        viewModel.setTargetTotal("200")
        assertTrue(viewModel.hasDraft)
    }

    @Test
    fun negotiatingClosesWhenTheFormStartsOver() {
        val viewModel = viewModel()
        viewModel.setNegotiating(true)
        assertTrue(viewModel.uiState.value.negotiating)

        viewModel.saveCurrentQuote()
        assertFalse(viewModel.uiState.value.negotiating, "depois de salvar o formulário recomeça")

        viewModel.setNegotiating(true)
        viewModel.resetForm()
        assertFalse(viewModel.uiState.value.negotiating)

        viewModel.setNegotiating(true)
        viewModel.setKind(QuoteKind.PRODUCT)
        assertFalse(viewModel.uiState.value.negotiating, "produto não negocia")
    }

    @Test
    fun anInvalidNegotiatedPriceStillLeavesTheFieldReachableThroughTheState() {
        val viewModel = viewModel()
        viewModel.setNegotiating(true)
        viewModel.setTargetTotal("abc")

        val state = viewModel.uiState.value
        assertNull(state.result.quote, "o preço combinado inválido tira a conta")
        assertTrue(state.input.targetTotalText.isNotBlank() && state.negotiating, "o campo segue aberto e com o texto pra corrigir")
    }

    @Test
    fun aNegotiatedPriceKeepsTheCalculatedTotalForThePanel() {
        val viewModel = viewModel()
        val calculated = viewModel.uiState.value.result.grandTotal!!
        assertNull(viewModel.uiState.value.result.calculatedTotal)

        viewModel.setTargetTotal("150")

        assertEquals(calculated, viewModel.uiState.value.result.calculatedTotal!!, 1e-9)
    }
}
