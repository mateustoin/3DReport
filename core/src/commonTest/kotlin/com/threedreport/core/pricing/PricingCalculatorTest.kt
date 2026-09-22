package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Valida o motor de cálculo contra os valores da planilha de referência
 * (docs/pricing-formulas.md, seção "Exemplo de referência").
 */
class PricingCalculatorTest {

    private val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    private val spreadsheetPrinter = PrinterProfile(
        id = "printer",
        name = "Impressora de referência",
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(
            machinePrice = 2700.0,
            paybackMonths = 12,
            printingDaysPerMonth = 25,
            printingHoursPerDay = 16.0,
        ),
    )

    private val spreadsheetSettings = PricingSettings(
        energyPricePerKwh = 1.23,
        failureRate = 0.10,
        finishingRate = 0.10,
        administrativeCost = 0.0,
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
        assertEquals(0.5625, spreadsheetPrinter.machineInvestment.costPerHour, 1e-9)
    }

    @Test
    fun costBreakdownMatchesSpreadsheet() {
        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings).costs

        assertEquals(3.58, costs.material, CENT_TOLERANCE)
        assertEquals(1.48, costs.energy, CENT_TOLERANCE)
        assertEquals(0.54, costs.maintenance, CENT_TOLERANCE)
        assertEquals(0.77, costs.failures, CENT_TOLERANCE)
        assertEquals(0.36, costs.finishing, CENT_TOLERANCE)
        assertEquals(1.78, costs.investmentReturn, CENT_TOLERANCE)
        assertEquals(0.0, costs.administrative, CENT_TOLERANCE)
        assertEquals(0.0, costs.labor, CENT_TOLERANCE)
        assertEquals(0.0, costs.fixedCost, CENT_TOLERANCE)
    }

    @Test
    fun productionAndSalePriceMatchSpreadsheet() {
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)

        assertEquals(8.51, quote.productionCost, CENT_TOLERANCE)
        assertEquals(17.02, quote.salePrice, CENT_TOLERANCE)
        assertEquals(quote.productionCost, quote.profit, 1e-9)
    }

    @Test
    fun administrativeCostIsAddedOncePerQuote() {
        val withModeling = spreadsheetSettings.copy(administrativeCost = 20.0)

        val base = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, withModeling)

        assertEquals(base.productionCost + 20.0, quote.productionCost, 1e-9)
    }

    @Test
    fun zeroInputsProduceOnlyFixedCosts() {
        val job = PrintJob(filament = pla, filamentLengthMeters = 0.0, printTimeMinutes = 0.0)
        val quote = PricingCalculator.calculate(job, spreadsheetPrinter, spreadsheetSettings.copy(administrativeCost = 5.0))

        assertEquals(5.0, quote.productionCost, 1e-9)
        assertEquals(10.0, quote.salePrice, 1e-9)
    }

    /** Trava os números da seção "O mesmo exemplo, cobrando o próprio trabalho" de pricing-formulas.md. */
    @Test
    fun documentedExampleWithLaborAndFixedCostMatches() {
        val settings = spreadsheetSettings.copy(
            laborRatePerHour = 30.0,
            monthlyFixedCost = 800.0,
            productiveHoursPerMonth = 200.0,
        )
        val job = spreadsheetJob.copy(laborMinutes = 40.0)

        val quote = PricingCalculator.calculate(job, spreadsheetPrinter, settings)

        assertEquals(12.67, quote.costs.fixedCost, CENT_TOLERANCE)
        assertEquals(20.00, quote.costs.labor, CENT_TOLERANCE)
        assertEquals(0.0, quote.costs.finishing, 1e-9)
        assertEquals(4.00, quote.costs.failures, CENT_TOLERANCE)
        assertEquals(44.05, quote.productionCost, CENT_TOLERANCE)
        assertEquals(88.10, quote.salePrice, CENT_TOLERANCE)
    }

    @Test
    fun laborIsChargedByTheTimeInformedInTheJob() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0)
        val job = spreadsheetJob.copy(laborMinutes = 40.0)

        val costs = PricingCalculator.calculate(job, spreadsheetPrinter, settings).costs

        assertEquals(20.0, costs.labor, 1e-9)
    }

    @Test
    fun finishingPercentageIsReplacedByLaborWhenAnHourlyRateIsConfigured() {
        val job = spreadsheetJob.copy(laborMinutes = 40.0)

        val legacy = PricingCalculator.calculate(job, spreadsheetPrinter, spreadsheetSettings).costs
        val byTime = PricingCalculator.calculate(
            job,
            spreadsheetPrinter,
            spreadsheetSettings.copy(laborRatePerHour = 30.0),
        ).costs

        // Sem taxa horária, o acabamento continua sendo o percentual sobre o material (legado)...
        assertEquals(0.36, legacy.finishing, CENT_TOLERANCE)
        assertEquals(0.0, legacy.labor, 1e-9)
        // ...com taxa horária, o acabamento passa a ser cobrado dentro da mão de obra.
        assertEquals(0.0, byTime.finishing, 1e-9)
        assertEquals(20.0, byTime.labor, 1e-9)
    }

    @Test
    fun failureReserveCoversEveryCostExceptTheAdministrativeOne() {
        val settings = spreadsheetSettings.copy(administrativeCost = 50.0, laborRatePerHour = 30.0)
        val job = spreadsheetJob.copy(laborMinutes = 40.0)

        val costs = PricingCalculator.calculate(job, spreadsheetPrinter, settings).costs

        val reprintable = costs.material + costs.energy + costs.maintenance +
            costs.investmentReturn + costs.fixedCost + costs.labor + costs.finishing
        assertEquals(reprintable * 0.10, costs.failures, 1e-9)
        // A modelagem já feita não é refeita quando a impressão falha, então não entra na reserva.
        assertEquals(50.0, costs.administrative, 1e-9)
    }

    @Test
    fun monthlyFixedCostIsDilutedPerPrintingHour() {
        val settings = spreadsheetSettings.copy(monthlyFixedCost = 800.0, productiveHoursPerMonth = 200.0)

        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings).costs

        // R$ 800 / 200 h = R$ 4,00 por hora de impressão; a peça leva 190 min.
        assertEquals(4.0 * (190.0 / 60.0), costs.fixedCost, 1e-9)
    }

    @Test
    fun fixedCostIsIgnoredWhenProductiveHoursWereNotInformed() {
        val settings = spreadsheetSettings.copy(monthlyFixedCost = 800.0, productiveHoursPerMonth = 0.0)

        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings).costs

        assertEquals(0.0, costs.fixedCost, 1e-9)
    }

    @Test
    fun defaultSettingsKeepTheOldBehaviourUntilTheNewFieldsAreFilled() {
        // Atualizar o app não pode mudar o preço de ninguém em silêncio: sem taxa horária, sem
        // custo fixo e sem minutos de trabalho, as parcelas novas ficam zeradas.
        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings).costs

        assertEquals(0.0, costs.labor, 1e-9)
        assertEquals(0.0, costs.fixedCost, 1e-9)
        assertEquals(0.36, costs.finishing, CENT_TOLERANCE)
    }

    @Test
    fun negativeInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            PrintJob(filament = pla, filamentLengthMeters = -1.0, printTimeMinutes = 10.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Filament(id = "x", name = "X", pricePerKg = -1.0, densityGPerCm3 = 1.24)
        }
    }

    @Test
    fun marketplaceFeeRaisesSalePriceButKeepsRealProfitUnchanged() {
        val settingsWithFee = spreadsheetSettings.copy(marketplaceFeeRate = 0.15)

        val withoutMarketplace = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settingsWithFee)
        val withMarketplace = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            settingsWithFee,
            appliesMarketplaceFee = true,
        )

        // Preço de tabela sobe pra compensar o desconto do marketplace...
        assertEquals(17.02, withoutMarketplace.salePrice, CENT_TOLERANCE)
        assertEquals(17.02 / 0.85, withMarketplace.salePrice, CENT_TOLERANCE)
        // ...mas o lucro real (depois do marketplace descontar a parte dele) fica igual.
        assertEquals(withoutMarketplace.profit, withMarketplace.profit, 1e-9)
    }

    @Test
    fun marketplaceFeeNotAppliedWhenFlagIsFalseEvenIfConfigured() {
        val settingsWithFee = spreadsheetSettings.copy(marketplaceFeeRate = 0.15)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settingsWithFee)

        assertEquals(0.0, quote.marketplaceFeeRate, 1e-9)
        assertEquals(17.02, quote.salePrice, CENT_TOLERANCE)
    }

    @Test
    fun invalidMarketplaceFeeRateIsRejected() {
        assertFailsWith<IllegalArgumentException> { spreadsheetSettings.copy(marketplaceFeeRate = -0.1) }
        assertFailsWith<IllegalArgumentException> { spreadsheetSettings.copy(marketplaceFeeRate = 1.0) }
    }

    private companion object {
        /** A planilha exibe valores com 2 casas; aceitamos diferença de até meio centavo. */
        const val CENT_TOLERANCE = 0.005
    }
}
