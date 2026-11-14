package com.threedreport.core.pricing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** "Arredondar pra R$ 18,90" do preço anunciado (decisão 102). */
class ShowcasePriceTest {

    @Test
    fun roundsUpToTheNextPriceEndingInNinety() {
        assertEquals(18.90, showcasePriceSuggestion(18.37)!!, 1e-9)
        assertEquals(19.90, showcasePriceSuggestion(18.95)!!, 1e-9)
        assertEquals(0.90, showcasePriceSuggestion(0.10)!!, 1e-9)
        assertEquals(18.90, showcasePriceSuggestion(18.00)!!, 1e-9)
    }

    @Test
    fun aPriceAlreadyEndingInNinetyHasNoSuggestion() {
        assertNull(showcasePriceSuggestion(18.90))
        // O centavo é arredondado antes: 18,899999... da conta é 18,90.
        assertNull(showcasePriceSuggestion(18.899999999))
    }
}
