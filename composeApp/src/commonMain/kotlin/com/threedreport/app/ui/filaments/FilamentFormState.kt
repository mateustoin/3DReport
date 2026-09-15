package com.threedreport.app.ui.filaments

import com.threedreport.core.model.Filament

/**
 * Rascunho do formulário de filamento. `id == null` significa que é um
 * filamento novo (ainda não salvo); caso contrário, é uma edição.
 */
data class FilamentFormState(
    val id: String? = null,
    val name: String = "",
    val pricePerKgText: String = "",
    val densityGPerCm3Text: String = "",
    val diameterMmText: String = Filament.DEFAULT_DIAMETER_MM.toString(),
    val errorMessage: String? = null,
)

internal fun Filament.toFormState() = FilamentFormState(
    id = id,
    name = name,
    pricePerKgText = pricePerKg.toString(),
    densityGPerCm3Text = densityGPerCm3.toString(),
    diameterMmText = diameterMm.toString(),
)
