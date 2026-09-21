package com.threedreport.app.ui.format

/** Converte minutos em horas e formata com 1 casa decimal, separador decimal brasileiro (vírgula). */
fun Double.minutesToHoursText(): String {
    val tenths = kotlin.math.round(this / 60.0 * 10).toLong()
    val whole = tenths / 10
    val decimal = kotlin.math.abs(tenths % 10)
    return if (decimal == 0L) "$whole h" else "$whole,$decimal h"
}
