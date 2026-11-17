package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_blocked_prefix
import _3dreport.composeapp.generated.resources.quote_error_deductions
import _3dreport.composeapp.generated.resources.quote_error_shipping_negative
import _3dreport.composeapp.generated.resources.quote_import_kept_rows_total
import _3dreport.composeapp.generated.resources.quote_import_unsupported_bgcode
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.resolve
import com.threedreport.app.ui.text.uiText
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** Os textos que o ViewModel e o importador escolhem saem dos recursos como escritos: porcentagem, aspas e plural. */
class QuoteTextsTest {

    @Test
    fun percentAndQuotesComeOutAsWritten() = runBlocking {
        assertEquals(
            "A taxa do canal somada ao imposto chega a 100% do valor de venda: não sobra nada pra você. " +
                "Revise a taxa do canal ou o imposto em Configurações.",
            uiText(Res.string.quote_error_deductions).resolve(),
        )
        assertEquals(
            "G-code binário (.bgcode) ainda não é lido. No PrusaSlicer, desligue \"G-code binário\" nas " +
                "configurações da impressora e exporte de novo.",
            uiText(Res.string.quote_import_unsupported_bgcode).resolve(),
        )
    }

    @Test
    fun joinedTextPutsThePrefixBeforeTheReason() = runBlocking {
        val blocked = UiText.Joined(listOf(uiText(Res.string.quote_blocked_prefix), uiText(Res.string.quote_error_shipping_negative)))

        assertEquals("Não salvei: Frete não pode ser negativo.", blocked.resolve())
    }

    @Test
    fun pluralPicksTheFormByQuantity() = runBlocking {
        assertEquals(
            "O G-code usa 3 filamentos, mas não informa o consumo de cada um (12,5 m no total): mantive as suas linhas de filamento como estavam.",
            UiText.Plural(Res.plurals.quote_import_kept_rows_total, 3, listOf(3, "12,5")).resolve(),
        )
    }
}
