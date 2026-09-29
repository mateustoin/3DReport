package com.threedreport.core.pricing

import com.threedreport.core.model.ChannelFeeSchedule
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SalesChannel

/**
 * Motor de cálculo de orçamento de impressão 3D.
 *
 * Função pura e sem estado: mesma entrada, mesma saída. As fórmulas estão
 * documentadas em `docs/pricing-formulas.md`.
 */
object PricingCalculator {

    /**
     * @param channel canal de venda escolhido pra este orçamento, ou `null`
     *   na venda direta. A taxa dele, somada ao imposto de
     *   [PricingSettings.taxRate], aumenta o valor de venda o suficiente pra
     *   compensar o que é descontado — a margem de lucro real (ver
     *   [Quote.profit]) fica igual à de uma venda sem dedução nenhuma, só o
     *   preço de tabela muda.
     * @param negotiatedSalePrice preço fechado na conversa com o cliente, em
     *   vez do que a margem configurada daria. Quando informado, vira o
     *   [Quote.salePrice] de verdade, e não um número só de simulação: assim
     *   o lucro, o histórico e o Dashboard passam a falar do valor que foi
     *   realmente cobrado. O custo de produção não muda, então um preço
     *   abaixo dele aparece como lucro negativo, que é o aviso de prejuízo.
     * @param extrasTotal serviços e frete cobrados junto (decisão 107): não passam pela margem, mas o
     *   canal e o imposto levam a parte deles, então o preço da peça sobe o suficiente pra cobrir isso.
     */
    fun calculate(
        job: PrintJob,
        printer: PrinterProfile,
        settings: PricingSettings,
        channel: SalesChannel? = null,
        quantity: Int = 1,
        laborMinutes: Double = 0.0,
        negotiatedSalePrice: Double? = null,
        extrasTotal: Double = 0.0,
    ): Quote = calculate(
        prints = listOf(job to printer),
        settings = settings,
        channel = channel,
        quantity = quantity,
        laborMinutes = laborMinutes,
        negotiatedSalePrice = negotiatedSalePrice,
        extrasTotal = extrasTotal,
    )

