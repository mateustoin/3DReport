package com.threedreport.app.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Números como o vendedor digita (decisão 107): "2.700" é dois mil e setecentos, "20%" é vinte. */
class DecimalInputTest {

    @Test
    fun thousandsWithADotAndCentsWithAComma() {
        assertEquals(2700.0, parseDecimal("2.700"))
        assertEquals(2700.5, parseDecimal("2.700,50"))
        assertEquals(1234567.89, parseDecimal("1.234.567,89"))
    }

    @Test
    fun aSingleDotThatIsNotAThousandsGroupIsADecimalPoint() {
        assertEquals(27.5, parseDecimal("27.5"))
        assertEquals(0.17, parseDecimal("0.17"))
    }

    @Test
    fun currencySymbolsPercentAndSpacesAreIgnored() {
        assertEquals(50.0, parseDecimal("R$ 50"))
        assertEquals(20.0, parseDecimal("20%"))
        assertEquals(19.9, parseDecimal(" 19,90 "))
    }

    @Test
    fun measuresReadADotAsDecimalEvenWithThreeDigits() {
        // "1.240" como densidade é 1,24, não mil duzentos e quarenta.
        assertEquals(1.24, parseDecimal("1.240", NumberKind.MEASURE))
        assertEquals(2700.0, parseDecimal("2.700", NumberKind.AMOUNT))
    }

    @Test
    fun negativeNumbersAreReadSoTheFormCanRefuseThemByName() {
        assertEquals(-10.0, parseDecimal("-10"))
    }

    @Test
    fun whatIsNotANumberIsRefused() {
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("Infinity"))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("1.23.4"))
        assertNull(parseDecimal(""))
    }

    @Test
    fun inputTextHasTheCommaAndNoFloatingPointNoise() {
        assertEquals("7", (0.07 * 100).toInputText())
        assertEquals("1,75", 1.75.toInputText())
        assertEquals("2700", 2700.0.toInputText())
        assertEquals("-0,5", (-0.5).toInputText())
    }

    @Test
    fun theHintShowsHowAnAmbiguousNumberWasRead() {
        assertEquals("= 2.700", interpretationHint("2.700", NumberKind.AMOUNT))
        assertNull(interpretationHint("2700", NumberKind.AMOUNT))
    }

    @Test
    fun friendlyErrorsNameTheField() {
        val error = runCatching { "-3".toRequiredPositive("Densidade", NumberKind.MEASURE) }.exceptionOrNull()
        assertEquals("\"Densidade\" precisa ser maior que zero.", error?.message)
    }
}
