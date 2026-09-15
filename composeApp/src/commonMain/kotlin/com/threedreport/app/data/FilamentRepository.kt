package com.threedreport.app.data

import com.threedreport.core.model.Filament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Catálogo de filamentos disponíveis na tela de orçamento.
 *
 * Implementação atual mantém a lista apenas em memória (perdida ao fechar o
 * app). Persistência real e cadastro de novos filamentos ficam para uma
 * decisão futura (ver "Pendentes de aprovação" em docs/decisions.md);
 * por ora a lista é pré-carregada com materiais comuns.
 */
class FilamentRepository {

    private val state = MutableStateFlow(DEFAULT_FILAMENTS)
    val filaments: StateFlow<List<Filament>> = state.asStateFlow()

    companion object {
        /** Densidades de referência: ver KDoc de [Filament.densityGPerCm3]. */
        val DEFAULT_FILAMENTS = listOf(
            Filament(name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            Filament(name = "ABS", pricePerKg = 90.0, densityGPerCm3 = 1.04),
            Filament(name = "PETG", pricePerKg = 110.0, densityGPerCm3 = 1.27),
        )
    }
}
