package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_reprice_channel_missing
import _3dreport.composeapp.generated.resources.history_reprice_consumable_missing
import _3dreport.composeapp.generated.resources.history_reprice_filament_missing
import _3dreport.composeapp.generated.resources.history_reprice_invalid
import _3dreport.composeapp.generated.resources.history_reprice_printer_missing
import _3dreport.composeapp.generated.resources.history_reprice_printer_not_saved
import _3dreport.composeapp.generated.resources.history_reprice_suffix
import _3dreport.composeapp.generated.resources.orders_deadline_expired
import _3dreport.composeapp.generated.resources.orders_deadline_late
import _3dreport.composeapp.generated.resources.orders_deadline_today
import _3dreport.composeapp.generated.resources.orders_deadline_until
import _3dreport.composeapp.generated.resources.orders_deadline_was
import com.threedreport.app.platform.contains
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.ui.quote.deliveryDaysLate
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.RepriceResult

/** Pedidos entregues há mais que isso saem do kanban (ver [kanbanQuotes]). */
const val RECENT_DELIVERED_DAYS = 30
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Função pura: aplica [filter] a [savedQuotes], do mais recente pro mais antigo. A busca acha pelo nome mostrado
 * ([shownName], o automático no idioma do app, item 7b da Leva 11B) e também pelo texto gravado.
 */
fun visibleQuotes(
    savedQuotes: List<SavedQuote>,
    filter: HistoryFilter,
    shownName: (SavedQuote) -> String = { it.name },
): List<SavedQuote> {
    val query = filter.query.trim()
    return savedQuotes
        .filter { quote ->
            quote.kind == filter.kind &&
                quote.matchesCategory(filter.category) &&
                (filter.status == null || quote.status == filter.status) &&
                filter.period.contains(quote.savedAtEpochMillis) &&
                (query.isEmpty() || quote.matchesQuery(query, shownName(quote)))
        }
        .sortedByDescending { it.savedAtEpochMillis }
}

/**
 * Função pura: os pedidos do kanban. Pedido em andamento aparece sempre, qualquer que seja o período (sumir com
 * um pedido aprovado porque é do mês passado escondia trabalho a fazer). Entregue mostra só os entregues nos
 * últimos [recentDays] dias, a não ser com [showAllDelivered]: com o tempo, a coluna viraria o arquivo morto
 * inteiro. Cancelado fica fora do quadro, que é o fluxo.
 */
fun kanbanQuotes(
    savedQuotes: List<SavedQuote>,
    filter: HistoryFilter,
    showAllDelivered: Boolean,
    nowEpochMillis: Long,
    recentDays: Int = RECENT_DELIVERED_DAYS,
    shownName: (SavedQuote) -> String = { it.name },
): List<SavedQuote> {
    val query = filter.query.trim()
    val recentStart = nowEpochMillis - recentDays * DAY_MILLIS
    return savedQuotes
        .filter { quote ->
            quote.isOrder &&
                quote.status in OrderStatus.PIPELINE &&
                (query.isEmpty() || quote.matchesQuery(query, shownName(quote))) &&
                (showAllDelivered || quote.status != OrderStatus.ENTREGUE || (quote.deliveredAtEpochMillis ?: 0) >= recentStart)
        }
        .sortedByDeadline()
}

/**
 * Função pura: a ordem de Pedidos, na tabela e em cada coluna do kanban. Primeiro os abertos, pelo prazo (atrasado,
 * hoje, próximos), depois os abertos sem prazo; por último Entregue e Cancelado, do mais recente pro mais antigo.
 * Entre iguais, o gravado por último vem antes.
 */
fun List<SavedQuote>.sortedByDeadline(): List<SavedQuote> =
    sortedWith(
        compareBy<SavedQuote> { it.status == OrderStatus.ENTREGUE || it.status == OrderStatus.CANCELADO }
            .thenBy { quote -> if (quote.status == OrderStatus.ENTREGUE || quote.status == OrderStatus.CANCELADO) 0L else quote.deliveryDateEpochDay ?: Long.MAX_VALUE }
            .thenByDescending { it.savedAtEpochMillis },
    )

/**
 * Função pura: o que [this] soma em cada moeda, a do app primeiro e as outras na ordem da lista de moedas. Moedas
 * diferentes nunca viram um número só. Sem nenhum valor, devolve zero na moeda do app. [amountOf] é o que conta de cada
 * pedido: o total, por padrão, ou o que falta pagar no "a receber" (decisão 146).
 */
fun List<SavedQuote>.totalsByCurrency(appCurrency: Currency, amountOf: (SavedQuote) -> Double = { it.totalWithServices }): List<CurrencyTotal> {
    val sums = groupBy { it.currency }.mapValues { (_, quotes) -> quotes.sumOf(amountOf) }
    if (sums.isEmpty()) return listOf(CurrencyTotal(appCurrency, 0.0))
    return sums.entries
        .sortedWith(compareBy<Map.Entry<Currency, Double>> { it.key != appCurrency }.thenBy { it.key.ordinal })
        .map { CurrencyTotal(it.key, it.value) }
}

/** Categorias dos produtos, uma por grafia, em ordem alfabética. */
fun productCategories(savedQuotes: List<SavedQuote>): List<String> =
    savedQuotes.filterNot { it.isOrder }.mapNotNull { it.category }
        .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

