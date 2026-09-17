package com.threedreport.app.ui.format

import com.threedreport.core.model.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun brlUsesCommaDecimalAndPeriodThousands() {
        assertEquals("R$ 1.234,56", 1234.56.toCurrencyText(Currency.BRL))
    }

    @Test
    fun usdUsesPeriodDecimalAndCommaThousands() {
        assertEquals("$ 1,234.56", 1234.56.toCurrencyText(Currency.USD))
    }

    @Test
    fun eurUsesCommaDecimalAndPeriodThousands() {
        assertEquals("€ 1.234,56", 1234.56.toCurrencyText(Currency.EUR))
    }

    @Test
    fun gbpUsesPeriodDecimalAndCommaThousands() {
        assertEquals("£ 1,234.56", 1234.56.toCurrencyText(Currency.GBP))
    }

    @Test
    fun valuesUnderAThousandHaveNoGroupingSeparator() {
        assertEquals("R$ 12,40", 12.4.toCurrencyText(Currency.BRL))
    }

    @Test
    fun negativeValuesKeepTheMinusSignBeforeTheSymbol() {
        assertEquals("-R$ 12,40", (-12.4).toCurrencyText(Currency.BRL))
    }

    @Test
    fun roundsToTwoDecimalPlaces() {
        assertEquals("R$ 0,01", 0.006.toCurrencyText(Currency.BRL))
    }

    @Test
    fun defaultsToBrlWhenNoCurrencyIsGiven() {
        assertEquals("R$ 12,40", 12.4.toCurrencyText())
    }

    @Test
    fun groupsMillionsWithMultipleSeparators() {
        assertEquals("R$ 1.234.567,89", 1234567.89.toCurrencyText(Currency.BRL))
    }
}
