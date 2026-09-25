package com.threedreport.app.ui.quote

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.QuoteService

/** O filamento e a cor que uma linha de fato usa (com o padrão aplicado quando nada foi escolhido). */
data class ResolvedFilament(val filament: Filament?, val color: FilamentColor?)

/** A impressora e os filamentos que uma impressão de fato usa, pra os seletores da tela. */
data class ResolvedPrint(val printer: PrinterProfile?, val filaments: List<ResolvedFilament>)

/**
 * Resultado derivado de [QuoteInputState] + os catálogos/configurações atuais.
 * [prints] traz, pra cada impressão, a impressora e os filamentos efetivamente
 * usados, para exibição nos seletores mesmo antes do cálculo estar completo. [selectedServices] são os
 * serviços marcados que já têm valor (não têm fallback, começam vazios);
 * [missingServicePrice] avisa que algum marcado ainda está sem valor.
 */
data class QuoteResult(
    val prints: List<ResolvedPrint> = emptyList(),
    val quote: Quote? = null,
    val errorMessage: String? = null,
    val selectedServices: List<QuoteService> = emptyList(),
    val salesChannel: SalesChannel? = null,
    val shippingCost: Double = 0.0,
    /**
     * Algum serviço marcado está com o valor vazio ou inválido. Bloqueia o salvar: contar como
     * zero cobraria de menos sem ninguém perceber.
     */
    val missingServicePrice: Boolean = false,
    /**
     * Campos com valor que não dá pra usar (texto que não é número, negativo), pela chave de
     * [QuoteFields]. Com qualquer um, não há [quote]: um valor inválido não vira zero nem 1 em silêncio.
     */
    val fieldErrors: Map<String, String> = emptyMap(),
    /**
     * Reabrindo um orçamento sem mexer em nada que muda o preço: [quote] é o cálculo congelado dele, e
     * [todaysQuote] mostra quanto sairia com os custos de hoje (decisão 108).
     */
    val keepsOriginalPrice: Boolean = false,
    val todaysQuote: Quote? = null,
    /** Reabrindo um orçamento com alguma mudança de preço: o cálculo antigo, pra tela mostrar "era R$ X". */
    val originalQuote: Quote? = null,
) {
    /**
     * Soma dos serviços escolhidos: os por peça multiplicam pela quantidade (10 unidades custam 10
     * pinturas), os por pedido entram uma vez (ver [QuoteService.total]). A tela mostra o "× N"
     * dos por peça explicitamente pra isso nunca virar surpresa na conta.
     */
    val servicesTotal: Double
        get() = selectedServices.sumOf { it.total(quote?.quantity ?: 1) }

    /** Valor de venda + serviços + frete — o que de fato será cobrado do cliente. */
    val grandTotal: Double?
        get() = quote?.let { it.salePrice + servicesTotal + shippingCost }
}

/** Chaves de [QuoteResult.fieldErrors]: um nome por campo, com a impressão e a linha quando é de uma delas. */
object QuoteFields {
    const val QUANTITY = "quantity"
    const val LABOR = "labor"
    const val SHIPPING = "shipping"
    const val TARGET = "target"

    fun printTime(printId: Int) = "time:$printId"

    fun runs(printId: Int) = "runs:$printId"

    fun length(printId: Int, rowId: Int) = "length:$printId:$rowId"

    fun service(serviceId: String) = "service:$serviceId"
}
