package com.threedreport.core.pix

import com.threedreport.core.costsOf
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dados de quem recebe o Pix, em Configurações, e o código de um pedido (decisão 145, itens 1 a 3). */
class PixReceiverTest {

    private val complete = BrandingSettings(pixKey = "(11) 99999-0000", pixReceiverName = "Ateliê da Ana", pixCity = "São Paulo")

    @Test
    fun withoutAnythingEverythingIsMissing() {
        val branding = BrandingSettings()

        assertNull(branding.pixReceiver)
        assertEquals(setOf(PixSetupIssue.KEY_MISSING, PixSetupIssue.NAME_MISSING, PixSetupIssue.CITY_MISSING), branding.pixSetupIssues)
    }

    @Test
    fun nameAndCityAreRequired() {
        assertEquals(setOf(PixSetupIssue.NAME_MISSING), complete.copy(pixReceiverName = " ").pixSetupIssues)
        assertEquals(setOf(PixSetupIssue.CITY_MISSING), complete.copy(pixCity = null).pixSetupIssues)
        assertNull(complete.copy(pixCity = null).pixReceiver)
        // Nome só com o que sai na limpeza (símbolos) também falta.
        assertEquals(setOf(PixSetupIssue.NAME_MISSING), complete.copy(pixReceiverName = "★★").pixSetupIssues)
    }

    @Test
    fun aKeyWithAccentIsAnIssueAndGivesNoPix() {
        val branding = complete.copy(pixKey = "joão@exemplo.com")

        assertEquals(setOf(PixSetupIssue.KEY_INVALID_CHARACTERS), branding.pixSetupIssues)
        assertNull(branding.pixReceiver)
    }

    @Test
    fun aKeyTooLongIsAnIssue() {
        assertEquals(setOf(PixSetupIssue.KEY_TOO_LONG), complete.copy(pixKey = "a".repeat(70) + "@exemplo.com").pixSetupIssues)
    }

    @Test
    fun aCompleteSetupGivesTheReceiverWithTheCleanValues() {
        val receiver = complete.pixReceiver!!

        assertEquals(PixKey("+5511999990000", PixKeyType.PHONE), receiver.key)
        assertEquals("ATELIE DA ANA", receiver.name)
        assertEquals("SAO PAULO", receiver.city)
        assertTrue(complete.pixSetupIssues.isEmpty())
    }

    private fun order(deposit: Double = 0.0, paid: Double? = null, currency: Currency = Currency.BRL) = SavedQuote(
        id = "1",
        name = "Vaso",
        quote = Quote(
            prints = listOf(
                quotedPrint(
                    PrintJob(
                        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                        filamentLengthMeters = 1.0,
                        printTimeMinutes = 60.0,
                    ),
                ),
            ),
            costs = costsOf(),
            salePrice = 174.34,
        ),
        savedAtEpochMillis = 0L,
        number = 42,
        depositAmount = deposit,
        payments = listOfNotNull(paid?.let { Payment("p", it, PaymentMethod.PIX, 0L) }),
        currency = currency,
    )

    @Test
    fun theCodeOfAnOrderChargesWhatIsDue() {
        val receiver = complete.pixReceiver!!

        val code = receiver.codeFor(order(deposit = 50.0))!!
        assertTrue(code.contains("540550.00"), code)
        assertTrue(code.contains("3DR0042"), code)
        assertTrue(code.contains("5913ATELIE DA ANA"), code)
        assertTrue(code.contains("0114+5511999990000"), code)

        assertTrue(receiver.codeFor(order())!!.contains("5406174.34"))
    }

    @Test
    fun noCodeForAPaidOrderOrAnotherCurrency() {
        val receiver = complete.pixReceiver!!

        assertNull(receiver.codeFor(order(paid = 174.34)))
        assertNull(receiver.codeFor(order(currency = Currency.EUR)))
    }
}
