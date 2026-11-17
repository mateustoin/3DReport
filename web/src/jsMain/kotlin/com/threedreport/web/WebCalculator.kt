@file:OptIn(ExperimentalJsExport::class)

package com.threedreport.web

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.pricing.PricingCalculator
import com.threedreport.core.pricing.PricingError

/**
 * Resultado da calculadora do site, só com números pra o JavaScript da página formatar. Valores
 * em R$ são do pedido inteiro, como em [com.threedreport.core.model.Quote]. Quando a entrada não
 * fecha a conta, [error] traz a mensagem do `core` e os números ficam em zero.
 */
@JsExport
class WebQuote(
    val salePrice: Double = 0.0,
    val unitSalePrice: Double = 0.0,
    val productionCost: Double = 0.0,
    val profit: Double = 0.0,
    val breakEvenSalePrice: Double = 0.0,
    val filamentWeightGrams: Double = 0.0,
    val material: Double = 0.0,
    val energy: Double = 0.0,
    val failures: Double = 0.0,
    val finishing: Double = 0.0,
    val labor: Double = 0.0,
    val error: String? = null,
)

/**
 * Calcula o orçamento pela mesma fórmula do app ([PricingCalculator]). Esta função **não faz
 * conta nenhuma**: só converte o que a página recebe (gramas, percentuais de 0 a 100) nos modelos
 * do `core`. Custos que dependem de cadastro no app (manutenção e retorno da máquina, custo fixo
 * mensal, administrativo) ficam de fora e entram como zero.
 *
 * @param filamentGrams peso de uma peça, como o fatiador informa.
 * @param laborMinutes seu tempo de trabalho no pedido inteiro, cobrado uma vez (decisão 94).
 */
@JsExport
fun calculateQuote(
    filamentGrams: Double,
    printTimeMinutes: Double,
    filamentPricePerKg: Double,
    printerPowerWatts: Double,
    energyPricePerKwh: Double,
    profitMarginPercent: Double,
    failureRatePercent: Double,
    finishingRatePercent: Double,
    laborRatePerHour: Double,
    laborMinutes: Double,
    quantity: Int,
    channelFeePercent: Double,
    channelFixedFeePerItem: Double,
    taxPercent: Double,
): WebQuote = if (channelFeePercent + taxPercent >= 100) {
    // O `core` recusa o mesmo caso, mas a conferência fica também aqui, nos percentuais digitados: 100% de
    // canal (ou de imposto) sozinho é recusado antes, ao montar o canal, e 70% + 30% em fração dá
    // 0,9999..., que o `core` aceitaria.
    WebQuote(error = DEDUCTIONS_REACH_FULL_PRICE_MESSAGE)
} else {
    try {
        val filament = Filament(
            id = "web",
            name = "Filamento",
            pricePerKg = filamentPricePerKg,
            densityGPerCm3 = WEB_DENSITY_G_PER_CM3,
        )
        val job = PrintJob(
            filament = filament,
            filamentLengthMeters = filamentGrams / filament.weightGrams(1.0),
            printTimeMinutes = printTimeMinutes,
        )
        val printer = PrinterProfile(
            id = "web",
            name = "Impressora",
            printerPowerWatts = printerPowerWatts,
            maintenanceCostPerHour = 0.0,
            machineInvestment = MachineInvestment(
                machinePrice = 0.0,
                paybackMonths = 1,
                printingDaysPerMonth = 1,
                printingHoursPerDay = 1.0,
            ),
        )
        val settings = PricingSettings(
            energyPricePerKwh = energyPricePerKwh,
            failureRate = failureRatePercent / 100,
            finishingRate = finishingRatePercent / 100,
            laborRatePerHour = laborRatePerHour,
            taxRate = taxPercent / 100,
            profitMargin = profitMarginPercent / 100,
        )
        val channel = if (channelFeePercent > 0 || channelFixedFeePerItem > 0) {
            SalesChannel(id = "web", name = "Canal", feeRate = channelFeePercent / 100, fixedFeePerItem = channelFixedFeePerItem)
        } else {
            null
        }
        val quote = PricingCalculator.calculate(
            job = job,
            printer = printer,
            settings = settings,
            channel = channel,
            quantity = quantity,
            laborMinutes = laborMinutes,
        )
        WebQuote(
            salePrice = quote.salePrice,
            unitSalePrice = quote.unitSalePrice,
            productionCost = quote.productionCost,
            profit = quote.profit,
            breakEvenSalePrice = quote.breakEvenSalePrice,
            filamentWeightGrams = quote.filamentWeightGrams,
            material = quote.costs.material,
            energy = quote.costs.energy,
            failures = quote.costs.failures,
            finishing = quote.costs.finishing,
            labor = quote.costs.labor,
        )
    } catch (error: PricingError) {
        WebQuote(error = errorMessage(error))
    } catch (_: IllegalArgumentException) {
        // As mensagens do `core` são pra quem programa (nomes de campo em inglês); aqui o motivo é sempre um valor negativo.
        WebQuote(error = "Confira os valores: nenhum deles pode ser negativo.")
    }
}

/**
 * A frase de cada erro de preço do `core` na calculadora do site. Sem o "Revise em Configurações" do app:
 * no site não há Configurações.
 */
private fun errorMessage(error: PricingError): String = when (error) {
    is PricingError.DeductionsReachFullPrice -> DEDUCTIONS_REACH_FULL_PRICE_MESSAGE
}

private const val DEDUCTIONS_REACH_FULL_PRICE_MESSAGE =
    "A taxa do canal somada ao imposto chega a 100% do valor de venda: não sobra nada pra você."

// Com a entrada em gramas, a densidade só serve pra ir e voltar do comprimento: não muda o custo.
private const val WEB_DENSITY_G_PER_CM3 = 1.24
