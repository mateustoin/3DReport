package com.threedreport.core.pricing

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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A montagem do pedido antes da conta (decisão 130): o que a tela de Orçamento decidia sozinha e agora
 * mora no `core`. Cada caso confere contra o [PricingCalculator] chamado com os valores já derivados, que
 * é o que a tela fazia.
 */
class OrderPricingTest {

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
    private val painting = QuoteService(id = "paint", name = "Pintura", price = 15.0, chargedPerOrder = false, laborMinutes = 20.0)
    private val delivery = QuoteService(id = "delivery", name = "Entrega", price = 8.0, chargedPerOrder = true)

    @Test
    fun withNothingAgreedItIsTheReferenceQuote() {
        val quote = OrderPricing.quote(prints, settings, channel = null, terms = OrderTerms())

        assertEquals(PricingCalculator.calculate(prints, settings), quote)
        assertEquals(17.02, quote.salePrice, 0.005)
    }

    @Test
    fun theAgreedTotalLosesServicesAndChargedShippingBeforeBecomingThePiecePrice() {
        val terms = OrderTerms(
            quantity = 3,
            laborMinutes = 40.0,
            services = listOf(painting, delivery),
            shippingCost = 18.0,
            customerTotal = 150.0,
        )

        val quote = OrderPricing.quote(prints, settings, shopee, terms)

        // Pintura por peça (15 · 3) e entrega por pedido (8), mais o frete de 18.
        assertEquals(45.0 + 8.0, terms.servicesTotal, 1e-9)
        assertEquals(45.0 + 8.0 + 18.0, terms.extrasTotal, 1e-9)
        assertEquals(150.0 - 53.0 - 18.0, terms.negotiatedSalePrice!!, 1e-9)
        assertEquals(
            PricingCalculator.calculate(
                prints = prints,
                settings = settings,
                channel = shopee,
                quantity = 3,
                laborMinutes = 40.0,
                negotiatedSalePrice = 150.0 - 53.0 - 18.0,
                extrasTotal = 53.0 + 18.0,
                services = listOf(painting, delivery),
            ),
            quote,
        )
        assertEquals(150.0, quote.customerTotal, 1e-9)
    }

    @Test
    fun freeShippingIsAbsorbedInsteadOfCharged() {
        val terms = OrderTerms(services = listOf(delivery), shippingCost = 18.0, shippingAbsorbed = true)

        val quote = OrderPricing.quote(prints, settings, shopee, terms)

        assertTrue(terms.absorbsShipping)
        assertEquals(0.0, terms.chargedShipping)
        assertEquals(
            PricingCalculator.calculate(
                prints = prints,
                settings = settings,
                channel = shopee,
                extrasTotal = 8.0,
                services = listOf(delivery),
                absorbedShippingCost = 18.0,
            ),
            quote,
        )
    }

    @Test
    fun freeShippingWithoutShippingIsNotFreeShipping() {
        val terms = OrderTerms(shippingCost = 0.0, shippingAbsorbed = true)

        assertFalse(terms.absorbsShipping)
        assertEquals(0.0, OrderPricing.quote(prints, settings, null, terms).absorbedShippingCost)
    }

    @Test
    fun aCatalogProductHasNoShippingNoRushAndNoAnnouncedPrice() {
        val rushSettings = settings.copy(rushSurchargeRate = 0.3)
        val terms = OrderTerms(
            shippingCost = 18.0,
            shippingAbsorbed = true,
            catalogProduct = true,
            announcedUnitPrice = 40.0,
            rush = true,
        )

        val quote = OrderPricing.quote(prints, rushSettings, null, terms)

        assertEquals(0.0, terms.effectiveShippingCost)
        assertFalse(terms.absorbsShipping)
        assertNull(terms.negotiatedSalePrice)
        assertEquals(PricingCalculator.calculate(prints, rushSettings), quote)
        assertFalse(quote.rush)
    }

    @Test
    fun theAnnouncedUnitPriceIsUsedPerPieceWhenNoTotalWasAgreed() {
        val terms = OrderTerms(quantity = 2, shippingCost = 10.0, announcedUnitPrice = 30.0)

        val quote = OrderPricing.quote(prints, settings, null, terms)

        assertEquals(60.0, quote.salePrice, 1e-9)
        assertEquals(70.0, quote.customerTotal, 1e-9)
    }

    @Test
    fun anAgreedTotalWinsOverTheAnnouncedPrice() {
        val terms = OrderTerms(quantity = 2, announcedUnitPrice = 30.0, customerTotal = 50.0)

        assertEquals(50.0, OrderPricing.quote(prints, settings, null, terms).salePrice, 1e-9)
    }

    @Test
    fun rushOnlyCountsWithASurchargeConfigured() {
        val withoutSurcharge = OrderPricing.quote(prints, settings, null, OrderTerms(rush = true))
        assertFalse(withoutSurcharge.rush)
        assertEquals(PricingCalculator.calculate(prints, settings), withoutSurcharge)

        val rushSettings = settings.copy(rushSurchargeRate = 0.3)
        val withSurcharge = OrderPricing.quote(prints, rushSettings, null, OrderTerms(rush = true))
        assertEquals(PricingCalculator.calculate(prints, rushSettings, rush = true), withSurcharge)
        assertTrue(withSurcharge.rush)
    }

    @Test
    fun consumablesGoThrough() {
        val ring = QuotedConsumable(id = "ring", name = "Argola", unitCost = 0.5, quantity = 2.0, chargedPerOrder = false)
        val quote = OrderPricing.quote(prints, settings, null, OrderTerms(quantity = 2, consumables = listOf(ring)))

        assertEquals(PricingCalculator.calculate(prints, settings, quantity = 2, consumables = listOf(ring)), quote)
    }

    @Test
    fun anAgreedTotalBelowTheExtrasDoesNotCoverThem() {
        val exact = OrderTerms(services = listOf(delivery), shippingCost = 10.0, customerTotal = 18.0)
        val below = exact.copy(customerTotal = 17.99)

        assertTrue(OrderTerms().coversExtras)
        assertTrue(exact.coversExtras)
        assertFalse(below.coversExtras)
        // A tela confere antes e mostra o erro no campo; chegar aqui assim é erro de quem chama.
        assertFailsWith<IllegalArgumentException> { OrderPricing.quote(prints, settings, null, below) }
    }
}
