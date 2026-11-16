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
import org.apache.pdfbox.rendering.PDFRenderer
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
        val info = documentInfoLine(savedQuote, PdfLayoutOptions(validityDays = 7, issuedOnEpochDay = issued), testClientTexts)

        assertEquals("Pedido #0042 · Emitido em ${formatDate(issued)} · Válido até ${formatDate(issued + 7)}", info)
    }

    @Test
    fun withoutValidityTheLineSaysOnlyNumberAndDate() {
        val info = documentInfoLine(savedQuote, PdfLayoutOptions(issuedOnEpochDay = 20_356L), testClientTexts)

        assertFalse(info!!.contains("Válido"))
    }

    @Test
    fun theClientNameOnlyGoesOnTheDocumentWhenTurnedOn() {
        val items = listOf(QuoteExportItem(savedQuote, null))

        assertFalse(textOf(renderSavedQuotesPdf(items, testClientTexts, null, null)).contains("Maria Cliente"))
        assertTrue(textOf(renderSavedQuotesPdf(items, testClientTexts, null, null, PdfLayoutOptions(showClientName = true))).contains("Para: Maria Cliente"))
    }

    @Test
    fun anAutomaticNameNeverBecomesTheTitle() {
        val autoNamed = savedQuote.copy(name = "Orçamento - 24/09/2026 14:30", autoNamed = true)

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(autoNamed, null)), testClientTexts, null, null))

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
    fun withManyServicesTheRowsContinueAndThePhotoGoesToItsOwnPageInsteadOfUpsideDown() {
        val services = List(40) { QuoteService(id = "s$it", name = "Serviço $it", price = 1.0, chargedPerOrder = true) }
        val crowded = savedQuote.copy(services = services)

        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(crowded, pngOf(400, 300))), testClientTexts, null, null)

        Loader.loadPDF(pdf).use { document ->
            assertTrue(document.numberOfPages >= 3, "${document.numberOfPages} páginas")
            val text = PDFTextStripper().getText(document)
            // Nenhuma linha se perde, e o bloco do valor fecha o fim das linhas.
            assertTrue((0 until 40).all { "Serviço $it" in text })
            assertTrue("TOTAL" in text)
            val lastPage = document.getPage(document.numberOfPages - 1).resources
            assertEquals(1, lastPage.xObjectNames.count())
            for (index in 0 until document.numberOfPages - 1) assertEquals(0, document.getPage(index).resources.xObjectNames.count())
        }
    }

    @Test
    fun thePhotoFitsOnTheFirstPageWithTheRowsAndThePriceBlock() {
        val withServices = savedQuote.copy(services = listOf(QuoteService(id = "p", name = "Pintura", price = 5.0, chargedPerOrder = true)), shippingCost = 12.0)

        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(withServices, pngOf(400, 300))), testClientTexts, null, null)

        Loader.loadPDF(pdf).use { document ->
            assertEquals(1, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            assertTrue("TOTAL" in text && "Pintura" in text && "Frete" in text)
            assertEquals(1, document.getPage(0).resources.xObjectNames.count())
        }
    }

    @Test
    fun theDocumentEmbedsTheAppFontsAndKeepsAccentsAndTheCurrencySymbol() {
        val options = PdfLayoutOptions(validityDays = 7, issuedOnEpochDay = 20_356L)
        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote.copy(name = "Cápsula de café"), null)), testClientTexts, null, null, options)

        Loader.loadPDF(pdf).use { document ->
            val fonts = document.getPage(0).resources.fontNames.map { document.getPage(0).resources.getFont(it) }
            assertTrue(fonts.isNotEmpty() && fonts.all { it.isEmbedded }, "fontes: ${fonts.map { it.name }}")
            val names = fonts.map { it.name }
            assertTrue(names.any { "SpaceGrotesk" in it } && names.any { "Inter" in it }, "$names")
            val text = PDFTextStripper().getText(document)
            assertTrue("Cápsula de café" in text && "VÁLIDO ATÉ" in text && "R$ 16,19" in text, text)
        }
    }

    @Test
    fun eachOptionOnlyAppearsWhenItIsTurnedOn() {
        val items = listOf(QuoteExportItem(savedQuote, null))
        fun textFor(brandName: String?, footerText: String?, options: PdfLayoutOptions) = textOf(renderSavedQuotesPdf(items, testClientTexts, brandName, footerText, options))
        val base = PdfLayoutOptions(issuedOnEpochDay = 20_356L)

        // Marca d'água e rodapé são independentes.
        assertTrue("Marca Teste" in textFor("Marca Teste", null, base))
        assertFalse("Marca Teste" in textFor(null, null, base))
        assertTrue("Rodapé Teste" in textFor(null, "Rodapé Teste", base))
        assertFalse("Rodapé Teste" in textFor("Marca Teste", null, base))
        // Assinatura do app.
        assertTrue("Gerado com 3DReport" in textFor(null, null, base.copy(showAppSignature = true)))
        assertFalse("Gerado com 3DReport" in textFor(null, null, base))
        // Validade e tempo de impressão.
        assertTrue("VÁLIDO ATÉ" in textFor(null, null, base.copy(validityDays = 7)))
        assertFalse("VÁLIDO ATÉ" in textFor(null, null, base))
        assertTrue("Tempo de impressão" in textFor(null, null, base.copy(showPrintTime = true)))
        assertFalse("Tempo de impressão" in textFor(null, null, base))
        // Cabeçalho: contato e logo só quando existem.
        assertTrue("contato@example.com" in textFor(null, null, base.copy(contactLines = listOf("contato@example.com"))))
        assertFalse("contato@example.com" in textFor(null, null, base))
    }

    @Test
    fun theBorderIsOnlyDrawnWhenTurnedOn() {
        fun leftBorderPixel(options: PdfLayoutOptions): Int {
            val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, null)), testClientTexts, null, null, options)
            return Loader.loadPDF(pdf).use { PDFRenderer(it).renderImageWithDPI(0, 72f) }.getRGB(20, 400) and 0xFFFFFF
        }

        assertEquals(0xFFFFFF, leftBorderPixel(PdfLayoutOptions()))
        assertTrue(leftBorderPixel(PdfLayoutOptions(showBorder = true)) != 0xFFFFFF)
    }

    @Test
    fun aPhotoWithTransparencyKeepsItAndAnOpaqueOneBecomesAJpeg() {
        fun suffixOf(photo: ByteArray): String {
            val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photo)), testClientTexts, null, null)
            return Loader.loadPDF(pdf).use { document ->
                val resources = document.getPage(0).resources
                resources.xObjectNames.map { resources.getXObject(it) }.filterIsInstance<PDImageXObject>().single().suffix
            }
        }

        assertEquals("png", suffixOf(transparentPngOf(200, 150)))
        assertEquals("jpg", suffixOf(pngOf(200, 150)))
    }

    private fun transparentPngOf(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in height / 4 until height * 3 / 4) for (x in width / 4 until width * 3 / 4) image.setRGB(x, y, 0xFF336699.toInt())
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test
    fun aPhoneSizedPhotoIsReducedBeforeGoingIntoThePdf() {
        val photo = pngOf(3000, 2000)

        val pdf = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photo)), testClientTexts, null, null)

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
