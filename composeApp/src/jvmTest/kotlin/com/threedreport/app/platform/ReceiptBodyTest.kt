package com.threedreport.app.platform

import com.threedreport.core.model.PaymentMethod
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A forma de pagamento dentro da frase do recibo vem de um recurso próprio, nos dois idiomas: em português, como antes
 * ("(Cartão)"); em inglês, no meio da frase ("by card", "by another method"). Nada é posto em minúscula no código.
 */
class ReceiptBodyTest {

    private val original = Locale.getDefault()

    @AfterTest
    fun backToPortuguese() {
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        Locale.setDefault(original)
    }

    private fun body(method: PaymentMethod, client: String? = "Maria"): String {
        val texts = runBlocking { loadClientDocumentTexts(pieceCounts = listOf(1)) }
        return texts.receiptBody(client, "R$ 50,00", method, "17/11/2026", orderNumber = null, piece = "Suporte")
    }

    @Test
    fun inPortugueseTheMethodIsWrittenAsBefore() {
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        assertEquals("Recebi de Maria o valor de R$ 50,00 (Pix) em 17/11/2026, referente a Suporte.", body(PaymentMethod.PIX))
        assertEquals("Recebi o valor de R$ 50,00 (Cartão) em 17/11/2026, referente a Suporte.", body(PaymentMethod.CARD, client = null))
        assertEquals("Recebi de Maria o valor de R$ 50,00 (Dinheiro) em 17/11/2026, referente a Suporte.", body(PaymentMethod.CASH))
        assertEquals("Recebi de Maria o valor de R$ 50,00 (Outro) em 17/11/2026, referente a Suporte.", body(PaymentMethod.OTHER))
    }

    @Test
    fun inEnglishTheMethodFitsTheMiddleOfTheSentence() {
        DesktopLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))
        assertEquals(true, body(PaymentMethod.CARD).contains(" by card on "), body(PaymentMethod.CARD))
        assertEquals(true, body(PaymentMethod.CASH).contains(" by cash on "), body(PaymentMethod.CASH))
        assertEquals(true, body(PaymentMethod.OTHER).contains(" by another method on "), body(PaymentMethod.OTHER))
        assertEquals(true, body(PaymentMethod.PIX).contains(" by Pix on "), body(PaymentMethod.PIX))
    }
}
