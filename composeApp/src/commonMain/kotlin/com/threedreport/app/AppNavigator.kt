package com.threedreport.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.threedreport.app.ui.navigation.AppDestination
import com.threedreport.app.ui.quote.QuoteOperation
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote

/**
 * A tela aberta e as regras de quando um orçamento em andamento atrapalha outra ação. Fica fora do
 * `@Composable` pra ter teste; o `App()` só lê [destination] e os pedidos de confirmação e desenha os diálogos.
 *
 * Há dois ViewModels do Orçamento (decisão 108): [quote], o da aba, e [edit], o da edição de um orçamento
 * salvo, que aparece no próprio Orçamento (decisão 113) por cima do rascunho da aba.
 *
 * @param onEditSaved chamado depois que uma edição foi salva (o `App()` mostra o aviso).
 */
class AppNavigator(
    startAt: AppDestination,
    private val quote: QuoteViewModel,
    private val edit: QuoteViewModel,
    private val onEditSaved: () -> Unit = {},
) {
    var destination: AppDestination by mutableStateOf(startAt)
        private set

    /** Ação que jogaria fora o orçamento em andamento na aba, esperando a confirmação. */
    var pendingDiscard: (() -> Unit)? by mutableStateOf(null)
        private set

    /** Ação que precisa da aba do Orçamento livre, esperando a pessoa encerrar a edição em andamento. */
    var pendingEndEditing: (() -> Unit)? by mutableStateOf(null)
        private set

    /** Cancelar uma edição alterada pede confirmação antes de perder o que foi mudado. */
    var confirmingEditDiscard: Boolean by mutableStateOf(false)
        private set

    /**
     * Se o Orçamento mostra a edição de um orçamento salvo. Lido na hora, pras regras abaixo; não é
     * observável pelo Compose, então o `App()` desenha a partir do `saveForm` que coleta, e não disto.
     */
    val editing: Boolean
        get() = edit.uiState.value.form.editingQuoteId != null

    fun goTo(target: AppDestination) {
        destination = target
    }

    private fun originOf(operation: QuoteOperation?) =
        if (operation?.originKind == QuoteKind.PRODUCT) AppDestination.CATALOG else AppDestination.ORDERS

    // Duplicar, Vender, Guardar no catálogo e Ctrl+N carregam no ViewModel da aba, que está por trás da
    // edição: primeiro a edição termina (sem alteração, termina sozinha), depois pergunta do rascunho.
    private fun unlessEditing(action: () -> Unit) {
        when {
            !editing -> action()

            edit.hasUnsavedEdits -> pendingEndEditing = action

            else -> {
                edit.resetForm()
                action()
            }
        }
    }

    private fun unlessDraft(action: () -> Unit) {
        unlessEditing { if (quote.hasDraft) pendingDiscard = action else action() }
    }

    private fun endEditing() {
        val origin = originOf(edit.uiState.value.form.operation)
        edit.resetForm()
        destination = origin
    }

    fun editQuote(savedQuote: SavedQuote) = unlessEditing {
        edit.loadForEditing(savedQuote)
        destination = AppDestination.QUOTE
    }

    fun duplicateQuote(savedQuote: SavedQuote) = unlessDraft {
        quote.duplicateForNewQuote(savedQuote)
        destination = AppDestination.QUOTE
    }

    fun sellProduct(product: SavedQuote) = unlessDraft {
        quote.sellFromProduct(product)
        destination = AppDestination.QUOTE
    }

    fun copyToCatalog(order: SavedQuote) = unlessDraft {
        quote.copyToCatalog(order)
        destination = AppDestination.QUOTE
    }

    /** Ctrl/Cmd+N no Orçamento. */
    fun newQuote() = unlessDraft(quote::resetForm)

    /** "Novo orçamento" de Pedidos: abre o Orçamento em branco, pra um pedido de cliente. Com um rascunho na aba, pergunta antes. */
    fun newOrder() = startBlank(QuoteKind.ORDER)

    /** "Novo produto" do Catálogo: abre o Orçamento em branco, no modo produto. Com um rascunho na aba, pergunta antes. */
    fun newProduct() = startBlank(QuoteKind.PRODUCT)

    private fun startBlank(kind: QuoteKind) = unlessDraft {
        quote.resetForm()
        quote.setKind(kind)
        destination = AppDestination.QUOTE
    }

    /**
     * Ctrl/Cmd+S no Orçamento: com uma edição aberta, salva e encerra a edição; sem ela, salva o rascunho
     * da aba (o orçamento novo, que volta ao começo depois de salvo). Devolve se salvou.
     */
    fun saveCurrent(): Boolean {
        return if (editing) saveEditing() else quote.saveCurrentQuote()
    }

    fun saveEditing(): Boolean {
        val saved = edit.saveCurrentQuote()
        if (saved) {
            endEditing()
            onEditSaved()
        }
        return saved
    }

    fun cancelEditing() {
        if (edit.hasUnsavedEdits) confirmingEditDiscard = true else endEditing()
    }

    /**
     * Vender, Duplicar e Guardar no catálogo começam em Pedidos ou no Catálogo: desistir devolve a
     * pessoa pra lá, com o formulário limpo.
     */
    fun cancelQuoteOperation() {
        val origin = originOf(quote.uiState.value.form.operation)
        quote.resetForm()
        destination = origin
    }

    fun confirmEditDiscard() {
        confirmingEditDiscard = false
        endEditing()
    }

    fun keepEditing() {
        confirmingEditDiscard = false
    }

    fun confirmEndEditing() {
        val action = pendingEndEditing ?: return
        pendingEndEditing = null
        edit.resetForm()
        action()
    }

    /** "Voltar pra edição": a ação pedida é esquecida e a pessoa volta ao Orçamento. */
    fun dismissEndEditing() {
        pendingEndEditing = null
        destination = AppDestination.QUOTE
    }

    fun confirmDiscard() {
        val action = pendingDiscard ?: return
        pendingDiscard = null
        action()
    }

    fun dismissDiscard() {
        pendingDiscard = null
    }
}
