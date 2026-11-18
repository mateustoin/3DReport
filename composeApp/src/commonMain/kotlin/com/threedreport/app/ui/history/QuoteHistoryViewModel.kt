package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_undo
import _3dreport.composeapp.generated.resources.export_copy_failed
import _3dreport.composeapp.generated.resources.history_error_photo_unreadable
import _3dreport.composeapp.generated.resources.history_notice_converted
import _3dreport.composeapp.generated.resources.history_notice_deleted
import _3dreport.composeapp.generated.resources.history_notice_details_saved
import _3dreport.composeapp.generated.resources.history_notice_notify_client
import _3dreport.composeapp.generated.resources.history_notice_photo_saved
import _3dreport.composeapp.generated.resources.history_notice_pix_copied
import _3dreport.composeapp.generated.resources.history_notice_repriced
import _3dreport.composeapp.generated.resources.history_notice_see_all
import _3dreport.composeapp.generated.resources.history_notice_see_orders
import _3dreport.composeapp.generated.resources.history_notice_status_cancelled
import _3dreport.composeapp.generated.resources.history_notice_status_changed
import _3dreport.composeapp.generated.resources.history_notice_status_moved
import _3dreport.composeapp.generated.resources.history_notice_tracking_copied
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OrderSettingsRepository
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
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.Client
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
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
 * Linhas, cartões, colunas do kanban e blocos de resumo saem prontos em [HistoryUiState] (ver [HistoryStateBuilder]);
 * a tela só desenha. O envio pra fora fica em [HistoryExports] e o "Editar detalhes" em [HistoryDetailsEditor]. Os
 * cadastros, as preferências e a lista de orçamentos são acompanhados dentro de [scope]; o resto muda na hora, pelos
 * eventos. Gerar PDF e imagem roda em [background]; os testes passam `Unconfined` e tudo acontece na hora.
 */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
    filamentRepository: FilamentRepository,
    printerRepository: PrinterRepository,
    settingsRepository: SettingsRepository,
    salesChannelRepository: SalesChannelRepository,
    /** Onde a visualização escolhida fica guardada (decisões 129 e 132). */
    private val preferencesRepository: PreferencesRepository,
    private val today: () -> Long = ::todayEpochDay,
    /** Cadastro de clientes (decisão 106), pro "Editar detalhes" ligar o pedido ao cliente. */
    private val clientRepository: ClientRepository? = null,
    private val clock: Clock = Clock.System,
    private val platform: PlatformServices = defaultPlatform,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    background: CoroutineDispatcher = Dispatchers.Unconfined,
    main: CoroutineDispatcher = Dispatchers.Unconfined,
    /** Pedidos ou Catálogo (decisão 111): cada tela tem o seu ViewModel, com o tipo fixo. */
    kind: QuoteKind = QuoteKind.ORDER,
    /** Leva pra tela de Pedidos, pro "Ver pedidos" do aviso depois de transformar um produto em pedido. */
    private val showOrders: () -> Unit = {},
    /** Cadastro de insumos (decisão 122), pro catálogo vivo recalcular com o custo de hoje de cada um. */
    consumableRepository: ConsumableRepository? = null,
    /** A moeda do app: o que os blocos de resumo e as colunas somam vem primeiro nela, e trocar de moeda refaz o estado. */
    private val currency: StateFlow<Currency> = MutableStateFlow(Currency.BRL),
    /** Configurações de Pedidos (Leva 11): dias pra orçamento parado e site de rastreio. */
    private val orderSettingsRepository: OrderSettingsRepository? = null,
) {
    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)

    /** O retorno da última ação, pra barra de avisos. */
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    private val builder = HistoryStateBuilder(
        repository, filamentRepository, printerRepository, settingsRepository, salesChannelRepository, preferencesRepository,
        clientRepository, consumableRepository, today, clock, { currency.value }, brandingRepository, orderSettingsRepository,
    )
    private val exporter = QuoteExporter(platform, brandingRepository, repository, scope, background, main) { notice -> noticeChannel.trySend(notice) }

    private val state = MutableStateFlow(builder.refresh(HistoryUiState(filter = HistoryFilter(kind = kind))))
    val uiState: StateFlow<HistoryUiState> = state.asStateFlow()

    /** Muda o estado e refaz tudo o que se deriva dele (filtro, seleção, preferência, repositórios): o trabalho caro. */
    private fun change(transform: (HistoryUiState) -> HistoryUiState) = state.update { builder.refresh(transform(it)) }

    /** Muda só o que a tela mostra por cima (diálogo, formulário, "Copiado", "Gerando"): nada na lista depende disso. */
    private fun patch(transform: (HistoryUiState) -> HistoryUiState) = state.update(transform)

    private val exports = HistoryExports(
        exporter, platform, brandingRepository, repository, today, { state.value }, ::patch, noticeChannel::trySend,
        editDeliveryDate = ::startEditingDeliveryDate, clearSelection = ::clearSelection,
    )
    private val detailsEditor = HistoryDetailsEditor(repository, clientRepository, platform, { state.value }, ::patch, noticeChannel::trySend)
    private val payments = HistoryPayments(
        repository, { state.value }, ::patch, noticeChannel::trySend, today, { currency.value },
        generateReceipt = { quote, paymentId -> exporter.exportReceipt(quote, paymentId) },
    )
    private val messages = HistoryMessages(repository, brandingRepository, platform, scope, main, { state.value }, ::patch, noticeChannel::trySend)

    init {
        // Qualquer coisa que a tela lê e que mude em outro lugar (um filamento, o cliente, a visualização) refaz o estado.
        val sources: List<Flow<*>> = listOf(
            repository.savedQuotes,
            filamentRepository.filaments,
            printerRepository.printers,
            settingsRepository.settings,
            salesChannelRepository.channels,
            preferencesRepository.preferences,
            currency,
            consumableRepository?.consumables ?: MutableStateFlow(emptyList<Consumable>()),
            clientRepository?.clients ?: MutableStateFlow(emptyList<Client>()),
            brandingRepository.branding,
            orderSettingsRepository?.settings ?: MutableStateFlow(OrderSettings()),
        )
        combine(sources) { }.onEach { change { it } }.launchIn(scope)
        exporter.busy.onEach { busy -> patch { it.copy(busy = busy) } }.launchIn(scope)
    }

    // ---- filtros e visualização

    fun setSearchQuery(query: String) = change { it.copy(filter = it.filter.copy(query = query)) }
    fun setStatusFilter(status: OrderStatus?) = change { it.copy(filter = it.filter.copy(status = status)) }
    fun setPeriodFilter(period: PeriodPreset) = change { it.copy(filter = it.filter.copy(period = period)) }
    fun setCategoryFilter(category: CategoryFilter) = change { it.copy(filter = it.filter.copy(category = category)) }

    /** Tira a busca, a situação, o período e a categoria (o "Limpar filtros" da lista vazia); o tipo da tela fica. */
    fun clearFilters() = change { it.copy(filter = HistoryFilter(kind = it.filter.kind)) }

    /** Lembra a visualização de Pedidos pra próxima abertura (decisão 129). */
    fun setOrdersView(view: OrdersView) {
        preferencesRepository.setOrdersView(view)
        // A seleção só existe na lista: trocar de visualização a desfaz, e não deixa marcados escondidos no kanban.
        clearSelection()
    }

    /** Lembra a visualização do Catálogo pra próxima abertura (decisão 132). */
    fun setCatalogView(view: CatalogView) {
        preferencesRepository.setCatalogView(view)
        clearSelection()
    }

    /**
     * A tela entrou em composição (ou o relógio andou): se o dia virou, refaz o estado, porque os selos de prazo e os
     * blocos de resumo dependem de "hoje". Sem mudança, não faz nada.
     */
    fun onShown() {
        if (state.value.today != today()) change { it }
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
        // O filtro de situação só vale na lista: no kanban as colunas são todas as situações, e mover não tira nada da vista.
        val statusFilter = state.value.filter.status.takeIf { state.value.ordersView == OrdersView.LIST }
        // Aprovado, Imprimindo, Pronto e Entregue oferecem "Avisar cliente" (decisão 148, item 24); nada abre sozinho. Com o
        // filtro escondendo o pedido, o aviso continua dizendo pra onde ele foi, mas o atalho é o de avisar.
        val messageKind = MessageTemplateKind.forStatusChange(status)?.takeIf { before.isOrder }
        val hidden = statusFilter != null && statusFilter != status
        when {
            messageKind != null -> noticeChannel.trySend(
                UiNotice(
                    uiText(if (hidden) Res.string.history_notice_status_moved else Res.string.history_notice_status_changed, before.name, status.labelText()),
                    actionLabel = uiText(Res.string.history_notice_notify_client),
                    action = { repository.savedQuotes.value.find { it.id == id }?.let { openMessage(it, messageKind) } },
                ),
            )

            hidden -> noticeChannel.trySend(
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

    /** O produto recalculado com os cadastros de hoje, pra saber se os custos mudaram (ver [HistoryStateBuilder.repriceFor]). */
    fun repriceFor(product: SavedQuote): RepriceResult = builder.repriceFor(product)

    /** Abre o diálogo com o antes e o depois; não faz nada se não der pra recalcular ou se nada mudou. */
    fun startRepricing(product: SavedQuote) {
        val result = repriceFor(product) as? RepriceResult.Repriced ?: return
        if (!result.changed) return
        val before = product.quote
        val after = result.quote
        patch {
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

    fun cancelRepricing() = patch { it.copy(repricing = null) }

    fun confirmRepricing() {
        val pending = state.value.repricing ?: return
        repository.updateQuote(pending.product.id, pending.newQuote)
        patch { it.copy(repricing = null) }
        noticeChannel.trySend(UiNotice(uiText(Res.string.history_notice_repriced, pending.product.name)))
    }

    // ---- prazo e configurações de impressão

    /** Dias que [deadlineEpochDay] já passou hoje, pro seletor de prazo (ver [deliveryDaysLate]). */
    fun deliveryDaysLate(deadlineEpochDay: Long?): Int = deliveryDaysLate(deadlineEpochDay, today())

    fun startEditingDeliveryDate(savedQuote: SavedQuote) = patch { it.copy(deliveryDateEditing = savedQuote) }

    fun cancelEditingDeliveryDate() = patch { it.copy(deliveryDateEditing = null) }

    /** Grava o prazo escolhido no diálogo (`null` remove) e fecha o diálogo. */
    fun saveDeliveryDate(deliveryDateEpochDay: Long?) {
        val editing = state.value.deliveryDateEditing ?: return
        repository.updateDeliveryDate(editing.id, deliveryDateEpochDay)
        patch { it.copy(deliveryDateEditing = null) }
    }

    fun startEditingPrintSettings(savedQuote: SavedQuote) = patch { it.copy(printSettingsEditing = savedQuote) }

    fun cancelEditingPrintSettings() = patch { it.copy(printSettingsEditing = null) }

    /** Grava as configurações de impressão do pedido aberto; vazias, apagam. O diálogo se fecha por conta própria. */
    fun savePrintSettings(settings: PrintSettings) {
        val editing = state.value.printSettingsEditing ?: return
        repository.updatePrintSettings(editing.id, settings.takeUnless { it.isEmpty })
    }

    // ---- editar detalhes (ver [HistoryDetailsEditor])

    fun startEditingDetails(savedQuote: SavedQuote) = detailsEditor.start(savedQuote)
    fun cancelEditingDetails() = detailsEditor.cancel()
    fun updateDetails(transform: (DetailsForm) -> DetailsForm) = detailsEditor.update(transform)
    fun chooseDetailsPhoto() = detailsEditor.choosePhoto()
    fun saveDetails() = detailsEditor.save()

    // ---- pagamentos (ver [HistoryPayments])

    fun openPayments(savedQuote: SavedQuote) = payments.open(savedQuote)
    fun closePayments() = payments.close()
    fun setDeposit(text: String) = payments.setDeposit(text)
    fun startRegisteringPayment() = payments.startRegistering()
    fun updatePaymentForm(transform: (PaymentForm) -> PaymentForm) = payments.updateForm(transform)
    fun cancelPaymentForm() = payments.cancelForm()
    fun registerPayment() = payments.register()
    fun requestDeletePayment(payment: Payment) = payments.requestDelete(payment)
    fun cancelDeletePayment() = payments.cancelDelete()
    fun confirmDeletePayment() = payments.confirmDelete()
    fun generateReceipt(paymentId: String) = payments.receipt(paymentId)

    // ---- mensagem pro cliente (ver [HistoryMessages])

    /** Abre "Mensagem pro cliente" no modelo do andamento atual; [kind] força outro. Sem modelo (Cancelado), não abre. */
    fun openMessage(savedQuote: SavedQuote, kind: MessageTemplateKind? = null) = messages.open(savedQuote, kind)

    /** "Retomar conversa" (decisão 149): a janela no modelo "Retomar orçamento". */
    fun followUp(savedQuote: SavedQuote) = messages.open(savedQuote, MessageTemplateKind.FOLLOW_UP)
    fun changeMessageKind(kind: MessageTemplateKind) = messages.changeKind(kind)
    fun editMessage(text: String) = messages.edit(text)
    fun closeMessage() = messages.close()
    fun sendMessageOnWhatsApp() = messages.openInWhatsApp()
    fun copyMessage() = messages.copy()

    /** "Marcar como cancelado", no menu do orçamento parado: o mesmo cancelar do "Mover para", com "Desfazer". */
    fun markCancelled(id: String) = updateStatus(id, OrderStatus.CANCELADO)

    // ---- Pix e rastreio (decisões 145 e 150)

    /** Copia o Pix copia e cola do pedido, quando há. */
    fun copyPixCode(savedQuote: SavedQuote) {
        val code = exporter.pixCodeFor(savedQuote) ?: return
        copyWithNotice(code, Res.string.history_notice_pix_copied)
    }

    fun copyTrackingCode(savedQuote: SavedQuote) {
        val code = savedQuote.trackingCode?.trim()?.takeIf { it.isNotEmpty() } ?: return
        copyWithNotice(code, Res.string.history_notice_tracking_copied)
    }

    /** Abre o site de rastreio com o código do pedido, se houver site em Configurações. */
    fun openTracking(savedQuote: SavedQuote) {
        val code = savedQuote.trackingCode ?: return
        val url = (orderSettingsRepository?.settings?.value ?: OrderSettings()).trackingUrl(code) ?: return
        platform.openUrl(url)
    }

    private fun copyWithNotice(text: String, done: org.jetbrains.compose.resources.StringResource) {
        if (platform.copyToClipboard(text)) {
            noticeChannel.trySend(UiNotice(uiText(done)))
        } else {
            noticeChannel.trySend(UiNotice(uiText(Res.string.export_copy_failed), isError = true))
        }
    }

    // ---- fotos, arquivos e envio pro cliente (ver [HistoryExports])

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    /** Miniatura da foto pro cartão, gerada uma vez e guardada (a foto de celular inteira travava a lista). */
    fun photoThumbnail(savedQuote: SavedQuote): ByteArray? = repository.photoThumbnail(savedQuote, THUMBNAIL_PX)

    fun downloadPhoto(savedQuote: SavedQuote) = exports.downloadPhoto(savedQuote)
    fun downloadModelFile(savedQuote: SavedQuote) = exports.downloadModelFile(savedQuote)
    fun exportPdf(savedQuote: SavedQuote) = exports.exportPdf(savedQuote)
    fun openInWhatsApp(savedQuote: SavedQuote) = exports.openInWhatsApp(savedQuote)
    fun saveShareableImage(savedQuote: SavedQuote) = exports.saveShareableImage(savedQuote)
    fun copyQuoteToClipboard(savedQuote: SavedQuote) = exports.copy(savedQuote)
    fun clearCopied() = patch { it.copy(copiedId = null) }
    fun confirmPendingExport() = exports.confirmPending()
    fun changeDateOfPendingExport() = exports.changeDateOfPending()
    fun dismissPendingExport() = exports.dismissPending()

    // ---- seleção e exportação em lote

    /** Entra no modo de seleção (as linhas ganham a caixa de marcar), já com [id] marcado quando vem de uma linha. */
    fun startSelection(id: String? = null) = change { it.copy(selectionMode = true, selectedIds = if (id == null) it.selectedIds else it.selectedIds + id) }

    fun toggleSelection(id: String) = change { it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id) }

    /** Marca tudo o que a lista mostra agora (o "Marcar todos"), somando aos que já estavam marcados. */
    fun selectAllVisible() = change { it.copy(selectedIds = it.selectedIds + it.rows.map { row -> row.quote.id }) }

    /** Sai do modo de seleção e desmarca tudo. */
    fun clearSelection() = change { it.copy(selectionMode = false, selectedIds = emptySet()) }

    fun selectedVisible(): List<SavedQuote> = exports.selectedVisible()
    fun exportSelectedPdf() = exports.exportSelectedPdf()
    fun exportCatalogPdf() = exports.exportCatalog()

    companion object {
        const val THUMBNAIL_PX = 256
    }
}
