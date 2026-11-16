package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_busy_catalog
import _3dreport.composeapp.generated.resources.export_busy_image
import _3dreport.composeapp.generated.resources.export_busy_pdf
import _3dreport.composeapp.generated.resources.export_copied
import _3dreport.composeapp.generated.resources.export_copy_failed
import _3dreport.composeapp.generated.resources.export_failed_generate
import _3dreport.composeapp.generated.resources.export_file_auto
import _3dreport.composeapp.generated.resources.export_file_auto_numbered
import _3dreport.composeapp.generated.resources.export_file_catalog
import _3dreport.composeapp.generated.resources.export_file_fallback
import _3dreport.composeapp.generated.resources.export_file_quotes
import _3dreport.composeapp.generated.resources.export_saved_catalog
import _3dreport.composeapp.generated.resources.export_saved_image
import _3dreport.composeapp.generated.resources.export_saved_pdf
import _3dreport.composeapp.generated.resources.export_whatsapp_copied
import _3dreport.composeapp.generated.resources.export_whatsapp_failed
import com.threedreport.app.AppLog
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.ClientDocumentTexts
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.ResolvedPdfBranding
import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.loadClientDocumentTexts
import com.threedreport.app.platform.renderCatalogPdf
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.components.toNotice
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Entrega um orçamento salvo pro cliente: PDF, imagem pro WhatsApp e o texto no WhatsApp. É o que Pedidos e o
 * painel do Orçamento ("Enviar pro cliente") compartilham, pra os dois gerarem o mesmo arquivo e dizerem a mesma coisa.
 *
 * Gerar o arquivo roda em [background] com [busy] ligado; os testes passam o padrão, `Unconfined`, e tudo
 * acontece na hora. O retorno de cada ação chega em [notify].
 */
