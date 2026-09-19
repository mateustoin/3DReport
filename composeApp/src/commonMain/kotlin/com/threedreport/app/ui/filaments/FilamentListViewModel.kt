package com.threedreport.app.ui.filaments

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.ui.format.toRequiredDouble
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
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
        formState.value = FilamentFormState(colors = listOf(FilamentColorFormState(id = newColorId())))
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

    fun addColorRow() {
        updateForm { it.copy(colors = it.colors + FilamentColorFormState(id = newColorId())) }
    }

    /** Não faz nada se for a última cor restante — o filamento sempre precisa de ao menos uma. */
    fun removeColorRow(colorId: String) {
        updateForm { form -> if (form.colors.size <= 1) form else form.copy(colors = form.colors.filterNot { it.id == colorId }) }
    }

    fun updateColorRow(colorId: String, transform: (FilamentColorFormState) -> FilamentColorFormState) {
        updateForm { form -> form.copy(colors = form.colors.map { if (it.id == colorId) transform(it) else it }) }
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
                materialType = current.materialType.trim().ifEmpty { null },
                colors = current.colors.ifEmpty { error("Cadastre ao menos uma cor") }.map {
                    FilamentColor(id = it.id, name = it.name.trim().ifEmpty { null }, hex = it.hex, inStock = it.inStock)
                },
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

    /** Alterna manualmente "Em estoque"/"Acabou" de uma cor específica — sem tentar calcular automaticamente. */
    fun toggleColorInStock(filamentId: String, colorId: String) {
        val filament = repository.filaments.value.find { it.id == filamentId } ?: return
        val updatedColors = filament.colors.map { if (it.id == colorId) it.copy(inStock = !it.inStock) else it }
        repository.update(filament.copy(colors = updatedColors))
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newColorId(): String = Uuid.random().toString()
}
