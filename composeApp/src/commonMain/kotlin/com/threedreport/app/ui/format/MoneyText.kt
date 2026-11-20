package com.threedreport.app.ui.format

import com.threedreport.core.model.Currency

/** O espaço entre o símbolo e o número: não separável, pra o valor nunca quebrar entre os dois. */
private const val NO_BREAK_SPACE = "\u00A0"

/**
 * Formata um valor monetário pra exibição, na convenção de [currency].
 * O símbolo e o número ficam unidos por espaço não separável (U+00A0), pra o valor nunca quebrar entre os dois.
 * Ex.: `1234.5` em BRL → `"R$ 1.234,50"`, em USD → `"$ 1,234.50"`).
 */
fun Double.toCurrencyText(currency: Currency = Currency.BRL): String {
    val cents = kotlin.math.round(this * 100).toLong()
    val sign = if (cents < 0) "-" else ""
    val absCents = kotlin.math.abs(cents)
    val whole = groupThousands((absCents / 100).toString(), currency.thousandsSeparator)
    val fraction = (absCents % 100).toString().padStart(2, '0')
    return "${sign}${currency.symbol}$NO_BREAK_SPACE$whole${currency.decimalSeparator}$fraction"
}

/** Como [toCurrencyText], arredondado pra unidade inteira, sem centavos (ex.: `2998.0` em BRL → `"R$ 2.998"`). */
fun Double.toWholeCurrencyText(currency: Currency = Currency.BRL): String {
    val units = kotlin.math.floor(kotlin.math.abs(this) + 0.5).toLong() * (if (this < 0) -1 else 1)
    val sign = if (units < 0) "-" else ""
    return "$sign${currency.symbol}$NO_BREAK_SPACE${groupThousands(kotlin.math.abs(units).toString(), currency.thousandsSeparator)}"
}

/** Ex.: `groupThousands("1234567", '.')` → `"1.234.567"`. */
private fun groupThousands(digits: String, separator: Char): String =
    digits.reversed().chunked(3).joinToString(separator.toString()).reversed()
