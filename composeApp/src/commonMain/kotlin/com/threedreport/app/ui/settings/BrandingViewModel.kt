package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.settings_docs_error_options
import _3dreport.composeapp.generated.resources.settings_docs_error_validity
import _3dreport.composeapp.generated.resources.settings_docs_logo_unreadable
import _3dreport.composeapp.generated.resources.settings_docs_notice_template_saved
import _3dreport.composeapp.generated.resources.settings_notice_saved
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.LogoChange
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.renderPdfFirstPagePng
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteTemplate
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel da seção "Documentos pro cliente" (parte da tela de Configurações, mas separado de
 * [SettingsViewModel] porque não é um parâmetro de custo, ver [BrandingRepository]). Um [uiState] só e os [notices].
 * Também cobre "Salvar como modelo" ([confirmSaveAsTemplate]), que tira uma foto dos campos de
 * apresentação do formulário (mesmo que ainda não tenham sido persistidos com [save]) e guarda
 * como um novo [QuoteTemplate]; logo e contato ficam de fora, porque são identidade, não
 * apresentação (decisão 86).
 *
 * [currency] e [pickImage] são injetáveis pros testes não dependerem do repositório de moeda nem
 * de diálogo nativo. Com [scope], o formulário acompanha o que for gravado por outro caminho.
 */
