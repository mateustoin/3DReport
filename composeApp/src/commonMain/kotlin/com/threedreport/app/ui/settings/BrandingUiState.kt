package com.threedreport.app.ui.settings

/** Estado da seção de personalização do PDF (marca d'água / rodapé). */
data class BrandingUiState(
    val watermarkTextInput: String = "",
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
    val errorMessage: String? = null,
    val savedConfirmation: Boolean = false,
    val isSavingAsTemplate: Boolean = false,
    val templateNameInput: String = "",
    val templateSaveError: String? = null,
    val templateSavedConfirmation: Boolean = false,
)
