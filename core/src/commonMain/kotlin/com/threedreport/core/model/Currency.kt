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
}
