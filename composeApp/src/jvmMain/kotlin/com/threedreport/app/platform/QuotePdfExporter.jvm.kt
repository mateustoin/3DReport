package com.threedreport.app.platform

import androidx.compose.ui.graphics.toAwtImage
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.history.clientTitle
import com.threedreport.app.ui.history.depositText
import com.threedreport.app.ui.history.label
import com.threedreport.app.ui.history.printTimeText
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Medidas e cores do documento. O PDF é do vendedor, não do app: sem azul-marinho nem âmbar, só tinta
// escura, cinzas e a logo dele. As medidas vêm do esboço aprovado (Leva 10B, parte 7), em "cqw": porcentagem da
// largura da página, que é como as proporções ficam iguais em qualquer folha.
// ---------------------------------------------------------------------------------------------

internal val PAGE_WIDTH = PDRectangle.A4.width
internal val PAGE_HEIGHT = PDRectangle.A4.height

/** Porcentagem da largura da página, em pontos (1/72 pol.). */
internal fun cqw(percent: Float): Float = percent * PAGE_WIDTH / 100f

/** Tinta do texto, e seus apagados. */
internal val INK = DocumentColors.ink
internal val INK_MUTED = DocumentColors.inkMuted

/** Linha de separação (cabeçalho e rodapé), linha fina entre as linhas de detalhe e o cinza dos blocos. */
internal val RULE = DocumentColors.rule
internal val ROW_RULE = DocumentColors.rowRule
internal val PANEL = DocumentColors.panel
private val BORDER = DocumentColors.pageBorder

internal val SIDE_MARGIN = cqw(8f)
internal val TOP_MARGIN = cqw(7f)
private val BOTTOM_MARGIN = cqw(7f)
internal val PANEL_RADIUS = cqw(1.2f)

// Tamanhos de letra.
private val BRAND_SIZE = cqw(4.2f)
private val CONTACT_SIZE = cqw(2.1f)
internal val META_SIZE = cqw(2.3f)
internal val TITLE_SIZE = cqw(6.4f)
internal val LABEL_SIZE = cqw(2f)
internal val VALUE_SIZE = cqw(3.2f)
private val PRICE_SIZE = cqw(8.4f)
internal val ROW_SIZE = cqw(2.3f)
internal val FOOT_SIZE = cqw(1.9f)

/** Espaço entre as letras de um rótulo em maiúsculas, em fração do corpo (0,06 em). */
private const val LABEL_TRACKING = 0.06f

/** Altura das letras maiúsculas e dos algarismos, em fração do corpo (Inter e Space Grotesk ficam perto de 0,7). */
private const val CAP_HEIGHT = 0.72f

/** [CAP_HEIGHT] pros outros arquivos do PDF (o bloco do Pix e o recibo). */
internal const val PDF_CAP_HEIGHT = CAP_HEIGHT

// Blocos de página.
internal val BLOCK_PAD_X = cqw(3.4f)
internal val BLOCK_PAD_Y = cqw(3f)
private val PHOTO_BOX_HEIGHT = cqw(52f)
private val PHOTO_BOX_PADDING = cqw(2f)
internal val ROW_HEIGHT = cqw(4.2f)
internal val GAP = cqw(2.6f)

/**
 * Espaço entre o rótulo pequeno e o valor do bloco do valor, e entre um grupo (rótulo e valor) e o seguinte. O entre grupos
 * é mais que o dobro do outro, pra "Entrega até" e "Válido até" não se confundirem num bloco só. Mais a folga de
 * arredondamento nas contas de altura.
 */
internal val LABEL_GAP = cqw(0.8f)

/** Respiro do rótulo do preço grande: da base do rótulo ao topo dos algarismos. Maior que [LABEL_GAP], que serve ao texto pequeno da direita. */
private val PRICE_LABEL_GAP = cqw(1.6f)
internal val GROUP_GAP = cqw(2.6f)
internal const val EPSILON = 0.5f

/** Rodapé: o texto fica na margem de baixo e a linha fina acima dele. */
private val FOOTER_RULE_Y = BOTTOM_MARGIN + FOOT_SIZE + cqw(1.6f)

/** O conteúdo termina acima deste ponto, pra nunca encostar na linha do rodapé. */
internal val CONTENT_BOTTOM = FOOTER_RULE_Y + GAP

/** Maior lado da logo embutida no PDF: o bastante pra ficar nítida no cabeçalho, sem inflar o arquivo. */
private const val LOGO_MAX_PIXELS = 512

/** Caixa máxima da logo no cabeçalho, em pontos (1/72 pol.). */
private const val LOGO_MAX_WIDTH = 150f
private const val LOGO_MAX_HEIGHT = 50f

/** Distância da borda opcional até o limite da página. */
private const val BORDER_INSET = 20f

/**
 * Maior lado da foto no PDF: 1200 px na página do orçamento, 800 px na grade do catálogo, em JPEG
 * (decisão 108). A foto do celular ia inteira e sem compressão, e um catálogo de 30 produtos passava de
 * centenas de MB, grande demais pra mandar no WhatsApp. Foto com transparência (miniatura do G-code) vai sem perda,
 * pra não ganhar um fundo branco sobre o cinza do quadro.
 */
private const val QUOTE_PHOTO_MAX_PIXELS = 1200
private const val CATALOG_PHOTO_MAX_PIXELS = 800
private const val PHOTO_JPEG_QUALITY = 0.85f

/** Quanto uma foto pequena pode ser ampliada no quadro (pontos por pixel); mais que isso vira borrão. */
private const val MAX_PHOTO_UPSCALE = 3f

/** Espaço mínimo pra foto caber na mesma página; menos que isso, ela vai pra uma página só dela. */
private const val MIN_PHOTO_HEIGHT = 160f

/** A marca d'água diagonal, em graus, e o quanto do quadro da foto ela pode ocupar. */
private const val WATERMARK_DEGREES = 32.0
private const val WATERMARK_FILL = 0.9f
private const val WATERMARK_ALPHA = 0.14f

/**
 * Decodifica a foto via Skia ([decodeImageBitmap], mesmo decoder da miniatura no app) em vez de
 * `javax.imageio.ImageIO` — o JDK não tem leitor de WebP registrado por padrão, então `ImageIO.read`
 * retornava `null` silenciosamente pra fotos `.webp` e a imagem sumia do PDF sem erro nenhum
 * (bug corrigido aqui; ver docs/roadmap.md). Skia decodifica os mesmos formatos que a miniatura já
 * usa, então os dois nunca mais divergem.
 */
