package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.SalesChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun quantityMultipliesEveryPerPieceCostButNotTheAdministrativeOne() {
        val settings = spreadsheetSettings.copy(administrativeCost = 50.0)

        val one = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings)
        val ten = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, quantity = 10)

        assertEquals(one.costs.material * 10, ten.costs.material, 1e-9)
        assertEquals(one.costs.energy * 10, ten.costs.energy, 1e-9)
        assertEquals(one.costs.investmentReturn * 10, ten.costs.investmentReturn, 1e-9)
        assertEquals(one.filamentWeightGrams * 10, ten.filamentWeightGrams, 1e-9)
        // A modelagem é feita uma vez pro pedido, não uma vez por peça.
        assertEquals(50.0, ten.costs.administrative, 1e-9)
        assertEquals(10, ten.quantity)
    }

    @Test
    fun setupTimeIsChargedOncePerOrderSoTheUnitPriceFallsWithQuantity() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0)
        val job = spreadsheetJob.copy(laborMinutes = 3.0)

        val one = PricingCalculator.calculate(settings = settings, job = job, printer = spreadsheetPrinter, quantity = 1, setupMinutes = 20.0)
        val ten = PricingCalculator.calculate(settings = settings, job = job, printer = spreadsheetPrinter, quantity = 10, setupMinutes = 20.0)

        // Preparo (20 min) cobrado uma vez nos dois; o trabalho por peça (3 min) é que multiplica.
        // Números iguais aos da tabela "Quantidade e lote" de docs/pricing-formulas.md.
        assertEquals(11.50, one.costs.labor, CENT_TOLERANCE)
        assertEquals(25.00, ten.costs.labor, CENT_TOLERANCE)
        assertEquals(2.50, ten.costs.labor / 10, CENT_TOLERANCE)

        // É isso que faz a unidade sair mais barata no lote, sem desconto artificial nenhum.
        assertTrue(ten.unitSalePrice < one.unitSalePrice)
        assertEquals(ten.salePrice / 10, ten.unitSalePrice, 1e-9)
    }

    @Test
    fun quantityOfOneKeepsTheExactSameResultAsBefore() {
        val explicit = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, quantity = 1)
        val default = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)

        assertEquals(default.productionCost, explicit.productionCost, 1e-9)
        assertEquals(default.salePrice, explicit.unitSalePrice, 1e-9)
    }

    @Test
    fun invalidQuantityOrSetupTimeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, quantity = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, setupMinutes = -1.0)
        }
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
    fun channelFeeRaisesSalePriceButKeepsRealProfitUnchanged() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.15)

        val direct = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        val viaShopee = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, channel = shopee)

        // Preço de tabela sobe pra compensar o desconto do canal...
        assertEquals(17.02, direct.salePrice, CENT_TOLERANCE)
        assertEquals(17.02 / 0.85, viaShopee.salePrice, CENT_TOLERANCE)
        // ...mas o lucro real (depois de o canal descontar a parte dele) fica igual.
        assertEquals(direct.profit, viaShopee.profit, 1e-9)
        assertEquals("Shopee", viaShopee.channelName)
    }

    @Test
    fun directSaleHasNoDeductionEvenWithChannelsRegistered() {
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, channel = null)

        assertEquals(0.0, quote.totalDeductionRate, 1e-9)
        assertEquals(17.02, quote.salePrice, CENT_TOLERANCE)
        assertEquals(null, quote.channelName)
    }

    @Test
    fun taxAndChannelFeeAddUpBeforeRaisingThePrice() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.15)
        val withTax = spreadsheetSettings.copy(taxRate = 0.06)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, withTax, channel = shopee)

        // Vender a P deixa P · (1 − 0,15 − 0,06) na mão; o preço sobe o bastante pra sobrar a
        // mesma coisa de uma venda sem dedução nenhuma.
        assertEquals(0.21, quote.totalDeductionRate, 1e-9)
        assertEquals(17.02 / 0.79, quote.salePrice, CENT_TOLERANCE)
        val direct = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        assertEquals(direct.profit, quote.profit, 1e-9)
    }

    /** Trava o exemplo da seção "Deduções da venda e frete" de pricing-formulas.md. */
    @Test
    fun documentedDeductionExampleMatches() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20)
        val settings = spreadsheetSettings.copy(taxRate = 0.06)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, channel = shopee)

        assertEquals(0.26, quote.totalDeductionRate, 1e-9)
        assertEquals(23.00, quote.salePrice, CENT_TOLERANCE)
        // Sobra o mesmo da venda direta: R$ 17,02 menos o custo de produção.
        assertEquals(17.02 - 8.51, quote.profit, CENT_TOLERANCE)
    }

    @Test
    fun deductionsThatEatTheWholeSaleAreRejected() {
        val absurdChannel = SalesChannel(id = "x", name = "Canal impossível", feeRate = 0.95)
        val withTax = spreadsheetSettings.copy(taxRate = 0.06)

        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, withTax, channel = absurdChannel)
        }
    }

    @Test
    fun invalidRatesAreRejected() {
        assertFailsWith<IllegalArgumentException> { SalesChannel(id = "x", name = "X", feeRate = -0.1) }
        assertFailsWith<IllegalArgumentException> { SalesChannel(id = "x", name = "X", feeRate = 1.0) }
        assertFailsWith<IllegalArgumentException> { spreadsheetSettings.copy(taxRate = 1.0) }
    }

    private companion object {
        /** A planilha exibe valores com 2 casas; aceitamos diferença de até meio centavo. */
        const val CENT_TOLERANCE = 0.005
    }
}
