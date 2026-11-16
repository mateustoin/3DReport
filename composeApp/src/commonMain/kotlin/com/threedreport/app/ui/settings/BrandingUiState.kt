package com.threedreport.app.ui.settings

import com.threedreport.app.data.LogoChange
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.BrandingSettings

/**
 * Estado da seção "Documentos pro cliente" em Configurações: marca, logo, contato e opções de
 * apresentação do PDF. Tudo aqui é rascunho até [BrandingViewModel.save], inclusive a logo.
 *
 * @property logoBytes a logo como está no formulário agora (a salva, a recém-escolhida, ou `null`
 *   depois de "Remover logo"), pra miniatura e pra prévia mostrarem o que vai ser salvo.
 * @property logoChange o que [BrandingViewModel.save] deve fazer com o arquivo da logo.
 * @property logoMissing a configuração aponta pra uma logo cujo arquivo sumiu da pasta de dados.
 * @property previewPng primeira página do PDF de exemplo, quando "Ver como fica" está aberto.
 * @property validityError o que corrigir na validade; [optionsError] o que corrigir nas opções do PDF (o nome
 *   precisa aparecer em algum lugar); [logoError] a imagem escolhida que não abre.
 * @property savingTemplate o diálogo "Salvar como template" aberto, com o nome digitado e o erro dele.
 * @property hasUnsavedChanges se o formulário difere do que está gravado (a logo incluída).
 */
data class BrandingUiState(
    val brandNameInput: String = "",
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
    val showBorder: Boolean = false,
    val validityDaysText: String = BrandingSettings.DEFAULT_VALIDITY_DAYS.toString(),
    val showClientName: Boolean = false,
    val contactWhatsAppInput: String = "",
    val contactEmailInput: String = "",
    val contactInstagramInput: String = "",
    val logoBytes: ByteArray? = null,
    val logoChange: LogoChange = LogoChange.Keep,
    val logoError: UiText? = null,
    val logoMissing: Boolean = false,
    val previewPng: ByteArray? = null,
    val validityError: UiText? = null,
    val optionsError: UiText? = null,
    val savingTemplate: TemplateNameState? = null,
    val hasUnsavedChanges: Boolean = false,
)

/** O nome do template sendo digitado e o erro dele. */
data class TemplateNameState(val name: String = "", val error: UiText? = null)
