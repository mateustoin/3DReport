package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile

/** Campos opcionais preenchidos ao salvar o orçamento atual no histórico. */
data class SaveQuoteFormState(
    val name: String = "",
    val photo: PickedFile? = null,
    /** `true` quando [photo] veio da miniatura embutida num G-code importado, não de escolha manual. */
    val photoFromGCode: Boolean = false,
    /** Arquivo STL do modelo (opcional), guardado pra recuperar/reaproveitar numa venda futura. */
    val stlFile: PickedFile? = null,
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
    val blockedMessage: String? = null,
    /** O que a tela está fazendo além de um orçamento novo (editar, duplicar, vender, copiar pro catálogo). */
    val operation: QuoteOperation? = null,
    /** Produto de onde este pedido nasceu (pelo "Vender" ou duplicando uma venda dele), gravado ao salvar. */
    val sourceProductId: String? = null,
    /** Categoria do produto no catálogo (decisão 102), texto livre; ignorada em pedido. */
    val category: String = "",
    /** Mensagem sobre a foto escolhida (imagem que não abre). */
    val photoError: String? = null,
) {
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
