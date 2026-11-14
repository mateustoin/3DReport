package com.threedreport.app.ui.services

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.services_error_minutes_invalid
import _3dreport.composeapp.generated.resources.services_notice_archived
import _3dreport.composeapp.generated.resources.services_notice_deleted
import _3dreport.composeapp.generated.resources.services_notice_restored
import _3dreport.composeapp.generated.resources.services_notice_saved
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.ui.components.PendingDelete
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseDurationMinutes
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tudo o que a tela de Serviços mostra: a lista (ativos e arquivados), o formulário aberto (nulo quando nenhum) e a
 * pergunta de exclusão pendente (nula quando nenhuma).
 */
data class ServicesUiState(
    val active: List<Service> = emptyList(),
    val archived: List<Service> = emptyList(),
    val form: ServiceFormState? = null,
    val pendingDelete: PendingDelete? = null,
) {
    /** Nenhum serviço cadastrado, nem arquivado: a tela mostra o estado vazio com o botão que cria o primeiro. */
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

/**
 * ViewModel da tela de Serviços. Um [uiState] só, e os [notices] (avisos de uma vez, como "Serviço arquivado").
 * A lista acompanha o repositório dentro de [scope]; o resto muda na hora, pelos eventos.
 */
class ServiceListViewModel(
    private val repository: ServiceRepository,
    scope: CoroutineScope,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada serviço antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
) {

    private val state = MutableStateFlow(ServicesUiState().withItems(repository.services.value))
    val uiState: StateFlow<ServicesUiState> = state.asStateFlow()

    private val noticeFlow = MutableSharedFlow<UiText>(extraBufferCapacity = NOTICE_BUFFER)
    val notices: SharedFlow<UiText> = noticeFlow.asSharedFlow()

    init {
        repository.services.onEach { items -> state.update { it.withItems(items) } }.launchIn(scope)
    }

    fun startAdd() {
        state.update { it.copy(form = ServiceFormState()) }
    }

    fun startEdit(id: String) {
        val service = repository.services.value.find { it.id == id } ?: return
        state.update { it.copy(form = service.toFormState()) }
    }

    fun cancelEdit() {
        state.update { it.copy(form = null) }
    }

    fun updateForm(transform: (ServiceFormState) -> ServiceFormState) {
        state.update { s ->
            s.copy(form = s.form?.let { transform(it).copy(nameError = null, priceError = null, minutesError = null) })
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = state.value.form ?: return
        val name = current.name.trim()
        val priceText = current.priceText.trim()
        val minutesText = current.minutesText.trim()
        val price = parseDecimal(priceText)
        val minutes = parseDurationMinutes(minutesText)

        val nameError = uiText(Res.string.common_error_name_required).takeIf { name.isEmpty() }
        val priceError = when {
            priceText.isEmpty() -> null
            price == null -> uiText(Res.string.common_error_number_invalid)
            price < 0 -> uiText(Res.string.common_error_negative)
            else -> null
        }
        val minutesError = uiText(Res.string.services_error_minutes_invalid).takeIf { minutesText.isNotEmpty() && minutes == null }
        if (nameError != null || priceError != null || minutesError != null) {
            state.update { it.copy(form = current.copy(nameError = nameError, priceError = priceError, minutesError = minutesError)) }
            return
        }

        val service = Service(
            id = current.id ?: Uuid.random().toString(),
            name = name,
            suggestedPrice = price.takeIf { priceText.isNotEmpty() },
            chargedPerOrder = current.chargedPerOrder,
            laborMinutes = minutes.takeIf { minutesText.isNotEmpty() },
        )
        if (current.id == null) repository.add(service) else repository.updateKeepingArchived(service)
        state.update { it.copy(form = null) }
        noticeFlow.tryEmit(uiText(Res.string.services_notice_saved))
    }

    /** Pergunta se pode excluir o serviço [id], com quantos pedidos e produtos o usam (decisão 115). */
    fun requestDelete(id: String) {
        val service = repository.services.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = PendingDelete(id, service.name, usageCount(id), canArchive = !service.archived)) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    /** Quantos pedidos e produtos usam o serviço [id]. */
    fun usageCount(id: String): Int = CatalogUsage.service(id, usageQuotes())

    /** Arquiva (ou restaura) o serviço [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        repository.setArchived(id, archived)
        state.update { it.copy(pendingDelete = null) }
        noticeFlow.tryEmit(uiText(if (archived) Res.string.services_notice_archived else Res.string.services_notice_restored))
    }

    fun delete(id: String) {
        repository.delete(id)
        state.update { s -> s.copy(form = s.form?.takeUnless { it.id == id }, pendingDelete = null) }
        noticeFlow.tryEmit(uiText(Res.string.services_notice_deleted))
    }

    private fun ServicesUiState.withItems(items: List<Service>): ServicesUiState {
        val (archived, active) = items.partition { it.archived }
        return copy(active = active, archived = archived)
    }

    private companion object {
        const val NOTICE_BUFFER = 8
    }
}