/** Produtos que já originaram algum pedido (pelo "Vender"), calculado uma vez por lista. */
fun soldProductIds(savedQuotes: List<SavedQuote>): Set<String> = savedQuotes.mapNotNullTo(HashSet()) { it.sourceProductId }

/**
 * "Transformar em pedido" só aparece pra produto que nunca foi vendido: depois de uma venda, o produto é a
 * origem daquele pedido, e movê-lo apagaria o catálogo de onde a venda saiu.
 */
fun canConvertToOrder(product: SavedQuote, soldProductIds: Set<String>): Boolean = !product.isOrder && product.id !in soldProductIds

private fun SavedQuote.matchesCategory(filter: CategoryFilter): Boolean = when (filter) {
    CategoryFilter.All -> true
    CategoryFilter.None -> category == null
    is CategoryFilter.Named -> category.equals(filter.name, ignoreCase = true)
}

private fun SavedQuote.matchesQuery(query: String, shownName: String): Boolean =
    shownName.contains(query, ignoreCase = true) ||
        name.contains(query, ignoreCase = true) ||
        client?.name?.contains(query, ignoreCase = true) == true ||
        displayNumber?.contains(query) == true

/**
 * O selo de prazo de um pedido (decisão 85: uma data, e não "N dias", pra o app saber quando venceu). Três
 * leituras que pedem ações diferentes: atrasado (cliente esperando), orçamento parado com prazo vencido (data
 * nova antes de reenviar) e no prazo ou já entregue. `null` sem prazo.
 */
fun SavedQuote.deadlineBadge(today: Long): DeadlineBadge? {
    val deadline = deliveryDateEpochDay ?: return null
    val date = formatShortDate(deadline)
    val daysLate = deliveryDaysLate(deadline, today)
    return when {
        isDeliveryOverdue(today) && status == OrderStatus.ORCADO ->
            DeadlineBadge(uiText(Res.string.orders_deadline_expired, date), DeadlineTone.STALE)

        isDeliveryOverdue(today) ->
            DeadlineBadge(UiText.Plural(Res.plurals.orders_deadline_late, daysLate, listOf(daysLate)), DeadlineTone.LATE)

        status == OrderStatus.ENTREGUE || status == OrderStatus.CANCELADO ->
            DeadlineBadge(uiText(Res.string.orders_deadline_was, date), DeadlineTone.DONE)

        deadline == today -> DeadlineBadge(uiText(Res.string.orders_deadline_today), DeadlineTone.TODAY)

        else -> DeadlineBadge(uiText(Res.string.orders_deadline_until, date), DeadlineTone.ON_TIME)
    }
}

/** Os pedidos de Orçado a Pronto: o que ainda tem prazo pra cumprir ("pra entregar hoje" e "atrasado"). */
private val OPEN = OrderStatus.PIPELINE - OrderStatus.ENTREGUE

/** Os pedidos de Aprovado a Pronto: já fechados com o cliente e ainda não entregues ("em andamento" e "a receber"). */
private val IN_PROGRESS = OPEN - OrderStatus.ORCADO

/**
 * Os blocos de resumo de Pedidos, sobre todos os pedidos (a busca e os filtros não mexem neles). "Em andamento" e "a
 * receber" contam de Aprovado a Pronto, sem Orçado (decisão 132); o que é a receber soma por moeda o que falta pagar de
 * cada pedido (decisão 146), que num pedido sem pagamento é o total.
 */
fun ordersSummary(savedQuotes: List<SavedQuote>, today: Long, appCurrency: Currency = Currency.BRL): OrdersSummary {
    val orders = savedQuotes.filter { it.isOrder }
    val inProgress = orders.filter { it.status in IN_PROGRESS }
    val tones = orders.filter { it.status in OPEN }.map { it.deadlineBadge(today)?.tone }
    return OrdersSummary(
        inProgress = inProgress.size,
        receivable = inProgress.totalsByCurrency(appCurrency) { it.remaining },
        dueToday = tones.count { it == DeadlineTone.TODAY },
        overdue = tones.count { it == DeadlineTone.LATE },
    )
}

/** O aviso do cartão do produto a partir do recálculo com os cadastros de hoje (decisão 102); `null` sem nada a dizer. */
fun RepriceResult.toNotice(): RepriceNotice? = when (this) {
    is RepriceResult.Repriced -> if (changed) RepriceNotice.CostsChanged(quote.tableSalePrice ?: quote.salePrice) else null

    is RepriceResult.Unavailable -> {
        val name = missingName.orEmpty()
        val cause = when (reason) {
            RepriceResult.Reason.FILAMENT_MISSING -> uiText(Res.string.history_reprice_filament_missing, name)

            RepriceResult.Reason.PRINTER_MISSING ->
                if (missingName != null) uiText(Res.string.history_reprice_printer_missing, name) else uiText(Res.string.history_reprice_printer_not_saved)

            RepriceResult.Reason.CHANNEL_MISSING -> uiText(Res.string.history_reprice_channel_missing, name)

            RepriceResult.Reason.CONSUMABLE_MISSING -> uiText(Res.string.history_reprice_consumable_missing, name)

            RepriceResult.Reason.INVALID -> uiText(Res.string.history_reprice_invalid)
        }
        RepriceNotice.Unavailable(UiText.Joined(listOf(cause, uiText(Res.string.history_reprice_suffix)), separator = " "))
    }
}
