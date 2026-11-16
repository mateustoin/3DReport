package com.threedreport.app.ui.services

import com.threedreport.app.ui.format.toDurationInputText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.Service

/**
 * Rascunho do formulário de serviço. `id == null` significa que é um serviço novo (ainda não salvo);
 * caso contrário, é uma edição. Cada erro fica no campo a que se refere.
 */
data class ServiceFormState(
    val id: String? = null,
    val name: String = "",
    /** Valor sugerido; vazio quando muda a cada pedido. */
    val priceText: String = "",
    val chargedPerOrder: Boolean = false,
    /** Seu tempo neste serviço (decisão 123), em minutos; vazio quando não é contado. */
    val minutesText: String = "",
    val nameError: UiText? = null,
    val priceError: UiText? = null,
    val minutesError: UiText? = null,
)

internal fun Service.toFormState(decimalSeparator: Char = ',') = ServiceFormState(
    id = id,
    name = name,
    priceText = suggestedPrice?.toInputText(decimalSeparator).orEmpty(),
    chargedPerOrder = chargedPerOrder,
    minutesText = laborMinutes?.toDurationInputText().orEmpty(),
)
