package com.threedreport.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Andamento de um pedido salvo, editável no Histórico.
 *
 * O nome gravado no arquivo ([SerialName]) é fixo e em inglês, separado do nome da constante: renomear
 * uma constante no código não pode mudar o significado dos dados já salvos (decisão 106).
 */
@Serializable
enum class OrderStatus(val label: String) {
    @SerialName("quoted")
    ORCADO("Orçado"),

    @SerialName("approved")
    APROVADO("Aprovado"),

    @SerialName("printing")
    EM_IMPRESSAO("Em impressão"),

    @SerialName("ready")
    PRONTO("Pronto"),

    @SerialName("delivered")
    ENTREGUE("Entregue"),

    /**
     * O cliente recusou ou desistiu. Fica fora das vendas e dos orçamentos em aberto, mas continua no
     * histórico (excluir apagaria a informação de que o orçamento existiu).
     */
    @SerialName("cancelled")
    CANCELADO("Cancelado"),
    ;

    /**
     * Se o cliente já fechou o pedido. [ORCADO] e [CANCELADO] ficam de fora. É a única regra de "o que
     * é venda" pro Dashboard (decisão 95), pra tela e relatório não divergirem.
     */
    val isSold: Boolean
        get() = this != ORCADO && this != CANCELADO

    /** Orçamento enviado que ainda espera resposta do cliente. */
    val isOpen: Boolean
        get() = this == ORCADO

    companion object {
        /** A ordem do fluxo de um pedido, sem [CANCELADO], que sai do fluxo (colunas do Kanban). */
        val PIPELINE: List<OrderStatus> = listOf(ORCADO, APROVADO, EM_IMPRESSAO, PRONTO, ENTREGUE)
    }
}

/**
 * Uma mudança de andamento de um pedido, com o momento em que aconteceu (decisão 106). É o que dá a
 * data da venda (quando saiu de Orçado), da entrega e de quando a impressão terminou.
 */
@Serializable
data class StatusChange(
    val status: OrderStatus,
    val atEpochMillis: Long,
)
