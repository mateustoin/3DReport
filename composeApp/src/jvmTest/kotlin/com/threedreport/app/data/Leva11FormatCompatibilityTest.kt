package com.threedreport.app.data

import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PaymentStatus
import com.threedreport.core.model.SavedQuote
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Dado gravado pela 2.2.1 (sem os campos da Leva 11) abre sem migração, com tudo neutro, e grava de volta sem perder
 * nada: o formato da pasta continua 2 ([DATA_FORMAT_VERSION]).
 */
class Leva11FormatCompatibilityTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    /** Um pedido como a 2.2.1 grava no `quotes.json`: registro carimbado, sem as chaves novas. */
    private val quotesOf221 = """
        [
          {"id":"q1","createdAtEpochMillis":1000,"updatedAtEpochMillis":2000,"deletedAtEpochMillis":null,
           "data":{"id":"q1","name":"Vaso","savedAtEpochMillis":1000,"autoNamed":false,"number":42,"status":"approved",
             "statusHistory":[{"status":"quoted","atEpochMillis":1000},{"status":"approved","atEpochMillis":2000}],
             "client":{"name":"Maria","contact":"(11) 99999-0000","id":"c1"},
             "shippingCost":12.0,"shippingAbsorbed":false,"deliveryDateEpochDay":20500,"currency":"BRL",
             "services":[{"id":"s1","name":"Pintura","price":10.0,"chargedPerOrder":true}],
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":23.0}}}
        ]
    """.trimIndent()

    @Test
    fun anOrderOf221OpensWithNothingOfPayment() {
        File(dataDir, "quotes.json").writeText(quotesOf221)

        val saved = QuoteHistoryRepository().savedQuotes.value.single()

        assertEquals(emptyList(), saved.payments)
        assertEquals(0.0, saved.depositAmount)
        assertNull(saved.trackingCode)
        assertNull(saved.notes)
        assertNull(saved.lastFollowUpEpochMillis)
        assertFalse(saved.hasPaymentTracking, "pedido antigo fica sem selo de pagamento")
        assertEquals(PaymentStatus.NOT_PAID, saved.paymentStatus)
        // "A receber" de um pedido antigo continua o total, como antes.
        assertEquals(23.0 + 10.0 + 12.0, saved.remaining, 1e-9)
        assertEquals(saved.totalWithServices, saved.remaining, 1e-9)
    }

    @Test
    fun anOrderOf221IsWrittenBackWithoutLosingAnything() {
        File(dataDir, "quotes.json").writeText(quotesOf221)
        val original = QuoteHistoryRepository().savedQuotes.value.single()

        // Uma mudança qualquer regrava o arquivo inteiro.
        QuoteHistoryRepository().addPayment(original.id, amount = 10.0, method = PaymentMethod.PIX, epochDay = 20_450)

        val text = File(dataDir, "quotes.json").readText()
        val rewritten = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(original, rewritten.copy(payments = emptyList()))
        assertEquals(42, rewritten.number)
        assertEquals(OrderStatus.APROVADO, rewritten.status)
        assertEquals("Maria", rewritten.client?.name)
        assertTrue(text.contains("\"payments\""), text)
        assertTrue(text.contains("\"method\": \"pix\""), text)
        assertEquals(DATA_FORMAT_VERSION, 2)
    }

    @Test
    fun theNewKeysAreReadBackAsWritten() {
        val json = """
            {"id":"1","name":"Peça","savedAtEpochMillis":0,"depositAmount":50.0,"trackingCode":"AA123456789BR","notes":"interno",
             "lastFollowUpEpochMillis":5,"payments":[{"id":"p1","amount":50.0,"method":"card","epochDay":20400}],
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":100.0}}
        """.trimIndent()

        val saved = dataJson.decodeFromString(SavedQuote.serializer(), json)

        assertEquals(PaymentMethod.CARD, saved.payments.single().method)
        assertEquals(PaymentStatus.DEPOSIT_PAID, saved.paymentStatus)
        assertEquals(saved, dataJson.decodeFromString(SavedQuote.serializer(), dataJson.encodeToString(SavedQuote.serializer(), saved)))
    }

    @Test
    fun theBrandingOf221HasNoPix() {
        val branding = dataJson.decodeFromString(
            BrandingSettings.serializer(),
            """{"brandName":"Ateliê","showWatermark":true,"showFooter":true,"showPrintTime":false,"showBorder":false,"quoteValidityDays":7}""",
        )

        assertNull(branding.pixKey)
        assertNull(branding.pixReceiver)
        assertEquals(emptyMap(), branding.messageTemplates)
    }

    @Test
    fun aBackupCarriesPaymentsAndTheNewSettings() {
        val repository = QuoteHistoryRepository()
        val quote = dataJson.decodeFromString(SavedQuote.serializer(), sampleQuoteJson).quote
        val saved = repository.save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        repository.addPayment(saved.id, 10.0, PaymentMethod.CASH, 20_400)
        repository.updateDetails(saved.id, QuoteDetails("Vaso", null, null, null, null, null, trackingCode = "AA1", notes = "nota"))
        OrderSettingsRepository().update(com.threedreport.core.model.OrderSettings(defaultDepositRate = 0.3, staleQuoteDays = 5))
        BrandingRepository().update(BrandingSettings(pixKey = "a@b.co", pixReceiverName = "Ana", pixCity = "SP"))
        val backup = BackupRepository().createBackupZip()

        // Estraga tudo e restaura.
        File(dataDir, "quotes.json").delete()
        File(dataDir, "order-settings.json").delete()
        File(dataDir, "branding.json").delete()
        assertIs<RestoreResult.Success>(BackupRepository().restoreFromZip(backup))

        val restored = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(10.0, restored.paidTotal)
        assertEquals("AA1", restored.trackingCode)
        assertEquals("nota", restored.notes)
        assertEquals(5, OrderSettingsRepository().settings.value.staleQuoteDays)
        assertEquals("a@b.co", BrandingRepository().branding.value.pixKey)
    }

    private val sampleQuoteJson = """
        {"id":"x","name":"x","savedAtEpochMillis":0,
         "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                  "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                  "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                  "salePrice":100.0}}
    """.trimIndent()
}
