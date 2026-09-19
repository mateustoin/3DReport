package com.threedreport.app.data

import com.threedreport.core.model.Filament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private val DEFAULT_FILAMENTS = listOf(
    Filament(id = "default-pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, materialType = "PLA"),
    Filament(id = "default-abs", name = "ABS", pricePerKg = 90.0, densityGPerCm3 = 1.04, materialType = "ABS"),
    Filament(id = "default-petg", name = "PETG", pricePerKg = 110.0, densityGPerCm3 = 1.27, materialType = "PETG"),
)

actual class FilamentRepository actual constructor() {
    private val file = File(appDataDir(), "filaments.json")
    private val state = MutableStateFlow(readJsonFile(file, DEFAULT_FILAMENTS))

    actual val filaments: StateFlow<List<Filament>> = state.asStateFlow()

    actual fun add(filament: Filament) {
        state.value = state.value + filament
        persist()
    }

    actual fun update(filament: Filament) {
        state.value = state.value.map { if (it.id == filament.id) filament else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}
