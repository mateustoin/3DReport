package com.threedreport.app.ui.history

import com.threedreport.app.data.CatalogView
import com.threedreport.app.data.OrdersView
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.components.OverdueItem
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.Client
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote

/** Como o selo de prazo de um pedido se pinta. O texto vai junto, em [DeadlineBadge]: a cor nunca fala sozinha. */
enum class DeadlineTone {
    /** Prazo à frente. */
    ON_TIME,

    /** Entrega hoje. */
    TODAY,

    /** Aprovado, imprimindo ou pronto, com a data já passada: tem cliente esperando. */
    LATE,

    /** Orçamento parado com a data vencida: pede data nova antes de reenviar, mas não é atraso. */
    STALE,

    /** Entregue ou cancelado: só lembra o que era. */
    DONE,
}

data class DeadlineBadge(val text: UiText, val tone: DeadlineTone)

/** Resultado de recalcular um produto com os cadastros de hoje, pro aviso "Custos mudaram" do cartão. */
sealed interface RepriceNotice {
    /** O preço calculado hoje seria [newPrice]. */
    data class CostsChanged(val newPrice: Double) : RepriceNotice

    /** Não deu pra recalcular; [reason] diz por quê e o que fazer. */
    data class Unavailable(val reason: UiText) : RepriceNotice
}

/**
 * Uma linha da tela (linha da tabela de Pedidos, cartão do kanban, cartão ou linha do Catálogo), com o que a
 * tela precisaria calcular. Pedido preenche [deadline]; produto, [reprice] e [canConvertToOrder].
 */
data class HistoryRow(
    val quote: SavedQuote,
    val deadline: DeadlineBadge? = null,
    /** Quantas impressões o pedido tem, quando mais de uma (decisão 114); `null` com uma só. */
    val printCount: Int? = null,
    val reprice: RepriceNotice? = null,
    /** Produto com custos mudados: dá pra abrir o "Atualizar preço". */
    val canReprice: Boolean = false,
    val canConvertToOrder: Boolean = false,
    /** Só com uma impressão dá pra editar as configurações aqui; com várias, é no Orçamento (decisão 114). */
    val canEditPrintSettings: Boolean = false,
)

/** Quanto uma lista de pedidos soma numa [currency]. Moedas diferentes nunca se somam num número só. */
data class CurrencyTotal(val currency: Currency, val amount: Double)

/** Uma coluna do kanban: a situação, os cartões e o que eles somam, uma soma por moeda (a do app primeiro). */
data class KanbanColumnState(val status: OrderStatus, val cards: List<HistoryRow>, val total: List<CurrencyTotal>)

/**
 * Os blocos de resumo de Pedidos. "Em andamento" são os pedidos de Aprovado a Pronto (sem Orçado); "a receber"
 * é o que eles somam, por moeda; "pra hoje" e "atrasados" seguem o mesmo selo de prazo dos cartões e contam de Orçado a Pronto.
 */
data class OrdersSummary(val inProgress: Int = 0, val receivable: List<CurrencyTotal> = emptyList(), val dueToday: Int = 0, val overdue: Int = 0)

/** "Atualizar preço" aberto: o produto como está e como ficaria com os cadastros de hoje. */
data class Repricing(
    val product: SavedQuote,
    val newQuote: Quote,
    val priceBefore: Double,
    val priceAfter: Double,
    val costBefore: Double,
    val costAfter: Double,
    val profitBefore: Double,
    val profitAfter: Double,
    /** O preço anunciado, que continua o mesmo, quando o produto tem um; `null` sem preço anunciado. */
    val announcedPrice: Double?,
    /** Quanto de prejuízo o preço anunciado daria com os custos de hoje, e o mínimo pra não sair no negativo. */
    val loss: Double?,
    val breakEven: Double?,
)

/** Os envios pro cliente que mostram o prazo, e por isso passam pelo aviso de prazo vencido. */
enum class ClientExport { PDF, IMAGE, WHATSAPP, COPY, SELECTED_PDF }

/** Envio segurado pelo aviso de prazo vencido: o que ia ser feito, com quais orçamentos e quais deles venceram. */
data class PendingExport(val export: ClientExport, val quotes: List<SavedQuote>, val overdue: List<OverdueItem>)

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

/** O diálogo "Editar detalhes" aberto: o orçamento, o que a pessoa já digitou e o problema com a foto, se houver. */
data class DetailsEditing(val quote: SavedQuote, val form: DetailsForm, val photoError: UiText? = null)

/**
 * Tudo o que Pedidos e o Catálogo mostram, num valor só. Cada tela tem o seu ViewModel, com o tipo fixo em
 * [filter] (decisão 111); os campos que só uma das telas usa ficam vazios na outra.
 *
 * @property rows o que a lista, a tabela ou a grade mostram: o filtro já aplicado. Em Pedidos, ordenados pelo prazo.
 * @property columns as colunas do kanban, na ordem do fluxo. Só Pedidos.
 * @property hasItems existe ao menos um item deste tipo, com ou sem filtro. Falso é a tela vazia de primeira vez.
 * @property hiddenDelivered quantos entregues antigos o kanban esconde, pro "ver mais".
 * @property cancelledCount pedidos cancelados, que ficam fora do kanban.
 * @property categories categorias dos produtos, uma por grafia, em ordem alfabética. Só o Catálogo.
 * @property hasUncategorized há produto sem categoria, pra oferecer "Sem categoria".
 * @property selectionMode as linhas mostram a caixa de marcar e o rodapé de exportar aparece.
 * @property selectedIds os marcados, inclusive os que o filtro esconde agora; [selectedVisibleCount] conta só os da lista.
 * @property today hoje, em dias desde 01/01/1970, a mesma régua do aviso de prazo vencido.
 * @property busy o que está sendo gerado agora ("Gerando PDF"), ou `null`.
 */
data class HistoryUiState(
    val filter: HistoryFilter,
    val ordersView: OrdersView = OrdersView.KANBAN,
    val catalogView: CatalogView = CatalogView.GRID,
    val showAllDelivered: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val copiedId: String? = null,
    val busy: UiText? = null,
    val deliveryDateEditing: SavedQuote? = null,
    val printSettingsEditing: SavedQuote? = null,
    val details: DetailsEditing? = null,
    val repricing: Repricing? = null,
    val pendingExport: PendingExport? = null,
    val rows: List<HistoryRow> = emptyList(),
    val columns: List<KanbanColumnState> = emptyList(),
    val summary: OrdersSummary = OrdersSummary(),
    val hasItems: Boolean = false,
    val hiddenDelivered: Int = 0,
    val cancelledCount: Int = 0,
    val categories: List<String> = emptyList(),
    val hasUncategorized: Boolean = false,
    val selectedVisibleCount: Int = 0,
    val clients: List<Client> = emptyList(),
    val today: Long = 0L,
    val currency: Currency = Currency.BRL,
) {
    val isOrders: Boolean get() = filter.kind == QuoteKind.ORDER
}
