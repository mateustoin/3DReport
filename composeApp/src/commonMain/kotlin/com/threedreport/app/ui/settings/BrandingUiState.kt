package com.threedreport.app.ui.settings

/** Estado da seção de personalização do PDF (marca d'água). */
data class BrandingUiState(
    val watermarkTextInput: String = "",
    val savedConfirmation: Boolean = false,
)
