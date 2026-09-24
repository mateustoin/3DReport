package com.threedreport.app.platform

import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
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
        client = Client(name = "Maria Cliente", contact = "maria@example.com"),
    )

    private val otherSavedQuote = savedQuote.copy(
        id = "2",
        name = "Vaso decorativo",
        quote = savedQuote.quote.copy(salePrice = 45.0),
        sourceLink = null,
    )

    @Test
    fun pdfContainsNameAndSalePriceButNotInternalData() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Suporte de celular"))
        assertTrue(text.contains("16,19"))
        assertFalse(text.contains(savedQuote.sourceLink!!))
        assertFalse(text.contains("8,09")) // custo de produção: não deve ir pro PDF do cliente
        assertFalse(text.contains(savedQuote.client!!.name)) // cliente: uso interno, não deve ir pro PDF
    }

    @Test
    fun pdfIsGeneratedWithAPhoto() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB).apply {
            graphics.apply { color = Color.RED; fillRect(0, 0, 10, 10) }
        }
        val photoBytes = ByteArrayOutputStream().use { out -> ImageIO.write(image, "png", out); out.toByteArray() }

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes)),
            watermarkText = null,
            footerText = null,
        )

        val document = Loader.loadPDF(pdfBytes)
        assertTrue(document.pages.count() == 1)
        document.close()
    }

    @Test
    fun pdfEmbedsAWebpPhotoInsteadOfSilentlyDroppingIt() {
        // Regressão: javax.imageio.ImageIO não lê WebP sem plugin, e retornava null em silêncio —
        // a foto desaparecia do PDF sem erro nenhum. A exportação decodifica via Skia agora
        // (mesmo decoder da miniatura no app), que lê WebP nativamente.
        val skiaBitmap = Bitmap().apply {
            allocN32Pixels(10, 10)
            erase(0xFFFF0000.toInt())
        }
        val webpBytes = SkiaImage.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.WEBP)!!.bytes

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, webpBytes)),
            watermarkText = null,
            footerText = null,
        )

        val document = Loader.loadPDF(pdfBytes)
        val embeddedImageCount = document.pages[0].resources.xObjectNames.count()
        document.close()

        assertTrue(embeddedImageCount > 0, "a foto WebP deveria ter sido embutida no PDF, não descartada em silêncio")
    }

    @Test
    fun pdfContainsWatermarkTextWhenProvided() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = "Marcenaria 3D do João",
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        // O texto diagonal quebra em várias "linhas" pro extrator (a posição Y de
        // cada caractere muda ao longo da diagonal); comparamos ignorando espaços.
        val normalizedText = text.replace(Regex("\\s+"), "")
        val normalizedWatermark = "Marcenaria 3D do João".replace(Regex("\\s+"), "")
        assertTrue(normalizedText.contains(normalizedWatermark))
    }

    @Test
    fun blankWatermarkAndFooterAreNotDrawn() {
        val withBlank = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = "   ",
            footerText = "   ",
        )
        val withNull = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val textWithBlank = Loader.loadPDF(withBlank).use { PDFTextStripper().getText(it) }
        val textWithNull = Loader.loadPDF(withNull).use { PDFTextStripper().getText(it) }

        assertEquals(textWithNull, textWithBlank)
    }

    @Test
    fun footerShowsBrandNameAsCleanContiguousText() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = "Marcenaria 3D do João",
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        // Diferente da marca d'água diagonal (que quebra em várias "linhas" pro
        // extrator — ver pdfContainsWatermarkTextWhenProvided), o rodapé não é
        // rotacionado, então deve aparecer como uma string contígua normal.
        assertTrue(text.contains("Marcenaria 3D do João"))
    }

    @Test
    fun watermarkAndFooterAreIndependent() {
        fun textFor(watermarkText: String?, footerText: String?) = Loader.loadPDF(
            renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, photoBytes = null)), watermarkText, footerText)
        ).use { PDFTextStripper().getText(it) }

        val onlyWatermark = textFor(watermarkText = "Marca", footerText = null)
        val onlyFooter = textFor(watermarkText = null, footerText = "Marca")
        val neither = textFor(watermarkText = null, footerText = null)

        // O rodapé sozinho contém "Marca" como string contígua; a marca d'água
        // sozinha quebra em fragmentos (é diagonal) mas ainda contém as letras.
        assertTrue(onlyFooter.contains("Marca"))
        assertFalse(neither.contains("Marca"))
        assertTrue(onlyWatermark.replace(Regex("\\s+"), "").contains("Marca"))
        assertFalse(onlyWatermark.contains("Marca")) // sem rodapé, não aparece como string contígua
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

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes)),
            watermarkText = "Marcenaria 3D do João",
            footerText = null,
        )

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

    @Test
    fun multipleItemsProduceOnePagePerItemInOrder() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, null), QuoteExportItem(otherSavedQuote, null)),
            watermarkText = null,
            footerText = null,
        )

        val document = Loader.loadPDF(pdfBytes)
        assertEquals(2, document.numberOfPages)

        val firstPageText = PDFTextStripper().apply { startPage = 1; endPage = 1 }.getText(document)
        val secondPageText = PDFTextStripper().apply { startPage = 2; endPage = 2 }.getText(document)
        document.close()

        assertTrue(firstPageText.contains("Suporte de celular"))
        assertTrue(firstPageText.contains("16,19"))
        assertTrue(secondPageText.contains("Vaso decorativo"))
        assertTrue(secondPageText.contains("45,00"))
    }

    @Test
    fun multipleItemsAllGetTheSameWatermarkAndFooter() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, null), QuoteExportItem(otherSavedQuote, null)),
            watermarkText = null,
            footerText = "Marcenaria 3D do João",
        )

        val document = Loader.loadPDF(pdfBytes)
        val firstPageText = PDFTextStripper().apply { startPage = 1; endPage = 1 }.getText(document)
        val secondPageText = PDFTextStripper().apply { startPage = 2; endPage = 2 }.getText(document)
        document.close()

        assertTrue(firstPageText.contains("Marcenaria 3D do João"))
        assertTrue(secondPageText.contains("Marcenaria 3D do João"))
    }

    @Test
    fun pdfListsEachServiceAndTheGrandTotalWhenPresent() {
        val quoteWithServices = savedQuote.copy(
            services = listOf(Service(id = "paint", name = "Pintura", price = 20.0)),
        )

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(quoteWithServices, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Pintura"))
        assertTrue(text.contains("20,00"))
        assertTrue(text.contains("Total"))
        assertTrue(text.contains("36,19")) // 16,19 (venda) + 20,00 (pintura)
    }

    @Test
    fun pdfShowsQuantityUnitPriceAndMultipliedServiceWhenQuantityIsGreaterThanOne() {
        val quoteWithQuantity = savedQuote.copy(
            quote = savedQuote.quote.copy(salePrice = 60.20, quantity = 10),
            services = listOf(Service(id = "paint", name = "Pintura", price = 15.0)),
        )

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(quoteWithQuantity, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("60,20"))
        assertTrue(text.contains("10 peças"))
        assertTrue(text.contains("Pintura"))
        assertTrue(text.contains("150,00")) // 15,00 (por peça) × 10 = 150,00
        assertTrue(text.contains("210,20")) // total: 60,20 (venda) + 150,00 (serviço)
        // O preço por unidade sai do total que o cliente paga (210,20 / 10), igual à tela.
        assertTrue(text.contains("21,02"))
    }

    @Test
    fun pdfShowsShippingAndTotalEvenWithNoServices() {
        val quoteWithShipping = savedQuote.copy(shippingCost = 12.0)

        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(quoteWithShipping, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Frete"))
        assertTrue(text.contains("12,00"))
        assertTrue(text.contains("Total"))
        assertTrue(text.contains("28,19")) // 16,19 (venda) + 12,00 (frete)
    }

    @Test
    fun noTotalLineInPdfWhenThereAreNoServices() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertFalse(text.contains("Total"))
    }

    @Test
    fun catalogPdfContainsNameAndPriceOfEachItemButNotInternalData() {
        val pdfBytes = renderCatalogPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null), QuoteExportItem(otherSavedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Suporte de celular"))
        assertTrue(text.contains("16,19"))
        assertTrue(text.contains("Vaso decorativo"))
        assertTrue(text.contains("45,00"))
        assertFalse(text.contains(savedQuote.sourceLink!!))
        assertFalse(text.contains(savedQuote.client!!.name))
        assertFalse(text.contains("8,09")) // custo de produção: não deve ir pro catálogo
    }

    @Test
    fun catalogPdfFitsFewItemsOnOnePage() {
        val pdfBytes = renderCatalogPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null), QuoteExportItem(otherSavedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val document = Loader.loadPDF(pdfBytes)
        assertEquals(1, document.numberOfPages)
        document.close()
    }

    @Test
    fun catalogPdfPaginatesWhenThereAreMoreItemsThanFitOnOnePage() {
        val items = (1..10).map { QuoteExportItem(savedQuote.copy(id = "item-$it", name = "Peça $it"), photoBytes = null) }

        val pdfBytes = renderCatalogPdf(items, watermarkText = null, footerText = null)

        val document = Loader.loadPDF(pdfBytes)
        assertTrue(document.numberOfPages > 1)
        document.close()
    }

    @Test
    fun catalogPdfIncludesWatermarkAndFooterOnEveryPage() {
        val items = (1..10).map { QuoteExportItem(savedQuote.copy(id = "item-$it", name = "Peça $it"), photoBytes = null) }

        val pdfBytes = renderCatalogPdf(items, watermarkText = "Marca", footerText = "Marcenaria 3D do João")

        val document = Loader.loadPDF(pdfBytes)
        assertTrue(document.numberOfPages > 1)
        for (pageIndex in 1..document.numberOfPages) {
            val pageText = PDFTextStripper().apply { startPage = pageIndex; endPage = pageIndex }.getText(document)
            assertTrue(pageText.contains("Marcenaria 3D do João"))
        }
        document.close()
    }

    @Test
    fun catalogPdfWithNoPhotoStillShowsNameAndPrice() {
        val pdfBytes = renderCatalogPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("Suporte de celular"))
        assertTrue(text.contains("16,19"))
    }

    @Test
    fun pdfUsesTheGivenCurrencyInsteadOfBrl() {
        val pdfBytes = renderSavedQuotesPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
            currency = Currency.USD,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("16.19"))
        assertFalse(text.contains("16,19"))
    }

    @Test
    fun catalogPdfUsesTheGivenCurrencyInsteadOfBrl() {
        val pdfBytes = renderCatalogPdf(
            listOf(QuoteExportItem(savedQuote, photoBytes = null)),
            watermarkText = null,
            footerText = null,
            currency = Currency.USD,
        )

        val text = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

        assertTrue(text.contains("16.19"))
        assertFalse(text.contains("16,19"))
    }

    private fun textOf(pdfBytes: ByteArray): String = Loader.loadPDF(pdfBytes).use { PDFTextStripper().getText(it) }

    @Test
    fun deliveryDateIsPrintedWhenPresent() {
        val withDeadline = savedQuote.copy(deliveryDateEpochDay = java.time.LocalDate.of(2026, 9, 30).toEpochDay())

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(withDeadline, null)), null, null))

        assertTrue(text.contains("Prazo de entrega: até 30/09/2026"))
    }

    @Test
    fun withoutDeadlineAndPrintTimeThePdfHasNeitherLine() {
        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, null)), null, null))

        assertFalse(text.contains("Prazo"))
        assertFalse(text.contains("Tempo de impressão"))
    }

    @Test
    fun printTimeAppearsOnlyWhenTurnedOn() {
        val items = listOf(QuoteExportItem(savedQuote, null))

        val withTime = textOf(renderSavedQuotesPdf(items, null, null, options = PdfLayoutOptions(showPrintTime = true)))
        val withoutTime = textOf(renderSavedQuotesPdf(items, null, null))

        assertTrue(withTime.contains("Tempo de impressão: 3 h 10 min"))
        assertFalse(withoutTime.contains("Tempo de impressão"))
    }

    @Test
    fun catalogNeverShowsTheDeliveryDate() {
        // O catálogo é vitrine: prazo é do pedido, não do produto.
        val withDeadline = savedQuote.copy(deliveryDateEpochDay = java.time.LocalDate.of(2026, 9, 30).toEpochDay())

        val text = textOf(renderCatalogPdf(listOf(QuoteExportItem(withDeadline, null)), null, null))

        assertFalse(text.contains("Prazo"))
    }

    /** Logo "de verdade" (PNG com ruído, pra não comprimir a quase nada), no tamanho que um vendedor usaria. */
    private fun noisyLogoPng(size: Int = 400): ByteArray {
        val random = java.util.Random(42)
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until size) for (y in 0 until size) image.setRGB(x, y, random.nextInt())
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test
    fun headerShowsBrandNameAndContactWhenThereIsIdentity() {
        val options = PdfLayoutOptions(brandName = "Minha Loja 3D", contactLines = listOf("WhatsApp (11) 99999-0000", "@minhaloja"))

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, null)), null, null, options = options))

        assertTrue(text.contains("Minha Loja 3D"))
        assertTrue(text.contains("WhatsApp (11) 99999-0000"))
        assertTrue(text.contains("@minhaloja"))
    }

    @Test
    fun brandNameAloneDoesNotCreateAHeader() {
        // Só o nome, sem logo nem contato: continua saindo só na marca d'água/rodapé, como antes.
        val items = listOf(QuoteExportItem(savedQuote, null))

        val withName = textOf(renderSavedQuotesPdf(items, null, null, options = PdfLayoutOptions(brandName = "Minha Loja 3D")))
        val plain = textOf(renderSavedQuotesPdf(items, null, null))

        assertEquals(plain, withName)
    }

    @Test
    fun emojiInTheNameOrContactDoesNotBreakTheExport() {
        val withEmoji = savedQuote.copy(name = "Vaso 🌵 decorativo")
        val options = PdfLayoutOptions(contactLines = listOf("WhatsApp 📱 (11) 99999-0000"))

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(withEmoji, null)), "Loja ✨", "Loja ✨", options = options))

        assertTrue(text.contains("Vaso decorativo"))
        assertTrue(text.contains("WhatsApp (11) 99999-0000"))
    }

    @Test
    fun theLogoIsEmbeddedOnceAndReusedAcrossPages() {
        val logo = noisyLogoPng()
        val options = PdfLayoutOptions(logoBytes = logo)

        val onePage = renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, null)), null, null, options = options)
        val tenPages = renderSavedQuotesPdf(List(10) { QuoteExportItem(savedQuote, null) }, null, null, options = options)

        assertEquals(10, Loader.loadPDF(tenPages).use { it.numberOfPages })
        assertTrue(tenPages.size < onePage.size * 2, "10 páginas com a mesma logo não podem pesar 10 logos: ${tenPages.size} vs ${onePage.size}")
    }

    @Test
    fun anUnreadableLogoIsIgnored() {
        val options = PdfLayoutOptions(logoBytes = byteArrayOf(1, 2, 3), contactLines = listOf("@loja"))

        val text = textOf(renderSavedQuotesPdf(listOf(QuoteExportItem(savedQuote, null)), null, null, options = options))

        assertTrue(text.contains("@loja"))
    }

    @Test
    fun catalogWithHeaderAndBorderStillListsEveryItem() {
        val options = PdfLayoutOptions(logoBytes = noisyLogoPng(64), brandName = "Loja", contactLines = listOf("@loja", "loja@example.com"), showBorder = true)
        val items = List(7) { index -> QuoteExportItem(savedQuote.copy(id = "$index", name = "Peça $index"), null) }

        val text = textOf(renderCatalogPdf(items, null, "Loja", options = options))

        (0 until 7).forEach { assertTrue(text.contains("Peça $it")) }
    }
}
