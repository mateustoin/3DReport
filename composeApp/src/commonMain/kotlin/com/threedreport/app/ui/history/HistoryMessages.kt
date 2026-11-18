package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_copy_failed
import _3dreport.composeapp.generated.resources.export_whatsapp_failed
import _3dreport.composeapp.generated.resources.message_notice_copied
import _3dreport.composeapp.generated.resources.message_whatsapp_copied
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A janela "Mensagem pro cliente" (decisão 148, item 23): o modelo já preenchido e editável, o seletor de modelo e os
 * botões "Abrir no WhatsApp" e "Copiar". Editar aqui não muda o modelo salvo. Enviar (ou copiar) o modelo "Retomar
 * orçamento" grava o momento do contato ([QuoteHistoryRepository.recordFollowUp]), e o selo de parado some (decisão 149).
 */
internal class HistoryMessages(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
    private val platform: PlatformServices,
    private val scope: CoroutineScope,
    private val main: CoroutineDispatcher,
    private val state: () -> HistoryUiState,
    private val change: ((HistoryUiState) -> HistoryUiState) -> Unit,
    private val notify: (UiNotice) -> Unit,
) {
    /** Abre a janela no modelo [kind], ou no que o andamento do pedido pede ([MessageTemplateKind.forOrder]). */
    fun open(quote: SavedQuote, kind: MessageTemplateKind? = null) {
        val current = repository.savedQuotes.value.find { it.id == quote.id } ?: quote
        val chosen = kind ?: MessageTemplateKind.forOrder(current) ?: return
        fill(current, chosen)
    }

    /** Troca o modelo: o texto é refeito a partir dele (o que foi editado no outro se perde). */
    fun changeKind(kind: MessageTemplateKind) {
        val quote = state().message?.quote ?: return
        fill(quote, kind)
    }

    private fun fill(quote: SavedQuote, kind: MessageTemplateKind) {
        scope.launch(main) {
            val text = clientMessageFor(quote, kind, brandingRepository.branding.value)
            change { it.copy(message = MessageEditing(quote, kind, text)) }
        }
    }

    fun edit(text: String) = change { s -> s.copy(message = s.message?.copy(text = text)) }

    fun close() = change { it.copy(message = null) }

    fun openInWhatsApp() {
        val message = state().message ?: return
        val link = whatsAppLink(message.quote.client?.contact, message.text)
        if (platform.openUrl(link)) {
            sent(message)
            return
        }
        // Sem navegador, o texto ainda chega no cliente: vai pra área de transferência.
        if (platform.copyToClipboard(message.text)) {
            sent(message)
            notify(UiNotice(uiText(Res.string.message_whatsapp_copied), isError = true))
        } else {
            notify(UiNotice(uiText(Res.string.export_whatsapp_failed), isError = true))
        }
    }

    fun copy() {
        val message = state().message ?: return
        if (platform.copyToClipboard(message.text)) {
            sent(message)
            notify(UiNotice(uiText(Res.string.message_notice_copied)))
        } else {
            notify(UiNotice(uiText(Res.string.export_copy_failed), isError = true))
        }
    }

    private fun sent(message: MessageEditing) {
        if (message.kind == MessageTemplateKind.FOLLOW_UP) repository.recordFollowUp(message.quote.id)
        close()
    }
}