    /**
     * O pedido com várias impressões (leva 9, decisão 105): cada uma com a própria impressora. O
     * material é somado filamento por filamento, e energia, manutenção, retorno da máquina e custo
     * fixo usam as horas e a impressora **daquela** impressão. Só depois de somar as impressões
     * entra o que é do pedido, uma vez só: o tempo de trabalho, o administrativo, a reserva de
     * falha, a margem (sobre tudo menos o trabalho, decisão 118), o canal, o imposto e o preço negociado. Somar orçamentos separados cobraria
     * o administrativo uma vez por impressão.
     *
     * @param prints as impressões, na ordem da tela, cada uma com a impressora em que roda.
     * @param laborMinutes todo o seu tempo de trabalho no pedido, cobrado uma vez (decisão 94).
     * @param extrasTotal serviços e frete cobrados junto com a peça (ver [Quote.extrasTotal]). O frete
     *   grátis não entra aqui, e sim em [absorbedShippingCost].
     * @param services os serviços cobrados (já somados em [extrasTotal]), só pra os que têm tempo
     *   informado contarem no lucro pelo que rendem acima da sua hora (decisão 123).
     * @param consumables insumos usados no pedido (decisão 122).
     * @param rush pedido urgente: a peça ganha [PricingSettings.rushSurchargeRate] (decisão 125).
     * @param absorbedShippingCost frete que você paga pra o cliente receber de graça (decisão 124).
     */
    fun calculate(
        prints: List<Pair<PrintJob, PrinterProfile>>,
        settings: PricingSettings,
        channel: SalesChannel? = null,
        quantity: Int = 1,
        laborMinutes: Double = 0.0,
        negotiatedSalePrice: Double? = null,
        extrasTotal: Double = 0.0,
        services: List<QuoteService> = emptyList(),
        consumables: List<QuotedConsumable> = emptyList(),
        rush: Boolean = false,
        absorbedShippingCost: Double = 0.0,
    ): Quote {
        require(prints.isNotEmpty()) { "um orçamento precisa de pelo menos uma impressão" }
        require(negotiatedSalePrice == null || negotiatedSalePrice >= 0) {
            "negotiatedSalePrice não pode ser negativo: $negotiatedSalePrice"
        }
        require(quantity >= 1) { "quantity deve ser pelo menos 1: $quantity" }
        require(laborMinutes >= 0) { "laborMinutes não pode ser negativo: $laborMinutes" }
        require(extrasTotal >= 0) { "extrasTotal não pode ser negativo: $extrasTotal" }
        require(absorbedShippingCost >= 0) { "absorbedShippingCost não pode ser negativo: $absorbedShippingCost" }

        val quotedPrints = prints.map { (job, printer) ->
            QuotedPrint(job = job, printerId = printer.id, printerName = printer.name, cost = printCost(job, printer, settings, quantity))
        }

        // Tempo de trabalho é do pedido: fatiar, montar a mesa, tirar, lixar e embalar se contam
        // uma vez, e é isso que faz o preço unitário cair quando a quantidade sobe.
        val labor = laborMinutes / MINUTES_PER_HOUR * settings.laborRatePerHour

        // Tudo que se paga de novo ao reimprimir o que falhou. O administrativo e os insumos ficam de fora:
        // uma modelagem já feita não precisa ser refeita, e argola e caixa entram depois da impressão.
        val reprintableCost = quotedPrints.sumOf { it.cost.total } + labor

        val costs = CostBreakdown(
            material = quotedPrints.sumOf { it.cost.material },
            energy = quotedPrints.sumOf { it.cost.energy },
            maintenance = quotedPrints.sumOf { it.cost.maintenance },
            failures = reprintableCost * settings.failureRate,
            finishing = quotedPrints.sumOf { it.cost.finishing },
            investmentReturn = quotedPrints.sumOf { it.cost.investmentReturn },
            administrative = settings.administrativeCost,
            labor = labor,
            fixedCost = quotedPrints.sumOf { it.cost.fixedCost },
            consumables = consumables.sumOf { it.total(quantity) },
        )

        // A margem vale sobre tudo menos a sua hora (decisão 118): o trabalho entra pelo valor dele. Com a
        // margem em cima, a hora cobrada virava o dobro, e o preço saía de um valor que ninguém paga. Os
        // insumos estão em costs.total, então passam pela margem como o material (decisão 122).
        val marginPrice = (costs.total - labor) * (1 + settings.profitMargin) + labor

        // Urgência é um acréscimo sobre a peça inteira, trabalho incluso, antes do canal e do imposto: é
        // cobrar mais por furar a fila, não um custo a mais (decisão 125).
        val rushSurcharge = if (rush) marginPrice * settings.rushSurchargeRate else 0.0
        val baseSalePrice = marginPrice + rushSurcharge

        // Canal e imposto são descontados do mesmo valor recebido, e o canal ainda leva um valor fixo por
        // item (decisão 121). O valor recebido é o total do cliente, serviços e frete inclusos (decisão
        // 107): a peça sobe o suficiente pra que, depois das deduções sobre tudo, sobre a base da peça mais
        // o repasse de serviços e frete. Com faixas de preço, a tabela escolhe a faixa coerente.
        val feeSchedule = channel?.feeSchedule ?: ChannelFeeSchedule.NONE
        val computedTablePrice = feeSchedule.priceLeaving(baseSalePrice, extrasTotal, quantity, settings.taxRate)

        // O preço mínimo do pedido é o último degrau da tabela (decisão 125): vale sobre a peça inteira,
        // sem serviços nem frete. Preço fechado com o cliente não é tabela, então passa por cima.
        val minimumApplies = settings.minimumOrderPrice > 0 && computedTablePrice < settings.minimumOrderPrice
        val tableSalePrice = if (minimumApplies) settings.minimumOrderPrice else computedTablePrice
        val salePrice = negotiatedSalePrice ?: tableSalePrice

        // O que o canal de fato cobra depende da faixa em que o preço final caiu.
        val appliedFee = feeSchedule.feeAt(salePrice / quantity)

        // Serviço com tempo informado conta no lucro pelo que rende acima da sua hora (decisão 123); sem
        // tempo, continua repasse (decisão 25).
        val timedServices = services.filter { it.isTimed }
        val serviceLaborMinutes = timedServices.sumOf { it.totalLaborMinutes(quantity) }
        val serviceLaborCost = serviceLaborMinutes / MINUTES_PER_HOUR * settings.laborRatePerHour
        val serviceProfit = timedServices.sumOf { it.total(quantity) } - serviceLaborCost

        // Frete grátis sai do lucro, não do preço (decisão 124). Pra quem quiser manter a margem, a peça
        // que cobre o frete e ainda deixa o que o preço de tabela deixaria. Parte do preço de tabela final
        // (com o mínimo aplicado), senão a sugestão podia ficar abaixo do preço de hoje.
        val shippingCoveringSalePrice = if (absorbedShippingCost > 0) {
            val tableReceipt = feeSchedule.receiptAt(tableSalePrice, extrasTotal, quantity, settings.taxRate) - extrasTotal
            feeSchedule.priceLeaving(tableReceipt + absorbedShippingCost, extrasTotal, quantity, settings.taxRate)
        } else {
            null
        }

        return Quote(
            prints = quotedPrints,
            costs = costs,
            salePrice = salePrice,
            quantity = quantity,
            laborMinutes = laborMinutes,
            channelId = channel?.id,
            channelName = channel?.name,
            channelFeeRate = appliedFee.feeRate,
            taxRate = settings.taxRate,
            tableSalePrice = tableSalePrice.takeIf { negotiatedSalePrice != null },
            extrasTotal = extrasTotal,
            consumables = consumables,
            channelFixedFee = appliedFee.fixedFeePerItem * quantity,
            channelFeeSchedule = channel?.feeSchedule,
            serviceLaborMinutes = serviceLaborMinutes,
            serviceLaborCost = serviceLaborCost,
            serviceProfit = serviceProfit,
            absorbedShippingCost = absorbedShippingCost,
            shippingCoveringSalePrice = shippingCoveringSalePrice,
            rush = rush,
            // O acréscimo só é o que o cliente paga quando o preço é o de tabela e o mínimo não passou por
            // cima dele; senão, mostrá-lo na nota seria mostrar dinheiro que não entra.
            rushSurcharge = if (negotiatedSalePrice == null && !minimumApplies) rushSurcharge else 0.0,
            priceBeforeMinimum = computedTablePrice.takeIf { minimumApplies },
        )
    }

