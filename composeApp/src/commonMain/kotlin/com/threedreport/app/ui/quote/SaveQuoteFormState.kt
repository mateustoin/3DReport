package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.SavedQuote

/** Campos opcionais preenchidos ao salvar o orçamento atual no histórico. */
data class SaveQuoteFormState(
    val name: String = "",
    /**
     * O nome automático do pedido de onde este formulário veio (editar, duplicar, vender, guardar no catálogo), ou
     * `null`. Enquanto [name] for ele, salvar mantém o nome como automático ([SavedQuote.autoNamed]).
     */
    val autoName: String? = null,
    val photo: PickedFile? = null,
    /** `true` quando [photo] veio de uma miniatura automática (a de um G-code importado ou a de um 3MF), não de escolha manual. */
    val photoAutomatic: Boolean = false,
    /** Arquivo do modelo, STL ou 3MF (opcional), guardado pra recuperar/reaproveitar numa venda futura. */
    val modelFile: PickedFile? = null,
    val sourceLink: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    /** Cliente do cadastro escolhido na sugestão (decisão 106), ou `null` quando foi digitado. */
    val clientId: String? = null,
    /** Prazo de entrega prometido ao cliente (dias desde 01/01/1970), ou `null` — ver `SavedQuote.deliveryDateEpochDay`. */
    val deliveryDateEpochDay: Long? = null,
    val savedConfirmation: Boolean = false,
    /** Se o que acabou de ser salvo foi um produto, pra confirmação dizer onde ele foi parar. */
    val savedAsProduct: Boolean = false,
    /** Número do que acabou de ser salvo ("#0042"), pra confirmação. */
    val savedNumber: String? = null,
    /** Por que o salvar (Ctrl+S) não aconteceu, pra tela avisar em vez de não fazer nada. */
    val blockedMessage: UiText? = null,
    /** O que a tela está fazendo além de um orçamento novo (editar, duplicar, vender, copiar pro catálogo). */
    val operation: QuoteOperation? = null,
    /** Produto de onde este pedido nasceu (pelo "Vender" ou duplicando uma venda dele), gravado ao salvar. */
    val sourceProductId: String? = null,
    /** Categoria do produto no catálogo (decisão 102), texto livre; ignorada em pedido. */
    val category: String = "",
    /** Mensagem sobre a foto escolhida (imagem que não abre). */
    val photoError: UiText? = null,
    /** O pedido que "Enviar pro cliente" gravou a partir do que está na tela (decisão 131), ou `null`. */
    val sent: SentQuote? = null,
) {
    /** Se [name] é o nome automático trazido de outro pedido, sem ninguém mexer (ver [autoName]). */
    val keepsAutoName: Boolean
        get() = autoName != null && name.trim() == autoName

    /** `id` do orçamento salvo sendo editado, ou `null` se este for um orçamento novo. */
    val editingQuoteId: String?
        get() = (operation as? QuoteOperation.Editing)?.savedQuote?.id

    /** Nome do orçamento de origem, só quando este formulário veio de "Duplicar". */
    val duplicatedFromName: String?
        get() = (operation as? QuoteOperation.Duplicating)?.fromName

    /** Nome do produto sendo vendido, só no "Vender". */
    val soldFromProductName: String?
        get() = (operation as? QuoteOperation.Selling)?.product?.name

    /** Nome do pedido de origem, só quando este formulário veio de "Guardar no catálogo". */
    val copiedFromOrderName: String?
        get() = (operation as? QuoteOperation.CopyingToCatalog)?.fromName
}

/**
 * O que "Enviar pro cliente" gravou: o pedido e como a tela estava ([input] e [form], sem os avisos), pra saber se
 * mudou algo desde então. Mora no formulário, e por isso some junto com ele quando a tela recomeça.
 */
data class SentQuote(val savedQuote: SavedQuote, val input: QuoteInputState, val form: SaveQuoteFormState)
