package com.threedreport.app

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.QuoteOperation
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * As regras de rascunho e edição (decisões 108 e 113), com os mesmos dois ViewModels do Orçamento que o
 * `App()` usa: o da aba e o da edição.
 */
class AppNavigatorTest {

    private lateinit var history: QuoteHistoryRepository
    private lateinit var tab: QuoteViewModel
    private lateinit var editor: QuoteViewModel
    private var editSavedCalls = 0

    @BeforeTest
    fun setUp() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        history = QuoteHistoryRepository()
        // Dois ViewModels sobre o mesmo histórico, como no app.
        val filaments = FilamentRepository()
        val printers = PrinterRepository()
        val settings = SettingsRepository()
        val services = ServiceRepository()
        val channels = SalesChannelRepository()
        tab = QuoteViewModel(filaments, printers, settings, services, channels, history)
        editor = QuoteViewModel(filaments, printers, settings, services, channels, history)
        editSavedCalls = 0
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun navigator(startAt: AppDestination = AppDestination.ORDERS) =
        AppNavigator(startAt, tab, editor, onEditSaved = { editSavedCalls++ })

    /** Salva um pedido (ou produto) pela aba e deixa a aba limpa, como ela fica antes de qualquer rascunho. */
    private fun saveQuote(name: String, kind: QuoteKind = QuoteKind.ORDER): SavedQuote {
        tab.setKind(kind)
        tab.setLengthMeters("12")
        tab.setPrintTimeMinutes("190")
        tab.setSaveName(name)
        assertTrue(tab.saveCurrentQuote())
        tab.resetForm()
        return history.savedQuotes.value.single { it.name == name }
    }

    private fun startDraftInTab() {
        tab.setLengthMeters("30")
        assertTrue(tab.hasDraft)
    }

    // ---- editar

    @Test
    fun editingWithoutADraftOpensTheEditionInTheQuoteTab() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()

        nav.editQuote(saved)

