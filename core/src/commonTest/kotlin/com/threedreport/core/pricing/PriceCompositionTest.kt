package com.threedreport.core.pricing

import com.threedreport.core.model.ChannelFeeTier
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Os quatro grupos da barra do painel de resultado somam o que o cliente paga, em qualquer combinação. */
class PriceCompositionTest {

    private val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
    private val printer = PrinterProfile(
        id = "printer",
        name = "Impressora de referência",
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(machinePrice = 2700.0, paybackMonths = 12, printingDaysPerMonth = 25, printingHoursPerDay = 16.0),
    )
    private val settings = PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, profitMargin = 1.0)
    private val prints = listOf(PrintJob(filament = pla, filamentLengthMeters = 12.0, printTimeMinutes = 190.0) to printer)
    private val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.2, fixedFeePerItem = 4.0)
    private val tiered = SalesChannel(
        id = "tiered",
        name = "Com faixas",
        feeRate = 0.0,
        tiers = listOf(
            ChannelFeeTier(upToUnitPrice = 30.0, feeRate = 0.14, fixedFeePerItem = 3.0),
            ChannelFeeTier(upToUnitPrice = null, feeRate = 0.2, fixedFeePerItem = 6.0),
        ),
    )
    private val timedPainting = QuoteService(id = "paint", name = "Pintura", price = 25.0, chargedPerOrder = false, laborMinutes = 30.0)
    private val delivery = QuoteService(id = "delivery", name = "Entrega", price = 8.0, chargedPerOrder = true)
    private val ring = QuotedConsumable(id = "ring", name = "Argola", unitCost = 0.5, quantity = 2.0, chargedPerOrder = false)

    private fun composition(settings: PricingSettings = this.settings, channel: SalesChannel? = null, terms: OrderTerms = OrderTerms()) =
        OrderPricing.quote(prints, settings, channel, terms).let { it to it.priceComposition(terms.chargedShipping) }

    @Test
    fun theReferenceExampleIsMaterialAndMachinePlusFinishingAndProfit() {
        val (quote, parts) = composition()

        assertEquals(quote.productionCost - quote.costs.finishing, parts.materialAndMachine, 1e-9)
        assertEquals(quote.costs.finishing, parts.laborAndServices, 1e-9)
        assertEquals(0.0, parts.feesAndTaxes, 1e-9)
        assertEquals(quote.profit, parts.profit, 1e-9)
        assertEquals(17.02, parts.total, 0.005)
    }

    @Test
    fun theFourGroupsAddUpToWhatTheCustomerPaysInAnyCombination() {
        val withTax = settings.copy(taxRate = 0.06, laborRatePerHour = 30.0, administrativeCost = 2.0)
        val cases = listOf(
            OrderTerms(),
            OrderTerms(quantity = 3, laborMinutes = 40.0),
            OrderTerms(quantity = 2, services = listOf(timedPainting, delivery), shippingCost = 18.0),
            OrderTerms(services = listOf(delivery), shippingCost = 18.0, shippingAbsorbed = true),
            OrderTerms(quantity = 2, consumables = listOf(ring), rush = true),
            OrderTerms(services = listOf(timedPainting), shippingCost = 12.0, customerTotal = 90.0),
            // Preço fechado abaixo do custo: lucro negativo, e os grupos ainda somam o total.
            OrderTerms(shippingCost = 10.0, customerTotal = 12.0),
            OrderTerms(catalogProduct = true, shippingCost = 18.0, announcedUnitPrice = 40.0),
        )
        val settingsOptions = listOf(settings, withTax, withTax.copy(rushSurchargeRate = 0.3, minimumOrderPrice = 60.0))
        for (terms in cases) {
            for (channel in listOf(null, shopee, tiered)) {
                for (options in settingsOptions) {
                    val (quote, parts) = composition(options, channel, terms)
                    val case = "$terms, ${channel?.id}, $options"
                    assertEquals(quote.customerTotal, parts.total, 1e-6, case)
                    assertEquals(quote.profit, parts.profit, 1e-9, case)
                    assertTrue(parts.materialAndMachine >= 0 && parts.laborAndServices >= 0 && parts.feesAndTaxes >= 0, case)
                }
            }
        }
    }

    @Test
    fun aTimedServiceCountsOnlyYourHourAsWorkAndTheRestAsProfit() {
        val terms = OrderTerms(services = listOf(timedPainting))
        val (quote, parts) = composition(settings.copy(laborRatePerHour = 30.0), terms = terms)

        // 30 min a R$ 30/h valem R$ 15 de trabalho; os outros R$ 10 da pintura são lucro.
        assertEquals(15.0, quote.serviceLaborCost, 1e-9)
        assertEquals(quote.costs.labor + quote.costs.finishing + 15.0, parts.laborAndServices, 1e-9)
    }

    @Test
    fun shippingChargedOrFreeIsInFeesAndTaxes() {
        val charged = composition(channel = shopee, terms = OrderTerms(shippingCost = 18.0))
        val free = composition(channel = shopee, terms = OrderTerms(shippingCost = 18.0, shippingAbsorbed = true))

        assertEquals(charged.first.channelFeeAmount + charged.first.taxAmount + 18.0, charged.second.feesAndTaxes, 1e-9)
        assertEquals(free.first.channelFeeAmount + free.first.taxAmount + 18.0, free.second.feesAndTaxes, 1e-9)
    }

    @Test
    fun chargedShippingMustFitInTheExtras() {
        val quote = OrderPricing.quote(prints, settings, null, OrderTerms(shippingCost = 10.0))

        assertFailsWith<IllegalArgumentException> { quote.priceComposition(chargedShipping = 11.0) }
        assertFailsWith<IllegalArgumentException> { quote.priceComposition(chargedShipping = -1.0) }
    }
}