private fun decodePhotoAsBufferedImage(bytes: ByteArray?): BufferedImage? =
    bytes?.let { runCatching { decodeImageBitmap(it).toAwtImage() }.getOrNull() }

/**
 * Tudo que é igual em todas as páginas de um documento: fontes, opções e a logo já convertida.
 * A logo vira **uma** imagem do PDF, reaproveitada em cada página; convertê-la por página faria
 * um PDF em lote de 20 orçamentos carregar a mesma logo 20 vezes.
 *
 * As fontes são as do app, embutidas no documento (só os caracteres usados), pra o PDF abrir igual em qualquer leitor.
 */
internal class PdfContext(
    val document: PDDocument,
    val options: PdfLayoutOptions,
    val texts: ClientDocumentTexts,
) {
    /** Space Grotesk 700: marca, título e preço. */
    val displayFont: PDFont = loadFont(ClientDocumentFonts.spaceGroteskBold)

    /** Inter 400: texto. */
    val bodyFont: PDFont = loadFont(ClientDocumentFonts.interRegular)

    /** Inter 600: valores e datas. */
    val strongFont: PDFont = loadFont(ClientDocumentFonts.interSemiBold)

    val logo: PDImageXObject? = options.logoBytes?.let { prepareLogo(document, it) }

    /** Se o cabeçalho de identidade aparece. Sem logo nem contato, a página é a de sempre. */
    val hasHeader: Boolean
        get() = logo != null || options.contacts.isNotEmpty()

    private fun loadFont(bytes: ByteArray): PDFont = PDType0Font.load(document, ByteArrayInputStream(bytes), true)
}

/** A foto reduzida, pronta pra desenhar. `null` se não houver foto ou ela não abrir. */
private fun preparePhoto(document: PDDocument, bytes: ByteArray?, maxPixels: Int): PDImageXObject? {
    val decoded = decodePhotoAsBufferedImage(bytes) ?: return null
    return runCatching {
        // Reduz uma vez (mantendo o alfa) e decide sobre a imagem já reduzida, pela regra única de transparência.
        val reduced = decoded.scaledToFit(maxPixels, keepAlpha = true)
        if (reduced.hasTransparency()) {
            LosslessFactory.createFromImage(document, reduced)
        } else {
            JPEGFactory.createFromImage(document, reduced.scaledToFit(maxPixels, keepAlpha = false), PHOTO_JPEG_QUALITY)
        }
    }.getOrNull()
}

/**
 * Quebra [text] em até [maxLines] linhas que cabem em [width] com [font] no tamanho [size]; a última
 * ganha "…" se ainda sobrar texto. Nome longo de peça saía cortado na borda da página.
 */
internal fun wrapText(font: PDFont, size: Float, text: String, width: Float, maxLines: Int): List<String> {
    val words = pdfSafe(font, text).split(' ').filter { it.isNotEmpty() }
    val lines = mutableListOf<String>()
    var current = ""
    var index = 0
    while (index < words.size && lines.size < maxLines) {
        val candidate = if (current.isEmpty()) words[index] else "$current ${words[index]}"
        if (font.widthOf(candidate, size) <= width || current.isEmpty()) {
            current = candidate
            index++
        } else {
            lines += current
            current = ""
        }
    }
    if (current.isNotEmpty() && lines.size < maxLines) lines += current
    if (index < words.size && lines.isNotEmpty()) {
        var last = lines.removeAt(lines.lastIndex)
        while (last.isNotEmpty() && font.widthOf("$last…", size) > width) last = last.dropLast(1).trimEnd()
        lines += "$last…"
    }
    return lines.ifEmpty { listOf("") }
}

/** Decodifica a logo (Skia, pelo mesmo motivo da foto), reduz se for grande e converte uma vez só. */
private fun prepareLogo(document: PDDocument, bytes: ByteArray): PDImageXObject? {
    val decoded = decodePhotoAsBufferedImage(bytes) ?: return null
    val largestSide = maxOf(decoded.width, decoded.height)
    val image = if (largestSide <= LOGO_MAX_PIXELS) {
        decoded
    } else {
        val scale = LOGO_MAX_PIXELS.toDouble() / largestSide
        val width = (decoded.width * scale).roundToInt().coerceAtLeast(1)
        val height = (decoded.height * scale).roundToInt().coerceAtLeast(1)
        // ARGB pra manter o fundo transparente da logo, que é o caso mais comum.
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { scaled ->
            val graphics = scaled.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(decoded, 0, 0, width, height, null)
            graphics.dispose()
        }
    }
    return runCatching { LosslessFactory.createFromImage(document, image) }.getOrNull()
}

/**
 * Deixa [text] desenhável com [font]. Uma fonte só desenha os caracteres que tem: um emoji no nome do orçamento ou
 * no contato fazia a exportação inteira falhar com exceção. Caractere que a fonte não codifica é descartado (e o
 * espaço duplo que sobra, juntado), porque um PDF com um símbolo a menos é muito melhor do que PDF nenhum.
 */
internal fun pdfSafe(font: PDFont, text: String): String {
    val safe = buildString {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val character = String(Character.toChars(codePoint))
            if (runCatching { font.encode(character) }.isSuccess) append(character)
            index += Character.charCount(codePoint)
        }
    }
    return if (safe.length == text.length) safe else safe.replace(Regex(" {2,}"), " ").trim()
}

/** Linha de texto simples na cor [color], sempre passando por [pdfSafe]. */
internal fun PDPageContentStream.text(font: PDFont, size: Float, x: Float, y: Float, text: String, color: Color = INK) {
    saveGraphicsState()
    setNonStrokingColor(color)
    beginText()
    setFont(font, size)
    newLineAtOffset(x, y)
    showText(pdfSafe(font, text))
    endText()
    restoreGraphicsState()
}

/** Como [text], mas alinhado à direita em [rightX]. */
internal fun PDPageContentStream.textRight(font: PDFont, size: Float, rightX: Float, y: Float, text: String, color: Color = INK) =
    text(font, size, rightX - font.widthOf(text, size), y, text, color)

/** Rótulo pequeno em maiúsculas, com as letras um pouco afastadas, à esquerda de [x] ou terminando nele. */
internal fun PDPageContentStream.overline(font: PDFont, size: Float, x: Float, y: Float, text: String, color: Color = INK_MUTED, rightAligned: Boolean = false) {
    val upper = pdfSafe(font, text.uppercase(Locale.ROOT))
    val spacing = size * LABEL_TRACKING
    val width = overlineWidth(font, size, text)
    saveGraphicsState()
    setNonStrokingColor(color)
    beginText()
    setFont(font, size)
    setCharacterSpacing(spacing)
    newLineAtOffset(if (rightAligned) x - width else x, y)
    showText(upper)
    endText()
    restoreGraphicsState()
}

