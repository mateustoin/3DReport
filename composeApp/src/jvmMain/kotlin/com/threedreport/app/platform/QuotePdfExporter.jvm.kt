package com.threedreport.app.platform

import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.SavedQuote
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

actual fun renderSavedQuotePdf(savedQuote: SavedQuote, photoBytes: ByteArray?, watermarkText: String?): ByteArray {
    PDDocument().use { document ->
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        val margin = 50f
        val footerReserve = 50f
        val titleFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        var cursorY = page.mediaBox.height - margin

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
            content.showText("Venda: ${savedQuote.quote.salePrice.toBrl()}")
            content.endText()
            cursorY -= 30f

            val bufferedImage = photoBytes?.let { ImageIO.read(ByteArrayInputStream(it)) }
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
            if (!watermarkText.isNullOrBlank()) {
                drawWatermark(content, page, titleFont, watermarkText)
                drawFooter(content, page, bodyFont, watermarkText, margin)
            }
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
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
