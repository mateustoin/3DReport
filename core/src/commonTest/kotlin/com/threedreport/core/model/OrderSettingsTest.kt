package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Configurações de Pedidos da Leva 11: sinal padrão, orçamento parado e site de rastreio (decisões 146, 149 e 150). */
class OrderSettingsTest {

    @Test
    fun startsNeutral() {
        val settings = OrderSettings()

        assertEquals(0.0, settings.defaultDepositRate)
        assertEquals(3, settings.staleQuoteDays)
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, settings.effectiveTrackingUrlTemplate(Currency.BRL))
    }

    @Test
    fun theTrackingLinkPutsTheCodeInPlace() {
        assertEquals(
            "https://rastreamento.correios.com.br/app/index.php?objetos=AA123456789BR",
            OrderSettings().trackingUrl(" AA123456789BR ", Currency.BRL),
        )
        assertEquals("https://x.test/r?c=A%20B%26C", OrderSettings(trackingUrlTemplate = "https://x.test/r?c={codigo}").trackingUrl("A B&C", Currency.BRL))
    }

    @Test
    fun aBlankSiteOrCodeGivesNoLink() {
        assertNull(OrderSettings(trackingUrlTemplate = "").trackingUrl("AA123456789BR", Currency.BRL))
        assertNull(OrderSettings(trackingUrlTemplate = "  ").trackingUrl("AA123456789BR", Currency.BRL))
        assertNull(OrderSettings().trackingUrl("  ", Currency.BRL))
    }

    @Test
    fun aSiteWithoutTheMarkerOpensAsIs() {
        assertEquals("https://x.test/rastreio", OrderSettings(trackingUrlTemplate = "https://x.test/rastreio").trackingUrl("AA1", Currency.BRL))
    }

    @Test
    fun outOfRangeValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { OrderSettings(defaultDepositRate = -0.1) }
        assertFailsWith<IllegalArgumentException> { OrderSettings(defaultDepositRate = 1.1) }
        assertFailsWith<IllegalArgumentException> { OrderSettings(staleQuoteDays = -1) }
    }

    // Decisão 16b da Leva 11B: o padrão dos Correios só vale com a moeda padrão em real.
    @Test
    fun theDefaultTrackingSiteDependsOnTheDefaultCurrency() {
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, OrderSettings().effectiveTrackingUrlTemplate(Currency.BRL))
        assertEquals("", OrderSettings().effectiveTrackingUrlTemplate(Currency.USD))
        assertNull(OrderSettings().trackingUrl("AA1", Currency.USD))
        assertEquals(
            "https://rastreamento.correios.com.br/app/index.php?objetos=AA1",
            OrderSettings().trackingUrl("AA1", Currency.BRL),
        )
        // Um site escolhido vale em qualquer moeda.
        assertEquals("https://x.test/?c=AA1", OrderSettings(trackingUrlTemplate = "https://x.test/?c={code}").trackingUrl("AA1", Currency.USD))
    }
}