/** Largura de um rótulo em maiúsculas com as letras afastadas, como o [overline] o desenha. */
private fun overlineWidth(font: PDFont, size: Float, text: String): Float {
    val upper = pdfSafe(font, text.uppercase(Locale.ROOT))
    return font.widthOf(upper, size) + upper.length * size * LABEL_TRACKING
}

internal fun PDFont.widthOf(text: String, size: Float): Float = getStringWidth(pdfSafe(this, text)) / 1000f * size

/** Retângulo de cantos arredondados (a curva de Bézier de um quarto de círculo tem 0,5523 do raio como alça). */
internal fun PDPageContentStream.roundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
    val r = minOf(radius, width / 2f, height / 2f)
    val k = 0.5523f * r
    moveTo(x + r, y)
    lineTo(x + width - r, y)
    curveTo(x + width - r + k, y, x + width, y + r - k, x + width, y + r)
    lineTo(x + width, y + height - r)
    curveTo(x + width, y + height - r + k, x + width - r + k, y + height, x + width - r, y + height)
    lineTo(x + r, y + height)
    curveTo(x + r - k, y + height, x, y + height - r + k, x, y + height - r)
    lineTo(x, y + r)
    curveTo(x, y + r - k, x + r - k, y, x + r, y)
    closePath()
}

internal fun PDPageContentStream.fillPanel(x: Float, y: Float, width: Float, height: Float, radius: Float = PANEL_RADIUS) {
    saveGraphicsState()
    setNonStrokingColor(PANEL)
    roundedRect(x, y, width, height, radius)
    fill()
    restoreGraphicsState()
}

internal fun PDPageContentStream.hairline(x1: Float, x2: Float, y: Float, color: Color = RULE) {
    saveGraphicsState()
    setStrokingColor(color)
    setLineWidth(0.5f)
    moveTo(x1, y)
    lineTo(x2, y)
    stroke()
    restoreGraphicsState()
}

/** Uma página sendo desenhada: o conteúdo, o cursor vertical (a próxima coisa começa abaixo dele) e a área da foto. */
internal class PageWriter(val page: PDPage, val content: PDPageContentStream, var y: Float) {
    /** O que desenhar por cima da marca d'água e do rodapé (o bloco do Pix). */
    var afterDecorations: (() -> Unit)? = null

    /** Onde a marca d'água fica: o quadro da foto, quando há. `[x, base, largura, altura]`. */
    var watermarkArea: FloatArray? = null
}

