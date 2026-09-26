package com.threedreport.app.ui.quote

import com.threedreport.core.model.QuoteKind
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

    /** Duplicando o orçamento chamado [fromName], de Pedidos ou do Catálogo ([fromKind]); vira um orçamento novo. */
    data class Duplicating(val fromName: String, val fromKind: QuoteKind) : QuoteOperation

    /** "Vender" o produto [product] do catálogo (vira um pedido novo, com a origem guardada). */
    data class Selling(val product: SavedQuote) : QuoteOperation

    /** "Guardar no catálogo" a partir do pedido chamado [fromName]. */
    data class CopyingToCatalog(val fromName: String) : QuoteOperation

    /** De onde a operação começou, pra desistir ou salvar devolver a pessoa pra lá (decisão 111). */
    val originKind: QuoteKind
        get() = when (this) {
            is Editing -> savedQuote.kind
            is Duplicating -> fromKind
            is Selling -> QuoteKind.PRODUCT
            is CopyingToCatalog -> QuoteKind.ORDER
        }
}
