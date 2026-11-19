package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.PdfLayoutOptions
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.renderCatalogPdf
import com.threedreport.app.platform.renderQuoteImage
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.testClientTexts
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.ContactChannel
import com.threedreport.core.model.ContactEntry
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.ImageBackground
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Caracterização do que o cliente recebe (Leva 10B, parte 7): o texto do PDF, do catálogo, da mensagem e da
 * imagem de um orçamento de exemplo. Os textos saíram do código pros recursos sem mudar um caractere.
 */
class ClientDocumentTextTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        dataDir.deleteRecursively()
        System.clearProperty("threedreport.dataDir")
    }

    private fun print(name: String, minutes: Double) = QuotedPrint(
        job = PrintJob(
            filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = minutes,
        ).copy(name = name),
        printerId = "printer",
        printerName = "Impressora",
        cost = PrintCost(material = 3.58, energy = 1.48, maintenance = 0.54, finishing = 0.36, investmentReturn = 1.78, fixedCost = 0.0),
    )

    private val costs = CostBreakdown(
        material = 3.58, energy = 1.48, maintenance = 0.54, failures = 0.36, finishing = 0.36,
        investmentReturn = 1.78, administrative = 0.0, labor = 0.0, fixedCost = 0.0,
    )

    /** Pedido com tudo: kit de duas peças, três unidades, serviços por peça e por pedido, frete, prazo e entrega expressa. */
    private val fullOrder = SavedQuote(
        id = "1",
        name = "Suporte de celular",
        quote = Quote(prints = listOf(print("Base", 190.0), print("Braço", 45.0)), costs = costs, salePrice = 48.57, quantity = 3, rush = true),
        services = listOf(
            QuoteService(id = "p", name = "Pintura", price = 5.0, chargedPerOrder = false),
            QuoteService(id = "e", name = "Embalagem", price = 3.0, chargedPerOrder = true),
        ),
        savedAtEpochMillis = 0L,
        client = Client(name = "Maria Cliente"),
        number = 42,
        shippingCost = 12.0,
        deliveryDateEpochDay = 20_361L,
    )

    /** Produto de nome automático, frete grátis e entrega expressa sem prazo. */
    private val autoProduct = SavedQuote(
        id = "2",
        name = "Orçamento - 24/09/2026 14:30",
        autoNamed = true,
        quote = Quote(prints = listOf(print("", 60.0)), costs = costs, salePrice = 16.19, rush = true),
        savedAtEpochMillis = 0L,
        kind = QuoteKind.PRODUCT,
        shippingCost = 10.0,
        shippingAbsorbed = true,
        category = "Chaveiros",
    )

    private val options = PdfLayoutOptions(
        showPrintTime = true,
        brandName = "Minha Loja",
        contacts = listOf(ContactEntry(ContactChannel.EMAIL, "(11) 99999-0000")),
        showAppSignature = true,
        validityDays = 7,
        issuedOnEpochDay = 20_356L,
        showClientName = true,
    )

    private fun textOf(pdf: ByteArray) = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }

    private fun exporter(platform: FakePlatform, branding: BrandingRepository = BrandingRepository()) = QuoteExporter(
        platform, branding, QuoteHistoryRepository(),
        CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, Dispatchers.Unconfined,
    ) {}

    /**
     * O desenho do PDF mudou (Leva 10B, parte 7): rótulo e valor viraram texto separado, em fontes e lugares diferentes
     * (o preço no bloco, a entrega e a validade ao lado). O que o cliente lê continua o mesmo, então a conferência é
     * por conjunto de linhas, página a página: cada linha esperada tem de estar na página. Os rótulos soltos do bloco
     * saem em maiúsculas, sem os dois-pontos que a linha única tinha ("Valor: R$ 16,19" virou "Valor" e "R$ 16,19").
     */
    @Test
    fun theQuotePdfStillSaysEverythingItSaidBefore() {
        val items = listOf(QuoteExportItem(fullOrder, null), QuoteExportItem(autoProduct, null))

        val pdf = renderSavedQuotesPdf(items, testClientTexts, "Minha Loja", "Minha Loja", options)

        val first = listOf(
            "Minha Loja (11) 99999-0000",
            "Pedido #0042 · Emitido em 25/09/2025",
            "Suporte de celular",
            "Para: Maria Cliente",
            "Kit com 2 peças",
            "Valor 3 × R$ 16,19 = R$ 48,57",
            "Pintura (× 3) R$ 15,00",
            "Embalagem R$ 3,00",
            "Frete R$ 12,00",
            "Tempo de impressão 11 h 45 min (3 peças)",
            "R$ 78,57",
            "TOTAL",
            "ENTREGA ATÉ",
            "30/09/2025",
            "Entrega expressa",
            "VÁLIDO ATÉ",
            "02/10/2025",
            "Minha Loja Gerado com 3DReport",
        )
        val second = listOf(
            "Minha Loja (11) 99999-0000",
            "Produto",
            "Emitido em 25/09/2025",
            "Valor R$ 16,19",
            "Frete grátis",
            "Tempo de impressão 1 h",
            "R$ 16,19",
            "TOTAL",
            "Entrega expressa",
            "Minha Loja Gerado com 3DReport",
        )
        assertPageHas(pdf, 1, first)
        assertPageHas(pdf, 2, second)
        // O que saiu do PDF: o aviso de prazo e a validade na linha do pedido.
        assertTrue("Prazo de entrega" !in textOf(pdf))
        assertTrue("Pedido #0042 · Emitido em 25/09/2025 · Válido" !in textOf(pdf))
    }

    private fun assertPageHas(pdf: ByteArray, page: Int, expected: List<String>) {
        val text = Loader.loadPDF(pdf).use { PDFTextStripper().apply { startPage = page; endPage = page }.getText(it) }
        val missing = expected.filter { it !in text }
        assertTrue(missing.isEmpty(), "faltou na página $page: $missing\n$text")
    }

    @Test
    fun theCatalogPdfSaysExactlyWhatItSaidBefore() {
        val items = listOf(
            QuoteExportItem(fullOrder, null),
            QuoteExportItem(autoProduct, null),
            QuoteExportItem(fullOrder.copy(id = "3", kind = QuoteKind.PRODUCT), null),
        )

        val text = textOf(renderCatalogPdf(items, testClientTexts, null, null, options))

        val page = listOf(
            "Minha Loja (11) 99999-0000",
            "Catálogo de produtos",
            "Chaveiros",
            "Produto",
            "R$ 16,19",
            "Outros",
            "Suporte de celular",
            "R$ 78,57",
            "Suporte de celular",
            "R$ 78,57",
            "Gerado com 3DReport",
        )
        // O PDFBox quebra a linha com o separador do sistema (CRLF no Windows): confere o texto, não o separador.
        assertEquals(page.joinToString("\n", postfix = "\n"), text.replace("\r\n", "\n"))
    }

    @Test
    fun theMessageSaysExactlyWhatItSaidBefore() {
        assertEquals(
            listOf(
                "Suporte de celular · #0042",
                "Valor: 3 × R$ 16,19 = R$ 48,57",
                "Pintura (× 3): R$ 15,00",
                "Embalagem: R$ 3,00",
                "Frete: R$ 12,00",
                "Total: R$ 78,57",
                "Prazo de entrega: até 30/09/2025 (entrega expressa)",
                "Tempo de impressão: 11 h 45 min (3 peças)",
            ).joinToString("\n"),
            fullOrder.toCopyPasteText(testClientTexts, showPrintTime = true),
        )
        assertEquals(
            "Produto\nValor: R$ 16,19\nFrete: grátis\nTotal: R$ 16,19\nEntrega expressa\nTempo de impressão: 1 h",
            autoProduct.toCopyPasteText(testClientTexts, showPrintTime = true),
        )
        assertEquals(
            "Orçamento #0007\nValor: R$ 16,19\nFrete: grátis\nTotal: R$ 16,19\nEntrega expressa",
            autoProduct.copy(kind = QuoteKind.ORDER, number = 7).toCopyPasteText(testClientTexts),
        )
    }

    /**
     * A imagem do exportador é, byte a byte, a que sai desenhando à mão os textos de antes: título, preço, preço por
     * unidade e prazo. A assinatura "Gerado com 3DReport", desenhada dentro da imagem, foi conferida comparando os
     * arquivos gerados antes e depois.
     */
    @Test
    fun theImageDrawsExactlyTheTextsItDrewBefore() {
        val platform = FakePlatform().apply { nextSaveLocation = "/tmp/" }
        val exporter = exporter(platform)

        exporter.saveShareableImage(fullOrder)
        platform.awaitWritten(1)
        exporter.saveShareableImage(autoProduct)
        platform.awaitWritten(2)

        val full = renderQuoteImage(
            texts = testClientTexts,
            title = "Suporte de celular",
            priceText = "R$ 78,57",
            unitPriceText = "3 peças · R$ 22,19 cada",
            photoBytes = null,
            brandText = null,
            deliveryText = "Entrega até 30/09 · entrega expressa",
        )
        val auto = renderQuoteImage(
            texts = testClientTexts,
            title = "Orçamento",
            priceText = "R$ 16,19",
            unitPriceText = null,
            photoBytes = null,
            brandText = null,
            deliveryText = "Entrega expressa",
        )
        assertContentEquals(full, platform.written.getValue("/tmp/Suporte de celular.png"))
        assertContentEquals(auto, platform.written.getValue("/tmp/Orçamento.png"))
    }

    /** O fundo escolhido em Documentos pro cliente chega na imagem que o Histórico exporta. */
    @Test
    fun theExportedImageUsesTheBackgroundChosenInSettings() {
        fun cornerOf(background: ImageBackground): Int {
            val branding = BrandingRepository().apply { update(BrandingSettings(imageBackground = background)) }
            val platform = FakePlatform().apply { nextSaveLocation = "/tmp/" }
            exporter(platform, branding).saveShareableImage(autoProduct)
            platform.awaitWritten(1)
            return ImageIO.read(ByteArrayInputStream(platform.written.getValue("/tmp/Orçamento.png"))).getRGB(4, 4) and 0xFFFFFF
        }

        assertEquals(0xF6F8FB, cornerOf(ImageBackground.LIGHT))
        assertEquals(0x0C2742, cornerOf(ImageBackground.DARK))
        assertEquals(0x0C2742, cornerOf(ImageBackground.AUTO), "sem foto, o automático é escuro")
    }

    @Test
    fun piecesUseTheRealPluralForm() {
        assertEquals("1 peça", testClientTexts.pieces(1))
        assertEquals("2 peças", testClientTexts.pieces(2))
        assertEquals("Gerado com 3DReport", testClientTexts.signatureLine("3DReport"))
        assertEquals(Triple("Gerado com ", "3DReport", ""), testClientTexts.signature("3DReport"))
    }
}
