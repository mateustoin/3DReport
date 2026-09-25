package com.threedreport.app.ui.quote

import com.threedreport.core.model.SavedQuote

/**
 * O que a tela de Orçamento está fazendo além de um orçamento novo (decisão 108). Um tipo só, no lugar
 * de cinco campos soltos (`editingQuoteId`, `duplicatedFromName`…) que podiam se combinar de jeitos
 * sem sentido.
 */
sealed interface QuoteOperation {

    /**
     * Reabrindo [savedQuote] pra corrigir. [originalInput] é como a tela ficou ao abrir: enquanto nada que
     * muda o preço for mexido, salvar mantém o cálculo congelado do orçamento (não reprecifica com os
     * custos de hoje só por corrigir o contato do cliente).
     */
    data class Editing(
        val savedQuote: SavedQuote,
        val originalInput: QuoteInputState,
        /** O formulário de salvar como abriu, pra saber se há alteração a perder ao cancelar. */
        val originalForm: SaveQuoteFormState,
    ) : QuoteOperation

    /** Duplicando o orçamento chamado [fromName] (vira um orçamento novo). */
    data class Duplicating(val fromName: String) : QuoteOperation

    /** "Vender" o produto [product] do catálogo (vira um pedido novo, com a origem guardada). */
    data class Selling(val product: SavedQuote) : QuoteOperation

    /** "Guardar no catálogo" a partir do pedido chamado [fromName]. */
    data class CopyingToCatalog(val fromName: String) : QuoteOperation
}
