package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Uma faixa de preço de um canal de venda: até [upToUnitPrice] por item, o canal cobra [feeRate] do
 * valor recebido mais [fixedFeePerItem] por item vendido. É como a Shopee cobra em 2026 (o valor fixo
 * sobe conforme a faixa de preço), e cada vendedor copia a tabela do próprio painel.
 *
 * @property upToUnitPrice preço de uma peça até o qual esta faixa vale (inclusive), em R$. `null` na
 *   última faixa, que vale pra tudo acima da anterior.
 */
@Serializable
data class ChannelFeeTier(
    val upToUnitPrice: Double? = null,
    val feeRate: Double,
    val fixedFeePerItem: Double = 0.0,
) {
    init {
        require(upToUnitPrice == null || upToUnitPrice > 0) { "upToUnitPrice deve ser positivo: $upToUnitPrice" }
        require(feeRate >= 0 && feeRate < 1) { "feeRate deve estar entre 0 (inclusive) e 1 (exclusive): $feeRate" }
        require(fixedFeePerItem >= 0) { "fixedFeePerItem não pode ser negativo: $fixedFeePerItem" }
    }
}

/** O que o canal cobra a um dado preço: percentual do valor recebido e valor fixo por item. */
data class ChannelFee(val feeRate: Double, val fixedFeePerItem: Double)

/**
 * Tudo o que um canal de venda cobra (decisão 121): um percentual do valor recebido, um valor fixo por
 * item vendido e, opcionalmente, faixas de preço que substituem os dois. Guardado no [SalesChannel] e
 * copiado no [Quote] (é um retrato, como o resto do orçamento), pra o preço mínimo e o lucro de um
 * pedido antigo continuarem com a tabela da época.
 *
 * A faixa é escolhida pelo **preço de uma peça** (o valor da peça no pedido dividido pela quantidade).
 *
 * @property tiers faixas em ordem crescente de [ChannelFeeTier.upToUnitPrice], com a última sem limite.
 *   Vazio usa [feeRate] e [fixedFeePerItem] pra qualquer preço.
 */
@Serializable
data class ChannelFeeSchedule(
    val feeRate: Double = 0.0,
    val fixedFeePerItem: Double = 0.0,
    val tiers: List<ChannelFeeTier> = emptyList(),
) {
    init {
        require(feeRate >= 0 && feeRate < 1) { "feeRate deve estar entre 0 (inclusive) e 1 (exclusive): $feeRate" }
        require(fixedFeePerItem >= 0) { "fixedFeePerItem não pode ser negativo: $fixedFeePerItem" }
        if (tiers.isNotEmpty()) {
            require(tiers.last().upToUnitPrice == null) { "a última faixa precisa valer pra qualquer preço acima da anterior" }
            val limits = tiers.dropLast(1).map { requireNotNull(it.upToUnitPrice) { "só a última faixa pode ficar sem limite" } }
            require(limits.zipWithNext().all { (a, b) -> a < b }) { "os limites das faixas precisam estar em ordem crescente: $limits" }
        }
    }

    /** Se o canal não cobra nada (venda direta, Pix). */
    val isFree: Boolean
        get() = tiers.isEmpty() && feeRate == 0.0 && fixedFeePerItem == 0.0

    /** O que o canal cobra quando uma peça sai por [unitPrice]. */
    fun feeAt(unitPrice: Double): ChannelFee {
        if (tiers.isEmpty()) return ChannelFee(feeRate, fixedFeePerItem)
        val tier = tiers.first { it.upToUnitPrice == null || unitPrice <= it.upToUnitPrice + PRICE_EPSILON }
        return ChannelFee(tier.feeRate, tier.fixedFeePerItem)
    }

    /**
     * O que sobra na sua mão quando a peça do pedido inteiro sai por [piecePrice], com [extras] (serviços e
     * frete) cobrados junto: o canal e o imposto levam o percentual deles do total, e o canal leva ainda o
     * valor fixo de cada item.
     */
    fun receiptAt(piecePrice: Double, extras: Double, quantity: Int, taxRate: Double): Double {
        val fee = feeAt(piecePrice / quantity)
        return (piecePrice + extras) * (1 - fee.feeRate - taxRate) - fee.fixedFeePerItem * quantity
    }

    /**
     * O menor valor da peça (o pedido inteiro, sem os [extras]) que deixa [receipt] mais os [extras] na sua
     * mão, depois do canal e do imposto: `(peça + extras) · (1 − taxa − imposto) − fixo · quantidade`.
     *
     * Sem faixas é a conta direta. Com faixas, a conta é feita com cada faixa e fica o menor preço que de
     * fato cobre o valor com a taxa da faixa em que ele cai. Na borda entre duas faixas pode não existir
     * preço exato (o fixo sobe de uma faixa pra outra); aí vale um centavo acima do limite, o primeiro
     * preço da faixa de cima que cobre. Não há iteração, então o resultado é sempre o mesmo.
     *
     * @throws IllegalArgumentException quando a taxa do canal somada ao imposto chega a 100% em todas as
     *   faixas: nenhum preço deixa dinheiro na sua mão.
     */
    fun priceLeaving(receipt: Double, extras: Double, quantity: Int, taxRate: Double): Double {
        require(quantity >= 1) { "quantity deve ser pelo menos 1: $quantity" }
        val options = if (tiers.isEmpty()) {
            listOf(Option(feeRate, fixedFeePerItem, lowerUnitPrice = null))
        } else {
            tiers.mapIndexed { index, tier -> Option(tier.feeRate, tier.fixedFeePerItem, tiers.getOrNull(index - 1)?.upToUnitPrice) }
        }
        val affordable = options.filter { it.feeRate + taxRate < 1 }
        require(affordable.isNotEmpty()) {
            "A taxa do canal somada ao imposto chega a 100% do valor de venda: não sobra nada pra você. " +
                "Revise a taxa do canal ou o imposto em Configurações."
        }
        if (tiers.isEmpty()) return affordable.single().exactPrice(receipt, extras, quantity, taxRate)

        val candidates = affordable.flatMap { option ->
            listOfNotNull(
                option.exactPrice(receipt, extras, quantity, taxRate),
                option.lowerUnitPrice?.let { (it + ONE_CENT) * quantity },
            )
        }
        // O que sobra na mão inclui o repasse dos extras, então é contra base + extras que se confere.
        return candidates
            .filter { receiptAt(it, extras, quantity, taxRate) >= receipt + extras - PRICE_EPSILON }
            .minOrNull()
            ?: candidates.max()
    }

    private data class Option(val feeRate: Double, val fixedFeePerItem: Double, val lowerUnitPrice: Double?) {
        fun exactPrice(receipt: Double, extras: Double, quantity: Int, taxRate: Double): Double =
            (receipt + extras + fixedFeePerItem * quantity) / (1 - feeRate - taxRate) - extras
    }

    companion object {
        /** Venda direta: nada é descontado. */
        val NONE = ChannelFeeSchedule()

        private const val ONE_CENT = 0.01
        private const val PRICE_EPSILON = 1e-9
    }
}
