package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Valida o motor de cálculo contra os valores da planilha de referência
 * (docs/pricing-formulas.md, seção "Exemplo de referência").
 */
class PricingCalculatorTest {

    private val pla = Filament(name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private val spreadsheetSettings = PricingSettings(
        energyPricePerKwh = 1.23,
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        failureRate = 0.10,
        finishingRate = 0.10,
        administrativeCost = 0.0,
        machineInvestment = MachineInvestment(
            machinePrice = 2700.0,
            paybackMonths = 12,
            printingDaysPerMonth = 25,
            printingHoursPerDay = 16.0,
        ),
        profitMargin = 1.0,
    )

    private val spreadsheetJob = PrintJob(filament = pla, filamentLengthMeters = 12.0, printTimeMinutes = 190.0)

    @Test
    fun filamentCrossSectionAndWeightMatchSpreadsheet() {
        assertEquals(2.405, pla.crossSectionAreaMm2, CENT_TOLERANCE)
        assertEquals(35.79, pla.weightGrams(12.0), CENT_TOLERANCE)
    }

    @Test
    fun machineCostPerHourMatchesSpreadsheet() {
        assertEquals(0.5625, spreadsheetSettings.machineInvestment.costPerHour, 1e-9)
    }

    @Test
    fun costBreakdownMatchesSpreadsheet() {
        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetSettings).costs

        assertEquals(3.58, costs.material, CENT_TOLERANCE)
        assertEquals(1.48, costs.energy, CENT_TOLERANCE)
        assertEquals(0.54, costs.maintenance, CENT_TOLERANCE)
        assertEquals(0.36, costs.failures, CENT_TOLERANCE)
        assertEquals(0.36, costs.finishing, CENT_TOLERANCE)
        assertEquals(1.78, costs.investmentReturn, CENT_TOLERANCE)
        assertEquals(0.0, costs.administrative, CENT_TOLERANCE)
    }

    @Test
    fun productionAndSalePriceMatchSpreadsheet() {
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetSettings)

        assertEquals(8.09, quote.productionCost, CENT_TOLERANCE)
        assertEquals(16.19, quote.salePrice, CENT_TOLERANCE)
        assertEquals(quote.productionCost, quote.profit, 1e-9)
    }

    @Test
    fun administrativeCostIsAddedOncePerQuote() {
        val withModeling = spreadsheetSettings.copy(administrativeCost = 20.0)

        val base = PricingCalculator.calculate(spreadsheetJob, spreadsheetSettings)
        val quote = PricingCalculator.calculate(spreadsheetJob, withModeling)

        assertEquals(base.productionCost + 20.0, quote.productionCost, 1e-9)
    }

    @Test
    fun zeroInputsProduceOnlyFixedCosts() {
        val job = PrintJob(filament = pla, filamentLengthMeters = 0.0, printTimeMinutes = 0.0)
        val quote = PricingCalculator.calculate(job, spreadsheetSettings.copy(administrativeCost = 5.0))

        assertEquals(5.0, quote.productionCost, 1e-9)
        assertEquals(10.0, quote.salePrice, 1e-9)
    }

    @Test
    fun negativeInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            PrintJob(filament = pla, filamentLengthMeters = -1.0, printTimeMinutes = 10.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Filament(name = "X", pricePerKg = -1.0, densityGPerCm3 = 1.24)
        }
    }

    private companion object {
        /** A planilha exibe valores com 2 casas; aceitamos diferença de até meio centavo. */
        const val CENT_TOLERANCE = 0.005
    }
}
