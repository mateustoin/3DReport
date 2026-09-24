package com.threedreport.app.ui.settings

import com.threedreport.app.data.LogoChange

/**
 * Estado da seção "Documentos pro cliente" em Configurações: marca, logo, contato e opções de
 * apresentação do PDF. Tudo aqui é rascunho até [BrandingViewModel.save], inclusive a logo.
 *
 * @property logoBytes a logo como está no formulário agora (a salva, a recém-escolhida, ou `null`
 *   depois de "Remover logo"), pra miniatura e pra prévia mostrarem o que vai ser salvo.
 * @property logoChange o que [BrandingViewModel.save] deve fazer com o arquivo da logo.
 * @property logoMissing a configuração aponta pra uma logo cujo arquivo sumiu da pasta de dados.
 * @property previewPng primeira página do PDF de exemplo, quando "Ver como fica" está aberto.
 */
data class BrandingUiState(
    val watermarkTextInput: String = "",
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
    val showBorder: Boolean = false,
    val contactWhatsAppInput: String = "",
    val contactEmailInput: String = "",
    val contactInstagramInput: String = "",
    val logoBytes: ByteArray? = null,
    val logoChange: LogoChange = LogoChange.Keep,
    val logoError: String? = null,
    val logoMissing: Boolean = false,
    val previewPng: ByteArray? = null,
    val errorMessage: String? = null,
    val savedConfirmation: Boolean = false,
    val isSavingAsTemplate: Boolean = false,
    val templateNameInput: String = "",
    val templateSaveError: String? = null,
    val templateSavedConfirmation: Boolean = false,
)
