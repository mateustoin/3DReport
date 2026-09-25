package com.threedreport.app.ui.services

import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.Service

/**
 * Rascunho do formulário de serviço. `id == null` significa que é um
 * serviço novo (ainda não salvo); caso contrário, é uma edição.
 */
data class ServiceFormState(
    val id: String? = null,
    val name: String = "",
    /** Valor sugerido; vazio quando muda a cada pedido. */
    val priceText: String = "",
    val chargedPerOrder: Boolean = false,
    val errorMessage: String? = null,
)

internal fun Service.toFormState() = ServiceFormState(
    id = id,
    name = name,
    priceText = suggestedPrice?.toInputText().orEmpty(),
    chargedPerOrder = chargedPerOrder,
)
