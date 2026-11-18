package com.threedreport.app.platform

import com.threedreport.core.model.Currency

private val EURO_REGIONS = setOf(
    "AT", "BE", "HR", "CY", "EE", "FI", "FR", "DE", "GR", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PT", "SK", "SI", "ES",
)

/**
 * A moeda que a primeira abertura de uma instalação nova sugere pela região do sistema (decisão 16a da Leva 11B): Brasil
 * é real, zona do euro é euro, Reino Unido é libra, e as outras regiões (e a falta de região) são dólar. A pessoa troca
 * no passo 1 do primeiro uso; quem já usa o app não passa por aqui.
 */
fun suggestedCurrency(region: String?): Currency = when (val code = region?.trim()?.uppercase()) {
    "BR" -> Currency.BRL
    "GB" -> Currency.GBP
    in EURO_REGIONS -> Currency.EUR
    else -> Currency.USD
}
