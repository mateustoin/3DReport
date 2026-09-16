package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Cliente vinculado a um orçamento salvo, uso **só interno**: nunca aparece
 * em nenhum export (PDF ou copiar/colar) — mesmo tratamento do link do
 * modelo (ver [SavedQuote.sourceLink]).
 *
 * @property name nome do cliente; nunca vazio (o campo inteiro é omitido em
 *   [SavedQuote] quando não preenchido, em vez de existir com nome vazio).
 * @property contact contato do cliente (telefone, e-mail, usuário de rede
 *   social etc.), opcional e em texto livre.
 */
@Serializable
data class Client(
    val name: String,
    val contact: String? = null,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
    }
}
