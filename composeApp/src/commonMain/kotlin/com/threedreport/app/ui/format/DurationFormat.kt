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

/**
 * Lê um tempo digitado, em minutos (decisão 107): "200" (minutos), "3h20", "3h 20min", "3:20", "3h",
 * "45min", "1d 2h" e com vírgula nos minutos ("3h20,5"). O fatiador mostra "3h 20m", e antes o campo só
 * aceitava minutos, obrigando a fazer a conta de cabeça. `null` se não der pra ler.
 */
fun parseDurationMinutes(text: String): Double? {
    val cleaned = text.trim().lowercase().replace(" ", "")
    if (cleaned.isEmpty()) return null
    parseDecimal(cleaned.removeSuffix("min").removeSuffix("m"), NumberKind.MEASURE)?.let { return it.takeIf { v -> v >= 0 } }

    Regex("""^(\d+):(\d{1,2})$""").matchEntire(cleaned)?.let { match ->
        val (hours, minutes) = match.destructured
        return hours.toDouble() * 60 + minutes.toDouble()
    }

    val match = Regex("""^(?:(\d+)d)?(?:(\d+(?:[.,]\d+)?)h)?(?:(\d+(?:[.,]\d+)?)(?:min|m)?)?(?:(\d+)s)?$""").matchEntire(cleaned) ?: return null
    val (days, hours, minutes, seconds) = match.destructured
    if (days.isEmpty() && hours.isEmpty() && minutes.isEmpty() && seconds.isEmpty()) return null
    fun part(value: String): Double? = if (value.isEmpty()) 0.0 else parseDecimal(value, NumberKind.MEASURE)
    val dayCount = part(days) ?: return null
    val hourCount = part(hours) ?: return null
    val minuteCount = part(minutes) ?: return null
    val secondCount = part(seconds) ?: return null
    return dayCount * 1440 + hourCount * 60 + minuteCount + secondCount / 60.0
}

/**
 * Um tempo em minutos pra pôr de volta num campo de tempo, no formato que [parseDurationMinutes] lê:
 * "3h20", "45" (só minutos, abaixo de uma hora) e com vírgula nos minutos quebrados ("3h20,5").
 */
fun Double.toDurationInputText(): String {
    if (!isFinite() || this < 0) return ""
    val hours = kotlin.math.floor(this / 60.0).toLong()
    val minutes = this - hours * 60
    val minutesText = minutes.toInputText(maxDecimals = 3)
    return when {
        hours == 0L -> minutesText
        minutesText == "0" -> "${hours}h"
        else -> "${hours}h$minutesText"
    }
}
