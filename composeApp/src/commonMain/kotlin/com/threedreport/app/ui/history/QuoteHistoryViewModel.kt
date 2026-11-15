package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_undo
import _3dreport.composeapp.generated.resources.export_copy_failed
import _3dreport.composeapp.generated.resources.history_error_photo_unreadable
import _3dreport.composeapp.generated.resources.history_error_pick_failed
import _3dreport.composeapp.generated.resources.history_notice_converted
import _3dreport.composeapp.generated.resources.history_notice_deleted
import _3dreport.composeapp.generated.resources.history_notice_details_saved
import _3dreport.composeapp.generated.resources.history_notice_photo_saved
import _3dreport.composeapp.generated.resources.history_notice_repriced
import _3dreport.composeapp.generated.resources.history_notice_see_all
import _3dreport.composeapp.generated.resources.history_notice_see_orders
import _3dreport.composeapp.generated.resources.history_notice_status_cancelled
import _3dreport.composeapp.generated.resources.history_notice_status_moved
import _3dreport.composeapp.generated.resources.history_notice_stl_saved
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OrdersView
import com.threedreport.app.data.PreferencesRepository
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
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.OverdueItem
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.quote.deliveryDaysLate
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.ProductRepricer
import com.threedreport.core.pricing.RepriceResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel das telas de Pedidos e do Catálogo (decisão 111: cada uma tem o seu, com o tipo fixo em
 * [HistoryUiState.filter]). Expõe um [uiState] só e os [notices] (o retorno de uma ação que não muda a lista na
 * hora, decisão 108: o PDF salvo com "Abrir pasta", o item excluído com "Desfazer").
 *
 * Linhas, cartões, colunas do kanban e blocos de resumo saem prontos em [HistoryUiState]; a tela só desenha. Os
 * cadastros, as preferências e a lista de orçamentos são acompanhados dentro de [scope]; o resto muda na hora,
 * pelos eventos. Gerar PDF e imagem roda em [background]; os testes passam `Unconfined` e tudo acontece na hora.
 */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
    private val filamentRepository: FilamentRepository,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
    private val salesChannelRepository: SalesChannelRepository,
    /** Onde a visualização escolhida fica guardada (decisões 129 e 132). */
    private val preferencesRepository: PreferencesRepository,
    private val today: () -> Long = ::todayEpochDay,
    /** Cadastro de clientes (decisão 106), pro "Editar detalhes" ligar o pedido ao cliente. */
    private val clientRepository: ClientRepository? = null,
    private val clock: Clock = Clock.System,
    private val platform: PlatformServices = defaultPlatform,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val background: CoroutineDispatcher = Dispatchers.Unconfined,
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
    /** Pedidos ou Catálogo (decisão 111): cada tela tem o seu ViewModel, com o tipo fixo. */
    kind: QuoteKind = QuoteKind.ORDER,
    /** Leva pra tela de Pedidos, pro "Ver pedidos" do aviso depois de transformar um produto em pedido. */
    private val showOrders: () -> Unit = {},
    /** Cadastro de insumos (decisão 122), pro catálogo vivo recalcular com o custo de hoje de cada um. */
    private val consumableRepository: ConsumableRepository? = null,
    /** A moeda do app, pra o que os blocos de resumo e as colunas somam. */
    private val currency: () -> Currency = { Currency.BRL },
) {
    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)

    /** O retorno da última ação, pra barra de avisos. */
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    // A geração dos arquivos pro cliente é a mesma do "Enviar pro cliente" do Orçamento (ver [QuoteExporter]).
    private val exporter = QuoteExporter(platform, brandingRepository, repository, scope, background, main) { notice -> noticeChannel.trySend(notice) }

    // O recálculo de cada produto é caro e não depende do filtro: refeito só quando um cadastro muda.
    private var repriceInputs: List<Any?> = emptyList()
    private var repriced: Map<String, RepriceResult> = emptyMap()

    private val state = MutableStateFlow(HistoryUiState(filter = HistoryFilter(kind = kind)).refreshed())
    val uiState: StateFlow<HistoryUiState> = state.asStateFlow()

    init {
        // Qualquer coisa que a tela lê e que mude em outro lugar (um filamento, o cliente, a visualização) refaz o estado.
        val sources: List<Flow<*>> = listOf(
            repository.savedQuotes,
            filamentRepository.filaments,
            printerRepository.printers,
            settingsRepository.settings,
            salesChannelRepository.channels,
            preferencesRepository.preferences,
            consumableRepository?.consumables ?: MutableStateFlow(emptyList<Consumable>()),
            clientRepository?.clients ?: MutableStateFlow(emptyList<Client>()),
        )
        combine(sources) { }.onEach { change { it } }.launchIn(scope)
        exporter.busy.onEach { busy -> change { it.copy(busy = busy) } }.launchIn(scope)
    }

    private fun change(transform: (HistoryUiState) -> HistoryUiState) = state.update { transform(it).refreshed() }

    // ---- o que a tela mostra, calculado a partir dos repositórios e do que a pessoa já mexeu

    private fun repricedProducts(all: List<SavedQuote>): Map<String, RepriceResult> {
        val inputs = listOf(
            all,
            filamentRepository.filaments.value,
            printerRepository.printers.value,
            settingsRepository.settings.value,
            salesChannelRepository.channels.value,
            consumableRepository?.consumables?.value,
        )
        val unchanged = inputs.size == repriceInputs.size && inputs.indices.all { inputs[it] === repriceInputs[it] }
        if (!unchanged) {
            repriceInputs = inputs
            repriced = all.filterNot { it.isOrder }.associate { it.id to repriceFor(it) }
        }
        return repriced
    }

    private fun HistoryUiState.refreshed(): HistoryUiState {
        val all = repository.savedQuotes.value
        val today = today()
        val prefs = preferencesRepository.preferences.value
        val orders = filter.kind == QuoteKind.ORDER
        val categories = if (orders) emptyList() else productCategories(all)
        // A categoria escolhida sumiu (o último produto dela foi excluído ou mudou): volta pra "Todas", senão a
        // lista ficaria vazia com um filtro que nem aparece mais.
        val chosen = filter.category as? CategoryFilter.Named
        val current = if (chosen != null && categories.none { it.equals(chosen.name, ignoreCase = true) }) filter.copy(category = CategoryFilter.All) else filter
        val visible = visibleQuotes(all, current)
        val sold = soldProductIds(all)
        val repricedProducts = if (orders) emptyMap() else repricedProducts(all)
        fun rowOf(quote: SavedQuote): HistoryRow {
            val notice = repricedProducts[quote.id]?.toNotice()
            return HistoryRow(
                quote = quote,
                deadline = if (orders) quote.deadlineBadge(today) else null,
                printCount = quote.quote.prints.size.takeIf { it > 1 },
                reprice = notice,
                canReprice = notice is RepriceNotice.CostsChanged,
                canConvertToOrder = canConvertToOrder(quote, sold),
                canEditPrintSettings = quote.quote.prints.size == 1,
            )
        }

        val kanban = if (orders) kanbanQuotes(all, current, showAllDelivered, clock.nowMillis()) else emptyList()
        val allDelivered = if (orders) kanbanQuotes(all, current, true, clock.nowMillis()).count { it.status == OrderStatus.ENTREGUE } else 0
        val byStatus = kanban.groupBy { it.status }
        return copy(
            filter = current,
            ordersView = prefs.ordersView,
            catalogView = prefs.catalogView,
            rows = (if (orders) visible.sortedByDeadline() else visible).map(::rowOf),
            columns = if (orders) {
                OrderStatus.PIPELINE.map { status ->
                    val cards = byStatus[status].orEmpty()
                    KanbanColumnState(status, cards.map(::rowOf), cards.sumOf { it.totalWithServices })
                }
            } else {
                emptyList()
            },
            summary = if (orders) ordersSummary(all, today) else OrdersSummary(),
            hasItems = all.any { it.kind == filter.kind },
            hiddenDelivered = allDelivered - kanban.count { it.status == OrderStatus.ENTREGUE },
            cancelledCount = if (orders) all.count { it.isOrder && it.status == OrderStatus.CANCELADO } else 0,
            categories = categories,
            hasUncategorized = all.any { !it.isOrder && it.category == null },
            selectedVisibleCount = visible.count { it.id in selectedIds },
            clients = clientRepository?.clients?.value.orEmpty(),
            today = today,
            currency = currency(),
        )
    }

    // ---- filtros e visualização

    fun setSearchQuery(query: String) = change { it.copy(filter = it.filter.copy(query = query)) }
    fun setStatusFilter(status: OrderStatus?) = change { it.copy(filter = it.filter.copy(status = status)) }
    fun setPeriodFilter(period: PeriodPreset) = change { it.copy(filter = it.filter.copy(period = period)) }
    fun setCategoryFilter(category: CategoryFilter) = change { it.copy(filter = it.filter.copy(category = category)) }

    /** Lembra a visualização de Pedidos pra próxima abertura (decisão 129). */
    fun setOrdersView(view: OrdersView) {
        preferencesRepository.setOrdersView(view)
        // A seleção só existe na lista: trocar de visualização a desfaz, e não deixa marcados escondidos no kanban.
        change { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    /** Lembra a visualização do Catálogo pra próxima abertura (decisão 132). */
    fun setCatalogView(view: CatalogView) {
        preferencesRepository.setCatalogView(view)
        change { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun setShowAllDelivered(show: Boolean) = change { it.copy(showAllDelivered = show) }

    // ---- andamento, exclusão e transformar em pedido

    /**
     * Muda o andamento. Quando o pedido sai da lista porque ela está filtrada por outro status, o aviso diz pra
     * onde ele foi, com o atalho de ver todos; cancelar oferece desfazer.
     */
    fun updateStatus(id: String, status: OrderStatus) {
        val before = repository.savedQuotes.value.find { it.id == id } ?: return
        if (before.status == status) return
        repository.updateStatus(id, status)
        val statusFilter = state.value.filter.status
        when {
            statusFilter != null && statusFilter != status -> noticeChannel.trySend(
                UiNotice(
                    uiText(Res.string.history_notice_status_moved, before.name, status.labelText()),
                    actionLabel = uiText(Res.string.history_notice_see_all),
                    action = { setStatusFilter(null) },
                ),
            )

            status == OrderStatus.CANCELADO -> noticeChannel.trySend(
                UiNotice(
                    uiText(Res.string.history_notice_status_cancelled, before.name),
                    actionLabel = uiText(Res.string.common_undo),
                    action = { repository.updateStatus(id, before.status) },
                ),
            )
        }
    }

    /** Manda pra lixeira, com "Desfazer" no aviso (os dados ficam 30 dias antes de sumir de vez). */
    fun delete(id: String) {
        val deleted = repository.savedQuotes.value.find { it.id == id } ?: return
        repository.delete(id)
        change { it.copy(selectedIds = it.selectedIds - id) }
        noticeChannel.trySend(
            UiNotice(
                uiText(Res.string.history_notice_deleted, deleted.name),
                actionLabel = uiText(Res.string.common_undo),
                action = { repository.restore(id) },
            ),
        )
    }

    fun convertToOrder(id: String) {
        val product = repository.savedQuotes.value.find { it.id == id } ?: return
        repository.convertToOrder(id)
        change { it.copy(selectedIds = it.selectedIds - id) }
        noticeChannel.trySend(
            UiNotice(
                uiText(Res.string.history_notice_converted, product.name),
                actionLabel = uiText(Res.string.history_notice_see_orders),
                action = showOrders,
            ),
        )
    }

    // ---- atualizar preço do produto (decisão 102)

    /**
     * O produto recalculado com os cadastros de hoje (decisão 102), pro cartão avisar quando os custos mudaram.
     * Pedido não passa por aqui, porque é retrato congelado da venda.
     */
    fun repriceFor(
        product: SavedQuote,
        filaments: List<Filament> = filamentRepository.filaments.value,
        printers: List<PrinterProfile> = printerRepository.printers.value,
        settings: PricingSettings = settingsRepository.settings.value,
        channels: List<SalesChannel> = salesChannelRepository.channels.value,
        consumables: List<Consumable> = consumableRepository?.consumables?.value.orEmpty(),
    ): RepriceResult = ProductRepricer.reprice(product, filaments, printers, settings, channels, consumables)

    /** Abre o diálogo com o antes e o depois; não faz nada se não der pra recalcular ou se nada mudou. */
    fun startRepricing(product: SavedQuote) {
        val result = repriceFor(product) as? RepriceResult.Repriced ?: return
        if (!result.changed) return
        val before = product.quote
        val after = result.quote
        change {
            it.copy(
                repricing = Repricing(
                    product = product,
                    newQuote = after,
                    priceBefore = before.tableSalePrice ?: before.salePrice,
                    priceAfter = after.tableSalePrice ?: after.salePrice,
                    costBefore = before.productionCost,
                    costAfter = after.productionCost,
                    profitBefore = before.profit,
                    profitAfter = after.profit,
                    announcedPrice = after.salePrice.takeIf { after.isNegotiated },
                    loss = (-after.profit).takeIf { after.profit < 0 },
                    breakEven = after.breakEvenSalePrice.takeIf { after.profit < 0 },
                ),
            )
        }
    }

    fun cancelRepricing() = change { it.copy(repricing = null) }

    fun confirmRepricing() {
        val pending = state.value.repricing ?: return
        repository.updateQuote(pending.product.id, pending.newQuote)
        change { it.copy(repricing = null) }
        noticeChannel.trySend(UiNotice(uiText(Res.string.history_notice_repriced, pending.product.name)))
    }

    // ---- prazo, configurações de impressão e detalhes

    /** Dias que [deadlineEpochDay] já passou hoje, pro seletor de prazo (ver [deliveryDaysLate]). */
    fun deliveryDaysLate(deadlineEpochDay: Long?): Int = deliveryDaysLate(deadlineEpochDay, today())

    fun startEditingDeliveryDate(savedQuote: SavedQuote) = change { it.copy(deliveryDateEditing = savedQuote) }

    fun cancelEditingDeliveryDate() = change { it.copy(deliveryDateEditing = null) }

    /** Grava o prazo escolhido no diálogo (`null` remove) e fecha o diálogo. */
    fun saveDeliveryDate(deliveryDateEpochDay: Long?) {
        val editing = state.value.deliveryDateEditing ?: return
        repository.updateDeliveryDate(editing.id, deliveryDateEpochDay)
        change { it.copy(deliveryDateEditing = null) }
    }

    fun startEditingPrintSettings(savedQuote: SavedQuote) = change { it.copy(printSettingsEditing = savedQuote) }

    fun cancelEditingPrintSettings() = change { it.copy(printSettingsEditing = null) }

    /** Grava as configurações de impressão do pedido aberto; vazias, apagam. O diálogo se fecha por conta própria. */
    fun savePrintSettings(settings: PrintSettings) {
        val editing = state.value.printSettingsEditing ?: return
        repository.updatePrintSettings(editing.id, settings.takeUnless { it.isEmpty })
    }

    /** Abre o "Editar detalhes": nome, cliente, foto, prazo, link e categoria, sem recalcular nada. */
    fun startEditingDetails(savedQuote: SavedQuote) {
        val form = DetailsForm(
            name = savedQuote.name.takeUnless { savedQuote.hasAutoName }.orEmpty(),
            clientName = savedQuote.client?.name.orEmpty(),
            clientContact = savedQuote.client?.contact.orEmpty(),
            clientId = savedQuote.client?.id,
            sourceLink = savedQuote.sourceLink.orEmpty(),
            photo = photoFile(savedQuote),
            deliveryDateEpochDay = savedQuote.deliveryDateEpochDay,
            category = savedQuote.category.orEmpty(),
        )
        change { it.copy(details = DetailsEditing(savedQuote, form)) }
    }

    fun cancelEditingDetails() = change { it.copy(details = null) }

    fun updateDetails(transform: (DetailsForm) -> DetailsForm) =
        change { s -> s.copy(details = s.details?.let { it.copy(form = transform(it.form), photoError = null) }) }

    /** Escolhe uma foto nova pro "Editar detalhes". Arquivo que não abre como imagem vira o aviso no diálogo. */
    fun chooseDetailsPhoto() {
        when (val picked = platform.pickFile(FileKind.IMAGE)) {
            PickResult.Cancelled -> Unit

            is PickResult.Failed -> showPhotoError(uiText(Res.string.history_error_pick_failed, picked.message))

            is PickResult.Picked ->
                if (runCatching { decodeImageBitmap(picked.file.bytes) }.isSuccess) {
                    updateDetails { it.copy(photo = picked.file) }
                } else {
                    showPhotoError(uiText(Res.string.history_error_photo_unreadable, picked.file.fileName))
                }
        }
    }

    private fun showPhotoError(error: UiText) =
        change { s -> s.copy(details = s.details?.copy(photoError = error)) }

    /** Grava o "Editar detalhes". O cliente digitado é achado ou criado no cadastro, como ao salvar o pedido. */
    fun saveDetails() {
        val editing = state.value.details ?: return
        val form = editing.form
        val clientName = form.clientName.trim()
        val client = if (!editing.quote.isOrder || clientName.isEmpty()) {
            null
        } else {
            val typed = Client(name = clientName, contact = form.clientContact.trim().ifEmpty { null }, id = form.clientId)
            clientRepository?.resolve(typed) ?: typed
        }
        val saved = repository.updateDetails(
            editing.quote.id,
            QuoteDetails(
                name = form.name,
                client = client,
                sourceLink = form.sourceLink,
                photo = form.photo,
                deliveryDateEpochDay = form.deliveryDateEpochDay,
                category = form.category,
            ),
        )
        change { it.copy(details = null) }
        if (saved != null) noticeChannel.trySend(UiNotice(uiText(Res.string.history_notice_details_saved, saved.name)))
    }

    // ---- fotos e arquivos

    /** A foto guardada de [savedQuote], como arquivo, pra o "Editar detalhes" começar com ela. */
    private fun photoFile(savedQuote: SavedQuote): PickedFile? {
        val fileName = savedQuote.photoFileName ?: return null
        return photoBytes(savedQuote)?.let { PickedFile(fileName, it) }
    }

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    /** Miniatura da foto pro cartão, gerada uma vez e guardada (a foto de celular inteira travava a lista). */
    fun photoThumbnail(savedQuote: SavedQuote): ByteArray? = repository.photoThumbnail(savedQuote, THUMBNAIL_PX)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        val extension = savedQuote.photoFileName?.substringAfterLast('.', "png") ?: "png"
        exporter.report(platform.saveFile(bytes, "${exporter.fileNameFor(savedQuote)}.$extension"), uiText(Res.string.history_notice_photo_saved))
    }

    fun downloadStl(savedQuote: SavedQuote) {
        val bytes = repository.stlBytes(savedQuote) ?: return
        exporter.report(platform.saveFile(bytes, "${exporter.fileNameFor(savedQuote)}.stl"), uiText(Res.string.history_notice_stl_saved))
    }

    // ---- envio pro cliente, com o aviso de prazo vencido
    // Os envios abaixo passam por holdIfOverdue: com prazo vencido, a tela pergunta antes (ver pendingExport).
    // O catálogo não, porque não mostra prazo.

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

    fun copyQuoteToClipboard(savedQuote: SavedQuote) {
        if (holdIfOverdue(ClientExport.COPY, listOf(savedQuote))) return
        performCopy(savedQuote)
    }

    // Copiar não gera aviso quando dá certo: o botão diz "Copiado" por um instante (ver [HistoryUiState.copiedId]).
    private fun performCopy(savedQuote: SavedQuote) {
        val text = savedQuote.toCopyPasteText(savedQuote.currency, brandingRepository.branding.value.showPrintTime)
        if (platform.copyToClipboard(text)) {
            change { it.copy(copiedId = savedQuote.id) }
        } else {
            noticeChannel.trySend(UiNotice(uiText(Res.string.export_copy_failed), isError = true))
        }
    }

    fun clearCopied() = change { it.copy(copiedId = null) }

    /** "Enviar assim mesmo" no aviso de prazo vencido: faz o envio que tinha sido segurado, sem perguntar de novo. */
    fun confirmPendingExport() {
        val pending = state.value.pendingExport ?: return
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
    fun changeDateOfPendingExport() {
        val pending = state.value.pendingExport ?: return
        change { it.copy(pendingExport = null) }
        pending.quotes.singleOrNull()?.let(::startEditingDeliveryDate)
    }

    fun dismissPendingExport() = change { it.copy(pendingExport = null) }

    /**
     * Segura o envio quando algum dos [quotes] tem prazo vencido e devolve `true` (a tela mostra o aviso a partir
     * de [HistoryUiState.pendingExport]). Vale pra todo status antes de Entregue: um prazo vencido num orçamento
     * parado pede data nova, e num pedido já aprovado é atraso que o cliente vai notar.
     */
    private fun holdIfOverdue(export: ClientExport, quotes: List<SavedQuote>): Boolean {
        val todayEpochDay = today()
        val overdue = quotes.filter { it.isDeliveryOverdue(todayEpochDay) }
        if (overdue.isEmpty()) return false
        val items = overdue.map { OverdueItem(it.name, it.deliveryDateEpochDay!!) }
        change { it.copy(pendingExport = PendingExport(export, quotes, items)) }
        return true
    }

    // ---- seleção e exportação em lote

    /** Entra no modo de seleção (as linhas ganham a caixa de marcar), já com [id] marcado quando vem de uma linha. */
    fun startSelection(id: String? = null) = change { it.copy(selectionMode = true, selectedIds = if (id == null) it.selectedIds else it.selectedIds + id) }

    fun toggleSelection(id: String) = change { it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id) }

    /** Sai do modo de seleção e desmarca tudo. */
    fun clearSelection() = change { it.copy(selectionMode = false, selectedIds = emptySet()) }

    /**
     * Os marcados que a lista está mostrando. Marcar, filtrar e exportar levaria junto itens que a pessoa não vê
     * mais; a contagem do rodapé usa a mesma regra, pra o número bater com o PDF.
     */
    fun selectedVisible(): List<SavedQuote> {
        val selected = state.value.selectedIds
        return visibleQuotes(repository.savedQuotes.value, state.value.filter).filter { it.id in selected }
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
    fun exportCatalogPdf() {
        val selected = selectedVisible().ifEmpty {
            if (state.value.filter.kind == QuoteKind.PRODUCT) visibleQuotes(repository.savedQuotes.value, state.value.filter) else emptyList()
        }
        if (selected.isEmpty()) return
        val clearsSelection = state.value.selectedIds.isNotEmpty()
        exporter.exportCatalogPdf(selected) { if (clearsSelection) clearSelection() }
    }

    companion object {
        const val THUMBNAIL_PX = 256
    }
}
