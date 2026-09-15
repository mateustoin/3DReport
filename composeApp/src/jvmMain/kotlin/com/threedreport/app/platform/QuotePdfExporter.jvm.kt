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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun renderSavedQuotePdf(savedQuote: SavedQuote, photoBytes: ByteArray?): ByteArray {
    PDDocument().use { document ->
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        val margin = 50f
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
                val maxHeight = cursorY - margin
                val scale = minOf(maxWidth / pdImage.width, maxHeight / pdImage.height, 1f)
                val drawWidth = pdImage.width * scale
                val drawHeight = pdImage.height * scale
                content.drawImage(pdImage, margin, cursorY - drawHeight, drawWidth, drawHeight)
            }
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}
