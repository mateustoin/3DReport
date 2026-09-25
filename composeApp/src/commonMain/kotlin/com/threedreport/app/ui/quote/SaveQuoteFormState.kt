package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.PrintSettings

/** Campos opcionais preenchidos ao salvar o orçamento atual no histórico. */
data class SaveQuoteFormState(
    val name: String = "",
    val photo: PickedFile? = null,
    /** `true` quando [photo] veio da miniatura embutida num G-code importado, não de escolha manual. */
    val photoFromGCode: Boolean = false,
    /**
     * Nome do arquivo de foto já existente em disco que [photo] ainda representa sem nenhuma
     * mudança (carregado por [QuoteViewModel.loadForEditing]/`duplicateForNewQuote`) — permite
     * salvar sem regravar/duplicar o arquivo. `null` sempre que o usuário escolhe/remove/substitui
     * a foto manualmente (a partir daí, `photo` não corresponde mais a nenhum arquivo existente).
     */
    val photoReferenceFileName: String? = null,
    /** Arquivo STL do modelo (opcional), guardado pra recuperar/reaproveitar numa venda futura. */
    val stlFile: PickedFile? = null,
    /** Mesmo espírito de [photoReferenceFileName], mas pro arquivo STL. */
    val stlReferenceFileName: String? = null,
    val sourceLink: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    /** Configurações de fatiamento (altura de camada, preenchimento, suporte), opcionais — ver KDoc de [PrintSettings]. */
    val printSettings: PrintSettings = PrintSettings(),
    /** Prazo de entrega prometido ao cliente (dias desde 01/01/1970), ou `null` — ver `SavedQuote.deliveryDateEpochDay`. */
    val deliveryDateEpochDay: Long? = null,
    val savedConfirmation: Boolean = false,
    /** Se o que acabou de ser salvo foi um produto, pra confirmação dizer onde ele foi parar. */
    val savedAsProduct: Boolean = false,
    /** `id` do orçamento salvo sendo editado, ou `null` se este for um orçamento novo. */
    val editingQuoteId: String? = null,
    /** Nome do orçamento de origem, só quando este formulário veio de "Duplicar" — exibido como aviso. */
    val duplicatedFromName: String? = null,
    /** Produto de onde este pedido está nascendo pelo "Vender" (decisão 101), gravado no pedido ao salvar. */
    val sourceProductId: String? = null,
    /** Nome do produto de [sourceProductId], exibido como aviso. */
    val soldFromProductName: String? = null,
    /** Nome do pedido de origem, só quando este formulário veio de "Guardar no catálogo" — exibido como aviso. */
    val copiedFromOrderName: String? = null,
)
