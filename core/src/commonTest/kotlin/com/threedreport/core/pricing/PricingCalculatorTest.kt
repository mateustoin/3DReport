package com.threedreport.core.pricing

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.SalesChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            // Quem cobra lixar e pintar nos 40 min de trabalho zera o percentual.
            finishingRate = 0.0,
            monthlyFixedCost = 800.0,
            productiveHoursPerMonth = 200.0,
        )
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, laborMinutes = 40.0)

        assertEquals(12.67, quote.costs.fixedCost, CENT_TOLERANCE)
        assertEquals(20.00, quote.costs.labor, CENT_TOLERANCE)
        assertEquals(0.0, quote.costs.finishing, 1e-9)
        assertEquals(4.00, quote.costs.failures, CENT_TOLERANCE)
        assertEquals(44.05, quote.productionCost, CENT_TOLERANCE)
        // A margem de 100% vale sobre os R$ 24,05 que não são trabalho; os R$ 20,00 da hora entram pelo
        // valor (decisão 118). Antes, com a margem em cima de tudo, dava R$ 88,10.
        assertEquals(68.10, quote.salePrice, CENT_TOLERANCE)
    }

    @Test
    fun laborAddsExactlyItsValueToThePriceWithoutMargin() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0, failureRate = 0.0)

        val without = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings)
        val with = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, laborMinutes = 40.0)

        assertEquals(20.0, with.salePrice - without.salePrice, 1e-9)
        assertEquals(without.profit, with.profit, 1e-9)
    }

    @Test
    fun theObtainedMarginIsTheConfiguredOneEvenWithLabor() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, laborMinutes = 40.0)

        assertEquals(settings.profitMargin, quote.actualProfitMargin, 1e-9)
    }

    @Test
    fun laborIsChargedByTheTimeInformedForTheOrder() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0)

        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, laborMinutes = 40.0).costs

        assertEquals(20.0, costs.labor, 1e-9)
    }

    @Test
    fun finishingPercentageAndLaborAreIndependentAndBothAdd() {
        val costs = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings.copy(laborRatePerHour = 30.0),
            laborMinutes = 40.0,
        ).costs

        assertEquals(0.36, costs.finishing, CENT_TOLERANCE)
        assertEquals(20.0, costs.labor, 1e-9)
    }

    /**
     * O caso relatado por um usuário: com a hora em zero, anotar o preço; configurar R$ 50/h; o
     * preço caía, porque o acabamento saía e os minutos de trabalho ainda estavam vazios.
     */
    @Test
    fun configuringAnHourlyRateNeverLowersThePrice() {
        val withoutRate = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        val withRate = spreadsheetSettings.copy(laborRatePerHour = 50.0)

        listOf(0.0, 10.0).forEach { minutes ->
            val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, withRate, laborMinutes = minutes)
            assertEquals(withoutRate.costs.finishing, quote.costs.finishing, 1e-9)
            assertTrue(quote.salePrice >= withoutRate.salePrice, "com $minutes min, o preço caiu")
        }
    }

    @Test
    fun failureReserveCoversEveryCostExceptTheAdministrativeOne() {
        val settings = spreadsheetSettings.copy(administrativeCost = 50.0, laborRatePerHour = 30.0)
        val costs = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, laborMinutes = 40.0).costs

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
    fun laborTimeIsChargedOncePerOrderSoTheUnitPriceFallsWithQuantity() {
        val settings = spreadsheetSettings.copy(laborRatePerHour = 30.0)

        // O tempo digitado é do pedido inteiro: 23 min pra uma peça, 50 min pro lote de 10 (os 20
        // de preparo mais 3 por peça). Números iguais aos da tabela "Quantidade e lote" de
        // docs/pricing-formulas.md.
        val one = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, quantity = 1, laborMinutes = 23.0)
        val ten = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, quantity = 10, laborMinutes = 50.0)

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
    fun invalidQuantityOrLaborTimeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, quantity = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, laborMinutes = -1.0)
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
    fun negotiatedPriceReplacesTheTablePriceAndReflectsInTheProfit() {
        val table = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)

        val negotiated = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings,
            negotiatedSalePrice = 12.00,
        )

        // Vira o preço de verdade, não uma simulação à parte: é o que vai pro histórico e pro
        // Dashboard, então lucro e margem têm que acompanhar.
        assertEquals(12.00, negotiated.salePrice, 1e-9)
        assertEquals(12.00 - table.productionCost, negotiated.profit, CENT_TOLERANCE)
        assertTrue(negotiated.profit < table.profit)
        assertEquals(table.productionCost, negotiated.productionCost, 1e-9)
    }

    @Test
    fun tablePriceIsKeptOnlyWhenThePriceIsNegotiated() {
        val table = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        val discounted = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings,
            negotiatedSalePrice = table.salePrice - 3.0,
        )
        val above = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings,
            negotiatedSalePrice = table.salePrice + 2.0,
        )

        assertNull(table.tableSalePrice)
        assertEquals(0.0, table.negotiatedDiscount)
        assertEquals(table.salePrice, discounted.tableSalePrice!!, 1e-9)
        assertEquals(3.0, discounted.negotiatedDiscount, 1e-9)
        // Cobrar acima da tabela entra como desconto negativo, pra a soma do período bater com o faturamento.
        assertEquals(-2.0, above.negotiatedDiscount, 1e-9)
    }

    @Test
    fun negotiatingBelowTheProductionCostShowsUpAsNegativeProfit() {
        val quote = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings,
            negotiatedSalePrice = 5.00,
        )

        assertTrue(quote.profit < 0, "vender abaixo do custo tem que aparecer como prejuízo")
    }

    @Test
    fun breakEvenPriceCoversProductionAfterDeductions() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20)
        val settings = spreadsheetSettings.copy(taxRate = 0.06)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, channel = shopee)

        // Vender exatamente pelo ponto de equilíbrio zera o lucro, nem um centavo a mais.
        val atBreakEven = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            settings,
            channel = shopee,
            negotiatedSalePrice = quote.breakEvenSalePrice,
        )
        assertEquals(0.0, atBreakEven.profit, 1e-9)
    }

    @Test
    fun actualMarginMatchesTheConfiguredOneWhenThePriceIsNotNegotiated() {
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)

        // Margem configurada de 100% precisa aparecer como 100% obtidos.
        assertEquals(1.0, quote.actualProfitMargin, 1e-9)
    }

    @Test
    fun negativeNegotiatedPriceIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, negotiatedSalePrice = -1.0)
        }
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

    @Test
    fun eachFilamentOfAMulticolorPrintIsChargedAtItsOwnPrice() {
        val petg = Filament(id = "petg", name = "PETG", pricePerKg = 200.0, densityGPerCm3 = 1.24)
        val multicolor = PrintJob(
            filaments = listOf(FilamentUsage(pla, 6.0), FilamentUsage(petg, 6.0)),
            printTimeMinutes = 190.0,
        )

        val quote = PricingCalculator.calculate(multicolor, spreadsheetPrinter, spreadsheetSettings)
        val allPla = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)

        // Metade do comprimento em PETG, que custa o dobro: material 1,5 vez o do mesmo tanto em PLA.
        // Antes, o G-code somava os extrusores e tudo saía pelo preço de um filamento só.
        assertEquals(allPla.costs.material * 1.5, quote.costs.material, 1e-9)
        assertEquals(allPla.costs.finishing * 1.5, quote.costs.finishing, 1e-9)
        assertEquals(allPla.filamentWeightGrams, quote.filamentWeightGrams, 1e-9)
        assertEquals(allPla.costs.energy, quote.costs.energy, 1e-9)
        assertEquals(listOf("PLA", "PETG"), quote.filamentTotals().map { it.filament.name })
    }

    @Test
    fun eachPrintUsesTheCostsOfItsOwnPrinter() {
        val smallPrinter = spreadsheetPrinter.copy(id = "small", name = "Pequena", printerPowerWatts = 120.0, maintenanceCostPerHour = 0.05)
        val head = PrintJob(filament = pla, filamentLengthMeters = 12.0, printTimeMinutes = 190.0)
        val base = PrintJob(filament = pla, filamentLengthMeters = 4.0, printTimeMinutes = 60.0)

        val order = PricingCalculator.calculate(listOf(head to spreadsheetPrinter, base to smallPrinter), spreadsheetSettings)
        val headAlone = PricingCalculator.calculate(head, spreadsheetPrinter, spreadsheetSettings)
        val baseAlone = PricingCalculator.calculate(base, smallPrinter, spreadsheetSettings)

        assertEquals(headAlone.costs.energy + baseAlone.costs.energy, order.costs.energy, 1e-9)
        assertEquals(headAlone.costs.maintenance + baseAlone.costs.maintenance, order.costs.maintenance, 1e-9)
        assertEquals(listOf("printer", "small"), order.prints.map { it.printerId })
        assertEquals(headAlone.prints.single().cost, order.prints[0].cost)
        assertEquals(190.0, order.printMinutesOn("printer"), 1e-9)
        assertEquals(60.0, order.printMinutesOn("small"), 1e-9)
        assertEquals(250.0, order.totalPrintTimeMinutes, 1e-9)
    }

    @Test
    fun whatBelongsToTheOrderIsChargedOnceNoMatterHowManyPrints() {
        val settings = spreadsheetSettings.copy(administrativeCost = 20.0, laborRatePerHour = 30.0)
        val part = PrintJob(filament = pla, filamentLengthMeters = 6.0, printTimeMinutes = 95.0)

        val order = PricingCalculator.calculate(
            listOf(part to spreadsheetPrinter, part to spreadsheetPrinter, part to spreadsheetPrinter),
            settings,
            laborMinutes = 40.0,
        )

        // Três orçamentos separados cobrariam a modelagem e o trabalho três vezes.
        assertEquals(20.0, order.costs.administrative, 1e-9)
        assertEquals(20.0, order.costs.labor, 1e-9)
        val reprintable = order.prints.sumOf { it.cost.total } + order.costs.labor
        assertEquals(reprintable * 0.10, order.costs.failures, 1e-9)
    }

    @Test
    fun runsRepeatThePrintBeforeTheQuantityMultiplies() {
        val plate = spreadsheetJob
        val once = PricingCalculator.calculate(plate, spreadsheetPrinter, spreadsheetSettings)
        val fourRuns = PricingCalculator.calculate(plate.copy(runs = 4), spreadsheetPrinter, spreadsheetSettings, quantity = 2)

        assertEquals(once.costs.material * 8, fourRuns.costs.material, 1e-9)
        assertEquals(once.costs.energy * 8, fourRuns.costs.energy, 1e-9)
        assertEquals(once.filamentWeightGrams * 8, fourRuns.filamentWeightGrams, 1e-9)
        assertEquals(190.0 * 8, fourRuns.totalPrintTimeMinutes, 1e-9)
    }

    @Test
    fun aPrintWithoutFilamentOrAnOrderWithoutPrintsIsRejected() {
        assertFailsWith<IllegalArgumentException> { PrintJob(filaments = emptyList(), printTimeMinutes = 10.0) }
        assertFailsWith<IllegalArgumentException> { spreadsheetJob.copy(runs = 0) }
        assertFailsWith<IllegalArgumentException> { PricingCalculator.calculate(emptyList(), spreadsheetSettings) }
    }

    @Test
    fun channelIdAndNameAreKeptForReopeningAndRepricing() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.15)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, channel = shopee)

        assertEquals("shopee", quote.channelId)
        assertEquals(0.15, quote.channelFeeRate, 1e-9)
    }

    /**
     * Trava o exemplo de "Serviços e frete também pagam a taxa" em pricing-formulas.md (decisão 107):
     * a peça de R$ 17,02 (venda direta) com R$ 10 de serviço e R$ 15 de frete, pela Shopee (20%) com
     * 6% de Simples. O canal e o imposto levam 26% de tudo o que o cliente paga, então a peça sobe o
     * suficiente pra cobrir também a parte que sai do serviço e do frete.
     */
    @Test
    fun documentedExtrasDeductionExampleMatches() {
        val shopee = SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20)
        val settings = spreadsheetSettings.copy(taxRate = 0.06)

        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, settings, channel = shopee, extrasTotal = 25.0)

        assertEquals(31.78, quote.salePrice, CENT_TOLERANCE)
        assertEquals(56.78, quote.customerTotal, CENT_TOLERANCE)
        // O lucro continua o de uma venda direta sem serviço nem frete.
        assertEquals(17.02 - 8.51, quote.profit, CENT_TOLERANCE)
    }

    @Test
    fun extrasWithoutDeductionsDoNotChangeThePiecePrice() {
        val plain = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings)
        val withExtras = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, extrasTotal = 40.0)

        assertEquals(plain.salePrice, withExtras.salePrice, 1e-9)
        assertEquals(plain.profit, withExtras.profit, 1e-9)
        assertEquals(plain.salePrice + 40.0, withExtras.customerTotal, 1e-9)
    }

    @Test
    fun breakEvenWithExtrasCoversTheFeesOnServicesAndShipping() {
        val card = SalesChannel(id = "cartao", name = "Cartão", feeRate = 0.05)
        val quote = PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, channel = card, extrasTotal = 30.0)

        val atBreakEven = PricingCalculator.calculate(
            spreadsheetJob,
            spreadsheetPrinter,
            spreadsheetSettings,
            channel = card,
            negotiatedSalePrice = quote.breakEvenSalePrice,
            extrasTotal = 30.0,
        )
        assertEquals(0.0, atBreakEven.profit, 1e-9)
        // Sem contar a taxa sobre os R$ 30, o mínimo seria menor e o vendedor pagaria pra vender.
        assertTrue(quote.breakEvenSalePrice > quote.productionCost / (1 - 0.05))
    }

    @Test
    fun negativeExtrasAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            PricingCalculator.calculate(spreadsheetJob, spreadsheetPrinter, spreadsheetSettings, extrasTotal = -1.0)
        }
    }

    private companion object {
        /** A planilha exibe valores com 2 casas; aceitamos diferença de até meio centavo. */
        const val CENT_TOLERANCE = 0.005
    }
}
