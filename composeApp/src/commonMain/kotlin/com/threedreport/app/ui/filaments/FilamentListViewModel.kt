package com.threedreport.app.ui.filaments

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.core.model.Filament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da tela de Filamentos: lista o catálogo e edita um item por vez
 * em [form] (nulo quando nenhum formulário está aberto).
 */
class FilamentListViewModel(private val repository: FilamentRepository) {

    val filaments: StateFlow<List<Filament>> = repository.filaments

    private val formState = MutableStateFlow<FilamentFormState?>(null)
    val form: StateFlow<FilamentFormState?> = formState.asStateFlow()

    fun startAdd() {
        formState.value = FilamentFormState()
    }

    fun startEdit(filament: Filament) {
        formState.value = filament.toFormState()
    }

    fun cancelEdit() {
        formState.value = null
    }

    fun updateForm(transform: (FilamentFormState) -> FilamentFormState) {
        formState.value = formState.value?.let { transform(it).copy(errorMessage = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = formState.value ?: return
        val result = runCatching {
            Filament(
                id = current.id ?: Uuid.random().toString(),
                name = current.name.trim().ifEmpty { error("Nome não pode ser vazio") },
                pricePerKg = current.pricePerKgText.toRequiredDouble("Preço/kg"),
                densityGPerCm3 = current.densityGPerCm3Text.toRequiredDouble("Densidade"),
                diameterMm = current.diameterMmText.toRequiredDouble("Diâmetro"),
                brand = current.brand.trim().ifEmpty { null },
                colorName = current.colorName.trim().ifEmpty { null },
                colorHex = current.colorHex,
                inStock = current.inStock,
            )
        }

        result.fold(
            onSuccess = { filament ->
                if (current.id == null) repository.add(filament) else repository.update(filament)
                formState.value = null
            },
            onFailure = { formState.value = current.copy(errorMessage = it.message) },
        )
    }

    fun delete(id: String) {
        repository.delete(id)
        if (formState.value?.id == id) formState.value = null
    }

    /** Alterna manualmente entre "Em estoque" e "Acabou" — sem tentar calcular automaticamente. */
    fun toggleInStock(id: String) {
        val filament = repository.filaments.value.find { it.id == id } ?: return
        repository.update(filament.copy(inStock = !filament.inStock))
    }
}
