package com.threedreport.app.ui.format

/** Formata uma fração (ex.: `0.15`) como percentual pra exibição (ex.: `"15%"`, `"12,5%"`). */
fun Double.toPercentText(): String {
    val hundredths = kotlin.math.round(this * 100 * 10).toLong()
    val whole = hundredths / 10
    val decimal = kotlin.math.abs(hundredths % 10)
    return if (decimal == 0L) "$whole%" else "$whole,$decimal%"
}
