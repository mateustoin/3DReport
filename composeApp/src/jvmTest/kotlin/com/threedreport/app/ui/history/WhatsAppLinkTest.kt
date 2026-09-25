package com.threedreport.app.ui.history

import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhatsAppLinkTest {

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
    )

    @Test
    fun brazilianPhoneWithAreaCodeGetsTheCountryCode() {
        assertEquals("5511999990000", toInternationalPhone("(11) 99999-0000"))
        assertEquals("551133334444", toInternationalPhone("11 3333-4444"))
    }

    @Test
    fun numberThatAlreadyHasACountryCodeIsKeptAsIs() {
        assertEquals("5511999990000", toInternationalPhone("+55 (11) 99999-0000"))
    }

    @Test
    fun contactWithoutARecognizablePhoneHasNoNumber() {
        assertNull(toInternationalPhone("maria@example.com"))
        assertNull(toInternationalPhone("@maria.3d"))
        assertNull(toInternationalPhone(""))
    }

    @Test
    fun linkCarriesThePhoneAndTheQuoteText() {
        val withPhone = savedQuote.copy(client = Client(name = "Maria", contact = "(11) 99999-0000"))

        val link = withPhone.toWhatsAppLink()

        assertTrue(link.startsWith("https://wa.me/5511999990000?text="), "link gerado: $link")
        // Acento e quebra de linha precisam ir codificados, senão o link quebra no meio do texto.
        assertTrue(link.contains("Suporte"))
        assertTrue(link.contains("%0A"), "a quebra de linha precisa ser codificada")
        assertFalse(link.contains(" "), "não pode sobrar espaço cru na URL")
    }

    @Test
    fun withoutAPhoneTheLinkStillCarriesTheTextSoWhatsAppAsksWhoToSendTo() {
        val link = savedQuote.copy(client = Client(name = "Maria", contact = "maria@example.com")).toWhatsAppLink()

        assertTrue(link.startsWith("https://wa.me/?text="), "link gerado: $link")
        assertTrue(link.contains("Suporte"))
    }

    @Test
    fun accentedTextIsEncodedAsUtf8() {
        // "Orçamento" tem cedilha: em UTF-8, o "ç" vira %C3%A7.
        val link = savedQuote.copy(name = "Orçamento").toWhatsAppLink()

        assertTrue(link.contains("Or%C3%A7amento"), "link gerado: $link")
    }
}
