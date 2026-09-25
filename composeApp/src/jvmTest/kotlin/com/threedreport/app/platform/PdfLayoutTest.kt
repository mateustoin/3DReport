package com.threedreport.app.platform

import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** O que a revisão pré-lançamento mudou no PDF (decisão 108), sem depender do decodificador do Skia. */
class PdfLayoutTest {

    private val savedQuote = SavedQuote(
        id = "1",
        name = "Suporte de celular",
        quote = Quote(
            prints = listOf(
                QuotedPrint(
                    job = PrintJob(
                        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                        filamentLengthMeters = 12.0,
                        printTimeMinutes = 190.0,
                    ),
                    printerId = "printer",
                    printerName = "Impressora",
                    cost = PrintCost(material = 3.58, energy = 1.48, maintenance = 0.54, finishing = 0.36, investmentReturn = 1.78, fixedCost = 0.0),
                ),
            ),
            costs = CostBreakdown(
                material = 3.58, energy = 1.48, maintenance = 0.54, failures = 0.36, finishing = 0.36,
                investmentReturn = 1.78, administrative = 0.0, labor = 0.0, fixedCost = 0.0,
            ),
            salePrice = 16.19,
        ),
        savedAtEpochMillis = 0L,
        client = Client(name = "Maria Cliente"),
        number = 42,
    )

    private fun textOf(pdf: ByteArray) = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }

    @Test
    fun theOrderNumberIssueDateAndValidityAreOnTheDocument() {
        val issued = 20_356L // 25/09/2025
        val info = documentInfoLine(savedQuote, PdfLayoutOptions(validityDays = 7, issuedOnEpochDay = issued))

        assertEquals("Pedido #0042 · Emitido em ${formatDate(issued)} · Válido até ${formatDate(issued + 7)}", info)
    }

    @Test
    fun withoutValidityTheLineSaysOnlyNumberAndDate() {
        val info = documentInfoLine(savedQuote, PdfLayoutOptions(issuedOnEpochDay = 20_356L))

        assertFalse(info!!.contains("Válido"))
    }

    @Test
    fun theClientNameOnlyGoesOnTheDocumentWhenTurnedOn() {
        val items = listOf(QuoteExportItem(savedQuote, null))

        assertFalse(textOf(renderSavedQuotesPdf(items, null, null)).contains("Maria Cliente"))
        assertTrue(textOf(renderSavedQuotesPdf(items, null, null, PdfLayoutOptions(showClientName = true))).contains("Para: Maria Cliente"))
    }

    @Test
    fun anAutomaticNameNeverBecomesTheTitle() {
        val autoNamed = savedQuote.copy(name = SavedQuote.AUTO_NAME_PREFIX + "24/09/2026 14:30")

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(autoNamed, null)), null, null))

        assertFalse(text.contains("24/09/2026 14:30"))
        assertTrue(text.contains("Orçamento"))
    }

    @Test
    fun aLongNameWrapsIntoTwoLinesInsteadOfLeavingThePage() {
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val lines = wrapText(font, 20f, "Suporte articulado para celular e tablet com base giratória e ajuste de altura", 300f, maxLines = 2)

        assertEquals(2, lines.size)
        assertTrue(lines.all { font.getStringWidth(it) / 1000f * 20f <= 300f }, "$lines")
        assertTrue(lines.last().endsWith("…"))
    }

    @Test
    fun withManyServicesThePhotoGoesToItsOwnPageInsteadOfUpsideDown() {
        val services = List(40) { QuoteService(id = "s$it", name = "Serviço $it", price = 1.0, chargedPerOrder = true) }
        val crowded = savedQuote.copy(services = services)

        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(crowded, pngOf(400, 300))), null, null)

        Loader.loadPDF(pdf).use { assertEquals(2, it.numberOfPages) }
    }

    @Test
    fun aPhoneSizedPhotoIsReducedBeforeGoingIntoThePdf() {
        val photo = pngOf(3000, 2000)

        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photo)), null, null)

        Loader.loadPDF(pdf).use { document ->
            val resources = document.getPage(0).resources
            val image = resources.xObjectNames.map { resources.getXObject(it) }.filterIsInstance<PDImageXObject>().single()
            assertTrue(maxOf(image.width, image.height) <= 1200, "${image.width}×${image.height}")
        }
        assertTrue(pdf.size < photo.size / 2, "PDF de ${pdf.size} bytes pra uma foto de ${photo.size}")
    }

    /** PNG com ruído, que não comprime: parecido com uma foto de celular em tamanho. */
    private fun pngOf(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val random = Random(1)
        for (y in 0 until height) for (x in 0 until width) image.setRGB(x, y, random.nextInt(0xFFFFFF))
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
}
