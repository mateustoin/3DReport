package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile

/** Campos opcionais preenchidos ao salvar o orçamento atual no histórico. */
data class SaveQuoteFormState(
    val name: String = "",
    val photo: PickedFile? = null,
    val sourceLink: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val savedConfirmation: Boolean = false,
)
