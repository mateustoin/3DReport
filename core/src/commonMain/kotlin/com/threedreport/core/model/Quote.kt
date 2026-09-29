package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Detalhamento dos custos de produção, em R$.
 *
 * Os valores são do **pedido inteiro** (já multiplicados pela quantidade do
 * orçamento, ver [Quote]), e não de uma unidade. Não são arredondados;
 * arredonde apenas na exibição.
 *
 * @property labor seu tempo de trabalho no pedido (ver [Quote.laborMinutes]), cobrado uma vez.
 *   Zero para quem não configurou taxa de mão de obra.
 * @property fixedCost parcela do custo fixo mensal do negócio que este pedido paga, proporcional às
 *   horas de impressão (ver [PricingSettings.fixedCostPerHour]).
 * @property finishing acabamento como percentual do material (ver [PricingSettings.finishingRate]).
 *   Soma junto com [labor]; quem cobra lixar e pintar em minutos deixa a taxa em zero.
 * @property failures reserva de falha, que incide sobre todos os outros custos menos
 *   [administrative] e [consumables] (ver [PricingSettings.failureRate]).
 * @property consumables insumos do pedido (argola, ímã, caixa, ver [QuotedConsumable]), decisão 122.
 *   Passam pela margem, como o material, mas não pela reserva de falha.
 */
@Serializable
data class CostBreakdown(
    val material: Double,
    val energy: Double,
    val maintenance: Double,
    val failures: Double,
    val finishing: Double,
    val investmentReturn: Double,
    val administrative: Double,
    val labor: Double,
    val fixedCost: Double,
    val consumables: Double = 0.0,
) {
    /** Soma de todos os custos: o valor de produção. */
    val total: Double
        get() = material + energy + maintenance + failures + finishing + investmentReturn +
            administrative + labor + fixedCost + consumables
}

/**
 * O que uma impressão custa por conta própria: material e máquina. Já multiplicado pelos
 * [PrintJob.runs] e pela [Quote.quantity]. O que é do pedido (trabalho, administrativo, falha)
 * não é dividido entre as impressões e só aparece em [Quote.costs].
 */
@Serializable
data class PrintCost(
    val material: Double,
    val energy: Double,
    val maintenance: Double,
    val finishing: Double,
    val investmentReturn: Double,
    val fixedCost: Double,
) {
    val total: Double
        get() = material + energy + maintenance + finishing + investmentReturn + fixedCost
}

/**
 * Retrato de uma impressão dentro de um orçamento: o que foi impresso, em qual impressora e quanto
 * custou. Congelado junto com o [Quote], como o resto do orçamento salvo.
 *
 * @property printerId/[printerName] a impressora usada no cálculo. O nome fica guardado à parte
 *   porque o perfil pode ser editado ou excluído do catálogo depois.
 */
@Serializable
data class QuotedPrint(
    val job: PrintJob,
    val printerId: String,
    val printerName: String,
    val cost: PrintCost,
)

/** Quanto de um filamento (numa cor) o pedido inteiro consome, pra conferir no estoque. */
data class FilamentTotal(val filament: FilamentSnapshot, val color: FilamentColor?, val weightGrams: Double)

