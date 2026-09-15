package com.threedreport.app.platform

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuotePdfExporterTest {

    private val savedQuote = SavedQuote(
        id = "1",
        name = "Suporte de celular",
        quote = Quote(
            job = PrintJob(
                filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                filamentLengthMeters = 12.0,
                printTimeMinutes = 190.0,
            ),
            filamentWeightGrams = 35.79,
            costs = CostBreakdown(3.58, 1.48, 0.54, 0.36, 0.36, 1.78, 0.0),
            productionCost = 8.09,
            salePrice = 16.19,
        ),
        sourceLink = "https://example.com/model",
        savedAtEpochMillis = 0L,
    )

    @Test
    fun pdfContainsNameAndSalePriceButNotInternalData() {
        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes = null, watermarkText = null)

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Suporte de celular"))
        assertTrue(text.contains("16,19"))
        assertFalse(text.contains(savedQuote.sourceLink!!))
        assertFalse(text.contains("8,09")) // custo de produção: não deve ir pro PDF do cliente
    }

    @Test
    fun pdfIsGeneratedWithAPhoto() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB).apply {
            graphics.apply { color = Color.RED; fillRect(0, 0, 10, 10) }
        }
        val photoBytes = ByteArrayOutputStream().use { out -> ImageIO.write(image, "png", out); out.toByteArray() }

        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes, watermarkText = null)

        val document = Loader.loadPDF(pdfBytes)
        assertTrue(document.pages.count() == 1)
        document.close()
    }

    @Test
    fun pdfContainsWatermarkTextWhenProvided() {
        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes = null, watermarkText = "Marcenaria 3D do João")

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        // O texto diagonal quebra em várias "linhas" pro extrator (a posição Y de
        // cada caractere muda ao longo da diagonal); comparamos ignorando espaços.
        val normalizedText = text.replace(Regex("\\s+"), "")
        val normalizedWatermark = "Marcenaria 3D do João".replace(Regex("\\s+"), "")
        assertTrue(normalizedText.contains(normalizedWatermark))
    }

    @Test
    fun blankWatermarkIsNotDrawn() {
        val withBlank = renderSavedQuotePdf(savedQuote, photoBytes = null, watermarkText = "   ")
        val withNull = renderSavedQuotePdf(savedQuote, photoBytes = null, watermarkText = null)

        val textWithBlank = Loader.loadPDF(withBlank).use { PDFTextStripper().getText(it) }
        val textWithNull = Loader.loadPDF(withNull).use { PDFTextStripper().getText(it) }

        assertEquals(textWithNull, textWithBlank)
    }

    @Test
    fun footerShowsBrandNameAsCleanContiguousText() {
        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes = null, watermarkText = "Marcenaria 3D do João")

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        // Diferente da marca d'água diagonal (que quebra em várias "linhas" pro
        // extrator — ver pdfContainsWatermarkTextWhenProvided), o rodapé não é
        // rotacionado, então deve aparecer como uma string contígua normal.
        assertTrue(text.contains("Marcenaria 3D do João"))
    }

    @Test
    fun watermarkStaysVisibleOverThePhotoInsteadOfBeingHiddenBehindIt() {
        // Reprodução do bug relatado: a marca d'água ficava encoberta pela foto
        // porque era desenhada antes dela. Cobrimos toda a área central da página
        // (onde a diagonal cruza) com uma foto de cor sólida e conferimos que o
        // pixel central, depois de renderizado, não é mais a cor pura da foto —
        // ou seja, a marca d'água foi composta por cima, visível.
        val pureBlue = Color.BLUE
        val image = BufferedImage(495, 682, BufferedImage.TYPE_INT_RGB).apply {
            graphics.apply { color = pureBlue; fillRect(0, 0, 495, 682) }
        }
        val photoBytes = ByteArrayOutputStream().use { out -> ImageIO.write(image, "png", out); out.toByteArray() }

        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes, watermarkText = "Marcenaria 3D do João")

        val document = Loader.loadPDF(pdfBytes)
        val rendered = PDFRenderer(document).renderImageWithDPI(0, 72f)
        document.close()

        // Um único pixel pode cair num vão entre letras (a marca d'água é texto,
        // não uma linha contínua); varremos uma janela ao redor do centro, onde a
        // diagonal cruza, e basta um pixel não-azul-puro pra provar que algo foi
        // composto por cima da foto ali.
        val centerX = rendered.width / 2
        val centerY = rendered.height / 2
        val windowRadius = 80
        var foundNonPurePixel = false
        for (x in (centerX - windowRadius)..(centerX + windowRadius)) {
            for (y in (centerY - windowRadius)..(centerY + windowRadius)) {
                val pixel = Color(rendered.getRGB(x, y))
                if (pixel.rgb != pureBlue.rgb) {
                    foundNonPurePixel = true
                }
            }
        }

        assertTrue(
            foundNonPurePixel,
            "Toda a janela ao redor do centro continua azul puro — a marca d'água não está aparecendo por cima da foto.",
        )
    }
}
