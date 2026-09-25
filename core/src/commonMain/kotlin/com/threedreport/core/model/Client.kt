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
 * @property id o cliente do cadastro de clientes de onde veio este retrato (decisão 106), ou `null`
 *   quando foi digitado sem cadastro. Nome e contato ficam guardados aqui mesmo assim: o pedido é um
 *   retrato, e editar o cadastro depois não muda um pedido antigo.
 */
@Serializable
data class Client(
    val name: String,
    val contact: String? = null,
    val id: String? = null,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
    }
}
