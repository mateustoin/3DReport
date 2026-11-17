package com.threedreport.core.pricing

import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteService
import com.threedreport.core.model.QuotedConsumable
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.SavedQuote

/**
 * O que foi combinado num pedido além das impressões, com os valores já lidos e conferidos pela tela
 * (decisão 130). Daqui saem as regras que vêm antes da conta: o preço da peça a partir do total fechado,
 * o frete grátis, o que não vale pra produto do catálogo e a urgência. Quem calcula é [OrderPricing.quote].
 *
 * Tudo nasce neutro: sem nada combinado, o resultado é o do [PricingCalculator] sem extras.
 *
 * @property quantity quantos pedidos iguais.
 * @property laborMinutes todo o seu tempo de trabalho no pedido, cobrado uma vez (decisão 94).
 * @property services serviços cobrados junto (decisão 107).
 * @property consumables insumos usados (decisão 122).
 * @property shippingCost o frete digitado. Use [effectiveShippingCost], [chargedShipping] e
 *   [absorbsShipping] pra saber o que ele vira.
 * @property shippingAbsorbed se a pessoa marcou frete grátis (decisão 124). Só vale como em [absorbsShipping].
 * @property catalogProduct se é um produto do catálogo, e não um pedido de cliente (decisão 101): não tem
 *   frete, preço anunciado nem urgência.
 * @property customerTotal o total fechado com o cliente, serviços e frete inclusos, ou `null` pra usar o
 *   preço de tabela. Precisa cobrir os extras (ver [coversExtras]).
 * @property announcedUnitPrice preço anunciado de uma peça, do produto de onde o pedido nasceu pelo
 *   "Vender" (decisão 102). Vale quando não há [customerTotal].
 * @property rush se a pessoa marcou o pedido como urgente (decisão 125). Só vale num pedido de cliente e
 *   com o acréscimo configurado.
 */
data class OrderTerms(
    val quantity: Int = 1,
    val laborMinutes: Double = 0.0,
    val services: List<QuoteService> = emptyList(),
    val consumables: List<QuotedConsumable> = emptyList(),
    val shippingCost: Double = 0.0,
    val shippingAbsorbed: Boolean = false,
    val catalogProduct: Boolean = false,
    val customerTotal: Double? = null,
    val announcedUnitPrice: Double? = null,
    val rush: Boolean = false,
) {
    /** O frete que vale: o digitado, ou zero num produto do catálogo, que não tem frete (decisão 101). */
    val effectiveShippingCost: Double
        get() = if (catalogProduct) 0.0 else shippingCost

    /** Se o frete grátis vale de fato (decisão 124): marcado, num pedido de cliente e com frete. */
    val absorbsShipping: Boolean
        get() = shippingAbsorbed && !catalogProduct && effectiveShippingCost > 0

    /** O frete que o cliente paga: zero com frete grátis (ver [SavedQuote.chargedShipping]). */
    val chargedShipping: Double
        get() = SavedQuote.chargedShipping(effectiveShippingCost, absorbsShipping)

    /** Os serviços do pedido inteiro, cada um por peça ou por pedido. */
    val servicesTotal: Double
        get() = services.sumOf { it.total(quantity) }

    /** Serviços e frete cobrados junto com a peça (ver [Quote.extrasTotal]). */
    val extrasTotal: Double
        get() = servicesTotal + chargedShipping

    /**
     * Se o [customerTotal] cobre serviços e frete. Um total que não cobre não vira peça de preço zero: a
     * tela mostra o erro no campo, porque salvar daria um total diferente do digitado.
     */
    val coversExtras: Boolean
        get() = customerTotal == null || customerTotal >= extrasTotal

    /**
     * O preço da peça que vale no lugar do de tabela: o total fechado menos serviços e frete cobrado, ou o
     * preço anunciado vezes a quantidade. `null` usa o preço de tabela.
     */
    val negotiatedSalePrice: Double?
        get() = customerTotal?.let { it - servicesTotal - chargedShipping }
            ?: announcedUnitPrice?.takeUnless { catalogProduct }?.let { it * quantity }

    /** Se o pedido é urgente de fato (decisão 125): marcado, num pedido de cliente e com acréscimo configurado. */
    fun rushApplies(settings: PricingSettings): Boolean = rush && !catalogProduct && settings.rushSurchargeRate > 0
}

/** O pedido montado a partir das impressões e do que foi combinado (decisão 130). */
object OrderPricing {

    /**
     * A conta do pedido: aplica as regras de [OrderTerms] e chama o [PricingCalculator].
     *
     * @param prints as impressões, cada uma com a impressora em que roda.
     * @param channel canal de venda, ou `null` na venda direta.
     * @throws PricingError quando a conta não fecha por um motivo que a tela precisa explicar.
     * @throws IllegalArgumentException quando [OrderTerms.coversExtras] é falso ou um valor é negativo: a
     *   tela confere antes de chamar.
     */
    fun quote(
        prints: List<Pair<PrintJob, PrinterProfile>>,
        settings: PricingSettings,
        channel: SalesChannel?,
        terms: OrderTerms,
    ): Quote {
        require(terms.coversExtras) { "customerTotal (${terms.customerTotal}) não cobre os extras (${terms.extrasTotal})" }
        return PricingCalculator.calculate(
            prints = prints,
            settings = settings,
            channel = channel,
            quantity = terms.quantity,
            laborMinutes = terms.laborMinutes,
            negotiatedSalePrice = terms.negotiatedSalePrice,
            extrasTotal = terms.extrasTotal,
            services = terms.services,
            consumables = terms.consumables,
            rush = terms.rushApplies(settings),
            absorbedShippingCost = if (terms.absorbsShipping) terms.effectiveShippingCost else 0.0,
        )
    }
}
