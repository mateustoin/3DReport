package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CurrencyRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.ResolvedPdfBranding
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.copyToClipboard
import com.threedreport.app.platform.openUrl
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.defaultDocumentsDirectory
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.periodStartEpochMillis
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.renderCatalogPdf
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.saveBytesToFile
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel da tela de Histórico: lista os orçamentos salvos e permite
 * excluir, baixar a foto, exportar um orçamento por vez, ou selecionar
 * vários e exportá-los juntos num PDF só (um orçamento por página).
 */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
    private val currencyRepository: CurrencyRepository,
    private val today: () -> Long = ::todayEpochDay,
) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

    private val pendingExportState = MutableStateFlow<PendingExport?>(null)

    /** Envio pro cliente segurado porque o prazo de algum orçamento já venceu (ver [holdIfOverdue]). */
    val pendingExport: StateFlow<PendingExport?> = pendingExportState.asStateFlow()

    private val deliveryDateEditingState = MutableStateFlow<SavedQuote?>(null)

    /** Orçamento com o diálogo "Prazo de entrega" aberto, ou `null`. */
    val deliveryDateEditing: StateFlow<SavedQuote?> = deliveryDateEditingState.asStateFlow()

    /** Hoje, em dias desde 01/01/1970, pra tela pintar os prazos vencidos com a mesma régua do aviso. */
    fun currentEpochDay(): Long = today()

    private val copiedIdState = MutableStateFlow<String?>(null)
    val copiedId: StateFlow<String?> = copiedIdState.asStateFlow()

    private val selectedIdsState = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = selectedIdsState.asStateFlow()

    private val filterState = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = filterState.asStateFlow()

    fun setSearchQuery(query: String) = filterState.update { it.copy(query = query) }
    fun setStatusFilter(status: OrderStatus?) = filterState.update { it.copy(status = status) }
    fun setPeriodFilter(period: PeriodPreset) = filterState.update { it.copy(period = period) }

    /**
     * Troca entre Pedidos e Produtos (decisão 101). A seleção é esvaziada, pra um PDF não levar
     * junto itens marcados na outra lista sem ninguém ver, e o filtro de status sai, porque produto
     * não tem andamento.
     */
    fun setKindFilter(kind: QuoteKind) {
        filterState.update { it.copy(kind = kind, status = null) }
        clearSelection()
    }

    /** Função pura: aplica [filter] a [savedQuotes], já ordenados do mais recente pro mais antigo. */
    fun visibleQuotes(savedQuotes: List<SavedQuote>, filter: HistoryFilter): List<SavedQuote> {
        val startEpochMillis = periodStartEpochMillis(filter.period)
        val normalizedQuery = filter.query.trim()

        return savedQuotes
            .filter { savedQuote ->
                savedQuote.kind == filter.kind &&
                    (filter.status == null || savedQuote.status == filter.status) &&
                    (startEpochMillis == null || savedQuote.savedAtEpochMillis >= startEpochMillis) &&
                    (normalizedQuery.isEmpty() || savedQuote.matchesQuery(normalizedQuery))
            }
            .sortedByDescending { it.savedAtEpochMillis }
    }

    private fun SavedQuote.matchesQuery(query: String): Boolean =
        name.contains(query, ignoreCase = true) || client?.name?.contains(query, ignoreCase = true) == true

    fun updateStatus(id: String, status: OrderStatus) = repository.updateStatus(id, status)

    /**
     * "Transformar em pedido" só aparece pra produto que nunca foi vendido: depois de uma venda, o
     * produto é a origem daquele pedido, e movê-lo apagaria o catálogo de onde a venda saiu.
     */
    fun canConvertToOrder(product: SavedQuote, savedQuotes: List<SavedQuote>): Boolean =
        !product.isOrder && savedQuotes.none { it.sourceProductId == product.id }

    fun convertToOrder(id: String) = repository.convertToOrder(id)

    fun updatePrintSettings(id: String, printSettings: PrintSettings?) = repository.updatePrintSettings(id, printSettings)

    fun startEditingDeliveryDate(savedQuote: SavedQuote) {
        deliveryDateEditingState.value = savedQuote
    }

    fun cancelEditingDeliveryDate() {
        deliveryDateEditingState.value = null
    }

    /** Grava o prazo escolhido no diálogo (`null` remove) e fecha o diálogo. */
    fun saveDeliveryDate(deliveryDateEpochDay: Long?) {
        val editing = deliveryDateEditingState.value ?: return
        repository.updateDeliveryDate(editing.id, deliveryDateEpochDay)
        deliveryDateEditingState.value = null
    }

    /**
     * "Enviar assim mesmo" no aviso de prazo vencido: faz o envio que tinha sido segurado, sem
     * perguntar de novo.
     */
    fun confirmPendingExport() {
        val pending = pendingExportState.value ?: return
        pendingExportState.value = null
        when (pending.export) {
            ClientExport.PDF -> performExportPdf(pending.quotes.single())
            ClientExport.IMAGE -> performSaveShareableImage(pending.quotes.single())
            ClientExport.WHATSAPP -> performOpenInWhatsApp(pending.quotes.single())
            ClientExport.COPY -> performCopyQuoteToClipboard(pending.quotes.single())
            ClientExport.SELECTED_PDF -> performExportSelectedPdf(pending.quotes)
        }
    }

    /**
     * "Alterar prazo" no aviso de prazo vencido: desiste do envio e abre o diálogo de prazo do
     * orçamento. Só faz sentido com um orçamento só; na exportação em lote o aviso oferece apenas
     * cancelar, porque não dá pra editar vários prazos de uma vez num diálogo só.
     */
    fun changeDateOfPendingExport() {
        val pending = pendingExportState.value ?: return
        pendingExportState.value = null
        pending.quotes.singleOrNull()?.let(::startEditingDeliveryDate)
    }

    fun dismissPendingExport() {
        pendingExportState.value = null
    }

    /**
     * Segura o envio quando algum dos [quotes] tem prazo vencido e devolve `true` (a tela mostra o
     * aviso a partir de [pendingExport]). Vale pra todo status antes de Entregue: um prazo vencido
     * num orçamento parado pede data nova, e num pedido já aprovado é atraso que o cliente vai
     * notar; nos dois casos, mandar a data velha sem perceber é o erro que o aviso evita.
     */
    private fun holdIfOverdue(export: ClientExport, quotes: List<SavedQuote>): Boolean {
        val todayEpochDay = today()
        if (quotes.none { it.isDeliveryOverdue(todayEpochDay) }) return false
        pendingExportState.value = PendingExport(export, quotes)
        return true
    }

    fun delete(id: String) {
        repository.delete(id)
        selectedIdsState.update { it - id }
    }

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        saveBytesToFile(bytes, savedQuote.photoFileName ?: "foto.png")
    }

    fun downloadStl(savedQuote: SavedQuote) {
        val bytes = repository.stlBytes(savedQuote) ?: return
        saveBytesToFile(bytes, savedQuote.stlFileName ?: "modelo.stl")
    }

    // Os envios pro cliente abaixo passam por holdIfOverdue: com prazo vencido, a tela pergunta
    // antes (ver pendingExport). O catálogo não, porque não mostra prazo.

    fun exportPdf(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.PDF, listOf(savedQuote))) return
        performExportPdf(savedQuote)
    }

    /** Abre a conversa do cliente no WhatsApp com o orçamento já escrito (ver [toWhatsAppLink]). */
    fun openInWhatsApp(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.WHATSAPP, listOf(savedQuote))) return
        performOpenInWhatsApp(savedQuote)
    }

    /**
     * Gera a imagem quadrada pra mandar no WhatsApp/status e abre o "salvar como". Usa o mesmo
     * texto de marca da marca d'água do PDF, pra o material do vendedor sair coerente entre os dois.
     */
    fun saveShareableImage(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.IMAGE, listOf(savedQuote))) return
        performSaveShareableImage(savedQuote)
    }

    fun copyQuoteToClipboard(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.COPY, listOf(savedQuote))) return
        performCopyQuoteToClipboard(savedQuote)
    }

    fun toggleSelection(id: String) {
        selectedIdsState.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selectedIdsState.value = emptySet()
    }

    fun exportSelectedPdf() {
        val selected = savedQuotes.value.filter { it.id in selectedIdsState.value }
        if (selected.isEmpty()) return
        if (holdIfOverdue(ClientExport.SELECTED_PDF, selected)) return
        performExportSelectedPdf(selected)
    }

    /**
     * Catálogo pra divulgação (vários itens por página, com foto), não um orçamento formal por página.
     * Com itens marcados, vai só a seleção. Sem nenhum, na lista de Produtos, vão todos os produtos
     * que a busca e o período estão mostrando (decisão 101): o catálogo é justamente essa lista.
     */
    fun exportCatalogPdf() {
        val all = savedQuotes.value
        val selected = if (selectedIdsState.value.isNotEmpty()) {
            all.filter { it.id in selectedIdsState.value }
        } else if (filterState.value.kind == QuoteKind.PRODUCT) {
            visibleQuotes(all, filterState.value)
        } else {
            emptyList()
        }
        if (selected.isEmpty()) return

        val branding = resolvePdfBranding()
        val items = selected.map { QuoteExportItem(it, photoBytes(it)) }
        val pdfBytes = renderCatalogPdf(items, branding.watermarkText, branding.footerText, currencyRepository.currency.value, branding.options)

        saveBytesToFile(
            pdfBytes,
            "${sanitizeFileName("Catálogo (${selected.size} itens)")}.pdf",
            defaultDocumentsDirectory(),
        )
        clearSelection()
    }

    private fun performExportPdf(savedQuote: SavedQuote) {
        val branding = resolvePdfBranding()
        val item = QuoteExportItem(savedQuote, photoBytes(savedQuote))
        val pdfBytes = renderSavedQuotesPdf(listOf(item), branding.watermarkText, branding.footerText, currencyRepository.currency.value, branding.options)
        saveBytesToFile(pdfBytes, "${sanitizeFileName(savedQuote.name)}.pdf", defaultDocumentsDirectory())
    }

    private fun performOpenInWhatsApp(savedQuote: SavedQuote) {
        openUrl(savedQuote.toWhatsAppLink(currencyRepository.currency.value, brandingRepository.branding.value.showPrintTime))
    }

    private fun performSaveShareableImage(savedQuote: SavedQuote) {
        val currency = currencyRepository.currency.value
        val quantity = savedQuote.quote.quantity
        val bytes = renderQuoteImage(
            title = savedQuote.name,
            priceText = savedQuote.totalWithServices.toCurrencyText(currency),
            unitPriceText = if (quantity > 1) {
                "$quantity peças · ${(savedQuote.totalWithServices / quantity).toCurrencyText(currency)} cada"
            } else {
                null
            },
            photoBytes = photoBytes(savedQuote),
            brandText = brandingRepository.branding.value.imageBrandLine(),
            // Curta ("30/09", sem o ano) porque a imagem é pra conversa do dia, não documento.
            deliveryText = savedQuote.deliveryDateEpochDay?.let { "Entrega até ${formatShortDate(it)}" },
        )
        saveBytesToFile(bytes, "${sanitizeFileName(savedQuote.name)}.png", defaultDocumentsDirectory())
    }

    private fun performCopyQuoteToClipboard(savedQuote: SavedQuote) {
        copyToClipboard(savedQuote.toCopyPasteText(currencyRepository.currency.value, brandingRepository.branding.value.showPrintTime))
        copiedIdState.value = savedQuote.id
    }

    private fun performExportSelectedPdf(selected: List<SavedQuote>) {
        val branding = resolvePdfBranding()
        val items = selected.map { QuoteExportItem(it, photoBytes(it)) }
        val pdfBytes = renderSavedQuotesPdf(items, branding.watermarkText, branding.footerText, currencyRepository.currency.value, branding.options)

        saveBytesToFile(
            pdfBytes,
            "${sanitizeFileName("Orçamentos (${selected.size} itens)")}.pdf",
            defaultDocumentsDirectory(),
        )
        clearSelection()
    }

    /** Marca, logo, contato e opções do PDF, resolvidos do jeito que a prévia de Configurações também usa. */
    private fun resolvePdfBranding(): ResolvedPdfBranding =
        brandingRepository.branding.value.resolvePdfBranding(brandingRepository.logoBytes())

    private fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("")
}

/** Os envios pro cliente que mostram o prazo, e por isso passam pelo aviso de prazo vencido. */
enum class ClientExport { PDF, IMAGE, WHATSAPP, COPY, SELECTED_PDF }

/** Envio segurado pelo aviso de prazo vencido: o que ia ser feito, e com quais orçamentos. */
data class PendingExport(val export: ClientExport, val quotes: List<SavedQuote>)
