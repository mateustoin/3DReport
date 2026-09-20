package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile

/** Campos opcionais preenchidos ao salvar o orçamento atual no histórico. */
data class SaveQuoteFormState(
    val name: String = "",
    val photo: PickedFile? = null,
    /** `true` quando [photo] veio da miniatura embutida num G-code importado, não de escolha manual. */
    val photoFromGCode: Boolean = false,
    val sourceLink: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val savedConfirmation: Boolean = false,
)