actual fun renderSavedQuotesPdf(
    items: List<QuoteExportItem>,
    texts: ClientDocumentTexts,
    brandName: String?,
    footerText: String?,
    options: PdfLayoutOptions,
): ByteArray {
    PDDocument().use { document ->
        val context = PdfContext(document, options, texts)

        items.forEach { item -> drawQuote(context, item, brandName, footerText) }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}

internal fun PdfContext.startPage(withHeader: Boolean): PageWriter {
    val page = PDPage(PDRectangle.A4).also(document::addPage)
    val content = PDPageContentStream(document, page)
    val top = PAGE_HEIGHT - TOP_MARGIN
    val y = if (withHeader && hasHeader) drawIdentityHeader(this, content, page, top) - GAP else top
    return PageWriter(page, content, y)
}

internal fun PdfContext.finishPage(writer: PageWriter, brandName: String?, footerText: String?) {
    drawPageDecorations(this, writer, brandName, footerText)
    writer.afterDecorations?.invoke()
    writer.content.close()
}

/** O que o cliente lê numa linha de detalhe: "Pintura (× 3)" à esquerda e o valor à direita. */
private class DetailRow(val label: String, val value: String)

/** Uma linha do lado direito do bloco do valor: rótulo pequeno em maiúsculas (se houver), valor, e o respiro até a próxima. [muted] é a linha de apoio (fonte de corpo, cor apagada), como a entrega expressa. */
internal class BlockLine(val label: String?, val value: String, val valueSize: Float, val gapBelow: Float, val muted: Boolean = false)

/** O bloco cinza: rótulo e preço grande à esquerda, entrega e validade à direita. */
internal class PriceBlock(val label: String, val price: String, val lines: List<BlockLine>, val note: String? = null)

/** As linhas de detalhe entre a foto e o bloco do valor: a conta das peças, serviços, frete e tempo de impressão. */
private fun detailRows(savedQuote: SavedQuote, options: PdfLayoutOptions, texts: ClientDocumentTexts): List<DetailRow> = buildList {
    val currency = savedQuote.currency
    val quantity = savedQuote.quote.quantity
    if (hasBreakdown(savedQuote)) add(DetailRow(texts.labelValue, valueText(savedQuote, texts)))
    savedQuote.services.forEach { service ->
        add(DetailRow(service.label(texts, quantity), service.total(quantity).toCurrencyText(currency)))
    }
    if (savedQuote.shippingAbsorbed) {
        add(DetailRow(texts.labelShipping, texts.labelShippingFree))
    } else if (savedQuote.shippingCost > 0) {
        add(DetailRow(texts.labelShipping, savedQuote.chargedShipping.toCurrencyText(currency)))
    }
    if (options.showPrintTime) {
        val duration = texts.duration(savedQuote.totalPrintTimeMinutes)
        add(DetailRow(texts.labelPrintTime, if (quantity > 1) texts.printTimeForPieces(duration, quantity) else duration))
    }
}

/** O valor deixa de ser só "o preço" quando há conta a mostrar: várias peças, serviços ou frete. Aí o bloco diz "Total". */
private fun hasBreakdown(savedQuote: SavedQuote): Boolean =
    savedQuote.quote.quantity > 1 || savedQuote.services.isNotEmpty() || savedQuote.shippingCost > 0

private fun priceBlock(savedQuote: SavedQuote, options: PdfLayoutOptions, texts: ClientDocumentTexts): PriceBlock {
    val lines = buildList {
        val deliveryDay = savedQuote.deliveryDateEpochDay
        val rush = savedQuote.quote.rush
        val validDays = options.validityDays?.takeIf { it > 0 && savedQuote.isOrder }
        // Entrega expressa (decisão 125) junto do prazo; sem prazo, vira a própria linha do bloco.
        if (deliveryDay != null) {
            add(BlockLine(texts.labelDelivery, formatDate(deliveryDay), VALUE_SIZE, gapBelow = if (rush) LABEL_GAP else GROUP_GAP))
            if (rush) add(BlockLine(null, texts.rush, ROW_SIZE, gapBelow = GROUP_GAP, muted = true))
        } else if (rush) {
            add(BlockLine(null, texts.rush, VALUE_SIZE, gapBelow = GROUP_GAP))
        }
        if (validDays != null) {
            add(BlockLine(texts.labelValidUntil, formatDate((options.issuedOnEpochDay ?: todayEpochDay()) + validDays), VALUE_SIZE, gapBelow = 0f))
        }
    }
    val closed = if (lines.isNotEmpty()) lines.dropLast(1) + lines.last().let { BlockLine(it.label, it.value, it.valueSize, gapBelow = 0f, muted = it.muted) } else lines
    return PriceBlock(
        label = if (hasBreakdown(savedQuote)) texts.labelTotal else texts.labelValue,
        price = savedQuote.totalWithServices.toCurrencyText(savedQuote.currency),
        lines = closed,
        note = savedQuote.depositText(texts),
    )
}

/** O menor corpo do preço antes de a coluna da direita descer pra baixo dele. */
private val MIN_PRICE_SIZE = cqw(5.2f)

/** Como o bloco do valor se organiza pra largura que tem: o corpo do preço e se a coluna da direita vai pra baixo dele. */
internal class PriceLayout(val priceSize: Float, val stacked: Boolean)

/** Largura da coluna da direita: a mais larga entre os rótulos e os valores. */
private fun rightColumnWidth(context: PdfContext, block: PriceBlock): Float = block.lines.maxOfOrNull { line ->
    val valueFont = if (line.muted) context.bodyFont else context.strongFont
    val valueWidth = valueFont.widthOf(line.value, line.valueSize)
    val labelWidth = line.label?.let { overlineWidth(context.bodyFont, LABEL_SIZE, it) } ?: 0f
    maxOf(valueWidth, labelWidth)
} ?: 0f

/**
 * O preço mede o próprio texto: encolhe o corpo, até [MIN_PRICE_SIZE], pra nunca invadir a coluna da direita (como o
 * `fitFont` da imagem). Se nem no mínimo cabe ao lado, a coluna da direita desce pra baixo do preço, que então usa a
 * largura inteira do bloco.
 */
internal fun priceLayout(context: PdfContext, block: PriceBlock, width: Float): PriceLayout {
    val inner = width - BLOCK_PAD_X * 2
    val fullWidth = context.displayFont.widthOf(block.price, PRICE_SIZE).coerceAtLeast(1f)
    val rightWidth = rightColumnWidth(context, block)
    val beside = if (rightWidth > 0f) inner - rightWidth - GROUP_GAP else inner
    if (fullWidth <= beside) return PriceLayout(PRICE_SIZE, stacked = false)
    val fitted = PRICE_SIZE * beside / fullWidth
    if (fitted >= MIN_PRICE_SIZE) return PriceLayout(fitted, stacked = false)
    return PriceLayout(minOf(PRICE_SIZE, PRICE_SIZE * inner / fullWidth), stacked = true)
}

/** Altura do que a coluna da esquerda ocupa, do alto do rótulo até a linha de base do preço. */
private fun leftColumnHeight(priceSize: Float, hasNote: Boolean): Float =
    LABEL_SIZE * CAP_HEIGHT + PRICE_LABEL_GAP + priceSize * CAP_HEIGHT + if (hasNote) noteOffset() else 0f

/** O que a linha do sinal (sob o preço, dentro do quadro) soma: o respiro e a altura das letras. */
private fun noteOffset(): Float = cqw(2.4f) + ROW_SIZE * CAP_HEIGHT

/** Altura da coluna da direita, do alto do primeiro rótulo até a linha de base do último valor. */
private fun rightColumnHeight(block: PriceBlock): Float = block.lines.sumOf { line ->
    ((if (line.label != null) LABEL_SIZE * CAP_HEIGHT + LABEL_GAP else 0f) + line.valueSize * CAP_HEIGHT + line.gapBelow).toDouble()
}.toFloat()

internal fun blockHeight(block: PriceBlock, layout: PriceLayout): Float {
    val left = leftColumnHeight(layout.priceSize, block.note != null)
    val right = rightColumnHeight(block)
    val content = if (layout.stacked && right > 0f) left + GROUP_GAP + right else maxOf(left, right)
    return BLOCK_PAD_Y * 2 + content + cqw(0.5f)
}

/** Desenha o bloco cinza com o valor, alinhando as duas colunas pela linha de base de baixo. */
internal fun drawPriceBlock(context: PdfContext, writer: PageWriter, block: PriceBlock, layout: PriceLayout, x: Float, width: Float) {
    val content = writer.content
    val height = blockHeight(block, layout)
    val bottom = writer.y - height
    content.fillPanel(x, bottom, width, height)

    val gap = LABEL_GAP
    val rightHeight = rightColumnHeight(block)
    // Empilhado, a coluna da direita fica embaixo, alinhada à esquerda como o preço; lado a lado, ela vai à direita.
    val rightBase = bottom + BLOCK_PAD_Y + cqw(0.5f)
    val baseline = (if (layout.stacked) rightBase + rightHeight + GROUP_GAP else rightBase) + if (block.note != null) noteOffset() else 0f
    content.text(context.displayFont, layout.priceSize, x + BLOCK_PAD_X, baseline, block.price)
    content.overline(context.bodyFont, LABEL_SIZE, x + BLOCK_PAD_X, baseline + layout.priceSize * CAP_HEIGHT + PRICE_LABEL_GAP, block.label)
    block.note?.let { content.text(context.strongFont, ROW_SIZE, x + BLOCK_PAD_X, baseline - noteOffset(), it, INK) }

    // Do alto pra baixo: cada item tem o rótulo (se houver) e o valor; o último valor fica na linha de base de baixo.
    var cursor = rightBase + rightHeight
    val right = x + width - BLOCK_PAD_X
    block.lines.forEach { line ->
        if (line.label != null) {
            val labelBaseline = cursor - LABEL_SIZE * CAP_HEIGHT
            if (layout.stacked) {
                content.overline(context.bodyFont, LABEL_SIZE, x + BLOCK_PAD_X, labelBaseline, line.label)
            } else {
                content.overline(context.bodyFont, LABEL_SIZE, right, labelBaseline, line.label, rightAligned = true)
            }
            cursor = labelBaseline - gap
        }
        val valueBaseline = cursor - line.valueSize * CAP_HEIGHT
        val font = if (line.muted) context.bodyFont else context.strongFont
        val color = if (line.muted) INK_MUTED else INK
        if (layout.stacked) {
            content.text(font, line.valueSize, x + BLOCK_PAD_X, valueBaseline, line.value, color)
        } else {
            content.textRight(font, line.valueSize, right, valueBaseline, line.value, color)
        }
        cursor = valueBaseline - line.gapBelow
    }
    writer.y = bottom
}

/**
 * Desenha o orçamento: cabeçalho de identidade (se houver logo ou contato), a linha de pedido e emissão, o nome da peça
 * (até duas linhas), "Para" e "Kit" (se houver), a foto sobre um quadro cinza, as linhas de detalhe (conta das peças,
 * serviços, frete, tempo de impressão) e o bloco do valor com a entrega e a validade. A foto vai pra uma página só dela
 * quando não cabe ao lado do resto (com muitos serviços, ela chegava a sair de cabeça pra baixo); as linhas que não
 * cabem continuam na página seguinte, e o bloco do valor fecha a última.
 */
private fun drawQuote(context: PdfContext, item: QuoteExportItem, brandName: String?, footerText: String?) {
    val savedQuote = item.savedQuote
    val texts = context.texts
    val contentWidth = PAGE_WIDTH - SIDE_MARGIN * 2
    val photo = preparePhoto(context.document, item.photoBytes, QUOTE_PHOTO_MAX_PIXELS)
    val rows = detailRows(savedQuote, context.options, texts)
    val block = priceBlock(savedQuote, context.options, texts)
    val priceLayout = priceLayout(context, block, contentWidth)
    val blockHeight = blockHeight(block, priceLayout)
    // Depois do preço: a linha do sinal e o bloco do Pix. Entram na conta da foto, pra ficarem na mesma página do preço.
    val pix = pixBlockFor(savedQuote, context.options)
    val pixHeight = pix?.let { pixBlockHeight(context, it, contentWidth) } ?: 0f
    val afterBlock = if (pix != null) GAP + pixHeight else 0f

    var writer = context.startPage(withHeader = true)
    val content = writer.content

    // Pedido e emissão em cima, e o nome da peça grande embaixo. A validade mora no bloco do valor.
    writer.y -= cqw(2f)
    documentInfoLine(savedQuote, context.options, texts, withValidity = false)?.let { info ->
        writer.y -= META_SIZE
        content.text(context.bodyFont, META_SIZE, SIDE_MARGIN, writer.y, info, INK_MUTED)
        writer.y -= cqw(0.8f)
    }
    wrapText(context.displayFont, TITLE_SIZE, savedQuote.clientTitle(texts, withNumber = false), contentWidth, maxLines = 2).forEach { line ->
        writer.y -= TITLE_SIZE
        content.text(context.displayFont, TITLE_SIZE, SIDE_MARGIN, writer.y, line)
        writer.y -= TITLE_SIZE * 0.15f
    }
    if (context.options.showClientName) {
        savedQuote.client?.name?.let { name ->
            writer.y -= cqw(1.4f) + META_SIZE
            content.text(context.bodyFont, META_SIZE, SIDE_MARGIN, writer.y, texts.to(name), INK_MUTED)
        }
    }
    val printCount = savedQuote.quote.prints.size
    if (printCount > 1) {
        writer.y -= cqw(1.2f) + META_SIZE
        content.text(context.bodyFont, META_SIZE, SIDE_MARGIN, writer.y, texts.kit(printCount), INK_MUTED)
    }
    writer.y -= GAP

    // A foto leva o espaço que sobrar: no máximo o quadro inteiro, no mínimo o bastante pra valer a pena.
    var photoPending = photo != null
    if (photo != null) {
        val below = (if (rows.isNotEmpty()) rows.size * ROW_HEIGHT + GAP else 0f) + blockHeight + afterBlock
        val room = writer.y - CONTENT_BOTTOM - below - GAP
        val boxHeight = minOf(PHOTO_BOX_HEIGHT, room)
        if (boxHeight >= MIN_PHOTO_HEIGHT) {
            drawPhotoBox(writer, photo, boxHeight)
            writer.y -= GAP
            photoPending = false
        }
    }

    rows.forEachIndexed { index, row ->
        val isLast = index == rows.lastIndex
        val needed = ROW_HEIGHT + if (isLast) GAP + blockHeight else 0f
        if (writer.y - needed < CONTENT_BOTTOM - EPSILON) {
            context.finishPage(writer, brandName, footerText)
            writer = context.startPage(withHeader = false)
        }
        drawDetailRow(context, writer, row, isFirst = index == 0 || writer.y == PAGE_HEIGHT - TOP_MARGIN)
    }
    if (rows.isNotEmpty()) writer.y -= GAP

    if (writer.y - blockHeight < CONTENT_BOTTOM - EPSILON) {
        context.finishPage(writer, brandName, footerText)
        writer = context.startPage(withHeader = false)
    }
    drawPriceBlock(context, writer, block, priceLayout, SIDE_MARGIN, contentWidth)
    if (pix != null) {
        // O bloco do Pix não é cortado entre páginas: se não cabe aqui, vai inteiro pra próxima.
        writer.y -= GAP
        if (writer.y - pixHeight < CONTENT_BOTTOM - EPSILON) {
            context.finishPage(writer, brandName, footerText)
            writer = context.startPage(withHeader = false)
        }
        val pixTop = writer.y
        writer.y -= pixHeight
        // Desenhado depois da marca d'água, que não deve passar por cima do QR nem do código.
        writer.afterDecorations = { drawPixBlock(context, writer.content, pix, SIDE_MARGIN, pixTop, contentWidth) }
    }
    context.finishPage(writer, brandName, footerText)

    if (photoPending && photo != null) {
        val photoPage = context.startPage(withHeader = false)
        drawPhotoBox(photoPage, photo, photoPage.y - CONTENT_BOTTOM)
        context.finishPage(photoPage, brandName, footerText)
    }
}

/** Uma linha de detalhe: rótulo apagado à esquerda, valor em negrito à direita, uma linha fina embaixo. */
private fun drawDetailRow(context: PdfContext, writer: PageWriter, row: DetailRow, isFirst: Boolean) {
    val content = writer.content
    val right = PAGE_WIDTH - SIDE_MARGIN
    if (isFirst) content.hairline(SIDE_MARGIN, right, writer.y, ROW_RULE)
    val baseline = writer.y - ROW_HEIGHT / 2f - ROW_SIZE * CAP_HEIGHT / 2f
    val valueWidth = context.strongFont.widthOf(row.value, ROW_SIZE)
    val labelRoom = right - SIDE_MARGIN - valueWidth - GAP
    val label = wrapText(context.bodyFont, ROW_SIZE, row.label, labelRoom, maxLines = 1).first()
    content.text(context.bodyFont, ROW_SIZE, SIDE_MARGIN, baseline, label, INK_MUTED)
    content.textRight(context.strongFont, ROW_SIZE, right, baseline, row.value)
    writer.y -= ROW_HEIGHT
    content.hairline(SIDE_MARGIN, right, writer.y, ROW_RULE)
}

/**
 * O quadro cinza com a foto dentro: na largura útil da página, a foto centralizada com a proporção que tem. O quadro
 * vira a área da marca d'água da página.
 */
private fun drawPhotoBox(writer: PageWriter, photo: PDImageXObject, height: Float) {
    val content = writer.content
    val width = PAGE_WIDTH - SIDE_MARGIN * 2
    val bottom = writer.y - height
    content.fillPanel(SIDE_MARGIN, bottom, width, height)

    val boxWidth = width - PHOTO_BOX_PADDING * 2
    val boxHeight = height - PHOTO_BOX_PADDING * 2
    val scale = minOf(boxWidth / photo.width, boxHeight / photo.height, MAX_PHOTO_UPSCALE)
    val drawWidth = photo.width * scale
    val drawHeight = photo.height * scale
    content.drawImage(photo, SIDE_MARGIN + (width - drawWidth) / 2f, bottom + (height - drawHeight) / 2f, drawWidth, drawHeight)

    writer.watermarkArea = floatArrayOf(SIDE_MARGIN, bottom, width, height)
    writer.y = bottom
}

/**
 * "Pedido #0042 · Emitido em 25/09/2026 · Válido até 02/10/2026", com o que houver. O número dá como o
 * cliente se referir ao orçamento; a validade protege o preço de um cliente que volta meses depois. No PDF a validade
 * mora no bloco do valor ([withValidity] falso); a linha inteira é a da mensagem e dos testes.
 */
internal fun documentInfoLine(
    savedQuote: SavedQuote,
    options: PdfLayoutOptions,
    texts: ClientDocumentTexts,
    withValidity: Boolean = true,
): String? {
    val issued = options.issuedOnEpochDay ?: todayEpochDay()
    return listOfNotNull(
        savedQuote.displayNumber?.takeIf { savedQuote.isOrder }?.let(texts::orderNumber),
        texts.issued(formatDate(issued)),
        options.validityDays?.takeIf { withValidity && it > 0 && savedQuote.isOrder }?.let { texts.validUntil(formatDate(issued + it)) },
    ).joinToString(" · ")
}

/** O valor das peças: "R$ 60,20", ou a conta "10 × R$ 6,02 = R$ 60,20" com mais de uma. */
internal fun valueText(savedQuote: SavedQuote, texts: ClientDocumentTexts): String {
    val quote = savedQuote.quote
    val currency = savedQuote.currency
    return if (quote.quantity > 1) {
        texts.valueTimesQuantity(quote.quantity, quote.unitSalePrice.toCurrencyText(currency), quote.salePrice.toCurrencyText(currency))
    } else {
        quote.salePrice.toCurrencyText(currency)
    }
}

/**
 * Borda, marca d'água e rodapé, por último: desenhados por cima do resto do conteúdo (inclusive a
 * foto), translúcidos o bastante pra não atrapalhar a leitura — do contrário ficam encobertos pela
 * foto. Independentes: cada um só aparece se configurado.
 */
private fun drawPageDecorations(context: PdfContext, writer: PageWriter, brandName: String?, footerText: String?) {
    val content = writer.content
    if (context.options.showBorder) drawBorder(content)
    if (!brandName.isNullOrBlank()) {
        // Sobre a foto, quando há; sem foto, no meio da página, como sempre foi.
        val area = writer.watermarkArea ?: floatArrayOf(SIDE_MARGIN, CONTENT_BOTTOM, PAGE_WIDTH - SIDE_MARGIN * 2, PAGE_HEIGHT - CONTENT_BOTTOM - TOP_MARGIN)
        drawWatermark(content, context.displayFont, brandName, area, clipToArea = writer.watermarkArea != null)
    }
    drawFooter(context, writer.page, content, footerText)
}

/** Rodapé: linha fina, o nome da marca à esquerda e "Gerado com 3DReport" à direita, na mesma linha. */
private fun drawFooter(context: PdfContext, page: PDPage, content: PDPageContentStream, footerText: String?) {
    val hasText = !footerText.isNullOrBlank()
    if (!hasText && !context.options.showAppSignature) return
    val right = PAGE_WIDTH - SIDE_MARGIN
    content.hairline(SIDE_MARGIN, right, FOOTER_RULE_Y)
    if (hasText) content.text(context.bodyFont, FOOT_SIZE, SIDE_MARGIN, BOTTOM_MARGIN, footerText!!, INK_MUTED)
    if (context.options.showAppSignature) drawAppSignature(context, page, content, right, BOTTOM_MARGIN)
}

/**
 * "Gerado com 3DReport" no canto inferior direito, o espaço que a decisão 86 deixou livre no rodapé pra isso.
 * Discreto de propósito: o documento é do vendedor, a assinatura é só uma porta pra outro vendedor conhecer o app. Só
 * "3DReport" é link pro site (decisão 91), pra o clique cair no nome do app e não numa frase inteira.
 */
private fun drawAppSignature(context: PdfContext, page: PDPage, content: PDPageContentStream, right: Float, baselineY: Float) {
    val font = context.bodyFont
    val (before, name, after) = context.texts.signature(APP_NAME)
    val prefixWidth = font.widthOf(before, FOOT_SIZE)
    val nameWidth = font.widthOf(name, FOOT_SIZE)
    val suffixWidth = if (after.isEmpty()) 0f else font.widthOf(after, FOOT_SIZE)
    val x = right - prefixWidth - nameWidth - suffixWidth

    content.text(font, FOOT_SIZE, x, baselineY, before + name + after, INK_MUTED)

    val link = PDAnnotationLink().apply {
        rectangle = PDRectangle(x + prefixWidth, baselineY - 2f, nameWidth, FOOT_SIZE + 4f)
        borderStyle = PDBorderStyleDictionary().apply { width = 0f }
        action = PDActionURI().apply { uri = APP_SITE_URL }
    }
    page.annotations.add(link)
}

/** Nome do app, que não se traduz; a frase em volta vem dos recursos ([ClientDocumentTexts.signature]). */
private const val APP_NAME = "3DReport"
private const val APP_SITE_URL = "https://mateustoin.github.io/3DReport/"

/**
 * Cabeçalho de papel timbrado: à esquerda a logo (ou, sem logo, o nome da marca em destaque), à
 * direita o contato alinhado, e uma linha fina embaixo. Devolve a altura da linha, pra quem chama continuar
 * desenhando abaixo dela. O canto de baixo da página, e não este cabeçalho, fica reservado pra assinatura "Gerado com
 * 3DReport" (leva 8, decisão 86).
 */
private fun drawIdentityHeader(
    context: PdfContext,
    content: PDPageContentStream,
    page: PDPage,
    top: Float,
    margin: Float = SIDE_MARGIN,
): Float {
    val options = context.options
    val brandName = options.brandName?.takeIf { it.isNotBlank() }
    val right = page.mediaBox.width - margin

    var leftHeight = 0f
    val logo = context.logo
    if (logo != null) {
        val scale = minOf(LOGO_MAX_WIDTH / logo.width, LOGO_MAX_HEIGHT / logo.height)
        val drawWidth = logo.width * scale
        val drawHeight = logo.height * scale
        content.drawImage(logo, margin, top - drawHeight, drawWidth, drawHeight)
        leftHeight = drawHeight
    } else if (brandName != null) {
        // Nome comprido encolhe até caber na metade da página, pra não bater no contato.
        val room = (right - margin) * 0.55f
        val size = minOf(BRAND_SIZE, BRAND_SIZE * room / context.displayFont.widthOf(brandName, BRAND_SIZE).coerceAtLeast(1f))
        val text = wrapText(context.displayFont, size, brandName, room, maxLines = 1).first()
        content.text(context.displayFont, size, margin, top - size, text)
        leftHeight = size * 1.2f
    }

    // Com logo, o nome vai pro bloco da direita junto do contato; sem logo, ele já está à esquerda.
    val rightLines = buildList {
        if (logo != null && brandName != null) add(Triple(context.displayFont, CONTACT_SIZE, brandName))
        options.contacts.forEach { add(Triple(context.bodyFont, CONTACT_SIZE, context.texts.contactLine(it))) }
    }
    var lineY = top
    val lineHeight = CONTACT_SIZE * 1.5f
    rightLines.forEachIndexed { index, (font, size, text) ->
        lineY -= if (index == 0) size else lineHeight
        content.textRight(font, size, right, lineY, text, if (font == context.displayFont) INK else INK_MUTED)
    }
    val rightHeight = top - lineY + if (rightLines.isNotEmpty()) CONTACT_SIZE * 0.3f else 0f

    val ruleY = top - maxOf(leftHeight, rightHeight) - cqw(2.4f)
    content.hairline(margin, right, ruleY)
    return ruleY
}

actual fun renderCatalogPdf(
    items: List<QuoteExportItem>,
    texts: ClientDocumentTexts,
    brandName: String?,
    footerText: String?,
    options: PdfLayoutOptions,
): ByteArray {
    PDDocument().use { document ->
        val context = PdfContext(document, options, texts)

        val margin = SIDE_MARGIN
        val columns = 2
        val gutter = CATALOG_GUTTER
        val titleSize = cqw(4f)
        val titleHeight = titleSize + GAP
        val photoSize = CATALOG_PHOTO_SIZE
        val cellHeight = photoSize + CATALOG_TEXT_HEIGHT
        val rowGap = CATALOG_ROW_GAP
        val cellWidth = (PAGE_WIDTH - margin * 2 - gutter * (columns - 1)) / columns

        // O cabeçalho de identidade tem altura variável (logo, número de contatos). Mede numa
        // página de rascunho, fora do documento, pra saber quantas linhas de produtos ainda cabem.
        val identityHeight = if (context.hasHeader) {
            PDDocument().use { scratch ->
                val scratchPage = PDPage(PDRectangle.A4).also(scratch::addPage)
                val scratchContext = PdfContext(scratch, options, texts)
                PDPageContentStream(scratch, scratchPage).use { content ->
                    val top = scratchPage.mediaBox.height - TOP_MARGIN
                    top - drawIdentityHeader(scratchContext, content, scratchPage, top) + GAP
                }
            }
        } else {
            0f
        }
        val headerHeight = titleHeight + identityHeight

        val sectionHeaderHeight = SECTION_HEADER_HEIGHT + SECTION_AFTER_GAP
        val blocks = catalogSections(items, texts.catalogOther).flatMap { (title, sectionItems) ->
            listOfNotNull(title?.let(CatalogBlock::Header)) + sectionItems.chunked(columns).map(CatalogBlock::Row)
        }

        // Fluxo de cima pra baixo: página nova só quando o próximo bloco não cabe. Sem categorias,
        // os blocos são só linhas de produtos e o resultado é a mesma grade de sempre.
        var writer: PageWriter? = null
        var pageStart = 0f

        fun finishPage() {
            val current = writer ?: return
            drawPageDecorations(context, current, brandName, footerText)
            current.content.close()
        }

        fun startPage() {
            finishPage()
            val newPage = PDPage(PDRectangle.A4).also(document::addPage)
            val newContent = PDPageContentStream(document, newPage)
            val top = PAGE_HEIGHT - TOP_MARGIN
            if (context.hasHeader) drawIdentityHeader(context, newContent, newPage, top)
            val titleTop = top - identityHeight
            newContent.text(context.displayFont, titleSize, margin, titleTop - titleSize, context.texts.catalogTitle)
            pageStart = top - headerHeight
            writer = PageWriter(newPage, newContent, pageStart)
        }

        blocks.forEach { block ->
            // O título da seção nunca fica sozinho no pé da página: só entra se couber uma linha junto.
            val needed = when (block) {
                is CatalogBlock.Header -> sectionHeaderHeight + cellHeight
                is CatalogBlock.Row -> cellHeight
            }
            if (writer == null || (writer!!.y - needed < CONTENT_BOTTOM && writer!!.y < pageStart)) startPage()
            val current = writer!!
            when (block) {
                is CatalogBlock.Header -> {
                    current.y = drawSectionHeader(context, current.content, block.title, margin, current.y, PAGE_WIDTH - margin * 2) - SECTION_AFTER_GAP
                }

                is CatalogBlock.Row -> {
                    block.items.forEachIndexed { col, item ->
                        val cellX = margin + col * (cellWidth + gutter)
                        drawCatalogCell(context, current.content, item, cellX, current.y, cellWidth, photoSize)
                    }
                    current.y -= cellHeight + rowGap
                }
            }
        }
        finishPage()

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}

/** Um pedaço do catálogo em fluxo: título de seção ou uma linha da grade de produtos. */
private sealed interface CatalogBlock {
    data class Header(val title: String) : CatalogBlock
    data class Row(val items: List<QuoteExportItem>) : CatalogBlock
}

/**
 * Seções do catálogo por categoria do produto (decisão 102): em ordem alfabética, com os sem
 * categoria em [otherTitle] ("Outros"), no fim. Se nenhum item tem categoria, uma seção só e sem título, que é o
 * catálogo de antes. A ordem dos itens dentro de cada seção é a que veio.
 */
internal fun catalogSections(items: List<QuoteExportItem>, otherTitle: String): List<Pair<String?, List<QuoteExportItem>>> {
    if (items.none { it.savedQuote.category != null }) return listOf(null to items)
    val (categorized, uncategorized) = items.partition { it.savedQuote.category != null }
    val sections = categorized
        .groupBy { it.savedQuote.category!!.lowercase() }
        .values
        .map { group -> group.first().savedQuote.category!! to group }
        .sortedBy { it.first.lowercase() }
    return if (uncategorized.isEmpty()) sections else sections + (otherTitle to uncategorized)
}

/**
 * Desenha uma célula da grade do catálogo: foto (se houver, centralizada num quadro cinza de [photoSize] de altura),
 * nome (até duas linhas) e preço, na moeda do produto.
 */
private fun drawCatalogCell(
    context: PdfContext,
    content: PDPageContentStream,
    item: QuoteExportItem,
    cellX: Float,
    cellTop: Float,
    cellWidth: Float,
    photoSize: Float,
) {
    val savedQuote = item.savedQuote
    // O quadro cinza fica mesmo sem foto, pra a grade não ganhar um buraco branco.
    content.fillPanel(cellX, cellTop - photoSize, cellWidth, photoSize, PANEL_RADIUS)
    preparePhoto(context.document, item.photoBytes, CATALOG_PHOTO_MAX_PIXELS)?.let { pdImage ->
        val inner = PHOTO_BOX_PADDING
        val scale = minOf((cellWidth - inner * 2) / pdImage.width, (photoSize - inner * 2) / pdImage.height, 1f)
        val drawWidth = pdImage.width * scale
        val drawHeight = pdImage.height * scale
        val offsetX = cellX + (cellWidth - drawWidth) / 2f
        val offsetY = cellTop - photoSize + (photoSize - drawHeight) / 2f
        content.drawImage(pdImage, offsetX, offsetY, drawWidth, drawHeight)
    }

    var textY = cellTop - photoSize - CATALOG_NAME_TOP
    wrapText(context.strongFont, CATALOG_NAME_SIZE, savedQuote.clientTitle(context.texts, withNumber = false), cellWidth, maxLines = 2).forEach { line ->
        content.text(context.strongFont, CATALOG_NAME_SIZE, cellX, textY, line)
        textY -= CATALOG_NAME_LINE
    }
    textY -= CATALOG_PRICE_GAP
    content.text(context.displayFont, CATALOG_PRICE_SIZE, cellX, textY, savedQuote.totalWithServices.toCurrencyText(savedQuote.currency))
}

/** Medidas da grade do catálogo, em pontos: o vão entre colunas e entre linhas, o quadro da foto e o texto embaixo dele. */
private const val CATALOG_GUTTER = 20f
private const val CATALOG_ROW_GAP = 20f
private const val CATALOG_PHOTO_SIZE = 180f
private const val CATALOG_TEXT_HEIGHT = 62f

/** Nome (Inter semibold) e preço (Space Grotesk) da célula, e a distância do quadro até a primeira linha do nome. */
private const val CATALOG_NAME_SIZE = 12f
private const val CATALOG_PRICE_SIZE = 14.5f
private const val CATALOG_NAME_LINE = 14f
private const val CATALOG_NAME_TOP = 18f
private const val CATALOG_PRICE_GAP = 4f

private const val SECTION_HEADER_HEIGHT = 26f
private const val SECTION_TITLE_SIZE = 14f
private const val SECTION_AFTER_GAP = 14f

/** Título de uma seção do catálogo: o nome em destaque e uma linha fina embaixo. Devolve o novo cursor, abaixo dela. */
private fun drawSectionHeader(context: PdfContext, content: PDPageContentStream, text: String, x: Float, top: Float, width: Float): Float {
    val size = SECTION_TITLE_SIZE
    val bottom = top - SECTION_HEADER_HEIGHT
    content.text(context.displayFont, size, x, top - size - 2f, text)
    content.hairline(x, x + width, bottom)
    return bottom
}

/** Borda fina e clara em volta da página, pra dar cara de documento. */
private fun drawBorder(content: PDPageContentStream) {
    content.saveGraphicsState()
    content.setStrokingColor(BORDER)
    content.setLineWidth(0.75f)
    content.addRect(BORDER_INSET, BORDER_INSET, PAGE_WIDTH - BORDER_INSET * 2, PAGE_HEIGHT - BORDER_INSET * 2)
    content.stroke()
    content.restoreGraphicsState()
}

/**
 * Texto grande, translúcido e na diagonal, centralizado em [area] (`[x, base, largura, altura]`). Encolhe se o nome
 * não cabe na diagonal da área; [clipToArea] corta o que passar, pra a marca d'água sobre a foto não vazar do quadro.
 */
private fun drawWatermark(content: PDPageContentStream, font: PDFont, text: String, area: FloatArray, clipToArea: Boolean) {
    val safeText = pdfSafe(font, text)
    val angle = Math.toRadians(WATERMARK_DEGREES)
    val unitWidth = font.getStringWidth(safeText) / 1000f
    val maxLength = minOf(area[2] * WATERMARK_FILL / cos(angle).toFloat(), area[3] * WATERMARK_FILL / sin(angle).toFloat())
    val fontSize = minOf(cqw(7f), maxLength / unitWidth.coerceAtLeast(0.01f))
    val textWidth = unitWidth * fontSize
    val centerX = area[0] + area[2] / 2f
    val centerY = area[1] + area[3] / 2f
    val startX = centerX - (textWidth / 2f) * cos(angle).toFloat()
    val startY = centerY - (textWidth / 2f) * sin(angle).toFloat()

    content.saveGraphicsState()
    if (clipToArea) {
        content.roundedRect(area[0], area[1], area[2], area[3], PANEL_RADIUS)
        content.clip()
    }
    val transparency = PDExtendedGraphicsState()
    transparency.nonStrokingAlphaConstant = WATERMARK_ALPHA
    content.setGraphicsStateParameters(transparency)
    content.setNonStrokingColor(INK)

    content.beginText()
    content.setFont(font, fontSize)
    content.setTextMatrix(Matrix.getRotateInstance(angle, startX, startY))
    content.showText(safeText)
    content.endText()
    content.restoreGraphicsState()
}

actual fun renderPdfFirstPagePng(pdf: ByteArray, dpi: Float): ByteArray =
    Loader.loadPDF(pdf).use { document ->
        val image = PDFRenderer(document).renderImageWithDPI(0, dpi)
        ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
