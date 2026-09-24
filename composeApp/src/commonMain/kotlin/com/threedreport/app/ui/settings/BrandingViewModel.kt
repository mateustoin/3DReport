package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.LogoChange
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.pickImageFile
import com.threedreport.app.platform.renderPdfFirstPagePng
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteTemplate
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da seção "Documentos pro cliente" (parte da tela de Configurações, mas separado de
 * [SettingsViewModel] porque não é um parâmetro de custo — ver [BrandingRepository]). Também
 * cobre "Salvar como template" ([confirmSaveAsTemplate]), que tira uma foto dos campos de
 * apresentação do formulário (mesmo que ainda não tenham sido persistidos com [save]) e guarda
 * como um novo [QuoteTemplate]; logo e contato ficam de fora, porque são identidade, não
 * apresentação (decisão 86).
 *
 * [currency] e [pickImage] são injetáveis pros testes não dependerem do repositório de moeda nem
 * de diálogo nativo.
 */
class BrandingViewModel(
    private val repository: BrandingRepository,
    private val templateRepository: TemplateRepository,
    private val currency: () -> Currency = { Currency.BRL },
    private val pickImage: () -> PickedFile? = ::pickImageFile,
) {

    private val state = MutableStateFlow(repository.branding.value.toUiState(repository.logoBytes()))
    val uiState: StateFlow<BrandingUiState> = state.asStateFlow()

    /** Toda edição de campo apaga erro e confirmação anteriores, que já não valem pro formulário novo. */
    private fun edit(transform: (BrandingUiState) -> BrandingUiState) =
        state.update { transform(it).copy(errorMessage = null, savedConfirmation = false) }

    fun update(text: String) = edit { it.copy(watermarkTextInput = text) }
    fun setShowWatermark(show: Boolean) = edit { it.copy(showWatermark = show) }
    fun setShowFooter(show: Boolean) = edit { it.copy(showFooter = show) }
    fun setShowPrintTime(show: Boolean) = edit { it.copy(showPrintTime = show) }
    fun setShowBorder(show: Boolean) = edit { it.copy(showBorder = show) }
    fun setContactWhatsApp(text: String) = edit { it.copy(contactWhatsAppInput = text) }
    fun setContactEmail(text: String) = edit { it.copy(contactEmailInput = text) }
    fun setContactInstagram(text: String) = edit { it.copy(contactInstagramInput = text) }

    /**
     * Abre o seletor de imagem e, se a imagem abrir, põe no formulário (grava só em [save]). A
     * checagem é na hora de escolher, e não na de exportar: descobrir que a logo não abre só ao
     * mandar o PDF pro cliente seria tarde demais.
     */
    fun pickLogo() {
        val picked = pickImage() ?: return
        val readable = runCatching { decodeImageBitmap(picked.bytes) }.isSuccess
        if (!readable) {
            state.update { it.copy(logoError = "Não consegui ler essa imagem. Tente um PNG ou JPG.") }
            return
        }
        edit { it.copy(logoBytes = picked.bytes, logoChange = LogoChange.Replace(picked), logoError = null, logoMissing = false) }
    }

    fun removeLogo() = edit { it.copy(logoBytes = null, logoChange = LogoChange.Remove, logoError = null, logoMissing = false) }

    /** Ver `SettingsViewModel.consumeSavedConfirmation`. */
    fun consumeSavedConfirmation() {
        state.value = state.value.copy(savedConfirmation = false)
    }

    fun consumeTemplateSavedConfirmation() {
        state.value = state.value.copy(templateSavedConfirmation = false)
    }

    fun save() {
        val current = state.value
        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                repository.update(settings, current.logoChange)
                state.value = current.copy(logoChange = LogoChange.Keep, errorMessage = null, savedConfirmation = true)
            },
            onFailure = { state.value = current.copy(errorMessage = it.message, savedConfirmation = false) },
        )
    }

    /**
     * "Ver como fica": gera o PDF de verdade de um orçamento de exemplo com o que está no formulário
     * agora (mesmo sem salvar) e guarda a primeira página como imagem. É o mesmo desenho que vai
     * pro cliente, então a prévia não tem como divergir do documento.
     */
    fun showPreview() {
        val current = state.value
        val branding = current.toSettings().resolvePdfBranding(current.logoBytes)
        val pdf = renderSavedQuotesPdf(
            items = listOf(QuoteExportItem(sampleQuote(), photoBytes = null)),
            watermarkText = branding.watermarkText,
            footerText = branding.footerText,
            currency = currency(),
            options = branding.options,
        )
        state.value = current.copy(previewPng = renderPdfFirstPagePng(pdf))
    }

    fun closePreview() {
        state.value = state.value.copy(previewPng = null)
    }

    fun startSaveAsTemplate() {
        state.value = state.value.copy(
            isSavingAsTemplate = true,
            templateNameInput = "",
            templateSaveError = null,
            templateSavedConfirmation = false,
        )
    }

    fun updateTemplateName(text: String) {
        state.value = state.value.copy(templateNameInput = text, templateSaveError = null)
    }

    fun cancelSaveAsTemplate() {
        state.value = state.value.copy(isSavingAsTemplate = false, templateNameInput = "", templateSaveError = null)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun confirmSaveAsTemplate() {
        val current = state.value
        val name = current.templateNameInput.trim()
        if (name.isBlank()) {
            state.value = current.copy(templateSaveError = "Nome não pode ser vazio")
            return
        }

        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                templateRepository.add(
                    QuoteTemplate(
                        id = Uuid.random().toString(),
                        name = name,
                        watermarkText = settings.watermarkText,
                        showWatermark = settings.showWatermark,
                        showFooter = settings.showFooter,
                        showPrintTime = settings.showPrintTime,
                        showBorder = settings.showBorder,
                    )
                )
                state.value = current.copy(
                    isSavingAsTemplate = false,
                    templateNameInput = "",
                    templateSaveError = null,
                    templateSavedConfirmation = true,
                )
            },
            onFailure = { state.value = current.copy(templateSaveError = it.message) },
        )
    }
}

