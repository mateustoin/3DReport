package com.threedreport.web

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.pricing.PricingCalculator
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebCalculatorTest {

    private fun assertClose(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 1e-9, "esperado $expected, veio $actual")

    private fun web(
        grams: Double = 50.0,
        channelFeePercent: Double = 20.0,
        taxPercent: Double = 6.0,
    ) = calculateQuote(
        filamentGrams = grams,
        printTimeMinutes = 150.0,
        filamentPricePerKg = 120.0,
        printerPowerWatts = 350.0,
        energyPricePerKwh = 1.23,
        profitMarginPercent = 100.0,
        failureRatePercent = 10.0,
        finishingRatePercent = 10.0,
        laborRatePerHour = 30.0,
        laborMinutes = 20.0,
        quantity = 3,
        channelFeePercent = channelFeePercent,
        taxPercent = taxPercent,
    )

    @Test
    fun resultadoIgualAoDoCoreChamadoDireto() {
        val filament = Filament(id = "f", name = "PLA", pricePerKg = 120.0, densityGPerCm3 = 1.24)
        val quote = PricingCalculator.calculate(
            job = PrintJob(filament, filamentLengthMeters = 50.0 / filament.weightGrams(1.0), printTimeMinutes = 150.0),
            printer = PrinterProfile("p", "Impressora", 350.0, 0.0, MachineInvestment(0.0, 1, 1, 1.0)),
            settings = PricingSettings(
                energyPricePerKwh = 1.23,
                failureRate = 0.10,
                finishingRate = 0.10,
                laborRatePerHour = 30.0,
                taxRate = 0.06,
                profitMargin = 1.0,
            ),
            channel = SalesChannel("c", "Shopee", 0.20),
            quantity = 3,
            laborMinutes = 20.0,
        )

        val result = web()

        assertNull(result.error)
        assertClose(quote.salePrice, result.salePrice)
        assertClose(quote.unitSalePrice, result.unitSalePrice)
        assertClose(quote.productionCost, result.productionCost)
        assertClose(quote.profit, result.profit)
        assertClose(quote.breakEvenSalePrice, result.breakEvenSalePrice)
        assertClose(quote.costs.labor, result.labor)
    }

    @Test
    fun pesoDigitadoVoltaIgualDepoisDePassarPeloComprimento() {
        val result = web(grams = 50.0)

        assertClose(150.0, result.filamentWeightGrams)
        assertClose(0.05 * 120.0 * 3, result.material)
    }

    @Test
    fun canalMaisImpostoEmCemPorCentoVoltaComoMensagem() {
        val result = web(channelFeePercent = 60.0, taxPercent = 40.0)

        assertNotNull(result.error)
        assertEquals(0.0, result.salePrice)
    }
}
