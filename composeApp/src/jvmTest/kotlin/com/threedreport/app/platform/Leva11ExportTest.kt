package com.threedreport.app.platform

import com.threedreport.app.ui.history.toCopyPasteText
import com.threedreport.app.ui.history.toWhatsAppLink
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * O que a Leva 11 acrescenta aos documentos do cliente: a linha do sinal, o Pix copia e cola no fim da mensagem, o
 * bloco "Pague com Pix" do PDF (nunca cortado, nunca no catálogo) e o recibo.
 */
class Leva11ExportTest {

    private val branding = BrandingSettings(brandName = "Sua Loja", pixKey = "loja@example.com", pixReceiverName = "Sua Loja", pixCity = "Sao Paulo")
    private val options = branding.resolvePdfBranding(null).options.copy(issuedOnEpochDay = 20_000L)

    private fun order(deposit: Double = 0.0, payments: List<Payment> = emptyList(), services: Int = 0) = SavedQuote(
        id = "1",
        name = "Suporte de celular",
        number = 42,
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
            salePrice = 174.34,
        ),
        savedAtEpochMillis = 0L,
        client = Client(name = "Maria", contact = "(11) 99999-0000"),
        depositAmount = deposit,
        payments = payments,
        notes = "OBSERVACAO INTERNA",
        trackingCode = "AB123456789BR",
        services = List(services) { QuoteService(id = "s$it", name = "Serviço $it", price = 1.0, chargedPerOrder = true) },
    )

    private fun pdf(order: SavedQuote, opts: PdfLayoutOptions = options) =
        renderSavedQuotesPdf(listOf(QuoteExportItem(order, null)), testClientTexts, "Sua Loja", "Sua Loja", opts)

    private fun pages(pdf: ByteArray): List<String> = Loader.loadPDF(pdf).use { document ->
        (1..document.numberOfPages).map { page -> PDFTextStripper().apply { startPage = page; endPage = page }.getText(document) }
    }

    private fun compact(text: String) = text.filterNot { it.isWhitespace() }

    // ---- mensagem

    @Test
    fun theMessageWithADepositSaysItAfterTheTotalAndEndsWithThePix() {
        val order = order(deposit = 50.0)
        val code = branding.pixReceiver!!.codeFor(order)!!

        val lines = order.toCopyPasteText(testClientTexts, pixCode = code).lines()

        assertEquals("Suporte de celular · #0042", lines[0])
        assertEquals("Valor: R$ 174,34", lines[1])
        assertEquals("Sinal de R$ 50,00 pra começar", lines[2])
        assertEquals("", lines[3])
        assertEquals("Pix copia e cola (R$ 50,00):", lines[4])
        assertEquals(code, lines[5], "o código vai sozinho na linha")
        assertEquals(6, lines.size)
    }

    @Test
    fun thePixAmountIsWhatIsMissingAfterTheDepositIsPaid() {
        val paid = order(deposit = 50.0, payments = listOf(Payment("p", 50.0, PaymentMethod.PIX, 1L)))
        val code = branding.pixReceiver!!.codeFor(paid)!!

        val text = paid.toCopyPasteText(testClientTexts, pixCode = code)

        assertTrue("Pix copia e cola (R$ 124,34):" in text, text)
        assertFalse("pra começar" in text, "depois do sinal pago a linha some")
        assertFalse("pra começar" in pages(pdf(paid)).single(), "e some do PDF também")
    }

    @Test
    fun withoutAKeyADepositOrPixTheMessageIsAsBefore() {
        val plain = order()

        assertEquals("Suporte de celular · #0042\nValor: R$ 174,34", plain.toCopyPasteText(testClientTexts))
    }

    @Test
    fun aPaidOrderHasNoPixLineEvenWithAKey() {
        val paid = order(payments = listOf(Payment("p", 174.34, PaymentMethod.PIX, 1L)))

        assertFalse("Pix" in paid.toCopyPasteText(testClientTexts, pixCode = "000201"))
        assertNull(branding.pixReceiver!!.codeFor(paid))
    }

    @Test
    fun theWhatsAppLinkCarriesTheDepositLineAndThePix() {
        val order = order(deposit = 50.0)
        val code = branding.pixReceiver!!.codeFor(order)!!

        val link = order.toWhatsAppLink(testClientTexts, pixCode = code)

        assertTrue(link.startsWith("https://wa.me/5511999990000?text="))
        assertTrue("Sinal%20de%20R%24%2050%2C00%20pra%20come%C3%A7ar" in link, link)
        assertTrue(code in java.net.URLDecoder.decode(link.substringAfter("text="), "UTF-8"), "o código inteiro entra no link")
    }

    @Test
    fun internalNotesAndTrackingNeverLeaveInAnyDocument() {
        val order = order(deposit = 50.0)
        val code = branding.pixReceiver!!.codeFor(order)!!
        val everything = order.toCopyPasteText(testClientTexts, pixCode = code) + pages(pdf(order)).joinToString("")

        assertFalse("OBSERVACAO" in everything)
        assertFalse("AB123456789BR" in everything)
    }

    // ---- PDF do orçamento

    @Test
    fun theQuotePdfShowsThePixBlockWithAmountKeyAndCode() {
        val order = order(deposit = 50.0)
        val code = branding.pixReceiver!!.codeFor(order)!!

        val text = pages(pdf(order)).single()

        assertTrue("PAGUE COM PIX" in text.uppercase() || "Pague com Pix" in text, text)
        assertTrue("Sinal: R$ 50,00" in text, text)
        assertTrue("Sinal de R$ 50,00 pra começar" in text, text)
        assertTrue("Chave: loja@example.com" in text, text)
        assertTrue(compact(code) in compact(text), "o copia e cola inteiro está no PDF")
    }

    @Test
    fun withoutADepositThePixBlockShowsTheValue() {
        val text = pages(pdf(order())).single()

        assertTrue("Valor: R$ 174,34" in text, text)
        assertFalse("pra começar" in text)
    }

    @Test
    fun withoutAKeyThePdfHasNoPixAndNoOtherChange() {
        val order = order()
        val without = pages(pdf(order, options.copy(pixReceiver = null))).single()

        assertFalse("Pix" in without)
        assertFalse("PIX" in without)
    }

    @Test
    fun withoutADepositAndWithoutAKeyThePdfTextIsTheSameAsBefore() {
        val plain = order()
        val before = pages(pdf(plain, options.copy(pixReceiver = null))).single()

        assertEquals(before, pages(pdf(plain, branding.copy(pixKey = null).resolvePdfBranding(null).options.copy(issuedOnEpochDay = 20_000L))).single())
    }

    @Test
    fun aForeignCurrencyOrderNeverShowsPix() {
        val usd = order().copy(currency = Currency.USD)

        assertFalse("PIX" in pages(pdf(usd)).single().uppercase())
    }

    @Test
    fun theCatalogNeverShowsPix() {
        val product = order(deposit = 50.0).copy(kind = QuoteKind.PRODUCT)
        val catalog = renderCatalogPdf(listOf(QuoteExportItem(product, null)), testClientTexts, "Sua Loja", "Sua Loja", options)

        assertFalse("PIX" in pages(catalog).joinToString("").uppercase())
    }

    @Test
    fun thePixBlockIsNeverCutBetweenPages() {
        // Com muitas linhas de serviço o preço desce até o fim da página: o bloco do Pix vai inteiro pra próxima ou fica inteiro.
        for (services in listOf(0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)) {
            val order = order(deposit = 50.0, services = services)
            val code = branding.pixReceiver!!.codeFor(order)!!
            val pages = pages(pdf(order))
            val withBlock = pages.filter { "Pague com Pix" in it || "PAGUE COM PIX" in it }

            assertEquals(1, withBlock.size, "$services serviços: o bloco aparece uma vez")
            assertTrue(compact(code) in compact(withBlock.single()), "$services serviços: o código está inteiro na mesma página do bloco")
            assertTrue("Chave: loja@example.com" in withBlock.single(), "$services serviços: a chave está na mesma página")
        }
    }

    // ---- recibo

    private fun receipt(order: SavedQuote, payment: Payment) =
        renderReceiptPdf(ReceiptData(order, payment, order.receiptNumber(payment.id)), testClientTexts, "Sua Loja", "Sua Loja", options)

    @Test
    fun theReceiptSaysWhoPaidHowMuchAndWhatIsLeft() {
        val payment = Payment("p1", 50.0, PaymentMethod.PIX, 20_043L, number = 1)
        val order = order(payments = listOf(payment))

        val pages = pages(receipt(order, payment))
        assertEquals(1, pages.size)
        val text = pages.single().replace("\n", " ")

        assertTrue("Recibo #0042-1" in text, text)
        assertTrue("Recebi de Maria o valor de R$ 50,00 (Pix) em " in text, text)
        assertTrue("referente ao pedido #0042, Suporte de celular." in text, text)
        assertTrue("Total do pedido" in text && "R$ 174,34" in text, text)
        assertTrue("Pago até agora" in text && "Falta pagar" in text && "R$ 124,34" in text, text)
        assertTrue("Sua Loja" in text)
        assertTrue("VALOR RECEBIDO" in text.uppercase() && "Emitido em" in text, text)
        assertTrue(Regex("Sua Loja").findAll(text).count() <= 2, "o nome da marca no máximo duas vezes: $text")
    }

    @Test
    fun theReceiptWithoutAClientSaysReceivedTheValue() {
        val payment = Payment("p1", 20.0, PaymentMethod.CASH, 20_043L, number = 3)
        val order = order(payments = listOf(payment)).copy(client = null)

        val text = pages(receipt(order, payment)).single().replace("\n", " ")

        assertTrue("Recibo #0042-3" in text, text)
        assertTrue("Recebi o valor de R$ 20,00 (Dinheiro) em " in text, text)
    }

    @Test
    fun theReceiptNeverCarriesInternalData() {
        val payment = Payment("p1", 50.0, PaymentMethod.PIX, 20_043L, number = 1)
        val order = order(payments = listOf(payment))

        val text = pages(receipt(order, payment)).joinToString("")

        assertFalse("OBSERVACAO" in text)
        assertFalse("AB123456789BR" in text)
        assertFalse("8,09" in text, "custo de produção")
        assertNotNull(order.client)
    }

    @Test
    fun aReceiptNumberStaysAfterAnEarlierPaymentIsDeleted() {
        val second = Payment("p2", 10.0, PaymentMethod.PIX, 20_043L, number = 2)
        val order = order(payments = listOf(second))

        assertEquals("#0042-2", order.receiptNumber("p2"))
        assertEquals(OrderStatus.ORCADO, order.status)
    }

    private fun assertNull(value: Any?) = kotlin.test.assertNull(value)
}
