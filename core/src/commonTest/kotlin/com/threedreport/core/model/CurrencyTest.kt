package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** O que só vale pro Brasil segue a moeda (item 16b da Leva 11B): só o real tem Pix, Correios e preço de referência. */
class CurrencyTest {

    @Test
    fun onlyTheRealHasTheBrazilOnlyFeatures() {
        assertEquals(listOf(Currency.BRL), Currency.entries.filter { it.hasBrazilOnlyFeatures })
    }

    @Test
    fun theBrazilOnlyFeaturesFollowTheCurrency() {
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, OrderSettings.defaultTrackingUrlTemplate(Currency.BRL))
        for (currency in Currency.entries.filterNot { it.hasBrazilOnlyFeatures }) {
            assertEquals("", OrderSettings.defaultTrackingUrlTemplate(currency), currency.code)
        }
    }
}