class BrandingViewModel(
    private val repository: BrandingRepository,
    private val templateRepository: TemplateRepository,
    private val currency: () -> Currency = { Currency.BRL },
    private val pickImage: () -> PickedFile? = { (defaultPlatform.pickFile(FileKind.IMAGE) as? PickResult.Picked)?.file },
    scope: CoroutineScope? = null,
) {

    /** O que está gravado e de onde o formulário saiu; serve pra saber se há edição por salvar. */
    private var baseline: BrandingSettings = repository.branding.value

    private val state = MutableStateFlow(baseline.toUiState(repository.logoBytes()))
    val uiState: StateFlow<BrandingUiState> = state.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    init {
        scope?.let { repository.branding.onEach(::syncWith).launchIn(it) }
    }

    /** Se o formulário tem alguma mudança ainda não salva. */
    val hasUnsavedChanges: Boolean
        get() = state.value.hasUnsavedChanges

    /**
     * Acompanha o que foi gravado por outro caminho (a restauração de um backup, por exemplo). Com o formulário sem
     * edição, ele passa a mostrar o que está gravado; com edição em andamento, o rascunho fica.
     */
    fun syncWith(saved: BrandingSettings) {
        if (saved == baseline) return
        val wasClean = !hasUnsavedChanges
        baseline = saved
        publish(if (wasClean) saved.toUiState(repository.logoBytes()) else state.value)
    }

    /**
     * "Carregar" um modelo: põe os campos de apresentação dele no rascunho do formulário (logo e contato ficam, são
     * identidade, decisão 86) e não grava. Quem decide é a barra de alterações: "Salvar" adota, "Descartar" volta
     * pro que estava gravado.
     */
    fun applyTemplate(template: QuoteTemplate) {
        edit {
            it.copy(
                brandNameInput = template.brandName.orEmpty(),
                showWatermark = template.showWatermark,
                showFooter = template.showFooter,
                showPrintTime = template.showPrintTime,
                showBorder = template.showBorder,
            )
        }
    }

    /** Publica [next] com a marca de "há o que salvar" recalculada contra o que está gravado. */
    private fun publish(next: BrandingUiState) {
        val dirty = next.logoChange != LogoChange.Keep ||
            runCatching { next.toSettings() }.getOrNull() != baseline.copy(logoFileName = null)
        state.value = next.copy(hasUnsavedChanges = dirty)
    }

    /** Toda edição de campo apaga os erros anteriores, que já não valem pro formulário novo. */
    private fun edit(transform: (BrandingUiState) -> BrandingUiState) =
        publish(transform(state.value).copy(validityError = null, optionsError = null))

    fun update(text: String) = edit { it.copy(brandNameInput = text) }
    fun setShowWatermark(show: Boolean) = edit { it.copy(showWatermark = show) }
    fun setShowFooter(show: Boolean) = edit { it.copy(showFooter = show) }
    fun setShowPrintTime(show: Boolean) = edit { it.copy(showPrintTime = show) }
    fun setShowBorder(show: Boolean) = edit { it.copy(showBorder = show) }
    fun setValidityDays(text: String) = edit { it.copy(validityDaysText = text) }
    fun setShowClientName(show: Boolean) = edit { it.copy(showClientName = show) }
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
            publish(state.value.copy(logoError = uiText(Res.string.settings_docs_logo_unreadable)))
            return
        }
        edit { it.copy(logoBytes = picked.bytes, logoChange = LogoChange.Replace(picked), logoError = null, logoMissing = false) }
    }

    fun removeLogo() = edit { it.copy(logoBytes = null, logoChange = LogoChange.Remove, logoError = null, logoMissing = false) }

    /** Volta o formulário pro que está gravado, logo incluída ("Descartar" da barra de alterações, decisão 112). */
    fun discard() {
        publish(baseline.toUiState(repository.logoBytes()))
    }

    /** Valida e grava. Com [announce] falso o aviso de salvo fica com quem salva mais de uma seção de uma vez. */
    fun save(announce: Boolean = true) {
        val current = state.value
        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                repository.update(settings, current.logoChange)
                baseline = repository.branding.value
                publish(current.copy(logoChange = LogoChange.Keep, validityError = null, optionsError = null))
                if (announce) noticeChannel.trySend(UiNotice(uiText(Res.string.settings_notice_saved)))
            },
            onFailure = { publish(current.withError(it)) },
        )
    }

    /**
     * "Ver como fica": gera o PDF de verdade de um orçamento de exemplo (chamado [sampleName]) com o que está no
     * formulário agora (mesmo sem salvar) e guarda a primeira página como imagem. É o mesmo desenho que vai
     * pro cliente, então a prévia não tem como divergir do documento.
     */
    fun showPreview(sampleName: String) {
        val current = state.value
        val settings = runCatching { current.toSettings() }.getOrElse {
            publish(current.withError(it))
            return
        }
        val branding = settings.resolvePdfBranding(current.logoBytes)
        val pdf = renderSavedQuotesPdf(
            items = listOf(QuoteExportItem(sampleQuote(sampleName, currency()), photoBytes = null)),
            brandName = branding.brandName,
            footerText = branding.footerText,
            options = branding.options,
        )
        publish(current.copy(previewPng = renderPdfFirstPagePng(pdf)))
    }

    fun closePreview() {
        publish(state.value.copy(previewPng = null))
    }

    fun startSaveAsTemplate() {
        publish(state.value.copy(savingTemplate = TemplateNameState()))
    }

    fun updateTemplateName(text: String) {
        publish(state.value.copy(savingTemplate = TemplateNameState(name = text)))
    }

    fun cancelSaveAsTemplate() {
        publish(state.value.copy(savingTemplate = null))
    }

    @OptIn(ExperimentalUuidApi::class)
    fun confirmSaveAsTemplate() {
        val current = state.value
        val form = current.savingTemplate ?: return
        val name = form.name.trim()
        if (name.isBlank()) {
            publish(current.copy(savingTemplate = form.copy(error = uiText(Res.string.common_error_name_required))))
            return
        }

        current.toSettingsOrError().fold(
            onSuccess = { settings ->
                templateRepository.add(
                    QuoteTemplate(
                        id = Uuid.random().toString(),
                        name = name,
                        brandName = settings.brandName,
                        showWatermark = settings.showWatermark,
                        showFooter = settings.showFooter,
                        showPrintTime = settings.showPrintTime,
                        showBorder = settings.showBorder,
                    ),
                )
                publish(current.copy(savingTemplate = null))
                noticeChannel.trySend(UiNotice(uiText(Res.string.settings_docs_notice_template_saved)))
            },
            onFailure = { publish(current.copy(savingTemplate = form.copy(error = (it as? FormError)?.text))) },
        )
    }
}