/**
 * Resultado de um orçamento.
 *
 * **Todos os valores em dinheiro e de massa aqui são do pedido inteiro**, já
 * multiplicados por [quantity]: é o que o cliente paga e o que sai do seu
 * carretel. O preço de uma unidade é [unitSalePrice]. O que continua sendo
 * "de uma rodada só" é cada [PrintJob] (comprimento e tempo), porque é assim
 * que o fatiador informa.
 *
 * @property prints as impressões do pedido, na ordem da tela. Nunca vazia.
 * @property quantity quantos pedidos iguais este orçamento cobre.
 * @property laborMinutes todo o seu tempo de trabalho no pedido, em minutos, cobrado **uma vez**
 *   independente de [quantity] e de quantas impressões houver (decisão 94).
 * @property costs detalhamento dos custos do pedido inteiro.
 * @property salePrice valor de venda (= produção · (1 + margem), já ajustado
 *   pra compensar [channelFeeRate] e [taxRate] quando aplicável: é o preço de fato
 *   cobrado do cliente, o marketplace não aparece pra ele).
 * @property channelId/[channelName] canal de venda usado (ver [SalesChannel]), `null` na venda
 *   direta. O nome fica guardado à parte, como o da impressora.
 * @property channelFeeRate taxa do canal já embutida em [salePrice] (`0.0` na venda direta).
 * @property taxRate imposto sobre a venda já embutido em [salePrice]
 *   (ver [PricingSettings.taxRate]).
 * @property tableSalePrice valor de venda que a margem configurada daria,
 *   guardado **só quando o preço foi negociado** com o cliente (aí
 *   [salePrice] é o preço fechado). `null` quando não houve negociação. Uso interno: nunca entra
 *   em nenhum export.
 * @property extrasTotal serviços e frete cobrados do cliente junto com a peça (decisão 107). Não
 *   passam pela margem, mas passam pelo canal e pelo imposto, que levam a parte deles do total
 *   recebido: por isso entram aqui, pra [profit] e [breakEvenSalePrice] saírem do total de verdade.
 *   Frete grátis não entra aqui, e sim em [absorbedShippingCost].
 * @property consumables insumos usados, com o custo da época (decisão 122); a soma está em
 *   [CostBreakdown.consumables].
 * @property channelFixedFee valor fixo que o canal cobra no pedido inteiro (o fixo por item vezes a
 *   quantidade, na faixa em que [salePrice] cai), decisão 121. Descontado do lucro, como [channelFeeRate].
 * @property channelFeeSchedule a tabela do canal na época (percentual, fixo e faixas), pra
 *   [breakEvenSalePrice] continuar exato num pedido antigo. `null` na venda direta e nos pedidos salvos
 *   antes da 2.2, que só tinham o percentual ([channelFeeRate]).
 * @property serviceLaborMinutes/[serviceLaborCost] seu tempo nos serviços que têm tempo informado, e o
 *   que ele vale pela sua hora (decisão 123).
 * @property serviceProfit o que esses serviços rendem acima da sua hora: o valor cobrado menos
 *   [serviceLaborCost]. Soma no [profit]. Negativo quando o serviço paga menos que a sua hora. Serviço
 *   sem tempo informado não entra (continua sendo repasse).
 * @property absorbedShippingCost frete que você paga pra o cliente receber de graça (decisão 124). Não é
 *   cobrado do cliente, então fica fora de [extrasTotal] e sai do [profit].
 * @property shippingCoveringSalePrice o valor da peça que manteria o lucro de tabela mesmo pagando o frete
 *   grátis, pra sugerir ao vendedor. `null` sem frete grátis.
 * @property rush se o pedido foi marcado como urgente, e [rushSurcharge] o acréscimo que isso deu na peça
 *   (decisão 125), já embutido em [salePrice].
 * @property priceBeforeMinimum o que a conta dava quando o preço mínimo do pedido
 *   ([PricingSettings.minimumOrderPrice]) subiu o preço de tabela (decisão 125). `null` quando o mínimo
 *   não foi aplicado.
 */
