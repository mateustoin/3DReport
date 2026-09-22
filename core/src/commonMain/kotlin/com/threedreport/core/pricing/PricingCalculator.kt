package com.threedreport.core.pricing

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote

/**
 * Motor de cálculo de orçamento de impressão 3D.
 *
 * Função pura e sem estado: mesma entrada, mesma saída. As fórmulas estão
 * documentadas em `docs/pricing-formulas.md`.
 */
object PricingCalculator {

    /**
     * @param appliesMarketplaceFee se `true`, aumenta o valor de venda o
     *   suficiente para compensar `settings.marketplaceFeeRate` — a margem
     *   de lucro real (ver [Quote.profit]) fica igual à de uma venda sem
     *   marketplace, só o preço de tabela muda.
     */
    fun calculate(
        job: PrintJob,
        printer: PrinterProfile,
        settings: PricingSettings,
        appliesMarketplaceFee: Boolean = false,
        quantity: Int = 1,
        setupMinutes: Double = 0.0,
    ): Quote {
        require(quantity >= 1) { "quantity deve ser pelo menos 1: $quantity" }
        require(setupMinutes >= 0) { "setupMinutes não pode ser negativo: $setupMinutes" }

        val hours = job.printTimeHours
        val weightGrams = job.filament.weightGrams(job.filamentLengthMeters)

        val material = weightGrams / GRAMS_PER_KG * job.filament.pricePerKg
        val energy = hours * (printer.printerPowerWatts / WATTS_PER_KW) * settings.energyPricePerKwh
        val maintenance = hours * printer.maintenanceCostPerHour
        val investmentReturn = hours * printer.machineInvestment.costPerHour
        val fixedCost = hours * settings.fixedCostPerHour
        val labor = job.laborHours * settings.laborRatePerHour
        // Acabamento como percentual do material só sobrevive enquanto não há mão de obra
        // configurada (ver KDoc de PricingSettings.finishingRate): lixar e pintar custa tempo, não
        // gramas de plástico, então quem cobra por hora já cobra acabamento em `labor`.
        val finishing = if (settings.chargesLaborByTime) 0.0 else material * settings.finishingRate

        // Preparar o arquivo, fatiar e montar a mesa se faz uma vez só, não uma vez por peça — é
        // isso que faz o preço unitário cair quando a quantidade sobe, sem desconto artificial.
        val setupCost = setupMinutes / MINUTES_PER_HOUR * settings.laborRatePerHour

        // Tudo que se paga de novo ao reimprimir o que falhou. O administrativo fica de fora: uma
        // modelagem já feita não precisa ser refeita.
        val perUnitReprintableCost = material + energy + maintenance + investmentReturn + fixedCost + labor + finishing
        val reprintableCost = perUnitReprintableCost * quantity + setupCost

        val costs = CostBreakdown(
            material = material * quantity,
            energy = energy * quantity,
            maintenance = maintenance * quantity,
            failures = reprintableCost * settings.failureRate,
            finishing = finishing * quantity,
            investmentReturn = investmentReturn * quantity,
            administrative = settings.administrativeCost,
            labor = labor * quantity + setupCost,
            fixedCost = fixedCost * quantity,
        )

        val productionCost = costs.total
        val baseSalePrice = productionCost * (1 + settings.profitMargin)
        val feeRate = if (appliesMarketplaceFee) settings.marketplaceFeeRate else 0.0
        val salePrice = if (feeRate > 0.0) baseSalePrice / (1 - feeRate) else baseSalePrice

        return Quote(
            job = job,
            filamentWeightGrams = weightGrams * quantity,
            costs = costs,
            productionCost = productionCost,
            salePrice = salePrice,
            marketplaceFeeRate = feeRate,
            printerId = printer.id,
            printerName = printer.name,
            quantity = quantity,
            setupMinutes = setupMinutes,
        )
    }

    private const val GRAMS_PER_KG = 1000.0
    private const val WATTS_PER_KW = 1000.0
    private const val MINUTES_PER_HOUR = 60.0
}
