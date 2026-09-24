package com.threedreport.core.model

import kotlinx.serialization.Serializable

/** Andamento de um orçamento salvo, editável no Histórico. */
@Serializable
enum class OrderStatus(val label: String) {
    ORCADO("Orçado"),
    APROVADO("Aprovado"),
    EM_IMPRESSAO("Em impressão"),
    PRONTO("Pronto"),
    ENTREGUE("Entregue"),
    ;

    /**
     * Se o cliente já fechou o pedido. Só [ORCADO] fica de fora: é orçamento enviado, não venda. É a
     * única regra de "o que é venda" pro Dashboard (decisão 95), pra tela e relatório não divergirem.
     */
    val isSold: Boolean
        get() = this != ORCADO
}
