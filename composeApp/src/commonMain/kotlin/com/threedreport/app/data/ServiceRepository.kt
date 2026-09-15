package com.threedreport.app.data

import com.threedreport.core.model.Service
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de serviços opcionais (pintura, lixamento, acabamento etc.),
 * editável pela tela de Serviços e escolhido por orçamento na tela de
 * Orçamento.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma). Diferente de filamentos/impressoras, começa **vazio** — não
 * há serviços "padrão" que façam sentido pra qualquer criador.
 */
expect class ServiceRepository() {
    val services: StateFlow<List<Service>>
    fun add(service: Service)
    fun update(service: Service)
    fun delete(id: String)
}