    /** Material e máquina de uma impressão, já vezes as rodadas dela e a quantidade do pedido. */
    private fun printCost(job: PrintJob, printer: PrinterProfile, settings: PricingSettings, quantity: Int): PrintCost {
        val hours = job.allRunsPrintTimeMinutes / MINUTES_PER_HOUR
        val material = job.filaments.sumOf { it.weightGrams / GRAMS_PER_KG * it.filament.pricePerKg } * job.runs
        val energy = hours * (printer.printerPowerWatts / WATTS_PER_KW) * settings.energyPricePerKwh
        val maintenance = hours * printer.maintenanceCostPerHour
        val investmentReturn = hours * printer.machineInvestment.costPerHour
        val fixedCost = hours * settings.fixedCostPerHour
        // Acabamento e mão de obra são independentes e os dois só somam: configurar a hora nunca
        // pode baixar o preço (decisão 93). Quem cobra lixar e pintar em minutos zera a taxa.
        val finishing = material * settings.finishingRate
        return PrintCost(
            material = material * quantity,
            energy = energy * quantity,
            maintenance = maintenance * quantity,
            finishing = finishing * quantity,
            investmentReturn = investmentReturn * quantity,
            fixedCost = fixedCost * quantity,
        )
    }

    private const val GRAMS_PER_KG = 1000.0
    private const val WATTS_PER_KW = 1000.0
    private const val MINUTES_PER_HOUR = 60.0
}
