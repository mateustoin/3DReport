package com.threedreport.app.ui.format

/** Formata peso em gramas com 1 casa decimal, separador decimal brasileiro (vírgula). */
fun Double.toWeightText(): String {
    val tenths = kotlin.math.round(this * 10).toLong()
    val whole = tenths / 10
    val decimal = kotlin.math.abs(tenths % 10)
    return if (decimal == 0L) "$whole g" else "$whole,$decimal g"
}
