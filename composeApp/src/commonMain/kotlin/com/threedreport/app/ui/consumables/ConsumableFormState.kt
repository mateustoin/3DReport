package com.threedreport.app.ui.consumables

import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.Consumable

/**
 * Rascunho do formulário de insumo. `id == null` significa que é um insumo novo (ainda não salvo);
 * caso contrário, é uma edição. Cada erro fica no campo a que se refere.
 */
data class ConsumableFormState(
    val id: String? = null,
    val name: String = "",
    val unitCostText: String = "",
    val chargedPerOrder: Boolean = false,
    val nameError: UiText? = null,
    val unitCostError: UiText? = null,
)

internal fun Consumable.toFormState(decimalSeparator: Char = ',') = ConsumableFormState(
    id = id,
    name = name,
    unitCostText = unitCost.toInputText(decimalSeparator),
    chargedPerOrder = chargedPerOrder,
)
