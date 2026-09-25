package com.threedreport.app.ui.services

import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.core.model.Service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela de Serviços: lista o catálogo e edita um item por vez
 * em [form] (nulo quando nenhum formulário está aberto).
 */
class ServiceListViewModel(private val repository: ServiceRepository) {

    val services: StateFlow<List<Service>> = repository.services

    private val formState = MutableStateFlow<ServiceFormState?>(null)
    val form: StateFlow<ServiceFormState?> = formState.asStateFlow()

    fun startAdd() {
        formState.value = ServiceFormState()
    }

    fun startEdit(service: Service) {
        formState.value = service.toFormState()
    }

    fun cancelEdit() {
        formState.value = null
    }

    fun updateForm(transform: (ServiceFormState) -> ServiceFormState) {
        formState.value = formState.value?.let { transform(it).copy(errorMessage = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value ?: return
        val result = runCatching {
            Service(
                id = current.id ?: Uuid.random().toString(),
                name = current.name.trim().ifEmpty { error("Nome não pode ser vazio") },
                suggestedPrice = current.priceText.trim().ifEmpty { null }
                    ?.toRequiredDouble("Valor sugerido")
                    ?.also { if (it < 0) error("Valor sugerido não pode ser negativo") },
                chargedPerOrder = current.chargedPerOrder,
            )
        }

        result.fold(
            onSuccess = { service ->
                if (current.id == null) repository.add(service) else repository.update(service)
                formState.value = null
            },
            onFailure = { formState.value = current.copy(errorMessage = it.message) },
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value?.id == id) formState.value = null
    }
}
