package com.threedreport.app.ui.format

/** Converte minutos em horas e formata com 1 casa decimal, separador decimal brasileiro (vírgula). */
fun Double.minutesToHoursText(): String {
    val tenths = kotlin.math.round(this / 60.0 * 10).toLong()
    val whole = tenths / 10
    val decimal = kotlin.math.abs(tenths % 10)
    return if (decimal == 0L) "$whole h" else "$whole,$decimal h"
}

/**
 * Minutos por extenso em horas e minutos, ex.: "6 h 30 min", "45 min", "3 h". É o formato pro
 * cliente (PDF, copiar/colar): "6,5 h" é conta de planilha, "6 h 30 min" é como se fala. Arredonda
 * pro minuto mais próximo.
 */
fun Double.minutesToDurationText(): String {
    val totalMinutes = kotlin.math.round(this).toLong().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "$minutes min"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes min"
    }
}
