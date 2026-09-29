package com.threedreport.app.ui.consumables

import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.Consumable

/**
 * Rascunho do formulário de insumo. `id == null` significa que é um insumo novo (ainda não salvo);
 * caso contrário, é uma edição.
 */
data class ConsumableFormState(
    val id: String? = null,
    val name: String = "",
    val unitCostText: String = "",
    val chargedPerOrder: Boolean = false,
    val errorMessage: String? = null,
)

internal fun Consumable.toFormState() = ConsumableFormState(
    id = id,
    name = name,
    unitCostText = unitCost.toInputText(),
    chargedPerOrder = chargedPerOrder,
)
