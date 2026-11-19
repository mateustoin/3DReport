package com.threedreport.app.ui.consumables

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.consumables_error_cost_required
import _3dreport.composeapp.generated.resources.consumables_notice_archived
import _3dreport.composeapp.generated.resources.consumables_notice_deleted
import _3dreport.composeapp.generated.resources.consumables_notice_restored
import _3dreport.composeapp.generated.resources.consumables_notice_saved
import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.StockRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.ui.components.PendingDelete
import com.threedreport.app.ui.filaments.ColorStock
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tudo o que a tela de Insumos mostra: a lista (ativos e arquivados), o formulário aberto (nulo quando nenhum) e a
 * pergunta de exclusão pendente (nula quando nenhuma).
 */
data class ConsumablesUiState(
    val active: List<Consumable> = emptyList(),
    val archived: List<Consumable> = emptyList(),
    val form: ConsumableFormState? = null,
    val pendingDelete: PendingDelete? = null,
    /** O saldo dos insumos com controle de saldo (Leva 12), pelo id do insumo, pra a linha mostrar "8 un em estoque" e, se baixo ou sem saldo, dizer. */
    val balances: Map<String, ColorStock> = emptyMap(),
) {
    /** Nenhum insumo cadastrado, nem arquivado: a tela mostra o estado vazio com o botão que cria o primeiro. */
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

/**
 * ViewModel da tela de Insumos (decisão 122). Um [uiState] só, e os [notices] (avisos de uma vez, como "Insumo
 * arquivado"). A lista acompanha o repositório dentro de [scope]; o resto muda na hora, pelos eventos.
 */
class ConsumableListViewModel(
    private val repository: ConsumableRepository,
    scope: CoroutineScope,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada insumo antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
    /** A moeda em uso: os números são lidos e mostrados com o separador decimal dela, o mesmo que o campo usa pra validar. */
    private val currency: () -> Currency = { Currency.BRL },
    /** O estoque (Leva 12): com ele, a linha do insumo controlado mostra o saldo. */
    private val stock: StockRepository? = null,
) {

    private val state = MutableStateFlow(ConsumablesUiState().withItems(repository.consumables.value))
    val uiState: StateFlow<ConsumablesUiState> = state.asStateFlow()

    private val noticeFlow = MutableSharedFlow<UiText>(extraBufferCapacity = NOTICE_BUFFER)
    val notices: SharedFlow<UiText> = noticeFlow.asSharedFlow()

    init {
        repository.consumables.onEach { items -> state.update { it.withItems(items) } }.launchIn(scope)
        stock?.let { tracking ->
            state.update { it.copy(balances = balancesOf(tracking)) }
            combine(tracking.items, tracking.movements) { _, _ -> }
                .onEach { state.update { it.copy(balances = balancesOf(tracking)) } }
                .launchIn(scope)
        }
    }

    fun startAdd() {
        state.update { it.copy(form = ConsumableFormState()) }
    }

    fun startEdit(id: String) {
        val consumable = repository.consumables.value.find { it.id == id } ?: return
        state.update { it.copy(form = consumable.toFormState(currency().decimalSeparator)) }
    }

    fun cancelEdit() {
        state.update { it.copy(form = null) }
    }

    fun updateForm(transform: (ConsumableFormState) -> ConsumableFormState) {
        state.update { s ->
            s.copy(form = s.form?.let { transform(it).copy(nameError = null, unitCostError = null) })
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = state.value.form ?: return
        val name = current.name.trim()
        val cost = current.unitCostText.trim().takeIf { it.isNotEmpty() }?.let { parseDecimal(it, NumberKind.AMOUNT, currency().decimalSeparator) }

        val nameError = uiText(Res.string.common_error_name_required).takeIf { name.isEmpty() }
        val costError = when {
            current.unitCostText.isBlank() -> uiText(Res.string.consumables_error_cost_required)
            cost == null -> uiText(Res.string.common_error_number_invalid)
            cost < 0 -> uiText(Res.string.common_error_negative)
            else -> null
        }
        if (nameError != null || costError != null || cost == null) {
            state.update { it.copy(form = current.copy(nameError = nameError, unitCostError = costError)) }
            return
        }

        val consumable = Consumable(
            id = current.id ?: Uuid.random().toString(),
            name = name,
            unitCost = cost,
            chargedPerOrder = current.chargedPerOrder,
        )
        if (current.id == null) repository.add(consumable) else repository.updateKeepingArchived(consumable)
        state.update { it.copy(form = null) }
        noticeFlow.tryEmit(uiText(Res.string.consumables_notice_saved))
    }

    /** Pergunta se pode excluir o insumo [id], com quantos pedidos e produtos o usam (decisão 115). */
    fun requestDelete(id: String) {
        val consumable = repository.consumables.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = PendingDelete(id, consumable.name, usageCount(id), canArchive = !consumable.archived)) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    /** Quantos pedidos e produtos usam o insumo [id]. */
    fun usageCount(id: String): Int = CatalogUsage.consumable(id, usageQuotes())

    /** Arquiva (ou restaura) o insumo [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        repository.setArchived(id, archived)
        state.update { it.copy(pendingDelete = null) }
        noticeFlow.tryEmit(uiText(if (archived) Res.string.consumables_notice_archived else Res.string.consumables_notice_restored))
    }

    fun delete(id: String) {
        repository.delete(id)
        state.update { s -> s.copy(form = s.form?.takeUnless { it.id == id }, pendingDelete = null) }
        noticeFlow.tryEmit(uiText(Res.string.consumables_notice_deleted))
    }

    private fun balancesOf(tracking: StockRepository): Map<String, ColorStock> {
        val balances = StockReport.balances(tracking.movements.value)
        return tracking.items.value.mapNotNull { item ->
            (item.ref as? StockItemRef.ConsumableRef)?.let {
                val balance = balances[item.ref] ?: 0.0
                val state = StockReport.state(item, balance)
                it.consumableId to ColorStock(balance, state != StockState.OK, state == StockState.OUT)
            }
        }.toMap()
    }

    private fun ConsumablesUiState.withItems(items: List<Consumable>): ConsumablesUiState {
        val (archived, active) = items.partition { it.archived }
        return copy(active = active, archived = archived)
    }

    private companion object {
        const val NOTICE_BUFFER = 8
    }
}
