package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_copy_failed
import _3dreport.composeapp.generated.resources.history_notice_photo_saved
import _3dreport.composeapp.generated.resources.history_notice_stl_saved
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.ui.components.OverdueItem
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote

/**
 * Tudo o que Pedidos e o Catálogo mandam pra fora: PDF, imagem, WhatsApp, texto copiado, foto e STL, e a exportação em lote.
 * A geração dos arquivos é a do "Enviar pro cliente" do Orçamento (ver [QuoteExporter]). Os envios que mostram o prazo
 * passam pelo aviso de prazo vencido ([HistoryUiState.pendingExport]); o catálogo não, porque não mostra prazo.
 */
internal class HistoryExports(
    private val exporter: QuoteExporter,
    private val platform: PlatformServices,
    private val brandingRepository: BrandingRepository,
    private val repository: QuoteHistoryRepository,
    private val today: () -> Long,
    private val state: () -> HistoryUiState,
    private val change: ((HistoryUiState) -> HistoryUiState) -> Unit,
    private val notify: (UiNotice) -> Unit,
    private val editDeliveryDate: (SavedQuote) -> Unit,
    private val clearSelection: () -> Unit,
) {
    fun exportPdf(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.PDF, listOf(savedQuote))) return
        exporter.exportPdf(savedQuote)
    }

    /** Abre a conversa do cliente no WhatsApp com o orçamento já escrito (ver [toWhatsAppLink]). */
    fun openInWhatsApp(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.WHATSAPP, listOf(savedQuote))) return
        exporter.openInWhatsApp(savedQuote)
    }

    /** Gera a imagem quadrada pra mandar no WhatsApp ou no status e abre o "salvar como". */
    fun saveShareableImage(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.IMAGE, listOf(savedQuote))) return
        exporter.saveShareableImage(savedQuote)
    }

    fun copy(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.COPY, listOf(savedQuote))) return
        performCopy(savedQuote)
    }

    // Copiar não gera aviso quando dá certo: o botão diz "Copiado" por um instante (ver [HistoryUiState.copiedId]).
    private fun performCopy(savedQuote: SavedQuote) {
        exporter.withCopyPasteText(savedQuote) { text ->
            if (platform.copyToClipboard(text)) {
                change { it.copy(copiedId = savedQuote.id) }
            } else {
                notify(UiNotice(uiText(Res.string.export_copy_failed), isError = true))
            }
        }
    }

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = repository.photoBytes(savedQuote) ?: return
        val extension = savedQuote.photoFileName?.substringAfterLast('.', "png") ?: "png"
        exporter.saveAttachment(savedQuote, bytes, extension, uiText(Res.string.history_notice_photo_saved))
    }

    fun downloadStl(savedQuote: SavedQuote) {
        val bytes = repository.stlBytes(savedQuote) ?: return
        exporter.saveAttachment(savedQuote, bytes, "stl", uiText(Res.string.history_notice_stl_saved))
    }

    /** "Enviar assim mesmo" no aviso de prazo vencido: faz o envio que tinha sido segurado, sem perguntar de novo. */
    fun confirmPending() {
        val pending = state().pendingExport ?: return
        change { it.copy(pendingExport = null) }
        when (pending.export) {
            ClientExport.PDF -> exporter.exportPdf(pending.quotes.single())
            ClientExport.IMAGE -> exporter.saveShareableImage(pending.quotes.single())
            ClientExport.WHATSAPP -> exporter.openInWhatsApp(pending.quotes.single())
            ClientExport.COPY -> performCopy(pending.quotes.single())
            ClientExport.SELECTED_PDF -> exporter.exportQuotesPdf(pending.quotes) { clearSelection() }
        }
    }

    /**
     * "Alterar prazo" no aviso de prazo vencido: desiste do envio e abre o diálogo de prazo do orçamento. Só faz
     * sentido com um orçamento só; na exportação em lote o aviso oferece apenas cancelar, porque não dá pra editar
     * vários prazos de uma vez num diálogo só.
     */
    fun changeDateOfPending() {
        val pending = state().pendingExport ?: return
        change { it.copy(pendingExport = null) }
        pending.quotes.singleOrNull()?.let(editDeliveryDate)
    }

    fun dismissPending() = change { it.copy(pendingExport = null) }

    /**
     * Segura o envio quando algum dos [quotes] tem prazo vencido e devolve `true` (a tela mostra o aviso). Vale pra todo
     * status antes de Entregue: um prazo vencido num orçamento parado pede data nova, e num pedido já aprovado é atraso
     * que o cliente vai notar; nos dois casos, mandar a data velha sem perceber é o erro que o aviso evita.
     */
    private fun holdIfOverdue(export: ClientExport, quotes: List<SavedQuote>): Boolean {
        val todayEpochDay = today()
        val overdue = quotes.filter { it.isDeliveryOverdue(todayEpochDay) }
        if (overdue.isEmpty()) return false
        val items = overdue.map { OverdueItem(it.name, it.deliveryDateEpochDay!!) }
        change { it.copy(pendingExport = PendingExport(export, quotes, items)) }
        return true
    }

    /**
     * Os marcados que a lista está mostrando. Marcar, filtrar e exportar levaria junto itens que a pessoa não vê
     * mais; a contagem do rodapé usa a mesma regra, pra o número bater com o PDF.
     */
    fun selectedVisible(): List<SavedQuote> {
        val selected = state().selectedIds
        return visibleQuotes(repository.savedQuotes.value, state().filter).filter { it.id in selected }
    }

    fun exportSelectedPdf() {
        val selected = selectedVisible()
        if (selected.isEmpty()) return
        if (holdIfOverdue(ClientExport.SELECTED_PDF, selected)) return
        exporter.exportQuotesPdf(selected) { clearSelection() }
    }

    /**
     * Catálogo pra divulgação (vários itens por página, com foto), não um orçamento formal por página. Com itens
     * marcados, vai só a seleção. Sem nenhum, no Catálogo, vão todos os produtos que a busca, a categoria e o
     * período estão mostrando (decisão 101): o catálogo é justamente essa lista.
     */
    fun exportCatalog() {
        val selected = selectedVisible().ifEmpty {
            if (state().filter.kind == QuoteKind.PRODUCT) visibleQuotes(repository.savedQuotes.value, state().filter) else emptyList()
        }
        if (selected.isEmpty()) return
        val clearsSelection = state().selectedIds.isNotEmpty()
        exporter.exportCatalogPdf(selected) { if (clearsSelection) clearSelection() }
    }
}
