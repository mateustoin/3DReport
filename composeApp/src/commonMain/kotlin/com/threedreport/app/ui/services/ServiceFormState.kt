package com.threedreport.app.ui.services

import com.threedreport.core.model.Service

/**
 * Rascunho do formulário de serviço. `id == null` significa que é um
 * serviço novo (ainda não salvo); caso contrário, é uma edição.
 */
data class ServiceFormState(
    val id: String? = null,
    val name: String = "",
    val priceText: String = "",
    val errorMessage: String? = null,
)

internal fun Service.toFormState() = ServiceFormState(
    id = id,
    name = name,
    priceText = price.toString(),
)