/** Um erro de validação do formulário: o texto que a tela mostra e em qual campo. */
private class FormError(val text: UiText, val onValidity: Boolean) : IllegalStateException()

private fun BrandingUiState.withError(error: Throwable): BrandingUiState {
    val formError = error as? FormError ?: return this
    return if (formError.onValidity) copy(validityError = formError.text) else copy(optionsError = formError.text)
}

/**
 * O formulário como [BrandingSettings]. A logo não entra aqui (ver [BrandingRepository.update]). A validade
 * inválida falha com a mensagem do campo.
 */
private fun BrandingUiState.toSettings() = BrandingSettings(
    brandName = brandNameInput.trim().ifEmpty { null },
    showWatermark = showWatermark,
    showFooter = showFooter,
    showPrintTime = showPrintTime,
    contactWhatsApp = contactWhatsAppInput.trim().ifEmpty { null },
    contactEmail = contactEmailInput.trim().ifEmpty { null },
    contactInstagram = contactInstagramInput.trim().ifEmpty { null },
    showBorder = showBorder,
    quoteValidityDays = validityDays(),
    showClientName = showClientName,
)

private fun BrandingUiState.validityDays(): Int {
    if (validityDaysText.isBlank()) return 0
    val days = parseWholeNumber(validityDaysText)
    if (days == null || days !in 0..BrandingSettings.MAX_VALIDITY_DAYS) {
        throw FormError(uiText(Res.string.settings_docs_error_validity, BrandingSettings.MAX_VALIDITY_DAYS), onValidity = true)
    }
    return days
}

/**
 * Com nome preenchido, ele precisa aparecer em algum lugar: marca d'água, rodapé, ou o cabeçalho,
 * que existe quando há logo ou contato. Só sem nenhum dos quatro o nome ficaria configurado à toa.
 */
private fun BrandingUiState.toSettingsOrError(): Result<BrandingSettings> {
    val settings = runCatching { toSettings() }.getOrElse { return Result.failure(it) }
    val hasHeader = logoBytes != null || settings.contactLines.isNotEmpty()
    if (settings.brandName != null && !showWatermark && !showFooter && !hasHeader) {
        return Result.failure(FormError(uiText(Res.string.settings_docs_error_options), onValidity = false))
    }
    return Result.success(settings)
}

private fun BrandingSettings.toUiState(logoBytes: ByteArray?) = BrandingUiState(
    brandNameInput = brandName.orEmpty(),
    showWatermark = showWatermark,
    showFooter = showFooter,
    showPrintTime = showPrintTime,
    showBorder = showBorder,
    validityDaysText = quoteValidityDays.toString(),
    showClientName = showClientName,
    contactWhatsAppInput = contactWhatsApp.orEmpty(),
    contactEmailInput = contactEmail.orEmpty(),
    contactInstagramInput = contactInstagram.orEmpty(),
    logoBytes = logoBytes,
    // Configurada, mas o arquivo sumiu da pasta (apagado à mão, backup parcial): avisa em vez de
    // deixar o PDF sair sem logo em silêncio.
    logoMissing = logoFileName != null && logoBytes == null,
)

/** Orçamento fictício da prévia: com prazo, número e cliente, pra todas as opções terem onde aparecer. */
private fun sampleQuote(name: String, currency: Currency) = SavedQuote(
    id = "preview",
    name = name,
    number = 42,
    client = Client(name = "Maria"),
    currency = currency,
    quote = Quote(
        prints = listOf(
            QuotedPrint(
                job = PrintJob(
                    filament = Filament(id = "preview", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                    filamentLengthMeters = 12.0,
                    printTimeMinutes = 190.0,
                ),
                printerId = "preview",
                printerName = "Impressora",
                cost = PrintCost(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ),
        ),
        costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        salePrice = 45.0,
    ),
    savedAtEpochMillis = 0L,
    deliveryDateEpochDay = todayEpochDay() + 7,
)
