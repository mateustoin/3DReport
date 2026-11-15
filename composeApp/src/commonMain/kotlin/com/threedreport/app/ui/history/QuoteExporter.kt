package com.threedreport.app.ui.history

import com.threedreport.app.AppLog
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.ResolvedPdfBranding
import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.formatShortDate
import com.threedreport.app.platform.imageBrandLine
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.resolvePdfBranding
import com.threedreport.app.ui.components.NoticeIds
import com.threedreport.app.ui.components.UserNotice
import com.threedreport.app.ui.components.toNotice
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val noticeIds: NoticeIds,
    private val notify: (UserNotice) -> Unit,
) {
    private val busyState = MutableStateFlow<String?>(null)

    /** O que está sendo gerado agora ("Gerando PDF…"), ou `null`. */
    val busy: StateFlow<String?> = busyState.asStateFlow()

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = historyRepository.photoBytes(savedQuote)

    fun exportPdf(savedQuote: SavedQuote) {
        saveGenerated("${fileNameFor(savedQuote)}.pdf", "Gerando PDF…", "PDF salvo.") {
            val branding = resolvePdfBranding()
            renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photoBytes(savedQuote))), branding.brandName, branding.footerText, branding.options)
        }
    }

    /** Abre a conversa do cliente no WhatsApp com o orçamento já escrito (ver [toWhatsAppLink]). */
    fun openInWhatsApp(savedQuote: SavedQuote) {
        val showPrintTime = brandingRepository.branding.value.showPrintTime
        if (platform.openUrl(savedQuote.toWhatsAppLink(savedQuote.currency, showPrintTime))) return
        // Sem navegador configurado, o texto ainda chega no cliente: vai pra área de transferência.
        val copied = platform.copyToClipboard(savedQuote.toCopyPasteText(savedQuote.currency, showPrintTime))
        report(
            UserNotice(
                if (copied) {
                    "Não consegui abrir o WhatsApp. O texto do orçamento foi copiado: cole na conversa com o cliente."
                } else {
                    "Não consegui abrir o WhatsApp nem copiar o texto. Use \"Copiar\" de novo em alguns segundos."
                },
                isError = true,
                id = noticeIds.next(),
            ),
        )
    }

    /**
     * Gera a imagem quadrada pra mandar no WhatsApp/status e abre o "salvar como". Usa o mesmo
     * texto de marca da marca d'água do PDF, pra o material do vendedor sair coerente entre os dois.
     */
    fun saveShareableImage(savedQuote: SavedQuote) {
        saveGenerated("${fileNameFor(savedQuote)}.png", "Gerando imagem…", "Imagem salva.") {
            val currency = savedQuote.currency
            val quantity = savedQuote.quote.quantity
            val piecesTotal = savedQuote.totalWithServices - savedQuote.chargedShipping
            // Curta ("30/09", sem o ano) porque a imagem é pra conversa do dia, não documento. Entrega
            // expressa (decisão 125) junto do prazo; sem prazo, vira o próprio texto de entrega.
            val deliveryDate = savedQuote.deliveryDateEpochDay?.let { "Entrega até ${formatShortDate(it)}" }
            val deliveryText = when {
                deliveryDate != null && savedQuote.quote.rush -> "$deliveryDate · entrega expressa"
                deliveryDate != null -> deliveryDate
                savedQuote.quote.rush -> "Entrega expressa"
                else -> null
            }
            renderQuoteImage(
                title = savedQuote.name.takeUnless { savedQuote.hasAutoName } ?: "Orçamento",
                priceText = savedQuote.totalWithServices.toCurrencyText(currency),
                unitPriceText = if (quantity > 1) "$quantity peças · ${(piecesTotal / quantity).toCurrencyText(currency)} cada" else null,
                photoBytes = photoBytes(savedQuote),
                brandText = brandingRepository.branding.value.imageBrandLine(),
                deliveryText = deliveryText,
            )
        }
    }

    /**
     * Pergunta onde salvar, gera o arquivo fora do thread da tela e grava. [onSaved] só roda quando o arquivo foi
     * salvo: cancelar o "Salvar como" não pode jogar fora o que a pessoa marcou.
     */
    fun saveGenerated(
        suggestedFileName: String,
        busyMessage: String,
        savedMessage: String,
        onSaved: () -> Unit = {},
        generate: () -> ByteArray,
    ) {
        if (busyState.value != null) return
        val path = platform.chooseSaveLocation(suggestedFileName) ?: return
        busyState.value = busyMessage
        scope.launch(background) {
            val result = try {
                platform.writeFile(path, generate())
            } catch (e: Exception) {
                AppLog.warn("Falha ao gerar $suggestedFileName", e)
                SaveResult.Failed("Não consegui gerar o arquivo. Os detalhes ficaram no log do app.")
            }
            withContext(main) {
                busyState.value = null
                if (result is SaveResult.Saved) onSaved()
                report(result, savedMessage)
            }
        }
    }

    /** O aviso de um arquivo gravado (ou do motivo de não ter gravado), pelo mesmo caminho dos outros. */
    fun report(result: SaveResult, savedMessage: String) {
        result.toNotice(savedMessage, noticeIds.next())?.let(notify)
    }

    private fun report(notice: UserNotice) = notify(notice)

    /** Marca, logo, contato e opções do PDF, resolvidos do jeito que a prévia de Configurações também usa. */
    fun resolvePdfBranding(): ResolvedPdfBranding =
        brandingRepository.branding.value.resolvePdfBranding(brandingRepository.logoBytes())

    /** Nome do arquivo pro cliente: o nome da peça, e o número quando o nome foi o automático. */
    fun fileNameFor(savedQuote: SavedQuote): String = sanitizeFileName(
        if (savedQuote.hasAutoName) "Orçamento ${savedQuote.displayNumber?.removePrefix("#") ?: ""}".trim() else savedQuote.name,
    )

    fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("").trim().ifEmpty { "orcamento" }
}
