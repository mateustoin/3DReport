package com.threedreport.app.platform

import androidx.compose.ui.graphics.toAwtImage
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Currency
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decodifica a foto via Skia ([decodeImageBitmap], mesmo decoder da miniatura no app) em vez de
 * `javax.imageio.ImageIO` — o JDK não tem leitor de WebP registrado por padrão, então `ImageIO.read`
 * retornava `null` silenciosamente pra fotos `.webp` e a imagem sumia do PDF sem erro nenhum
 * (bug corrigido aqui; ver docs/roadmap.md). Skia decodifica os mesmos formatos que a miniatura já
 * usa, então os dois nunca mais divergem.
 */
private fun decodePhotoAsBufferedImage(bytes: ByteArray?): BufferedImage? =
    bytes?.let { runCatching { decodeImageBitmap(it).toAwtImage() }.getOrNull() }

actual fun renderSavedQuotesPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency,
): ByteArray {
    PDDocument().use { document ->
        val titleFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        items.forEach { item ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            drawQuotePage(document, page, titleFont, bodyFont, item, watermarkText, footerText, currency)
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}

/** Desenha nome, valor de venda (+ serviços/total, se houver), foto (se houver) e marca d'água/rodapé (se configurados) numa única página. */
private fun drawQuotePage(
    document: PDDocument,
    page: PDPage,
    titleFont: PDType1Font,
    bodyFont: PDType1Font,
    item: QuoteExportItem,
    watermarkText: String?,
    footerText: String?,
    currency: Currency,
) {
    val margin = 50f
    val footerReserve = 50f
    var cursorY = page.mediaBox.height - margin
    val savedQuote = item.savedQuote

    PDPageContentStream(document, page).use { content ->
        content.beginText()
        content.setFont(titleFont, 20f)
        content.newLineAtOffset(margin, cursorY)
        content.showText(savedQuote.name)
        content.endText()
        cursorY -= 30f

        content.beginText()
        content.setFont(bodyFont, 14f)
        content.newLineAtOffset(margin, cursorY)
        content.showText("Venda: ${savedQuote.quote.salePrice.toCurrencyText(currency)}")
        content.endText()
        cursorY -= 24f

        if (savedQuote.services.isNotEmpty()) {
            savedQuote.services.forEach { service ->
                content.beginText()
                content.setFont(bodyFont, 12f)
                content.newLineAtOffset(margin, cursorY)
                content.showText("${service.name}: ${service.price.toCurrencyText(currency)}")
                content.endText()
                cursorY -= 18f
            }

            content.beginText()
            content.setFont(titleFont, 14f)
            content.newLineAtOffset(margin, cursorY)
            content.showText("Total: ${savedQuote.totalWithServices.toCurrencyText(currency)}")
            content.endText()
            cursorY -= 24f
        }
        cursorY -= 6f

        val bufferedImage = decodePhotoAsBufferedImage(item.photoBytes)
        if (bufferedImage != null) {
            val pdImage = LosslessFactory.createFromImage(document, bufferedImage)
            val maxWidth = page.mediaBox.width - margin * 2
            val maxHeight = cursorY - footerReserve
            val scale = minOf(maxWidth / pdImage.width, maxHeight / pdImage.height, 1f)
            val drawWidth = pdImage.width * scale
            val drawHeight = pdImage.height * scale
            content.drawImage(pdImage, margin, cursorY - drawHeight, drawWidth, drawHeight)
        }

        // Marca d'água e rodapé por último: desenhados por cima do resto do
        // conteúdo (inclusive a foto), translúcidos o bastante pra não
        // atrapalhar a leitura — do contrário ficam encobertos pela foto.
        // Independentes: cada um só aparece se o respectivo texto vier preenchido.
        if (!watermarkText.isNullOrBlank()) {
            drawWatermark(content, page, titleFont, watermarkText)
        }
        if (!footerText.isNullOrBlank()) {
            drawFooter(content, page, bodyFont, footerText, margin)
        }
    }
}

actual fun renderCatalogPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency,
): ByteArray {
    PDDocument().use { document ->
        val titleFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        val margin = 40f
        val columns = 2
        val gutter = 20f
        val headerHeight = 50f
        val photoSize = 180f
        val cellHeight = photoSize + 44f
        val rowGap = 20f
        val cellWidth = (PDRectangle.A4.width - margin * 2 - gutter * (columns - 1)) / columns

        val usableHeight = PDRectangle.A4.height - margin * 2 - headerHeight
        val rowsPerPage = maxOf(1, ((usableHeight + rowGap) / (cellHeight + rowGap)).toInt())
        val itemsPerPage = rowsPerPage * columns

        items.chunked(itemsPerPage).forEach { pageItems ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                val top = page.mediaBox.height - margin
                content.beginText()
                content.setFont(titleFont, 18f)
                content.newLineAtOffset(margin, top - 14f)
                content.showText("Catálogo de produtos")
                content.endText()

                pageItems.forEachIndexed { index, item ->
                    val row = index / columns
                    val col = index % columns
                    val cellX = margin + col * (cellWidth + gutter)
                    val cellTop = top - headerHeight - row * (cellHeight + rowGap)
                    drawCatalogCell(document, content, item, cellX, cellTop, cellWidth, photoSize, titleFont, bodyFont, currency)
                }

                if (!watermarkText.isNullOrBlank()) {
                    drawWatermark(content, page, titleFont, watermarkText)
                }
                if (!footerText.isNullOrBlank()) {
                    drawFooter(content, page, bodyFont, footerText, margin)
                }
            }
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}

/** Desenha uma célula da grade do catálogo: foto (se houver, centralizada e escalada até [photoSize]) + nome + venda. */
private fun drawCatalogCell(
    document: PDDocument,
    content: PDPageContentStream,
    item: QuoteExportItem,
    cellX: Float,
    cellTop: Float,
    cellWidth: Float,
    photoSize: Float,
    titleFont: PDType1Font,
    bodyFont: PDType1Font,
    currency: Currency,
) {
    val savedQuote = item.savedQuote
    val bufferedImage = decodePhotoAsBufferedImage(item.photoBytes)
    if (bufferedImage != null) {
        val pdImage = LosslessFactory.createFromImage(document, bufferedImage)
        val scale = minOf(photoSize / pdImage.width, photoSize / pdImage.height, 1f)
        val drawWidth = pdImage.width * scale
        val drawHeight = pdImage.height * scale
        val offsetX = cellX + (cellWidth - drawWidth) / 2f
        val offsetY = cellTop - photoSize + (photoSize - drawHeight) / 2f
        content.drawImage(pdImage, offsetX, offsetY, drawWidth, drawHeight)
    }

    var textY = cellTop - photoSize - 16f
    content.beginText()
    content.setFont(titleFont, 12f)
    content.newLineAtOffset(cellX, textY)
    content.showText(savedQuote.name)
    content.endText()
    textY -= 16f

    content.beginText()
    content.setFont(bodyFont, 12f)
    content.newLineAtOffset(cellX, textY)
    content.showText(savedQuote.totalWithServices.toCurrencyText(currency))
    content.endText()
}

/** Texto grande, cinza claro e diagonal, centralizado na página, por cima do resto do conteúdo. */
private fun drawWatermark(content: PDPageContentStream, page: PDPage, font: PDType1Font, text: String) {
    val fontSize = 48f
    val angleRadians = Math.toRadians(45.0)
    val textWidth = font.getStringWidth(text) / 1000f * fontSize
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
    content.showText(text)
    content.endText()
    content.restoreGraphicsState()
}

/** Rodapé discreto: linha fina + nome da marca centralizado, no rodapé de qualquer página A4. */
private fun drawFooter(content: PDPageContentStream, page: PDPage, font: PDType1Font, brandName: String, margin: Float) {
    val fontSize = 9f
    val textY = 28f
    val lineY = textY + 14f

    content.saveGraphicsState()
    content.setStrokingColor(Color(200, 200, 200))
    content.setLineWidth(0.5f)
    content.moveTo(margin, lineY)
    content.lineTo(page.mediaBox.width - margin, lineY)
    content.stroke()

    content.setNonStrokingColor(Color(120, 120, 120))
    val textWidth = font.getStringWidth(brandName) / 1000f * fontSize
    val centerX = (page.mediaBox.width - textWidth) / 2f
    content.beginText()
    content.setFont(font, fontSize)
    content.newLineAtOffset(centerX, textY)
    content.showText(brandName)
    content.endText()
    content.restoreGraphicsState()
}
