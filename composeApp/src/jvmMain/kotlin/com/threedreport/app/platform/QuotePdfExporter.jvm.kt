package com.threedreport.app.platform

import androidx.compose.ui.graphics.toAwtImage
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.history.deliveryDateText
import com.threedreport.app.ui.history.printTimeText
import com.threedreport.core.model.Currency
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.Loader
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Maior lado da logo embutida no PDF: o bastante pra ficar nítida no cabeçalho, sem inflar o arquivo. */
private const val LOGO_MAX_PIXELS = 512

/** Caixa máxima da logo no cabeçalho, em pontos (1/72 pol.). */
private const val LOGO_MAX_WIDTH = 140f
private const val LOGO_MAX_HEIGHT = 56f

/** Distância da borda opcional até o limite da página. */
private const val BORDER_INSET = 20f

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
 */
private class PdfContext(
    val document: PDDocument,
    val currency: Currency,
    val options: PdfLayoutOptions,
) {
    val titleFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    val logo: PDImageXObject? = options.logoBytes?.let { prepareLogo(document, it) }

    /** Se o cabeçalho de identidade aparece. Sem logo nem contato, a página é a de sempre. */
    val hasHeader: Boolean
        get() = logo != null || options.contactLines.isNotEmpty()
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
 * Deixa [text] desenhável com [font]. As fontes padrão do PDF (Helvetica) só conhecem a tabela
 * WinAnsi: um emoji no nome do orçamento ou no contato fazia a exportação inteira falhar com
 * exceção. Caractere que a fonte não codifica é descartado (e o espaço duplo que sobra, juntado),
 * porque um PDF com um símbolo a menos é muito melhor do que PDF nenhum.
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

/** Linha de texto simples, sempre passando por [pdfSafe]. */
private fun PDPageContentStream.text(font: PDFont, size: Float, x: Float, y: Float, text: String) {
    beginText()
    setFont(font, size)
    newLineAtOffset(x, y)
    showText(pdfSafe(font, text))
    endText()
}

private fun PDFont.widthOf(text: String, size: Float): Float = getStringWidth(pdfSafe(this, text)) / 1000f * size

actual fun renderSavedQuotesPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency,
    options: PdfLayoutOptions,
): ByteArray {
    PDDocument().use { document ->
        val context = PdfContext(document, currency, options)

        items.forEach { item ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            drawQuotePage(context, page, item, watermarkText, footerText)
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}

/**
 * Desenha o cabeçalho de identidade (se houver logo ou contato), nome, valor de venda
 * (+ serviços/frete/total, se houver), o preço por unidade quando a quantidade é maior que 1,
 * prazo e tempo de impressão, foto (se houver), borda e marca d'água/rodapé (se configurados)
 * numa única página. Sem identidade, borda, quantidade, frete e prazo (os padrões), a saída é
 * idêntica à de antes desses campos existirem.
 */
private fun drawQuotePage(
    context: PdfContext,
    page: PDPage,
    item: QuoteExportItem,
    watermarkText: String?,
    footerText: String?,
) {
    val margin = 50f
    val footerReserve = 50f
    val savedQuote = item.savedQuote
    val currency = context.currency
    val titleFont = context.titleFont
    val bodyFont = context.bodyFont

    PDPageContentStream(context.document, page).use { content ->
        var cursorY = page.mediaBox.height - margin
        if (context.hasHeader) {
            cursorY = drawIdentityHeader(context, content, page, margin, cursorY) - 28f
        }

        content.text(titleFont, 20f, margin, cursorY, savedQuote.name)
        cursorY -= 30f

        val quantity = savedQuote.quote.quantity

        content.text(bodyFont, 14f, margin, cursorY, "Venda: ${savedQuote.quote.salePrice.toCurrencyText(currency)}")
        cursorY -= 24f

        savedQuote.services.forEach { service ->
            val label = if (quantity > 1 && !service.chargedPerOrder) "${service.name} (× $quantity)" else service.name
            content.text(bodyFont, 12f, margin, cursorY, "$label: ${service.total(quantity).toCurrencyText(currency)}")
            cursorY -= 18f
        }

        if (savedQuote.shippingCost > 0) {
            content.text(bodyFont, 12f, margin, cursorY, "Frete: ${savedQuote.shippingCost.toCurrencyText(currency)}")
            cursorY -= 18f
        }

        // O "Total" precisa aparecer mesmo com frete e sem nenhum serviço, senão o cliente vê
        // "Venda" e "Frete" soltos, sem a soma.
        if (savedQuote.services.isNotEmpty() || savedQuote.shippingCost > 0) {
            content.text(titleFont, 14f, margin, cursorY, "Total: ${savedQuote.totalWithServices.toCurrencyText(currency)}")
            cursorY -= 24f
        }

        // Sempre sobre o total que o cliente paga (com serviços), igual à tela de Orçamento: se
        // fosse sobre o valor de venda, a mesma peça teria dois preços unitários diferentes.
        if (quantity > 1) {
            val unitPrice = savedQuote.totalWithServices / quantity
            content.text(bodyFont, 11f, margin, cursorY, "$quantity peças · ${unitPrice.toCurrencyText(currency)} cada")
            cursorY -= 18f
        }

        // Em destaque, e não como mais uma linha da lista: é o que o cliente usa pra decidir se
        // autoriza a fabricação (pedido do teste externo, decisão 83).
        savedQuote.deliveryDateText()?.let { text ->
            cursorY -= 4f
            cursorY = drawHighlightBand(content, titleFont, text, margin, cursorY, page.mediaBox.width - margin * 2)
            // O cursor é linha de base: a próxima linha precisa descer a altura dela, não só um respiro.
            cursorY -= 20f
        }

        if (context.options.showPrintTime) {
            content.text(bodyFont, 11f, margin, cursorY, savedQuote.printTimeText())
            cursorY -= 18f
        }
        cursorY -= 6f

        val bufferedImage = decodePhotoAsBufferedImage(item.photoBytes)
        if (bufferedImage != null) {
            val pdImage = LosslessFactory.createFromImage(context.document, bufferedImage)
            val maxWidth = page.mediaBox.width - margin * 2
            val maxHeight = cursorY - footerReserve
            val scale = minOf(maxWidth / pdImage.width, maxHeight / pdImage.height, 1f)
            val drawWidth = pdImage.width * scale
            val drawHeight = pdImage.height * scale
            content.drawImage(pdImage, margin, cursorY - drawHeight, drawWidth, drawHeight)
        }

        drawPageDecorations(context, content, page, watermarkText, footerText, margin)
    }
}

/**
 * Borda, marca d'água e rodapé, por último: desenhados por cima do resto do conteúdo (inclusive a
 * foto), translúcidos o bastante pra não atrapalhar a leitura — do contrário ficam encobertos pela
 * foto. Independentes: cada um só aparece se configurado.
 */
private fun drawPageDecorations(
    context: PdfContext,
    content: PDPageContentStream,
    page: PDPage,
    watermarkText: String?,
    footerText: String?,
    margin: Float,
) {
    // Com borda, rodapé e assinatura sobem um pouco pra não encostar nela.
    val footerY = if (context.options.showBorder) 36f else 28f
    if (context.options.showBorder) drawBorder(content, page)
    if (!watermarkText.isNullOrBlank()) drawWatermark(content, page, context.titleFont, watermarkText)
    if (!footerText.isNullOrBlank()) drawFooter(content, page, context.bodyFont, footerText, margin, textY = footerY)
    if (context.options.showAppSignature) drawAppSignature(content, page, context.bodyFont, margin, baselineY = footerY)
}

/**
 * "Gerado com 3DReport" no canto inferior direito, o espaço que a decisão 86 deixou livre no
 * rodapé pra isso: fica na mesma linha do nome da marca (centralizado), sem disputar com ele.
 * Pequeno e claro de propósito: o documento é do vendedor, a assinatura é só uma porta pra outro
 * vendedor conhecer o app. Só "3DReport" é link pro site (decisão 91), pra o clique cair no nome
 * do app e não numa frase inteira.
 */
private fun drawAppSignature(content: PDPageContentStream, page: PDPage, font: PDFont, margin: Float, baselineY: Float) {
    val fontSize = 7f
    val prefixWidth = font.widthOf(APP_SIGNATURE_PREFIX, fontSize)
    val nameWidth = font.widthOf(APP_NAME, fontSize)
    val x = page.mediaBox.width - margin - prefixWidth - nameWidth

    content.saveGraphicsState()
    content.setNonStrokingColor(Color(165, 165, 165))
    content.text(font, fontSize, x, baselineY, APP_SIGNATURE_PREFIX + APP_NAME)
    content.restoreGraphicsState()

    val link = PDAnnotationLink().apply {
        rectangle = PDRectangle(x + prefixWidth, baselineY - 2f, nameWidth, fontSize + 4f)
        borderStyle = PDBorderStyleDictionary().apply { width = 0f }
        action = PDActionURI().apply { uri = APP_SITE_URL }
    }
    page.annotations.add(link)
}

private const val APP_SIGNATURE_PREFIX = "Gerado com "
private const val APP_NAME = "3DReport"
private const val APP_SITE_URL = "https://mateustoin.github.io/3DReport/"

/**
 * Cabeçalho de papel timbrado: à esquerda a logo (ou, sem logo, o nome da marca em destaque), à
 * direita o nome e os contatos alinhados, e uma linha fina embaixo. Devolve a altura da linha,
 * pra quem chama continuar desenhando abaixo dela. O canto de baixo da página, e não este
 * cabeçalho, fica reservado pra assinatura "Gerado com 3DReport" (leva 8, decisão 86).
 */
private fun drawIdentityHeader(
    context: PdfContext,
    content: PDPageContentStream,
    page: PDPage,
    margin: Float,
    top: Float,
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
        content.text(context.titleFont, 16f, margin, top - 16f, brandName)
        leftHeight = 20f
    }

    // Com logo, o nome vai pro bloco da direita junto do contato; sem logo, ele já está à esquerda.
    val rightLines = buildList {
        if (logo != null && brandName != null) add(Triple(context.titleFont, 11f, brandName))
        options.contactLines.forEach { add(Triple(context.bodyFont, 9f, it)) }
    }
    var lineY = top
    content.saveGraphicsState()
    rightLines.forEachIndexed { index, (font, size, text) ->
        lineY -= if (index == 0) size else size + 3f
        content.setNonStrokingColor(if (font == context.titleFont) Color(0x22, 0x22, 0x22) else Color(90, 90, 90))
        content.text(font, size, right - font.widthOf(text, size), lineY, text)
    }
    content.restoreGraphicsState()
    val rightHeight = top - lineY + if (rightLines.isNotEmpty()) 3f else 0f

    val ruleY = top - maxOf(leftHeight, rightHeight) - 8f
    content.saveGraphicsState()
    content.setStrokingColor(Color(200, 200, 200))
    content.setLineWidth(0.5f)
    content.moveTo(margin, ruleY)
    content.lineTo(right, ruleY)
    content.stroke()
    content.restoreGraphicsState()
    return ruleY
}

actual fun renderCatalogPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency,
    options: PdfLayoutOptions,
): ByteArray {
    PDDocument().use { document ->
        val context = PdfContext(document, currency, options)

        val margin = 40f
        val columns = 2
        val gutter = 20f
        val titleHeight = 50f
        val photoSize = 180f
        val cellHeight = photoSize + 44f
        val rowGap = 20f
        val cellWidth = (PDRectangle.A4.width - margin * 2 - gutter * (columns - 1)) / columns

        // O cabeçalho de identidade tem altura variável (logo, número de contatos). Mede numa
        // página de rascunho, fora do documento, pra saber quantas linhas de produtos ainda cabem.
        val identityHeight = if (context.hasHeader) {
            PDDocument().use { scratch ->
                val scratchPage = PDPage(PDRectangle.A4).also(scratch::addPage)
                val scratchContext = PdfContext(scratch, currency, options)
                PDPageContentStream(scratch, scratchPage).use { content ->
                    val top = scratchPage.mediaBox.height - margin
                    top - drawIdentityHeader(scratchContext, content, scratchPage, margin, top) + 12f
                }
            }
        } else {
            0f
        }
        val headerHeight = titleHeight + identityHeight

        val sectionHeaderHeight = 26f + 14f
        val blocks = catalogSections(items).flatMap { (title, sectionItems) ->
            listOfNotNull(title?.let(CatalogBlock::Header)) + sectionItems.chunked(columns).map(CatalogBlock::Row)
        }

        // Fluxo de cima pra baixo: página nova só quando o próximo bloco não cabe. Sem categorias,
        // os blocos são só linhas de produtos e o resultado é a mesma grade de sempre.
        var page: PDPage? = null
        var content: PDPageContentStream? = null
        var pageStart = 0f
        var cursor = 0f

        fun finishPage() {
            val current = content ?: return
            drawPageDecorations(context, current, page!!, watermarkText, footerText, margin)
            current.close()
        }

        fun startPage() {
            finishPage()
            val newPage = PDPage(PDRectangle.A4).also(document::addPage)
            val newContent = PDPageContentStream(document, newPage)
            val top = newPage.mediaBox.height - margin
            if (context.hasHeader) drawIdentityHeader(context, newContent, newPage, margin, top)
            val titleTop = top - identityHeight
            newContent.text(context.titleFont, 18f, margin, titleTop - 14f, "Catálogo de produtos")
            page = newPage
            content = newContent
            pageStart = top - headerHeight
            cursor = pageStart
        }

        blocks.forEach { block ->
            // O título da seção nunca fica sozinho no pé da página: só entra se couber uma linha junto.
            val needed = when (block) {
                is CatalogBlock.Header -> sectionHeaderHeight + cellHeight
                is CatalogBlock.Row -> cellHeight
            }
            if (content == null || (cursor - needed < margin && cursor < pageStart)) startPage()
            val current = content!!
            when (block) {
                is CatalogBlock.Header -> {
                    cursor = drawHighlightBand(current, context.titleFont, block.title, margin, cursor, PDRectangle.A4.width - margin * 2) - 14f
                }
                is CatalogBlock.Row -> {
                    block.items.forEachIndexed { col, item ->
                        val cellX = margin + col * (cellWidth + gutter)
                        drawCatalogCell(context, current, item, cellX, cursor, cellWidth, photoSize)
                    }
                    cursor -= cellHeight + rowGap
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
 * categoria em "Outros", no fim. Se nenhum item tem categoria, uma seção só e sem título, que é o
 * catálogo de antes. A ordem dos itens dentro de cada seção é a que veio.
 */
internal fun catalogSections(items: List<QuoteExportItem>): List<Pair<String?, List<QuoteExportItem>>> {
    if (items.none { it.savedQuote.category != null }) return listOf(null to items)
    val (categorized, uncategorized) = items.partition { it.savedQuote.category != null }
    val sections = categorized
        .groupBy { it.savedQuote.category!!.lowercase() }
        .values
        .map { group -> group.first().savedQuote.category!! to group }
        .sortedBy { it.first.lowercase() }
    return if (uncategorized.isEmpty()) sections else sections + ("Outros" to uncategorized)
}

/** Desenha uma célula da grade do catálogo: foto (se houver, centralizada e escalada até [photoSize]) + nome + venda. */
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
    val bufferedImage = decodePhotoAsBufferedImage(item.photoBytes)
    if (bufferedImage != null) {
        val pdImage = LosslessFactory.createFromImage(context.document, bufferedImage)
        val scale = minOf(photoSize / pdImage.width, photoSize / pdImage.height, 1f)
        val drawWidth = pdImage.width * scale
        val drawHeight = pdImage.height * scale
        val offsetX = cellX + (cellWidth - drawWidth) / 2f
        val offsetY = cellTop - photoSize + (photoSize - drawHeight) / 2f
        content.drawImage(pdImage, offsetX, offsetY, drawWidth, drawHeight)
    }

    var textY = cellTop - photoSize - 16f
    content.text(context.titleFont, 12f, cellX, textY, savedQuote.name)
    textY -= 16f
    content.text(context.bodyFont, 12f, cellX, textY, savedQuote.totalWithServices.toCurrencyText(context.currency))
}

/**
 * Faixa de fundo âmbar claro com um filete âmbar à esquerda (a cor de destaque do app, decisão 35)
 * e [text] em negrito por cima. Devolve o novo cursor, logo abaixo da faixa.
 */
private fun drawHighlightBand(
    content: PDPageContentStream,
    font: PDFont,
    text: String,
    x: Float,
    top: Float,
    width: Float,
): Float {
    val fontSize = 13f
    val height = 26f
    val bottom = top - height

    content.saveGraphicsState()
    content.setNonStrokingColor(Color(0xFF, 0xF3, 0xD6))
    content.addRect(x, bottom, width, height)
    content.fill()
    content.setNonStrokingColor(Color(0xF5, 0xA6, 0x23))
    content.addRect(x, bottom, 4f, height)
    content.fill()
    content.setNonStrokingColor(Color(0x33, 0x2A, 0x14))
    content.text(font, fontSize, x + 12f, bottom + (height - fontSize) / 2f + 3f, text)
    content.restoreGraphicsState()

    return bottom
}

/** Borda fina e clara em volta da página, pra dar cara de documento. */
private fun drawBorder(content: PDPageContentStream, page: PDPage) {
    content.saveGraphicsState()
    content.setStrokingColor(Color(190, 190, 190))
    content.setLineWidth(0.75f)
    content.addRect(BORDER_INSET, BORDER_INSET, page.mediaBox.width - BORDER_INSET * 2, page.mediaBox.height - BORDER_INSET * 2)
    content.stroke()
    content.restoreGraphicsState()
}

/** Texto grande, cinza claro e diagonal, centralizado na página, por cima do resto do conteúdo. */
private fun drawWatermark(content: PDPageContentStream, page: PDPage, font: PDFont, text: String) {
    val fontSize = 48f
    val safeText = pdfSafe(font, text)
    val angleRadians = Math.toRadians(45.0)
    val textWidth = font.getStringWidth(safeText) / 1000f * fontSize
    val centerX = page.mediaBox.width / 2f
    val centerY = page.mediaBox.height / 2f
    val startX = centerX - (textWidth / 2f) * cos(angleRadians).toFloat()
    val startY = centerY - (textWidth / 2f) * sin(angleRadians).toFloat()

    content.saveGraphicsState()
    val transparency = PDExtendedGraphicsState()
    transparency.nonStrokingAlphaConstant = 0.18f
    content.setGraphicsStateParameters(transparency)
    content.setNonStrokingColor(Color.GRAY)

    content.beginText()
    content.setFont(font, fontSize)
    content.setTextMatrix(Matrix.getRotateInstance(angleRadians, startX, startY))
    content.showText(safeText)
    content.endText()
    content.restoreGraphicsState()
}

/**
 * Rodapé discreto: linha fina + nome da marca centralizado, no rodapé de qualquer página A4. O
 * canto direito fica livre de propósito, reservado pra assinatura "Gerado com 3DReport" (leva 8).
 */
private fun drawFooter(content: PDPageContentStream, page: PDPage, font: PDFont, brandName: String, margin: Float, textY: Float) {
    val fontSize = 9f
    val lineY = textY + 14f

    content.saveGraphicsState()
    content.setStrokingColor(Color(200, 200, 200))
    content.setLineWidth(0.5f)
    content.moveTo(margin, lineY)
    content.lineTo(page.mediaBox.width - margin, lineY)
    content.stroke()

    content.setNonStrokingColor(Color(120, 120, 120))
    val centerX = (page.mediaBox.width - font.widthOf(brandName, fontSize)) / 2f
    content.text(font, fontSize, centerX, textY, brandName)
    content.restoreGraphicsState()
}

actual fun renderPdfFirstPagePng(pdf: ByteArray, dpi: Float): ByteArray =
    Loader.loadPDF(pdf).use { document ->
        val image = PDFRenderer(document).renderImageWithDPI(0, dpi)
        ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
