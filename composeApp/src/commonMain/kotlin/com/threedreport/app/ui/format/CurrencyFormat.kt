package com.threedreport.app.ui.format

/** Formata um valor em reais para exibição (ex.: `12.4` → `"R$ 12,40"`). */
fun Double.toBrl(): String {
    val cents = kotlin.math.round(this * 100).toLong()
    val sign = if (cents < 0) "-" else ""
    val absCents = kotlin.math.abs(cents)
    return "${sign}R$ ${absCents / 100},${(absCents % 100).toString().padStart(2, '0')}"
}
