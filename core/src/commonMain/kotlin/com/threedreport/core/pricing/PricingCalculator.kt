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
    ): Quote {
        val hours = job.printTimeHours
        val weightGrams = job.filament.weightGrams(job.filamentLengthMeters)

        val material = weightGrams / GRAMS_PER_KG * job.filament.pricePerKg
        val energy = hours * (printer.printerPowerWatts / WATTS_PER_KW) * settings.energyPricePerKwh

        val costs = CostBreakdown(
            material = material,
            energy = energy,
            maintenance = hours * printer.maintenanceCostPerHour,
            failures = material * settings.failureRate,
            finishing = material * settings.finishingRate,
            investmentReturn = hours * printer.machineInvestment.costPerHour,
            administrative = settings.administrativeCost,
        )

        val productionCost = costs.total
        val baseSalePrice = productionCost * (1 + settings.profitMargin)
        val feeRate = if (appliesMarketplaceFee) settings.marketplaceFeeRate else 0.0
        val salePrice = if (feeRate > 0.0) baseSalePrice / (1 - feeRate) else baseSalePrice

        return Quote(
            job = job,
            filamentWeightGrams = weightGrams,
            costs = costs,
            productionCost = productionCost,
            salePrice = salePrice,
            marketplaceFeeRate = feeRate,
            printerId = printer.id,
            printerName = printer.name,
        )
    }

    private const val GRAMS_PER_KG = 1000.0
    private const val WATTS_PER_KW = 1000.0
}
