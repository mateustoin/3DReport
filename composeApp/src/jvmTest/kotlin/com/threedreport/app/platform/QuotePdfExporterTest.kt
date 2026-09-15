package com.threedreport.app.platform

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
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
        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes = null)

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

        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes)

        val document = Loader.loadPDF(pdfBytes)
        assertTrue(document.pages.count() == 1)
        document.close()
    }
}
