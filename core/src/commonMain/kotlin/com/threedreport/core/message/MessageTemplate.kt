package com.threedreport.core.message

import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote

/**
 * Os modelos de mensagem pro cliente, um por momento do pedido (decisão 148). O texto padrão de cada um mora nos
 * recursos traduzíveis do `composeApp`; o editado pela pessoa fica em
 * [com.threedreport.core.model.BrandingSettings.messageTemplates], pela [key].
 *
 * @property key nome fixo gravado no arquivo, separado do nome da constante (como os `@SerialName` de [OrderStatus]).
 */
enum class MessageTemplateKind(val key: String) {
    APPROVED("approved"),
    PRINTING("printing"),
    READY("ready"),
    OUT_FOR_DELIVERY("outForDelivery"),
    PAYMENT_REMINDER("paymentReminder"),
    FOLLOW_UP("followUp"),
    ;

    companion object {
        /** O modelo da chave gravada, ou `null` se esta versão não conhece. */
        fun ofKey(key: String): MessageTemplateKind? = entries.firstOrNull { it.key == key }

        /**
         * O modelo oferecido ao mudar o andamento pra [newStatus] (decisão 148, item 24): Aprovado, Imprimindo, Pronto e
         * Entregue ("Saiu pra entrega"). Orçado e Cancelado não oferecem mensagem (`null`).
         */
        fun forStatusChange(newStatus: OrderStatus): MessageTemplateKind? = when (newStatus) {
            OrderStatus.APROVADO -> APPROVED
            OrderStatus.EM_IMPRESSAO -> PRINTING
            OrderStatus.PRONTO -> READY
            OrderStatus.ENTREGUE -> OUT_FOR_DELIVERY
            OrderStatus.ORCADO, OrderStatus.CANCELADO -> null
        }

        /**
         * O modelo em que "Mensagem pro cliente" abre, pelo andamento atual de [order] (decisão 148, item 24): Orçado
         * abre "Retomar orçamento"; Pronto ou Entregue com falta a pagar abrem "Lembrete de pagamento"; o resto, o
         * modelo do andamento. `null` em Cancelado e em produto do catálogo, que não têm modelo.
         */
        fun forOrder(order: SavedQuote): MessageTemplateKind? {
            if (!order.isOrder) return null
            return when (order.status) {
                OrderStatus.ORCADO -> FOLLOW_UP
                OrderStatus.PRONTO, OrderStatus.ENTREGUE -> if (order.remaining > 0.0) PAYMENT_REMINDER else forStatusChange(order.status)
                else -> forStatusChange(order.status)
            }
        }
    }
}

/** Marcadores dos modelos de mensagem (decisão 148, item 21). O [token] é o que a pessoa escreve no modelo. */
enum class MessagePlaceholder(val token: String) {
    CLIENT("{cliente}"),
    ORDER("{pedido}"),
    PIECE("{peca}"),
    DEADLINE("{prazo}"),
    VALUE("{valor}"),
    REMAINING("{falta}"),
    TRACKING("{rastreio}"),
    STORE("{loja}"),
}

/** Marca do lugar de um marcador sem valor, durante a limpeza. Não aparece em texto digitado. */
private const val GAP = '\u0000'

private val TOKENS = Regex(MessagePlaceholder.entries.joinToString("|") { Regex.escape(it.token) })
private val GAPS_IN_A_ROW = Regex("$GAP[ \\t]*$GAP")
private val EMPTY_PARENTHESES = Regex("\\([ \\t]*$GAP[ \\t]*\\)")
private val GAP_BEFORE_PUNCTUATION = Regex("[,;:]?[ \\t]*$GAP[ \\t]*(?=[,.;:!?)])")
private val GAP_AT_LINE_START = Regex("^[ \\t]*$GAP[ \\t]*", RegexOption.MULTILINE)
private val GAP_AT_LINE_END = Regex("[ \\t]*$GAP[ \\t]*$", RegexOption.MULTILINE)
private val GAP_BETWEEN_WORDS = Regex("[ \\t]*$GAP[ \\t]*")

/**
 * Preenche o modelo [template] com [values] (decisão 148, item 21). Função pura: quem chama já passa cada valor
 * formatado (dinheiro, data) no idioma do app.
 *
 * Marcador sem valor (ausente, `null` ou em branco) some junto com o espaço que sobraria: no meio da frase fica um
 * espaço só; antes de pontuação sai o espaço (e a vírgula de antes, em "Oi, {cliente}!" vira "Oi!"); parênteses que
 * ficariam vazios saem; no começo e no fim da linha não sobra espaço. O resto do texto, inclusive as quebras de linha
 * e chaves que não são marcadores, fica como está. O valor não é lido de novo como modelo.
 */
fun fillMessageTemplate(template: String, values: Map<MessagePlaceholder, String?>): String {
    val filled = TOKENS.replace(template.replace(GAP, ' ')) { match ->
        val placeholder = MessagePlaceholder.entries.first { it.token == match.value }
        values[placeholder]?.trim()?.takeIf { it.isNotEmpty() }?.replace(GAP, ' ') ?: GAP.toString()
    }
    if (GAP !in filled) return filled
    var text = filled
    while (GAPS_IN_A_ROW.containsMatchIn(text)) text = GAPS_IN_A_ROW.replace(text, GAP.toString())
    text = EMPTY_PARENTHESES.replace(text, GAP.toString())
    text = GAP_BEFORE_PUNCTUATION.replace(text, "")
    text = GAP_AT_LINE_START.replace(text, "")
    text = GAP_AT_LINE_END.replace(text, "")
    return GAP_BETWEEN_WORDS.replace(text, " ")
}

/**
 * Os valores dos marcadores pra o pedido [order]: cliente (nome), número, nome da peça, prazo, total, o que falta
 * (`null` quando não falta nada), código de rastreio e o nome da loja ([storeName]). [formatMoney] e [formatDate]
 * formatam na moeda do pedido e no idioma do app (o `core` não formata pra tela).
 */
fun orderMessageValues(
    order: SavedQuote,
    storeName: String?,
    formatMoney: (Double) -> String,
    formatDate: (epochDay: Long) -> String,
): Map<MessagePlaceholder, String?> = mapOf(
    MessagePlaceholder.CLIENT to order.client?.name?.trim(),
    MessagePlaceholder.ORDER to order.displayNumber,
    MessagePlaceholder.PIECE to order.name.trim(),
    MessagePlaceholder.DEADLINE to order.deliveryDateEpochDay?.let(formatDate),
    MessagePlaceholder.VALUE to formatMoney(order.totalWithServices),
    MessagePlaceholder.REMAINING to order.remaining.takeIf { it > 0.0 }?.let(formatMoney),
    MessagePlaceholder.TRACKING to order.trackingCode?.trim(),
    MessagePlaceholder.STORE to storeName?.trim(),
)
