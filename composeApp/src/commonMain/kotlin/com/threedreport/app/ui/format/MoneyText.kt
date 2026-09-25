package com.threedreport.app.ui.format

import com.threedreport.core.model.Currency

/**
 * Formata um valor monetário pra exibição, na convenção de [currency] (ex.:
 * `1234.5` em BRL → `"R$ 1.234,50"`, em USD → `"$ 1,234.50"`).
 */
fun Double.toCurrencyText(currency: Currency = Currency.BRL): String {
    val cents = kotlin.math.round(this * 100).toLong()
    val sign = if (cents < 0) "-" else ""
    val absCents = kotlin.math.abs(cents)
    val whole = groupThousands((absCents / 100).toString(), currency.thousandsSeparator)
    val fraction = (absCents % 100).toString().padStart(2, '0')
    return "${sign}${currency.symbol} $whole${currency.decimalSeparator}$fraction"
}

/** Ex.: `groupThousands("1234567", '.')` → `"1.234.567"`. */
private fun groupThousands(digits: String, separator: Char): String =
    digits.reversed().chunked(3).joinToString(separator.toString()).reversed()