/** O formulário como [BrandingSettings], sem validar. A logo não entra aqui (ver [BrandingRepository.update]). */
private fun BrandingUiState.toSettings() = BrandingSettings(
    watermarkText = watermarkTextInput.trim().ifEmpty { null },
    showWatermark = showWatermark,
    showFooter = showFooter,
    showPrintTime = showPrintTime,
    contactWhatsApp = contactWhatsAppInput.trim().ifEmpty { null },
    contactEmail = contactEmailInput.trim().ifEmpty { null },
    contactInstagram = contactInstagramInput.trim().ifEmpty { null },
    showBorder = showBorder,
)

/**
 * Com nome preenchido, ele precisa aparecer em algum lugar: marca d'água, rodapé, ou o cabeçalho,
 * que existe quando há logo ou contato. Só sem nenhum dos quatro o nome ficaria configurado à toa.
 */
private fun BrandingUiState.toSettingsOrError(): Result<BrandingSettings> {
    val settings = toSettings()
    val hasHeader = logoBytes != null || settings.contactLines.isNotEmpty()
    if (settings.watermarkText != null && !showWatermark && !showFooter && !hasHeader) {
        return Result.failure(IllegalStateException("Selecione ao menos uma opção: marca d'água ou rodapé."))
    }
    return Result.success(settings)
}

private fun BrandingSettings.toUiState(logoBytes: ByteArray?) = BrandingUiState(
    watermarkTextInput = watermarkText.orEmpty(),
    showWatermark = showWatermark,
    showFooter = showFooter,
    showPrintTime = showPrintTime,
    showBorder = showBorder,
    contactWhatsAppInput = contactWhatsApp.orEmpty(),
    contactEmailInput = contactEmail.orEmpty(),
    contactInstagramInput = contactInstagram.orEmpty(),
    logoBytes = logoBytes,
    // Configurada, mas o arquivo sumiu da pasta (apagado à mão, backup parcial): avisa em vez de
    // deixar o PDF sair sem logo em silêncio.
    logoMissing = logoFileName != null && logoBytes == null,
)

/** Orçamento fictício da prévia: com prazo, pra todas as opções terem onde aparecer. */
private fun sampleQuote() = SavedQuote(
    id = "preview",
    name = "Exemplo: suporte de celular",
    quote = Quote(
        job = PrintJob(
            filament = Filament(id = "preview", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = 190.0,
        ),
        filamentWeightGrams = 35.8,
        costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        productionCost = 0.0,
        salePrice = 45.0,
    ),
    savedAtEpochMillis = 0L,
    deliveryDateEpochDay = todayEpochDay() + 7,
)
