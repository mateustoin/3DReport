package com.threedreport.core.pricing

import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.SalesChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * As regras da Leva 10 (decisões 121 a 125), sobre o mesmo exemplo de referência de
 * `PricingCalculatorTest` (produção R$ 8,51, venda R$ 17,02). Cada regra desligada não pode mudar nada.
 */
class PricingCalculatorLeva10Test {

    private val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
    private val printer = PrinterProfile(
        id = "printer",
        name = "Impressora de referência",
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(machinePrice = 2700.0, paybackMonths = 12, printingDaysPerMonth = 25, printingHoursPerDay = 16.0),
    )
    private val settings = PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, profitMargin = 1.0)
    private val job = PrintJob(filament = pla, filamentLengthMeters = 12.0, printTimeMinutes = 190.0)

    private fun quote(
        settings: PricingSettings = this.settings,
        channel: SalesChannel? = null,
        quantity: Int = 1,
        negotiatedSalePrice: Double? = null,
        extrasTotal: Double = 0.0,
        services: List<QuoteService> = emptyList(),
        consumables: List<QuotedConsumable> = emptyList(),
        rush: Boolean = false,
        absorbedShippingCost: Double = 0.0,
    ) = PricingCalculator.calculate(
        prints = listOf(job to printer),
        settings = settings,
        channel = channel,
        quantity = quantity,
        negotiatedSalePrice = negotiatedSalePrice,
        extrasTotal = extrasTotal,
        services = services,
        consumables = consumables,
        rush = rush,
        absorbedShippingCost = absorbedShippingCost,
    )

    private val reference = quote()

    @Test
    fun everythingOffKeepsTheReferenceNumbers() {
        assertEquals(8.51, reference.productionCost, CENT)
        assertEquals(17.02, reference.salePrice, CENT)
        assertEquals(0.0, reference.channelFixedFee)
        assertEquals(0.0, reference.costs.consumables)
        assertEquals(0.0, reference.serviceProfit)
        assertFalse(reference.rush)
        assertFalse(reference.minimumPriceApplied)
        assertNull(reference.channelFeeSchedule)
        assertNull(reference.shippingCoveringSalePrice)
    }

    // --- Taxa fixa por item (decisão 121) ---

    @Test
    fun theFixedFeePerItemRaisesThePriceAndKeepsTheProfit() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20, fixedFeePerItem = 4.0)
        val withFee = quote(channel = shopee, quantity = 3)
        val direct = quote(quantity = 3)

        assertEquals((direct.salePrice + 4.0 * 3) / 0.8, withFee.salePrice, 1e-9)
        assertEquals(12.0, withFee.channelFixedFee, 1e-9)
        assertEquals(direct.profit, withFee.profit, 1e-9)
        assertEquals(withFee.customerTotal * 0.2 + 12.0, withFee.channelFeeAmount, 1e-9)
    }

    @Test
    fun aPercentOnlyChannelBehavesAsBefore() {
        val pix = SalesChannel(id = "c", name = "Cartão", feeRate = 0.04)
        val quote = quote(channel = pix, extrasTotal = 10.0)
        assertEquals((reference.salePrice + 10.0) / 0.96 - 10.0, quote.salePrice, 1e-9)
        assertEquals(0.0, quote.channelFixedFee)
    }

    @Test
    fun tiersPickTheFeeOfThePriceTheOrderEndsUpIn() {
        val channel = SalesChannel(
            id = "s", name = "Shopee", feeRate = 0.0,
            tiers = listOf(
                ChannelFeeTier(upToUnitPrice = 20.0, feeRate = 0.20, fixedFeePerItem = 4.0),
                ChannelFeeTier(upToUnitPrice = null, feeRate = 0.14, fixedFeePerItem = 16.0),
            ),
        )
        // (17,02 + 4) / 0,8 = 26,27 passa de 20: na segunda faixa, (17,02 + 16) / 0,86 = 38,39.
        val quote = quote(channel = channel)
        assertEquals((reference.salePrice + 16.0) / 0.86, quote.salePrice, 1e-9)
        assertEquals(0.14, quote.channelFeeRate)
        assertEquals(16.0, quote.channelFixedFee)
        assertEquals(reference.profit, quote.profit, 1e-9)

        // Fechado por R$ 18,00, cai na primeira faixa, e o canal cobra a taxa dela.
        val negotiated = quote(channel = channel, negotiatedSalePrice = 18.0)
        assertEquals(0.20, negotiated.channelFeeRate)
        assertEquals(4.0, negotiated.channelFixedFee)
        assertEquals(18.0 * 0.8 - 4.0 - reference.productionCost, negotiated.profit, 1e-9)
    }

    @Test
    fun theBreakEvenPriceLeavesNoProfitEvenWithTiers() {
        val channel = SalesChannel(
            id = "s", name = "Shopee", feeRate = 0.0,
            tiers = listOf(
                ChannelFeeTier(upToUnitPrice = 20.0, feeRate = 0.20, fixedFeePerItem = 4.0),
                ChannelFeeTier(upToUnitPrice = null, feeRate = 0.14, fixedFeePerItem = 16.0),
            ),
        )
        val quote = quote(channel = channel)
        val atBreakEven = quote(channel = channel, negotiatedSalePrice = quote.breakEvenSalePrice)
        assertEquals(0.0, atBreakEven.profit, 1e-9)
    }

    // --- Insumos (decisão 122) ---

    @Test
    fun consumablesEnterTheMarginButNotTheFailureReserve() {
        val argola = QuotedConsumable.of(Consumable(id = "a", name = "Argola", unitCost = 0.50), quantity = 1.0)
        val caixa = QuotedConsumable.of(Consumable(id = "c", name = "Caixa", unitCost = 2.0, chargedPerOrder = true), quantity = 1.0)
        val quote = quote(quantity = 4, consumables = listOf(argola, caixa))
        val without = quote(quantity = 4)

        // 4 argolas e 1 caixa.
        assertEquals(0.5 * 4 + 2.0, quote.costs.consumables, 1e-9)
        assertEquals(without.costs.failures, quote.costs.failures, 1e-9)
        assertEquals(without.productionCost + 4.0, quote.productionCost, 1e-9)
        // Margem de 100%: o insumo entra duas vezes no preço.
        assertEquals(without.salePrice + 4.0 * 2, quote.salePrice, 1e-9)
    }

    // --- Serviços com tempo (decisão 123) ---

    @Test
    fun aServiceWithoutTimeStaysOutOfTheProfit() {
        val pintura = QuoteService(id = "p", name = "Pintura", price = 25.0, chargedPerOrder = false)
        val quote = quote(extrasTotal = 25.0, services = listOf(pintura))
        assertEquals(0.0, quote.serviceProfit)
        assertEquals(reference.profit, quote.profit, 1e-9)
    }

    @Test
    fun aTimedServiceCountsWhatItEarnsAboveYourHour() {
        val withRate = settings.copy(laborRatePerHour = 30.0)
        val pintura = QuoteService(id = "p", name = "Pintura", price = 25.0, chargedPerOrder = false, laborMinutes = 30.0)
        val quote = quote(settings = withRate, quantity = 2, extrasTotal = 50.0, services = listOf(pintura))
        val without = quote(settings = withRate, quantity = 2, extrasTotal = 50.0)

        // 2 pinturas de 30 min: 1 h a R$ 30 = R$ 30, cobradas R$ 50.
        assertEquals(60.0, quote.serviceLaborMinutes, 1e-9)
        assertEquals(30.0, quote.serviceLaborCost, 1e-9)
        assertEquals(20.0, quote.serviceProfit, 1e-9)
        assertEquals(without.profit + 20.0, quote.profit, 1e-9)
        assertEquals(without.salePrice, quote.salePrice, 1e-9)
    }

    @Test
    fun aServiceThatPaysLessThanYourHourLowersTheProfit() {
        val withRate = settings.copy(laborRatePerHour = 60.0)
        val pintura = QuoteService(id = "p", name = "Pintura", price = 10.0, chargedPerOrder = true, laborMinutes = 30.0)
        val quote = quote(settings = withRate, extrasTotal = 10.0, services = listOf(pintura))
        assertEquals(-20.0, quote.serviceProfit, 1e-9)
    }

    // --- Frete grátis (decisão 124) ---

    @Test
    fun freeShippingComesOutOfTheProfitNotThePrice() {
        val quote = quote(absorbedShippingCost = 18.0)
        assertEquals(reference.salePrice, quote.salePrice, 1e-9)
        assertEquals(reference.customerTotal, quote.customerTotal, 1e-9)
        assertEquals(reference.profit - 18.0, quote.profit, 1e-9)
        assertEquals(reference.breakEvenSalePrice + 18.0, quote.breakEvenSalePrice, 1e-9)
    }

    @Test
    fun theShippingCoveringPriceKeepsTheTableProfit() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20, fixedFeePerItem = 4.0)
        val quote = quote(channel = shopee, absorbedShippingCost = 18.0)
        val covering = assertNotNull(quote.shippingCoveringSalePrice)
        val atCovering = quote(channel = shopee, absorbedShippingCost = 18.0, negotiatedSalePrice = covering)
        assertEquals(quote(channel = shopee).profit, atCovering.profit, 1e-9)
    }

    // --- Preço mínimo e urgência (decisão 125) ---

    @Test
    fun theMinimumOrderPriceRaisesOnlyTheTablePrice() {
        val withMinimum = settings.copy(minimumOrderPrice = 25.0)
        val quote = quote(settings = withMinimum)
        assertEquals(25.0, quote.salePrice, 1e-9)
        assertEquals(reference.salePrice, assertNotNull(quote.priceBeforeMinimum), 1e-9)
        assertTrue(quote.minimumPriceApplied)

        val negotiated = quote(settings = withMinimum, negotiatedSalePrice = 20.0)
        assertEquals(20.0, negotiated.salePrice, 1e-9)
        assertEquals(25.0, assertNotNull(negotiated.tableSalePrice), 1e-9)
    }

    @Test
    fun aMinimumBelowTheCalculatedPriceDoesNothing() {
        val quote = quote(settings = settings.copy(minimumOrderPrice = 10.0))
        assertEquals(reference.salePrice, quote.salePrice, 1e-9)
        assertFalse(quote.minimumPriceApplied)
    }

    @Test
    fun rushAddsItsPercentOnThePieceOnly() {
        val withRush = settings.copy(rushSurchargeRate = 0.30)
        val quote = quote(settings = withRush, rush = true, extrasTotal = 20.0)
        val normal = quote(settings = withRush, extrasTotal = 20.0)

        assertEquals(reference.salePrice * 0.30, quote.rushSurcharge, 1e-9)
        assertEquals(normal.salePrice + quote.rushSurcharge, quote.salePrice, 1e-9)
        assertEquals(normal.profit + quote.rushSurcharge, quote.profit, 1e-9)
        assertFalse(normal.rush)
        assertEquals(0.0, normal.rushSurcharge)
    }

    @Test
    fun aTieredChannelWithServicesStillLeavesTheSameProfit() {
        val channel = SalesChannel(
            id = "s", name = "Shopee", feeRate = 0.0,
            tiers = listOf(
                ChannelFeeTier(upToUnitPrice = 20.0, feeRate = 0.20, fixedFeePerItem = 4.0),
                ChannelFeeTier(upToUnitPrice = null, feeRate = 0.14, fixedFeePerItem = 16.0),
            ),
        )
        val quote = quote(channel = channel, extrasTotal = 25.0)
        assertEquals(quote(extrasTotal = 25.0).profit, quote.profit, 1e-9)
    }

    @Test
    fun theShippingCoveringPriceNeverGoesBelowTheMinimum() {
        val withMinimum = settings.copy(minimumOrderPrice = 40.0)
        val quote = quote(settings = withMinimum, absorbedShippingCost = 10.0)
        assertEquals(40.0, quote.salePrice, 1e-9)
        assertEquals(50.0, assertNotNull(quote.shippingCoveringSalePrice), 1e-9)
    }

    @Test
    fun rushIsNotShownWhenTheMinimumOrANegotiatedPriceReplacesTheTable() {
        val withRush = settings.copy(rushSurchargeRate = 0.5, minimumOrderPrice = 100.0)
        val floored = quote(settings = withRush, rush = true)
        assertTrue(floored.rush)
        assertEquals(0.0, floored.rushSurcharge)

        val negotiated = quote(settings = settings.copy(rushSurchargeRate = 0.5), rush = true, negotiatedSalePrice = 20.0)
        assertTrue(negotiated.rush)
        assertEquals(0.0, negotiated.rushSurcharge)
    }

    @Test
    fun theBreakEvenPriceIsNeverNegative() {
        val withRate = settings.copy(laborRatePerHour = 20.0)
        val pintura = QuoteService(id = "p", name = "Pintura", price = 100.0, chargedPerOrder = true, laborMinutes = 30.0)
        val quote = quote(settings = withRate, extrasTotal = 100.0, services = listOf(pintura))
        assertEquals(0.0, quote.breakEvenSalePrice)
    }

    private companion object {
        const val CENT = 0.005
    }
}
