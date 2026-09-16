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
}