@Serializable
data class Quote(
    val prints: List<QuotedPrint>,
    val costs: CostBreakdown,
    val salePrice: Double,
    val quantity: Int = 1,
    val laborMinutes: Double = 0.0,
    val channelId: String? = null,
    val channelName: String? = null,
    val channelFeeRate: Double = 0.0,
    val taxRate: Double = 0.0,
    val tableSalePrice: Double? = null,
    val extrasTotal: Double = 0.0,
    val consumables: List<QuotedConsumable> = emptyList(),
    val channelFixedFee: Double = 0.0,
    val channelFeeSchedule: ChannelFeeSchedule? = null,
    val serviceLaborMinutes: Double = 0.0,
    val serviceLaborCost: Double = 0.0,
    val serviceProfit: Double = 0.0,
    val absorbedShippingCost: Double = 0.0,
    val shippingCoveringSalePrice: Double? = null,
    val rush: Boolean = false,
    val rushSurcharge: Double = 0.0,
    val priceBeforeMinimum: Double? = null,
) {
    init {
        require(prints.isNotEmpty()) { "um orçamento precisa de pelo menos uma impressão" }
        require(quantity >= 1) { "quantity deve ser pelo menos 1: $quantity" }
        require(laborMinutes >= 0) { "laborMinutes não pode ser negativo: $laborMinutes" }
        require(extrasTotal >= 0) { "extrasTotal não pode ser negativo: $extrasTotal" }
        require(channelFixedFee >= 0) { "channelFixedFee não pode ser negativo: $channelFixedFee" }
        require(serviceLaborMinutes >= 0) { "serviceLaborMinutes não pode ser negativo: $serviceLaborMinutes" }
        require(absorbedShippingCost >= 0) { "absorbedShippingCost não pode ser negativo: $absorbedShippingCost" }
        require(rushSurcharge >= 0) { "rushSurcharge não pode ser negativo: $rushSurcharge" }
    }

    /** Valor de produção do pedido (= [CostBreakdown.total]). */
    val productionCost: Double
        get() = costs.total

    /** Massa de filamento do pedido inteiro, em gramas: todas as impressões, rodadas e unidades. */
    val filamentWeightGrams: Double
        get() = prints.sumOf { it.job.allRunsWeightGrams } * quantity

    /** Tempo de máquina do pedido inteiro, em minutos, somando todas as impressoras. */
    val totalPrintTimeMinutes: Double
        get() = prints.sumOf { it.job.allRunsPrintTimeMinutes } * quantity

    /** Impressoras que este pedido ocupa. */
    val printerIds: Set<String>
        get() = prints.mapTo(linkedSetOf()) { it.printerId }

    /** Tempo de máquina do pedido inteiro em [printerId], em minutos (zero se não usa ela). */
    fun printMinutesOn(printerId: String): Double =
        prints.filter { it.printerId == printerId }.sumOf { it.job.allRunsPrintTimeMinutes } * quantity

    /**
     * Consumo do pedido inteiro por filamento e cor, na ordem em que aparecem ("PLA preto 320 g,
     * PETG cinza 80 g"). O mesmo filamento na mesma cor em impressões diferentes vira uma linha só.
     */
    fun filamentTotals(): List<FilamentTotal> =
        prints.flatMap { print -> print.job.filaments.map { usage -> usage to print.job.runs } }
            .groupBy { (usage, _) -> usage.filament.id to usage.color?.id }
            .values
            .map { group ->
                val (first, _) = group.first()
                FilamentTotal(first.filament, first.color, group.sumOf { (usage, runs) -> usage.weightGrams * runs } * quantity)
            }

    /** Tudo que é descontado da venda antes de o dinheiro chegar em você, em percentual. */
    val totalDeductionRate: Double
        get() = channelFeeRate + taxRate

    /**
     * A tabela do canal usada neste pedido. Pedido salvo antes da 2.2 só guardava o percentual, e é com
     * ele que continua sendo calculado.
     */
    val effectiveFeeSchedule: ChannelFeeSchedule
        get() = channelFeeSchedule ?: ChannelFeeSchedule(feeRate = channelFeeRate)

    /** Quanto o canal leva deste pedido, em R$: o percentual sobre o total do cliente mais o valor fixo. */
    val channelFeeAmount: Double
        get() = customerTotal * channelFeeRate + channelFixedFee

    /** Quanto de imposto sai deste pedido, em R$. */
    val taxAmount: Double
        get() = customerTotal * taxRate

    /** Custo dos insumos do pedido (ver [CostBreakdown.consumables]). */
    val consumablesCost: Double
        get() = costs.consumables

    /** Se o preço mínimo do pedido subiu o preço de tabela (ver [priceBeforeMinimum]). */
    val minimumPriceApplied: Boolean
        get() = priceBeforeMinimum != null

    /** Tudo que o cliente paga: a peça ([salePrice]) mais serviços e frete ([extrasTotal]). */
    val customerTotal: Double
        get() = salePrice + extrasTotal

    /**
     * Lucro líquido real do pedido: o que chega depois de canal e imposto levarem a parte deles do
     * total cobrado ([customerTotal]) e o canal levar o valor fixo ([channelFixedFee]), menos o repasse de
     * serviços e frete, a produção e o frete grátis que você paga ([absorbedShippingCost]), mais o que os
     * serviços com tempo rendem acima da sua hora ([serviceProfit]). Sem deduções nem nada disso, é só
     * venda − produção.
     */
    val profit: Double
        get() = customerTotal * (1 - totalDeductionRate) - channelFixedFee - extrasTotal - productionCost -
            absorbedShippingCost + serviceProfit

    /** Preço de uma unidade: [salePrice] dividido por [quantity]. */
    val unitSalePrice: Double
        get() = salePrice / quantity

    /**
     * Menor valor de venda da peça que ainda não dá prejuízo: cobre o custo de produção depois de
     * canal e imposto levarem a parte deles do total (serviços e frete inclusos). Vender exatamente
     * por isso significa trabalhar de graça; abaixo disso, você paga pra imprimir. É a referência
     * pra negociar sem ter que refazer a conta de cabeça.
     */
    val breakEvenSalePrice: Double
        get() = effectiveFeeSchedule.priceLeaving(
            receipt = productionCost + absorbedShippingCost - serviceProfit,
            extras = extrasTotal,
            quantity = quantity,
            taxRate = taxRate,
        )

    /** Se o preço foi fechado com o cliente em vez de vir da margem (ver [tableSalePrice]). */
    val isNegotiated: Boolean
        get() = tableSalePrice != null

    /**
     * Quanto o preço fechado ficou abaixo do de tabela: positivo quando houve
     * desconto, negativo quando o cliente pagou acima da tabela, zero sem
     * negociação. Somar isso num período dá o desconto líquido concedido.
     */
    val negotiatedDiscount: Double
        get() = (tableSalePrice ?: salePrice) - salePrice

    /**
     * A margem de fato obtida neste orçamento: o lucro como fração do custo de produção **sem o trabalho**,
     * que é a base da margem (decisão 118). No preço de tabela, sem canal nem imposto, é a margem configurada.
     */
    val actualProfitMargin: Double
        get() {
            val base = productionCost - costs.labor
            return if (base > 0) profit / base else 0.0
        }
}
