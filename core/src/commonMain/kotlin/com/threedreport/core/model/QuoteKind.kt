package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * O que um [SavedQuote] é (decisão 101): um **pedido** (venda, com cliente e andamento) ou um
 * **produto** do catálogo (peça que o vendedor oferece, com preço mas sem cliente nem andamento).
 * Um produto vira pedido pelo "Vender" do Histórico, que cria um pedido novo e deixa o produto
 * intacto.
 */
@Serializable
enum class QuoteKind(val label: String) {
    ORDER("Pedido"),
    PRODUCT("Produto"),
}
