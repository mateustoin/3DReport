package com.threedreport.app.ui.format

import com.threedreport.app.platform.AppLocale

/** Formata peso em gramas com 1 casa decimal e o separador do idioma em uso ([AppLocale]): "12,5 g", "12.5 g". */
fun Double.toWeightText(): String = "${oneDecimalText()} g"

/**
 * Um número que não é dinheiro com no máximo 1 casa decimal e o separador do idioma em uso ([AppLocale], decisão 153):
 * "12,5" em português, "12.5" em inglês, "12" sem casa, "-0,5" com o sinal mesmo entre -1 e 0. O que arredonda pra zero
 * sai "0", sem sinal. Dinheiro segue a moeda, não isto.
 */
fun Double.oneDecimalText(): String {
    val tenths = kotlin.math.round(this * 10).toLong()
    val sign = if (tenths < 0) "-" else ""
    val absolute = kotlin.math.abs(tenths)
    val whole = absolute / 10
    val decimal = absolute % 10
    return if (decimal == 0L) "$sign$whole" else "$sign$whole${AppLocale.decimalSeparator}$decimal"
}
