package com.threedreport.app.data

import com.threedreport.core.model.Filament
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de filamentos, editável pela tela de Filamentos e escolhido por
 * orçamento na tela de Orçamento.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), pré-carregada com materiais comuns no primeiro uso.
 */
expect class FilamentRepository() {
    val filaments: StateFlow<List<Filament>>
    fun add(filament: Filament)
    fun update(filament: Filament)
    fun delete(id: String)
}
