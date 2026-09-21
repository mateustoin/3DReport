package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile

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
    val savedConfirmation: Boolean = false,
    /** `id` do orçamento salvo sendo editado, ou `null` se este for um orçamento novo. */
    val editingQuoteId: String? = null,
    /** Nome do orçamento de origem, só quando este formulário veio de "Duplicar" — exibido como aviso. */
    val duplicatedFromName: String? = null,
)
