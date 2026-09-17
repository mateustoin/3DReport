package com.threedreport.app.data

import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de templates de orçamento (presets nomeados de marca d'água/
 * rodapé) — ver [QuoteTemplate]. Começa vazio: não há um template padrão
 * que sirva pra qualquer criador (mesmo princípio de [ServiceRepository]).
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma).
 */
expect class TemplateRepository() {
    val templates: StateFlow<List<QuoteTemplate>>
    fun add(template: QuoteTemplate)
    fun update(template: QuoteTemplate)
    fun delete(id: String)
}