class QuoteExporter(
    private val platform: PlatformServices,
    private val brandingRepository: BrandingRepository,
    private val historyRepository: QuoteHistoryRepository,
    private val scope: CoroutineScope,
    private val background: CoroutineDispatcher,
    private val main: CoroutineDispatcher,
    private val notify: (UiNotice) -> Unit,
) {
    private val busyState = MutableStateFlow<UiText?>(null)

    /** O que está sendo gerado agora ("Gerando PDF…"), ou `null`. */
    val busy: StateFlow<UiText?> = busyState.asStateFlow()

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = historyRepository.photoBytes(savedQuote)

    fun exportPdf(savedQuote: SavedQuote) {
        saveGenerated({ "${fileNameFor(savedQuote)}.pdf" }, uiText(Res.string.export_busy_pdf), uiText(Res.string.export_saved_pdf)) {
            val branding = resolvePdfBranding()
            val items = listOf(QuoteExportItem(savedQuote, photoBytes(savedQuote)))
            renderSavedQuotesPdf(items, loadClientDocumentTexts(listOf(savedQuote)), branding.brandName, branding.footerText, branding.options)
        }
    }

    /**
     * Lê os textos da mensagem pro cliente (recursos traduzíveis) e entrega em [main]. Com `Unconfined`, nos testes,
     * a leitura termina na hora e [block] roda antes de a função voltar.
     */
    private fun withMessageTexts(savedQuote: SavedQuote, block: (ClientDocumentTexts) -> Unit) {
        scope.launch(main) { block(loadClientDocumentTexts(listOf(savedQuote))) }
    }

    /** O texto do orçamento pra colar numa conversa (ver [toCopyPasteText]), com os textos já lidos dos recursos. */
    fun withCopyPasteText(savedQuote: SavedQuote, block: (String) -> Unit) {
        val showPrintTime = brandingRepository.branding.value.showPrintTime
        withMessageTexts(savedQuote) { texts -> block(savedQuote.toCopyPasteText(texts, savedQuote.currency, showPrintTime)) }
    }

    /** Abre a conversa do cliente no WhatsApp com o orçamento já escrito (ver [toWhatsAppLink]). */
    fun openInWhatsApp(savedQuote: SavedQuote) {
        val showPrintTime = brandingRepository.branding.value.showPrintTime
        withMessageTexts(savedQuote) { texts ->
            if (platform.openUrl(savedQuote.toWhatsAppLink(texts, savedQuote.currency, showPrintTime))) return@withMessageTexts
            // Sem navegador configurado, o texto ainda chega no cliente: vai pra área de transferência.
            val copied = platform.copyToClipboard(savedQuote.toCopyPasteText(texts, savedQuote.currency, showPrintTime))
            notify(
                UiNotice(
                    uiText(if (copied) Res.string.export_whatsapp_copied else Res.string.export_whatsapp_failed),
                    isError = true,
                ),
            )
        }
    }

    /** Copia o texto do orçamento pra área de transferência e avisa se deu certo ou não. */
    fun copyText(savedQuote: SavedQuote) {
        withCopyPasteText(savedQuote) { text ->
            val copied = platform.copyToClipboard(text)
            notify(UiNotice(uiText(if (copied) Res.string.export_copied else Res.string.export_copy_failed), isError = !copied))
        }
    }

    /**
     * Gera a imagem quadrada pra mandar no WhatsApp/status e abre o "salvar como". Usa o mesmo
     * texto de marca da marca d'água do PDF, pra o material do vendedor sair coerente entre os dois.
     */
    fun saveShareableImage(savedQuote: SavedQuote) {
        saveGenerated({ "${fileNameFor(savedQuote)}.png" }, uiText(Res.string.export_busy_image), uiText(Res.string.export_saved_image)) {
            val currency = savedQuote.currency
            val quantity = savedQuote.quote.quantity
            val piecesTotal = savedQuote.totalWithServices - savedQuote.chargedShipping
            // Curta ("30/09", sem o ano) porque a imagem é pra conversa do dia, não documento. Entrega
            // expressa (decisão 125) junto do prazo; sem prazo, vira o próprio texto de entrega.
            val texts = loadClientDocumentTexts(listOf(savedQuote))
            val deliveryDate = savedQuote.deliveryDateEpochDay?.let { texts.imageDelivery(formatShortDate(it)) }
            val deliveryText = when {
                deliveryDate != null && savedQuote.quote.rush -> texts.imageDeliveryRush(deliveryDate)
                deliveryDate != null -> deliveryDate
                savedQuote.quote.rush -> texts.rush
                else -> null
            }
            renderQuoteImage(
                texts = texts,
                title = savedQuote.name.takeUnless { savedQuote.autoNamed } ?: texts.autoOrderTitle(number = null),
                priceText = savedQuote.totalWithServices.toCurrencyText(currency),
                unitPriceText = if (quantity > 1) texts.imageUnitPrice(quantity, (piecesTotal / quantity).toCurrencyText(currency)) else null,
                photoBytes = photoBytes(savedQuote),
                brandText = brandingRepository.branding.value.imageBrandLine(),
                deliveryText = deliveryText,
            )
        }
    }

    /**
     * Pergunta onde salvar, gera o arquivo fora do thread da tela e grava. [onSaved] só roda quando o arquivo foi
     * salvo: cancelar o "Salvar como" não pode jogar fora o que a pessoa marcou. O nome sugerido vem dos recursos
     * traduzíveis, lidos em [main] antes de abrir o "Salvar como".
     */
    fun saveGenerated(
        suggestedFileName: suspend () -> String,
        busyMessage: UiText,
        savedMessage: UiText,
        onSaved: () -> Unit = {},
        generate: suspend () -> ByteArray,
    ) {
        if (busyState.value != null) return
        scope.launch(main) {
            val fileName = suggestedFileName()
            val path = platform.chooseSaveLocation(fileName) ?: return@launch
            if (busyState.value != null) return@launch
            busyState.value = busyMessage
            // `null`: não deu pra gerar o arquivo (o motivo vai pro log).
            val result = withContext(background) {
                try {
                    platform.writeFile(path, generate())
                } catch (e: Exception) {
                    AppLog.warn("Falha ao gerar $fileName", e)
                    null
                }
            }
            busyState.value = null
            if (result is SaveResult.Saved) onSaved()
            if (result == null) {
                notify(UiNotice(uiText(Res.string.export_failed_generate), isError = true))
            } else {
                report(result, savedMessage)
            }
        }
    }

    /** Vários orçamentos num PDF só, um por página. [onSaved] só roda se o arquivo foi gravado. */
    fun exportQuotesPdf(quotes: List<SavedQuote>, onSaved: () -> Unit = {}) {
        saveGenerated({ "${sanitizeFileName(getPluralString(Res.plurals.export_file_quotes, quotes.size, quotes.size))}.pdf" }, uiText(Res.string.export_busy_pdf), uiText(Res.string.export_saved_pdf), onSaved) {
            val branding = resolvePdfBranding()
            renderSavedQuotesPdf(quotes.map { QuoteExportItem(it, photoBytes(it)) }, loadClientDocumentTexts(quotes), branding.brandName, branding.footerText, branding.options)
        }
    }

    /** O catálogo pra divulgação: vários produtos por página, com foto, não um orçamento formal por página. */
    fun exportCatalogPdf(products: List<SavedQuote>, onSaved: () -> Unit = {}) {
        saveGenerated({ "${sanitizeFileName(getPluralString(Res.plurals.export_file_catalog, products.size, products.size))}.pdf" }, uiText(Res.string.export_busy_catalog), uiText(Res.string.export_saved_catalog), onSaved) {
            val branding = resolvePdfBranding()
            renderCatalogPdf(products.map { QuoteExportItem(it, photoBytes(it)) }, loadClientDocumentTexts(products), branding.brandName, branding.footerText, branding.options)
        }
    }

    /** O aviso de um arquivo gravado (ou do motivo de não ter gravado), pelo mesmo caminho dos outros. */
    fun report(result: SaveResult, savedMessage: UiText) {
        result.toNotice(savedMessage)?.let(notify)
    }

    /** Marca, logo, contato e opções do PDF, resolvidos do jeito que a prévia de Configurações também usa. */
    fun resolvePdfBranding(): ResolvedPdfBranding =
        brandingRepository.branding.value.resolvePdfBranding(brandingRepository.logoBytes())

    /** Nome do arquivo pro cliente, sem a extensão: o nome da peça, e o número quando o nome foi o automático. */
    suspend fun fileNameFor(savedQuote: SavedQuote): String {
        val number = savedQuote.displayNumber?.removePrefix("#")
        return sanitizeFileName(
            when {
                !savedQuote.autoNamed -> savedQuote.name
                number != null -> getString(Res.string.export_file_auto_numbered, number)
                else -> getString(Res.string.export_file_auto)
            },
        )
    }

    /**
     * Grava [bytes] (a foto ou o STL de [savedQuote]) com o nome dele e a [extension], perguntando onde, e avisa com
     * [savedMessage]. O nome sugerido vem dos recursos, lidos em [main].
     */
    fun saveAttachment(savedQuote: SavedQuote, bytes: ByteArray, extension: String, savedMessage: UiText) {
        scope.launch(main) { report(platform.saveFile(bytes, "${fileNameFor(savedQuote)}.$extension"), savedMessage) }
    }

    /** [name] sem o que não pode ir num nome de arquivo; sem nada que sirva, um nome genérico. */
    suspend fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("").trim()
            .ifEmpty { getString(Res.string.export_file_fallback) }
}
