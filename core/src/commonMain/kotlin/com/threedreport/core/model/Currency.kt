package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Moeda usada pra formatar valores no app (UI, PDF, copiar/colar). Cada
 * moeda tem sua própria convenção de separador — não é só trocar o símbolo
 * (ex.: BRL usa vírgula decimal e ponto de milhar; USD é o inverso).
 */
@Serializable
enum class Currency(
    val code: String,
    val symbol: String,
    val decimalSeparator: Char,
    val thousandsSeparator: Char,
) {
    BRL("BRL", "R$", ',', '.'),
    USD("USD", "$", '.', ','),
    EUR("EUR", "€", ',', '.'),
    GBP("GBP", "£", '.', ','),
    ;

    /**
     * Se o que só vale pro Brasil aparece com esta moeda (decisões 145 e 155, item 16b da Leva 11B): Pix (forma de
     * pagamento, cobrança nos documentos e a seção de Configurações), o site de rastreio padrão dos Correios e o preço de
     * referência das impressoras pré-cadastradas. Segue a moeda (do pedido ou a padrão), nunca o idioma. É a regra num
     * lugar só: quem mostra ou esconde uma dessas coisas pergunta aqui.
     */
    val hasBrazilOnlyFeatures: Boolean
        get() = this == BRL
}
