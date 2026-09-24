package com.threedreport.app.ui.quote

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SalesChannel
import com.threedreport.core.model.QuoteService

/**
 * Resultado derivado de [QuoteInputState] + os catálogos/configurações atuais.
 * [filament]/[printer] são os itens efetivamente usados (com fallback para o
 * primeiro do catálogo quando nada foi escolhido ainda), para exibição nos
 * dropdowns mesmo antes do cálculo estar completo. [selectedServices] são os
 * serviços marcados que já têm valor (não têm fallback, começam vazios);
 * [missingServicePrice] avisa que algum marcado ainda está sem valor.
 */
data class QuoteResult(
    val filament: Filament? = null,
    val filamentColor: FilamentColor? = null,
    val printer: PrinterProfile? = null,
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
