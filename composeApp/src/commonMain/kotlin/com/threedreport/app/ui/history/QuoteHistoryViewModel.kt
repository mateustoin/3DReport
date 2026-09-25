package com.threedreport.app.ui.history

import com.threedreport.app.AppLog
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteDetails
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PeriodPreset
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.ResolvedPdfBranding
import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.contains
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.renderCatalogPdf
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.NoticeIds
import com.threedreport.app.ui.components.UserNotice
import com.threedreport.app.ui.components.toNotice
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.ProductRepricer
import com.threedreport.core.pricing.RepriceResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel da tela de Histórico: lista os orçamentos salvos e permite excluir, baixar a foto,
 * exportar um orçamento por vez, ou selecionar vários e exportá-los juntos num PDF só (um orçamento
 * por página).
 *
 * Toda ação que não muda a lista na hora termina num [notice] (decisão 108): o PDF salvo com "Abrir
 * pasta", o erro de um arquivo aberto em outro programa, o item excluído com "Desfazer". Gerar PDF e
 * imagem roda em [background] com [busy] ligado; os testes passam o padrão, `Unconfined`, e tudo
 * acontece na hora.
 */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
    salesChannelRepository: SalesChannelRepository,
    private val today: () -> Long = ::todayEpochDay,
    /** Cadastro de clientes (decisão 106), pro "Editar detalhes" ligar o pedido ao cliente. */
    private val clientRepository: ClientRepository? = null,
    private val clock: Clock = Clock.System,
    private val platform: PlatformServices = defaultPlatform,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val background: CoroutineDispatcher = Dispatchers.Unconfined,
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

    /** Clientes do cadastro, pras sugestões do "Editar detalhes". */
    val clients: StateFlow<List<Client>> = clientRepository?.clients ?: MutableStateFlow(emptyList())

    private val noticeIds = NoticeIds()
    private val noticeState = MutableStateFlow<UserNotice?>(null)

    /** O retorno da última ação, pra barra de avisos. */
    val notice: StateFlow<UserNotice?> = noticeState.asStateFlow()

    fun consumeNotice(shown: UserNotice) = noticeState.update { if (it == shown) null else it }

    private fun notify(message: String, actionLabel: String? = null, isError: Boolean = false, action: (() -> Unit)? = null) {
        noticeState.value = UserNotice(message, actionLabel, action, isError, noticeIds.next())
    }

    private val busyState = MutableStateFlow<String?>(null)

    /** O que está sendo gerado agora ("Gerando PDF…"), ou `null`. */
    val busy: StateFlow<String?> = busyState.asStateFlow()

    private val pendingExportState = MutableStateFlow<PendingExport?>(null)

    /** Envio pro cliente segurado porque o prazo de algum orçamento já venceu (ver [holdIfOverdue]). */
    val pendingExport: StateFlow<PendingExport?> = pendingExportState.asStateFlow()

    private val deliveryDateEditingState = MutableStateFlow<SavedQuote?>(null)

    /** Orçamento com o diálogo "Prazo de entrega" aberto, ou `null`. */
    val deliveryDateEditing: StateFlow<SavedQuote?> = deliveryDateEditingState.asStateFlow()

    private val detailsEditingState = MutableStateFlow<SavedQuote?>(null)

    /** Orçamento com o diálogo "Editar detalhes" aberto, ou `null`. */
    val detailsEditing: StateFlow<SavedQuote?> = detailsEditingState.asStateFlow()

    /** Hoje, em dias desde 01/01/1970, pra tela pintar os prazos vencidos com a mesma régua do aviso. */
    fun currentEpochDay(): Long = today()

    /** Agora, pro recorte de "entregues recentes" do Kanban. */
    fun nowMillis(): Long = clock.nowMillis()

    private val copiedIdState = MutableStateFlow<String?>(null)

    /** Orçamento cujo texto acabou de ser copiado, pro botão dizer "Copiado!" por um instante. */
    val copiedId: StateFlow<String?> = copiedIdState.asStateFlow()

    fun clearCopied() {
        copiedIdState.value = null
    }

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
        filterState.update { it.copy(kind = kind, status = null, category = CategoryFilter.All) }
        clearSelection()
    }

    fun setCategoryFilter(category: CategoryFilter) = filterState.update { it.copy(category = category) }

    /** Categorias dos produtos, uma por grafia, em ordem alfabética, pros chips de filtro. */
    fun productCategories(savedQuotes: List<SavedQuote>): List<String> =
        savedQuotes.filterNot { it.isOrder }.mapNotNull { it.category }
            .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

    private val repricingState = MutableStateFlow<Repricing?>(null)

    /** Produto com o diálogo "Atualizar preço" aberto, com o antes e o depois, ou `null`. */
    val repricing: StateFlow<Repricing?> = repricingState.asStateFlow()

    // Cadastros que o recálculo do produto usa. A tela coleta os quatro, pra o aviso "Custos
    // mudaram" aparecer (ou sumir) assim que um filamento ou a hora de trabalho mudar.
    val filaments: StateFlow<List<Filament>> = filamentRepository.filaments
    val printers: StateFlow<List<PrinterProfile>> = printerRepository.printers
    val settings: StateFlow<PricingSettings> = settingsRepository.settings
    val salesChannels: StateFlow<List<SalesChannel>> = salesChannelRepository.channels

    /**
     * O produto recalculado com os cadastros de hoje (decisão 102), pro card avisar quando os
     * custos mudaram. Pedido não passa por aqui, porque é retrato congelado da venda.
     */
    fun repriceFor(
        product: SavedQuote,
        filaments: List<Filament> = this.filaments.value,
        printers: List<PrinterProfile> = this.printers.value,
        settings: PricingSettings = this.settings.value,
        channels: List<SalesChannel> = salesChannels.value,
    ): RepriceResult = ProductRepricer.reprice(product, filaments, printers, settings, channels)

    /** Abre o diálogo com o antes e o depois; não faz nada se não der pra recalcular ou se nada mudou. */
    fun startRepricing(product: SavedQuote) {
        val result = repriceFor(product) as? RepriceResult.Repriced ?: return
        if (!result.changed) return
        repricingState.value = Repricing(product, result.quote)
    }

    fun cancelRepricing() {
        repricingState.value = null
    }

    fun confirmRepricing() {
        val pending = repricingState.value ?: return
        repository.updateQuote(pending.product.id, pending.newQuote)
        repricingState.value = null
        notify("Preço de \"${pending.product.name}\" atualizado.")
    }

    /** Função pura: aplica [filter] a [savedQuotes], do mais recente pro mais antigo. */
    fun visibleQuotes(savedQuotes: List<SavedQuote>, filter: HistoryFilter): List<SavedQuote> {
        val normalizedQuery = filter.query.trim()
        return savedQuotes
            .filter { savedQuote ->
                savedQuote.kind == filter.kind &&
                    savedQuote.matchesCategory(filter.category) &&
                    (filter.status == null || savedQuote.status == filter.status) &&
                    filter.period.contains(savedQuote.savedAtEpochMillis) &&
                    (normalizedQuery.isEmpty() || savedQuote.matchesQuery(normalizedQuery))
            }
            .sortedByDescending { it.savedAtEpochMillis }
    }

    /**
     * Função pura: os pedidos do Kanban. Pedido em andamento aparece sempre, qualquer que seja o
     * período (sumir com um pedido aprovado porque é do mês passado escondia trabalho a fazer). Entregue
     * mostra só os entregues nos últimos [recentDays] dias, a não ser com [showAllDelivered]: com o tempo,
     * a coluna viraria o arquivo morto inteiro. Cancelado fica fora do quadro, que é o fluxo.
     */
    fun kanbanQuotes(
        savedQuotes: List<SavedQuote>,
        filter: HistoryFilter,
        showAllDelivered: Boolean,
        nowEpochMillis: Long,
        recentDays: Int = RECENT_DELIVERED_DAYS,
    ): List<SavedQuote> {
        val normalizedQuery = filter.query.trim()
        val recentStart = nowEpochMillis - recentDays * DAY_MILLIS
        return savedQuotes
            .filter { savedQuote ->
                savedQuote.isOrder &&
                    savedQuote.status in OrderStatus.PIPELINE &&
                    (normalizedQuery.isEmpty() || savedQuote.matchesQuery(normalizedQuery)) &&
                    (showAllDelivered || savedQuote.status != OrderStatus.ENTREGUE || (savedQuote.deliveredAtEpochMillis ?: 0) >= recentStart)
            }
            .sortedByDescending { it.savedAtEpochMillis }
    }

    private fun SavedQuote.matchesCategory(filter: CategoryFilter): Boolean = when (filter) {
        CategoryFilter.All -> true
        CategoryFilter.None -> category == null
        is CategoryFilter.Named -> category.equals(filter.name, ignoreCase = true)
    }

    private fun SavedQuote.matchesQuery(query: String): Boolean =
        name.contains(query, ignoreCase = true) ||
            client?.name?.contains(query, ignoreCase = true) == true ||
            displayNumber?.contains(query) == true

    /**
     * Muda o andamento. Quando o pedido sai da lista porque ela está filtrada por outro status, o aviso
     * diz pra onde ele foi, com o atalho de ver todos; cancelar oferece desfazer.
     */
    fun updateStatus(id: String, status: OrderStatus) {
        val before = savedQuotes.value.find { it.id == id } ?: return
        if (before.status == status) return
        repository.updateStatus(id, status)
        val statusFilter = filterState.value.status
        when {
            statusFilter != null && statusFilter != status -> notify(
                "\"${before.name}\" foi pra ${status.label} e saiu da lista filtrada.",
                actionLabel = "Ver todos",
                action = { setStatusFilter(null) },
            )
            status == OrderStatus.CANCELADO -> notify(
                "\"${before.name}\" cancelado.",
                actionLabel = "Desfazer",
                action = { repository.updateStatus(id, before.status) },
            )
        }
    }

    /**
     * Produtos que já originaram algum pedido (pelo "Vender"). Calculado uma vez por lista, e não por
     * card: perguntar produto a produto percorria a lista inteira pra cada um.
     */
    fun soldProductIds(savedQuotes: List<SavedQuote>): Set<String> = savedQuotes.mapNotNullTo(HashSet()) { it.sourceProductId }

    /**
     * "Transformar em pedido" só aparece pra produto que nunca foi vendido: depois de uma venda, o
     * produto é a origem daquele pedido, e movê-lo apagaria o catálogo de onde a venda saiu.
     */
    fun canConvertToOrder(product: SavedQuote, soldProductIds: Set<String>): Boolean =
        !product.isOrder && product.id !in soldProductIds

    fun convertToOrder(id: String) {
        val product = savedQuotes.value.find { it.id == id } ?: return
        repository.convertToOrder(id)
        selectedIdsState.update { it - id }
        notify("\"${product.name}\" agora é um pedido, em Orçado.", actionLabel = "Ver pedidos", action = { setKindFilter(QuoteKind.ORDER) })
    }

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

    /** Abre o "Editar detalhes": nome, cliente, foto, prazo, link e categoria, sem recalcular nada. */
    fun startEditingDetails(savedQuote: SavedQuote) {
        detailsEditingState.value = savedQuote
    }

    fun cancelEditingDetails() {
        detailsEditingState.value = null
    }

    /** A foto guardada de [savedQuote], como arquivo, pra o "Editar detalhes" começar com ela. */
    fun photoFile(savedQuote: SavedQuote): PickedFile? {
        val fileName = savedQuote.photoFileName ?: return null
        return photoBytes(savedQuote)?.let { PickedFile(fileName, it) }
    }

    /** Escolhe uma foto nova pro "Editar detalhes". Devolve o motivo quando o arquivo não serve. */
    fun pickPhoto(): PhotoPick = when (val picked = platform.pickFile(FileKind.IMAGE)) {
        PickResult.Cancelled -> PhotoPick.Cancelled
        is PickResult.Failed -> PhotoPick.Failed(picked.message)
        is PickResult.Picked -> if (runCatching { decodeImageBitmap(picked.file.bytes) }.isSuccess) {
            PhotoPick.Picked(picked.file)
        } else {
            PhotoPick.Failed("Não consegui abrir ${picked.file.fileName} como imagem. Use PNG ou JPG.")
        }
    }

    /** Grava o "Editar detalhes". O cliente digitado é achado ou criado no cadastro, como ao salvar o pedido. */
    fun saveDetails(form: DetailsForm) {
        val editing = detailsEditingState.value ?: return
        val clientName = form.clientName.trim()
        val client = if (!editing.isOrder || clientName.isEmpty()) {
            null
        } else {
            val typed = Client(name = clientName, contact = form.clientContact.trim().ifEmpty { null }, id = form.clientId)
            clientRepository?.resolve(typed) ?: typed
        }
        val saved = repository.updateDetails(
            editing.id,
            QuoteDetails(
                name = form.name,
                client = client,
                sourceLink = form.sourceLink,
                photo = form.photo,
                deliveryDateEpochDay = form.deliveryDateEpochDay,
                category = form.category,
            ),
        )
        detailsEditingState.value = null
        if (saved != null) notify("Detalhes de \"${saved.name}\" salvos.")
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

    /** Manda pra lixeira, com "Desfazer" no aviso (os dados ficam 30 dias antes de sumir de vez). */
    fun delete(id: String) {
        val deleted = savedQuotes.value.find { it.id == id } ?: return
        repository.delete(id)
        selectedIdsState.update { it - id }
        notify("\"${deleted.name}\" excluído.", actionLabel = "Desfazer", action = { repository.restore(id) })
    }

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    /** Miniatura da foto pro card, gerada uma vez e guardada (a foto de celular inteira travava a lista). */
    fun photoThumbnail(savedQuote: SavedQuote): ByteArray? = repository.photoThumbnail(savedQuote, THUMBNAIL_PX)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        val extension = savedQuote.photoFileName?.substringAfterLast('.', "png") ?: "png"
        report(platform.saveFile(bytes, "${fileNameFor(savedQuote)}.$extension"), "Foto salva.")
    }

    fun downloadStl(savedQuote: SavedQuote) {
        val bytes = repository.stlBytes(savedQuote) ?: return
        report(platform.saveFile(bytes, "${fileNameFor(savedQuote)}.stl"), "STL salvo.")
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

    /**
     * Os marcados que a lista está mostrando. Marcar, filtrar e exportar levaria junto itens que a pessoa
     * não vê mais; a contagem da barra de seleção usa a mesma regra, pra o número bater com o PDF.
     */
    fun selectedVisible(): List<SavedQuote> {
        val selected = selectedIdsState.value
        return visibleQuotes(savedQuotes.value, filterState.value).filter { it.id in selected }
    }

    fun exportSelectedPdf() {
        val selected = selectedVisible()
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
        val selected = selectedVisible().ifEmpty {
            if (filterState.value.kind == QuoteKind.PRODUCT) visibleQuotes(savedQuotes.value, filterState.value) else emptyList()
        }
        if (selected.isEmpty()) return
        val clearsSelection = selectedIdsState.value.isNotEmpty()
        saveGenerated("${sanitizeFileName("Catálogo (${selected.size} itens)")}.pdf", "Gerando catálogo…", "Catálogo salvo.", clearsSelection) {
            val branding = resolvePdfBranding()
            renderCatalogPdf(selected.map { QuoteExportItem(it, photoBytes(it)) }, branding.brandName, branding.footerText, branding.options)
        }
    }

    private fun performExportPdf(savedQuote: SavedQuote) {
        saveGenerated("${fileNameFor(savedQuote)}.pdf", "Gerando PDF…", "PDF salvo.") {
            val branding = resolvePdfBranding()
            renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photoBytes(savedQuote))), branding.brandName, branding.footerText, branding.options)
        }
    }

    private fun performOpenInWhatsApp(savedQuote: SavedQuote) {
        val showPrintTime = brandingRepository.branding.value.showPrintTime
        if (platform.openUrl(savedQuote.toWhatsAppLink(savedQuote.currency, showPrintTime))) return
        // Sem navegador configurado, o texto ainda chega no cliente: vai pra área de transferência.
        val copied = platform.copyToClipboard(savedQuote.toCopyPasteText(savedQuote.currency, showPrintTime))
        notify(
            if (copied) {
                "Não consegui abrir o WhatsApp. O texto do orçamento foi copiado: cole na conversa com o cliente."
            } else {
                "Não consegui abrir o WhatsApp nem copiar o texto. Use \"Copiar\" de novo em alguns segundos."
            },
            isError = true,
        )
    }

    private fun performSaveShareableImage(savedQuote: SavedQuote) {
        saveGenerated("${fileNameFor(savedQuote)}.png", "Gerando imagem…", "Imagem salva.") {
            val currency = savedQuote.currency
            val quantity = savedQuote.quote.quantity
            val piecesTotal = savedQuote.totalWithServices - savedQuote.shippingCost
            renderQuoteImage(
                title = savedQuote.name.takeUnless { savedQuote.hasAutoName } ?: "Orçamento",
                priceText = savedQuote.totalWithServices.toCurrencyText(currency),
                unitPriceText = if (quantity > 1) "$quantity peças · ${(piecesTotal / quantity).toCurrencyText(currency)} cada" else null,
                photoBytes = photoBytes(savedQuote),
                brandText = brandingRepository.branding.value.imageBrandLine(),
                // Curta ("30/09", sem o ano) porque a imagem é pra conversa do dia, não documento.
                deliveryText = savedQuote.deliveryDateEpochDay?.let { "Entrega até ${formatShortDate(it)}" },
            )
        }
    }

    private fun performCopyQuoteToClipboard(savedQuote: SavedQuote) {
        val text = savedQuote.toCopyPasteText(savedQuote.currency, brandingRepository.branding.value.showPrintTime)
        if (platform.copyToClipboard(text)) {
            copiedIdState.value = savedQuote.id
        } else {
            notify("Não consegui copiar: outro programa está usando a área de transferência. Tente de novo.", isError = true)
        }
    }

    private fun performExportSelectedPdf(selected: List<SavedQuote>) {
        saveGenerated("${sanitizeFileName("Orçamentos (${selected.size} itens)")}.pdf", "Gerando PDF…", "PDF salvo.", clearsSelection = true) {
            val branding = resolvePdfBranding()
            renderSavedQuotesPdf(selected.map { QuoteExportItem(it, photoBytes(it)) }, branding.brandName, branding.footerText, branding.options)
        }
    }

    /**
     * Pergunta onde salvar, gera o arquivo fora do thread da tela e grava. A seleção só é esvaziada
     * quando o arquivo foi salvo: cancelar o "Salvar como" não pode jogar fora o que a pessoa marcou.
     */
    private fun saveGenerated(
        suggestedFileName: String,
        busyMessage: String,
        savedMessage: String,
        clearsSelection: Boolean = false,
        generate: () -> ByteArray,
    ) {
        if (busyState.value != null) return
        val path = platform.chooseSaveLocation(suggestedFileName) ?: return
        busyState.value = busyMessage
        scope.launch(background) {
            val result = try {
                platform.writeFile(path, generate())
            } catch (e: Exception) {
                AppLog.warn("Falha ao gerar $suggestedFileName", e)
                SaveResult.Failed("Não consegui gerar o arquivo. Os detalhes ficaram no log do app.")
            }
            withContext(main) {
                busyState.value = null
                if (clearsSelection && result is SaveResult.Saved) clearSelection()
                report(result, savedMessage)
            }
        }
    }

    private fun report(result: SaveResult, savedMessage: String) {
        result.toNotice(savedMessage, noticeIds.next())?.let { noticeState.value = it }
    }

    /** Marca, logo, contato e opções do PDF, resolvidos do jeito que a prévia de Configurações também usa. */
    private fun resolvePdfBranding(): ResolvedPdfBranding =
        brandingRepository.branding.value.resolvePdfBranding(brandingRepository.logoBytes())

    /** Nome do arquivo pro cliente: o nome da peça, e o número quando o nome foi o automático. */
    private fun fileNameFor(savedQuote: SavedQuote): String = sanitizeFileName(
        if (savedQuote.hasAutoName) "Orçamento ${savedQuote.displayNumber?.removePrefix("#") ?: ""}".trim() else savedQuote.name,
    )

    private fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("").trim().ifEmpty { "orcamento" }

    companion object {
        const val THUMBNAIL_PX = 256
        const val RECENT_DELIVERED_DAYS = 30
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

/** "Atualizar preço" aberto: o produto como está e como ficaria com os cadastros de hoje. */
data class Repricing(val product: SavedQuote, val newQuote: Quote)

/** Os envios pro cliente que mostram o prazo, e por isso passam pelo aviso de prazo vencido. */
enum class ClientExport { PDF, IMAGE, WHATSAPP, COPY, SELECTED_PDF }

/** Envio segurado pelo aviso de prazo vencido: o que ia ser feito, e com quais orçamentos. */
data class PendingExport(val export: ClientExport, val quotes: List<SavedQuote>)

/** O que o "Editar detalhes" grava. [photo] `null` remove a foto. */
data class DetailsForm(
    val name: String,
    val clientName: String = "",
    val clientContact: String = "",
    val clientId: String? = null,
    val sourceLink: String = "",
    val photo: PickedFile? = null,
    val deliveryDateEpochDay: Long? = null,
    val category: String = "",
)

/** Resultado de escolher a foto no "Editar detalhes". */
sealed interface PhotoPick {
    data class Picked(val file: PickedFile) : PhotoPick
    data object Cancelled : PhotoPick
    data class Failed(val message: String) : PhotoPick
}
