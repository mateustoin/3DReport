package com.threedreport.app.ui.consumables

import com.threedreport.core.model.SavedQuote
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.ui.format.toRequiredNonNegative
import com.threedreport.core.model.Consumable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela de Insumos (decisão 122): lista o catálogo e edita um item por vez em [form]
 * (nulo quando nenhum formulário está aberto). Espelha [com.threedreport.app.ui.services.ServiceListViewModel].
 */
class ConsumableListViewModel(
    private val repository: ConsumableRepository,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada insumo antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
) {

    val consumables: StateFlow<List<Consumable>> = repository.consumables

    private val formState = MutableStateFlow<ConsumableFormState?>(null)
    val form: StateFlow<ConsumableFormState?> = formState.asStateFlow()

    fun startAdd() {
        formState.value = ConsumableFormState()
    }

    fun startEdit(consumable: Consumable) {
        formState.value = consumable.toFormState()
    }

    fun cancelEdit() {
        formState.value = null
    }

    fun updateForm(transform: (ConsumableFormState) -> ConsumableFormState) {
        formState.value = formState.value?.let { transform(it).copy(errorMessage = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value ?: return
        val result = runCatching {
            Consumable(
                id = current.id ?: Uuid.random().toString(),
                name = current.name.trim().ifEmpty { error("Nome não pode ser vazio") },
                unitCost = current.unitCostText.toRequiredNonNegative("Custo por unidade"),
                chargedPerOrder = current.chargedPerOrder,
            )
        }

        result.fold(
            onSuccess = { consumable ->
                if (current.id == null) repository.add(consumable) else repository.updateKeepingArchived(consumable)
                formState.value = null
            },
            onFailure = { formState.value = current.copy(errorMessage = it.message) },
        )
    }

    /** Quantos pedidos e produtos usam o insumo [id]. */
    fun usageCount(id: String): Int = CatalogUsage.consumable(id, usageQuotes())

    /** Arquiva (ou restaura) o insumo [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) = repository.setArchived(id, archived)

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value?.id == id) formState.value = null
    }
}
