package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductRepricerTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
    private val printer = PrinterProfile(
        id = "p1",
        name = "Impressora",
        printerPowerWatts = 200.0,
        maintenanceCostPerHour = 0.1,
        machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
    )
    private val settings = PricingSettings(energyPricePerKwh = 1.0, failureRate = 0.1, finishingRate = 0.0, profitMargin = 1.0)
    private val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.2)

    private fun productOf(channel: SalesChannel? = null, announced: Double? = null, quantity: Int = 1): SavedQuote {
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament = filament, filamentLengthMeters = 10.0, printTimeMinutes = 120.0),
            printer = printer,
            settings = settings,
            channel = channel,
            quantity = quantity,
            laborMinutes = 15.0,
            negotiatedSalePrice = announced,
        )
        return SavedQuote(id = "1", name = "Chaveiro", quote = quote, savedAtEpochMillis = 0L, kind = QuoteKind.PRODUCT)
    }

    private fun reprice(
        product: SavedQuote,
        filaments: List<Filament> = listOf(filament),
        printers: List<PrinterProfile> = listOf(printer),
        settings: PricingSettings = this.settings,
        channels: List<SalesChannel> = listOf(shopee),
    ) = ProductRepricer.reprice(product, filaments, printers, settings, channels)

    @Test
    fun unchangedCostsKeepThePrice() {
        val product = productOf(quantity = 3)

        val result = assertIs<RepriceResult.Repriced>(reprice(product))

        assertFalse(result.changed)
        assertEquals(product.quote.salePrice, result.quote.salePrice, 1e-9)
        assertEquals(3, result.quote.quantity)
        assertEquals(15.0, result.quote.laborMinutes)
    }

    @Test
    fun pricierFilamentRaisesThePrice() {
        val product = productOf()

        val result = assertIs<RepriceResult.Repriced>(reprice(product, filaments = listOf(filament.copy(pricePerKg = 150.0))))

        assertTrue(result.changed)
        assertTrue(result.quote.salePrice > product.quote.salePrice)
        assertEquals(150.0, result.quote.prints.single().job.filaments.single().filament.pricePerKg)
    }

    @Test
    fun announcedPriceIsKeptAndOnlyTheCalculatedOneMoves() {
        val product = productOf(announced = 30.0)

        val result = assertIs<RepriceResult.Repriced>(reprice(product, settings = settings.copy(profitMargin = 2.0)))

        assertTrue(result.changed)
        assertEquals(30.0, result.quote.salePrice, 1e-9)
        assertTrue(result.quote.tableSalePrice!! > product.quote.tableSalePrice!!)
    }

    @Test
    fun channelIsFoundByIdAndItsCurrentNameAndFeeApply() {
        val product = productOf(channel = shopee)

        // Renomear o canal no cadastro não deixa o produto sem canal.
        val result = assertIs<RepriceResult.Repriced>(reprice(product, channels = listOf(shopee.copy(name = "Shopee Brasil", feeRate = 0.25))))

        assertTrue(result.changed)
        assertEquals(0.25, result.quote.channelFeeRate)
        assertEquals("Shopee Brasil", result.quote.channelName)
    }

    @Test
    fun missingFilamentPrinterOrChannelCannotBeRepriced() {
        val product = productOf(channel = shopee)

        assertEquals(
            RepriceResult.Unavailable(RepriceResult.Reason.FILAMENT_MISSING, "PLA"),
            reprice(product, filaments = emptyList()),
        )
        assertEquals(
            RepriceResult.Unavailable(RepriceResult.Reason.PRINTER_MISSING, "Impressora"),
            reprice(product, printers = emptyList()),
        )
        assertEquals(
            RepriceResult.Unavailable(RepriceResult.Reason.CHANNEL_MISSING, "Shopee"),
            reprice(product, channels = emptyList()),
        )
    }

    @Test
    fun everyFilamentOfEveryPrintIsRepriced() {
        val petg = Filament(id = "petg", name = "PETG", pricePerKg = 120.0, densityGPerCm3 = 1.27)
        val quote = PricingCalculator.calculate(
            prints = listOf(
                PrintJob(filaments = listOf(FilamentUsage(filament, 5.0), FilamentUsage(petg, 2.0)), printTimeMinutes = 60.0) to printer,
                PrintJob(filament = petg, filamentLengthMeters = 8.0, printTimeMinutes = 90.0) to printer,
            ),
            settings = settings,
        )
        val product = SavedQuote(id = "1", name = "Diorama", quote = quote, savedAtEpochMillis = 0L, kind = QuoteKind.PRODUCT)

        val result = assertIs<RepriceResult.Repriced>(reprice(product, filaments = listOf(filament, petg.copy(pricePerKg = 240.0))))

        assertTrue(result.changed)
        assertEquals(listOf(100.0, 240.0, 240.0), result.quote.prints.flatMap { p -> p.job.filaments.map { it.filament.pricePerKg } })
        assertEquals(
            RepriceResult.Unavailable(RepriceResult.Reason.FILAMENT_MISSING, "PETG"),
            reprice(product, filaments = listOf(filament)),
        )
    }
}
