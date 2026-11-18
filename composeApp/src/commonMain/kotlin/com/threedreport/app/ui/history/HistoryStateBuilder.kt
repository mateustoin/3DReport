package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.localEpochDayOf
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.ProductRepricer
import com.threedreport.core.pricing.RepriceResult

/**
 * Monta o [HistoryUiState] a partir do que os repositórios guardam e do que a pessoa já mexeu (filtro, seleção, diálogos).
 * É o que o ViewModel chama a cada mudança; as contas (ordenar, somar, selo de prazo) são as funções puras de `HistoryQueries`.
 */
internal class HistoryStateBuilder(
    private val repository: QuoteHistoryRepository,
    private val filamentRepository: FilamentRepository,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
    private val salesChannelRepository: SalesChannelRepository,
    private val preferencesRepository: PreferencesRepository,
    private val clientRepository: ClientRepository?,
    private val consumableRepository: ConsumableRepository?,
    private val today: () -> Long,
    private val clock: Clock,
    private val currency: () -> Currency,
    private val brandingRepository: BrandingRepository? = null,
    private val orderSettingsRepository: OrderSettingsRepository? = null,
) {
    // O recálculo de cada produto é caro e não depende do filtro: refeito só quando um cadastro muda.
    private var repriceInputs: List<Any?> = emptyList()
    private var repriced: Map<String, RepriceResult> = emptyMap()

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

    /** [state] com tudo o que se deriva dos repositórios e do filtro refeito. */
    fun refresh(state: HistoryUiState): HistoryUiState {
        val all = repository.savedQuotes.value
        val today = today()
        val appCurrency = currency()
        val prefs = preferencesRepository.preferences.value
        val orders = state.filter.kind == QuoteKind.ORDER
        val categories = if (orders) emptyList() else productCategories(all)
        // A categoria escolhida sumiu (o último produto dela foi excluído ou mudou, ou não resta produto sem categoria):
        // volta pra "Todas", senão a lista ficaria vazia com um filtro que nem aparece mais.
        val hasUncategorized = all.any { !it.isOrder && it.category == null }
        val chosen = state.filter.category
        val orphan = when (chosen) {
            is CategoryFilter.Named -> categories.none { it.equals(chosen.name, ignoreCase = true) }
            CategoryFilter.None -> orders || !hasUncategorized
            CategoryFilter.All -> false
        }
        val filter = if (orphan) state.filter.copy(category = CategoryFilter.All) else state.filter
        val visible = visibleQuotes(all, filter)
        val sold = soldProductIds(all)
        val repricedProducts = if (orders) emptyMap() else repricedProducts(all)
        val orderSettings = orderSettingsRepository?.settings?.value ?: OrderSettings()
        val pixReceiver = brandingRepository?.branding?.value?.pixReceiver
        fun rowOf(quote: SavedQuote): HistoryRow {
            val notice = repricedProducts[quote.id]?.toNotice()
            val order = orders && quote.isOrder
            return HistoryRow(
                quote = quote,
                deadline = if (orders) quote.deadlineBadge(today) else null,
                printCount = quote.quote.prints.size.takeIf { it > 1 },
                reprice = notice,
                canReprice = notice is RepriceNotice.CostsChanged,
                canConvertToOrder = canConvertToOrder(quote, sold),
                canEditPrintSettings = quote.quote.prints.size == 1,
                // Só de Aprovado em diante: em Orçado o sinal ainda não é cobrado (decisão 146, item 35).
                paymentStatus = quote.paymentStatus.takeIf { order && quote.hasPaymentTracking && quote.status != OrderStatus.ORCADO && quote.status != OrderStatus.CANCELADO },
                staleDays = if (order) quote.staleDaysIfStale(today, orderSettings.staleQuoteDays, ::localEpochDayOf) else null,
                messageKind = if (order) MessageTemplateKind.forOrder(quote) else null,
                // Só a pergunta "há Pix?": o código inteiro (com o CRC) é montado quando alguém copia ou exporta.
                hasPix = order && pixReceiver != null && quote.pixCharge != null,
                trackingUrl = quote.trackingCode?.takeIf { order && it.isNotBlank() }?.let { orderSettings.trackingUrl(it, currency()) },
            )
        }

        val now = clock.nowMillis()
        val kanban = if (orders) kanbanQuotes(all, filter, state.showAllDelivered, now) else emptyList()
        val allDelivered = if (orders) kanbanQuotes(all, filter, true, now).count { it.status == OrderStatus.ENTREGUE } else 0
        val byStatus = kanban.groupBy { it.status }
        return state.copy(
            filter = filter,
            ordersView = prefs.ordersView,
            catalogView = prefs.catalogView,
            rows = (if (orders) visible.sortedByDeadline() else visible).map(::rowOf),
            columns = if (orders) {
                OrderStatus.PIPELINE.map { status ->
                    val cards = byStatus[status].orEmpty()
                    KanbanColumnState(status, cards.map(::rowOf), cards.totalsByCurrency(appCurrency))
                }
            } else {
                emptyList()
            },
            summary = if (orders) ordersSummary(all, today, appCurrency) else OrdersSummary(),
            hasItems = all.any { it.kind == filter.kind },
            hiddenDelivered = allDelivered - kanban.count { it.status == OrderStatus.ENTREGUE },
            cancelledCount = if (orders) all.count { it.isOrder && it.status == OrderStatus.CANCELADO } else 0,
            categories = categories,
            hasUncategorized = hasUncategorized,
            selectedVisibleCount = visible.count { it.id in state.selectedIds },
            clients = clientRepository?.clients?.value.orEmpty(),
            today = today,
            currency = appCurrency,
            payments = state.payments?.let { open -> all.find { it.id == open.quote.id }?.let { open.copy(quote = it) } },
        )
    }
}
