package com.threedreport.app.data

import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Arquivos gravados pela 2.1 (sem os campos da Leva 10) abrem na 2.2 sem migração e com o mesmo preço
 * (regra do `DataFormat.kt`: campo novo com padrão não muda o formato).
 */
class Leva10FormatCompatibilityTest {

    @Test
    fun aChannelFromBeforeHasNoFixedFeeNorTiers() {
        val channel = dataJson.decodeFromString(SalesChannel.serializer(), """{"id":"s","name":"Shopee","feeRate":0.2,"archived":false}""")
        assertEquals(0.0, channel.fixedFeePerItem)
        assertEquals(emptyList(), channel.tiers)
    }

    @Test
    fun settingsAndServicesFromBeforeKeepTheNewRulesOff() {
        val settings = dataJson.decodeFromString(
            PricingSettings.serializer(),
            """{"energyPricePerKwh":1.23,"failureRate":0.1,"finishingRate":0.1,"profitMargin":1.0}""",
        )
        assertEquals(0.0, settings.minimumOrderPrice)
        assertEquals(0.0, settings.rushSurchargeRate)

        val service = dataJson.decodeFromString(Service.serializer(), """{"id":"p","name":"Pintura","suggestedPrice":25.0}""")
        assertNull(service.laborMinutes)
    }

    @Test
    fun anOrderFromBeforeKeepsItsPriceAndProfit() {
        val json = """
            {"id":"1","name":"Peça","savedAtEpochMillis":0,"shippingCost":12.0,
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":23.0,"channelFeeRate":0.2,"extrasTotal":12.0}}
        """.trimIndent()
        val saved = dataJson.decodeFromString(SavedQuote.serializer(), json)
        val quote: Quote = saved.quote

        assertFalse(saved.shippingAbsorbed)
        assertEquals(12.0, saved.chargedShipping)
        assertEquals(23.0 + 12.0, saved.totalWithServices, 1e-9)
        assertEquals(0.0, quote.costs.consumables)
        assertEquals(0.0, quote.channelFixedFee)
        assertNull(quote.channelFeeSchedule)
        // O lucro e o preço mínimo continuam com a conta da 2.1: só o percentual do canal.
        assertEquals((23.0 + 12.0) * 0.8 - 12.0 - quote.productionCost, quote.profit, 1e-9)
        assertEquals((quote.productionCost + 12.0) / 0.8 - 12.0, quote.breakEvenSalePrice, 1e-9)
    }
}