        assertEquals(AppDestination.QUOTE, nav.destination)
        assertTrue(nav.editing)
        assertEquals(saved.id, editor.uiState.value.form.editingQuoteId)
        assertNull(nav.pendingDiscard)
        assertNull(nav.pendingEndEditing)
    }

    @Test
    fun editingNeverAsksAboutTheTabDraftAndLeavesItAlone() {
        val saved = saveQuote("Pedido A")
        startDraftInTab()
        val draft = tab.uiState.value.input
        val nav = navigator()

        nav.editQuote(saved)

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(draft, tab.uiState.value.input)
    }

    @Test
    fun editingWhileAnUnchangedEditionIsOpenReplacesItWithoutAsking() {
        val first = saveQuote("Pedido A")
        val second = saveQuote("Pedido B")
        val nav = navigator()
        nav.editQuote(first)
        nav.goTo(AppDestination.ORDERS)

        nav.editQuote(second)

        assertNull(nav.pendingEndEditing)
        assertEquals(second.id, editor.uiState.value.form.editingQuoteId)
        assertEquals(AppDestination.QUOTE, nav.destination)
    }

    @Test
    fun editingWhileAnEditedEditionIsOpenAsksToEndItFirst() {
        val first = saveQuote("Pedido A")
        val second = saveQuote("Pedido B")
        val nav = navigator()
        nav.editQuote(first)
        editor.setSaveName("Pedido A alterado")
        nav.goTo(AppDestination.ORDERS)

        nav.editQuote(second)

        assertNotNull(nav.pendingEndEditing)
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertEquals(first.id, editor.uiState.value.form.editingQuoteId, "nada muda antes da resposta")
        assertEquals("Pedido A alterado", editor.uiState.value.form.name)
    }

    @Test
    fun confirmingTheEndOfTheEditionDiscardsItAndRunsThePendingAction() {
        val first = saveQuote("Pedido A")
        val second = saveQuote("Pedido B")
        val nav = navigator()
        nav.editQuote(first)
        editor.setSaveName("Pedido A alterado")
        nav.goTo(AppDestination.ORDERS)
        nav.editQuote(second)

        nav.confirmEndEditing()

        assertNull(nav.pendingEndEditing)
        assertEquals(second.id, editor.uiState.value.form.editingQuoteId)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals("Pedido A", history.savedQuotes.value.first { it.id == first.id }.name, "nada foi salvo")
    }

    @Test
    fun goingBackToTheEditionForgetsThePendingActionAndShowsTheQuoteTab() {
        val first = saveQuote("Pedido A")
        val second = saveQuote("Pedido B")
        val nav = navigator()
        nav.editQuote(first)
        editor.setSaveName("Pedido A alterado")
        nav.goTo(AppDestination.ORDERS)
        nav.editQuote(second)

        nav.dismissEndEditing()

        assertNull(nav.pendingEndEditing)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(first.id, editor.uiState.value.form.editingQuoteId)
        assertEquals("Pedido A alterado", editor.uiState.value.form.name)
    }

    // ---- duplicar, vender, guardar no catálogo

    @Test
    fun duplicatingWithoutADraftLoadsTheTabAndOpensIt() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()

        nav.duplicateQuote(saved)

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.Duplicating)
    }

    @Test
    fun duplicatingWithADraftAsksBeforeDiscardingIt() {
        val saved = saveQuote("Pedido A")
        startDraftInTab()
        val nav = navigator()

        nav.duplicateQuote(saved)

        assertNotNull(nav.pendingDiscard)
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertNull(tab.uiState.value.form.operation, "o rascunho continua como estava")
        assertEquals("30", tab.uiState.value.input.prints.first().filaments.first().lengthText)
    }

    @Test
    fun confirmingTheDiscardReplacesTheDraft() {
        val saved = saveQuote("Pedido A")
        startDraftInTab()
        val nav = navigator()
        nav.duplicateQuote(saved)

        nav.confirmDiscard()

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.Duplicating)
        assertEquals("12", tab.uiState.value.input.prints.first().filaments.first().lengthText)
    }

    @Test
    fun decliningTheDiscardKeepsTheDraftAndTheScreen() {
        val saved = saveQuote("Pedido A")
        startDraftInTab()
        val nav = navigator()
        nav.duplicateQuote(saved)

        nav.dismissDiscard()

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertEquals("30", tab.uiState.value.input.prints.first().filaments.first().lengthText)
    }

    @Test
    fun sellingAndCopyingToCatalogAlsoAskAboutTheDraft() {
        val product = saveQuote("Produto", QuoteKind.PRODUCT)
        val order = saveQuote("Pedido A")
        startDraftInTab()
        val nav = navigator(AppDestination.CATALOG)

        nav.sellProduct(product)
        assertNotNull(nav.pendingDiscard)
        nav.dismissDiscard()

        nav.copyToCatalog(order)
        assertNotNull(nav.pendingDiscard)
        assertEquals(AppDestination.CATALOG, nav.destination)

        nav.confirmDiscard()
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.CopyingToCatalog)
        assertEquals(AppDestination.QUOTE, nav.destination)
    }

    // ---- novo orçamento e novo produto (cabeçalhos de Pedidos e do Catálogo)

    @Test
    fun aNewOrderOpensTheBlankQuoteTabAsAnOrder() {
        val nav = navigator(AppDestination.ORDERS)
        tab.setKind(QuoteKind.PRODUCT)

        nav.newOrder()

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(QuoteKind.ORDER, tab.uiState.value.input.kind)
        assertFalse(tab.hasDraft)
    }

    @Test
    fun aNewProductOpensTheBlankQuoteTabAsAProduct() {
        val nav = navigator(AppDestination.CATALOG)

        nav.newProduct()

        assertNull(nav.pendingDiscard)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(QuoteKind.PRODUCT, tab.uiState.value.input.kind)
    }

    @Test
    fun aNewProductWithADraftAsksBeforeDiscardingItAndThenOpensTheProductMode() {
        val nav = navigator(AppDestination.CATALOG)
        startDraftInTab()

        nav.newProduct()

        assertNotNull(nav.pendingDiscard)
        assertEquals(AppDestination.CATALOG, nav.destination, "a tela só muda depois da confirmação")

        nav.confirmDiscard()

        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(QuoteKind.PRODUCT, tab.uiState.value.input.kind)
        assertFalse(tab.hasDraft)
    }

    @Test
    fun decliningANewOrderKeepsTheDraftAndTheScreen() {
        val nav = navigator(AppDestination.ORDERS)
        startDraftInTab()

        nav.newOrder()
        nav.dismissDiscard()

        assertEquals(AppDestination.ORDERS, nav.destination)
        assertTrue(tab.hasDraft)
    }

    @Test
    fun sellingWithoutADraftOpensTheSaleDirectly() {
        val product = saveQuote("Produto", QuoteKind.PRODUCT)
        val nav = navigator(AppDestination.CATALOG)

        nav.sellProduct(product)

        assertNull(nav.pendingDiscard)
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.Selling)
        assertEquals(AppDestination.QUOTE, nav.destination)
    }

    @Test
    fun duplicatingWithAnEditedEditionAndADraftAsksTwiceInOrder() {
        val saved = saveQuote("Pedido A")
        startDraftInTab()
        val nav = navigator()
        nav.editQuote(saved)
        editor.setSaveName("Alterado")
        nav.goTo(AppDestination.ORDERS)

        nav.duplicateQuote(saved)
        // Primeiro a edição, que está por cima; o rascunho só depois que ela terminar.
        assertNotNull(nav.pendingEndEditing)
        assertNull(nav.pendingDiscard)

        nav.confirmEndEditing()
        assertNull(nav.pendingEndEditing)
        assertFalse(nav.editing)
        assertNotNull(nav.pendingDiscard)
        assertEquals(AppDestination.ORDERS, nav.destination)

        nav.confirmDiscard()
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.Duplicating)
    }

    @Test
    fun duplicatingWithAnUnchangedEditionEndsItSilently() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)
        nav.goTo(AppDestination.ORDERS)

        nav.duplicateQuote(saved)

        assertNull(nav.pendingEndEditing)
        assertFalse(nav.editing)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertTrue(tab.uiState.value.form.operation is QuoteOperation.Duplicating)
    }

    // ---- cancelar a edição

    @Test
    fun cancellingAnUnchangedEditionGoesBackToOrdersWithoutAsking() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)

        nav.cancelEditing()

        assertFalse(nav.confirmingEditDiscard)
        assertFalse(nav.editing)
        assertEquals(AppDestination.ORDERS, nav.destination)
    }

    @Test
    fun cancellingAnEditedEditionAsksFirstAndKeepsTheEditionUntilConfirmed() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)
        editor.setSaveName("Alterado")

        nav.cancelEditing()

        assertTrue(nav.confirmingEditDiscard)
        assertTrue(nav.editing)
        assertEquals(AppDestination.QUOTE, nav.destination)

        nav.keepEditing()
        assertFalse(nav.confirmingEditDiscard)
        assertTrue(nav.editing)
        assertEquals("Alterado", editor.uiState.value.form.name)

        nav.cancelEditing()
        nav.confirmEditDiscard()
        assertFalse(nav.confirmingEditDiscard)
        assertFalse(nav.editing)
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertEquals("Pedido A", history.savedQuotes.value.single().name)
    }

    @Test
    fun cancellingTheEditionOfAProductGoesBackToTheCatalog() {
        val product = saveQuote("Produto", QuoteKind.PRODUCT)
        val nav = navigator(AppDestination.CATALOG)
        nav.editQuote(product)

        nav.cancelEditing()

        assertEquals(AppDestination.CATALOG, nav.destination)
    }

    // ---- salvar a edição

    @Test
    fun savingAnOrderEditionGoesBackToOrdersAndTellsIt() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)
        editor.setSaveName("Pedido corrigido")

        nav.saveEditing()

        assertEquals("Pedido corrigido", history.savedQuotes.value.single().name)
        assertFalse(nav.editing)
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertEquals(1, editSavedCalls)
    }

    @Test
    fun savingAProductEditionGoesBackToTheCatalog() {
        val product = saveQuote("Produto", QuoteKind.PRODUCT)
        val nav = navigator(AppDestination.CATALOG)
        nav.editQuote(product)
        editor.setSaveName("Produto corrigido")

        nav.saveEditing()

        assertEquals(AppDestination.CATALOG, nav.destination)
        assertEquals(1, editSavedCalls)
    }

    @Test
    fun savingAnEditionThatCannotBeSavedStaysOnTheEdition() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)
        editor.setLengthMeters("abc")

        nav.saveEditing()

        assertTrue(nav.editing)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(0, editSavedCalls)
        assertNotNull(editor.uiState.value.form.blockedMessage)
    }

    // ---- desistir de uma operação da aba, Ctrl+N

    @Test
    fun cancellingATabOperationGoesBackToItsOriginAndClearsTheForm() {
        val product = saveQuote("Produto", QuoteKind.PRODUCT)
        val order = saveQuote("Pedido A")
        val nav = navigator()

        nav.sellProduct(product)
        nav.cancelQuoteOperation()
        assertEquals(AppDestination.CATALOG, nav.destination)
        assertFalse(tab.hasDraft)

        nav.duplicateQuote(order)
        nav.cancelQuoteOperation()
        assertEquals(AppDestination.ORDERS, nav.destination)

        nav.duplicateQuote(product)
        nav.cancelQuoteOperation()
        assertEquals(AppDestination.CATALOG, nav.destination, "duplicar um produto volta pro Catálogo")

        nav.copyToCatalog(order)
        nav.cancelQuoteOperation()
        assertEquals(AppDestination.ORDERS, nav.destination)
        assertFalse(tab.hasDraft)
    }

    @Test
    fun newQuoteShortcutClearsTheTabOnlyAfterAskingAboutADraft() {
        val nav = navigator(AppDestination.QUOTE)

        nav.newQuote()
        assertNull(nav.pendingDiscard)

        startDraftInTab()
        nav.newQuote()
        assertNotNull(nav.pendingDiscard)
        assertTrue(tab.hasDraft)

        nav.confirmDiscard()
        assertFalse(tab.hasDraft)
    }

    @Test
    fun newQuoteShortcutDuringAnEditedEditionAsksToEndItBeforeAnythingElse() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        nav.editQuote(saved)
        editor.setSaveName("Alterado")

        nav.newQuote()

        assertNotNull(nav.pendingEndEditing)
        assertNull(nav.pendingDiscard)
        assertTrue(nav.editing)
    }

    @Test
    fun goToChangesTheDestination() {
        val nav = navigator(AppDestination.DASHBOARD)
        assertEquals(AppDestination.DASHBOARD, nav.destination)

        nav.goTo(AppDestination.SETTINGS)

        assertEquals(AppDestination.SETTINGS, nav.destination)
    }

    // ---- Ctrl+S

    @Test
    fun ctrlSWithoutAnOpenEditionSavesTheTab() {
        val nav = navigator(AppDestination.QUOTE)
        tab.setLengthMeters("12")
        tab.setPrintTimeMinutes("190")
        tab.setSaveName("Pedido da aba")

        assertTrue(nav.saveCurrent())

        assertEquals("Pedido da aba", history.savedQuotes.value.single().name)
        assertEquals(AppDestination.QUOTE, nav.destination)
        assertEquals(0, editSavedCalls)
        assertFalse(tab.hasDraft, "salvar um orçamento novo deixa a aba limpa")
    }

    @Test
    fun ctrlSWithoutAnOpenEditionAndAnIncompleteTabSavesNothingAndSaysWhy() {
        val nav = navigator(AppDestination.QUOTE)

        assertFalse(nav.saveCurrent())

        assertTrue(history.savedQuotes.value.isEmpty())
        assertNotNull(tab.uiState.value.form.blockedMessage)
    }

    @Test
    fun ctrlSWithAnOpenEditionSavesTheEditionAndLeavesTheTabAlone() {
        val saved = saveQuote("Pedido A")
        val nav = navigator()
        startDraftInTab()
        val draft = tab.uiState.value.input
        nav.editQuote(saved)
        editor.setSaveName("Pedido A corrigido")

        assertTrue(nav.saveCurrent())

        assertEquals("Pedido A corrigido", history.savedQuotes.value.single().name)
        assertEquals(1, editSavedCalls)
        assertFalse(nav.editing)
        assertEquals(draft, tab.uiState.value.input)
    }
}
