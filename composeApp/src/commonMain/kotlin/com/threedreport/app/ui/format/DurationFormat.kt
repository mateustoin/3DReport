package com.threedreport.app.ui.format

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.duration_decimal_hours
import _3dreport.composeapp.generated.resources.duration_hours
import _3dreport.composeapp.generated.resources.duration_hours_minutes
import _3dreport.composeapp.generated.resources.duration_minutes
import androidx.compose.runtime.Composable
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.fillTemplate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Uma duração em horas e minutos inteiros, sem texto: quem mostra escolhe as palavras ([DurationTexts]). */
data class DurationParts(val hours: Long, val minutes: Long)

/** Minutos em horas e minutos inteiros, arredondando pro minuto mais próximo; negativo vira zero. */
fun Double.toDurationParts(): DurationParts {
    val totalMinutes = kotlin.math.round(this).toLong().coerceAtLeast(0)
    return DurationParts(hours = totalMinutes / 60, minutes = totalMinutes % 60)
}

/**
 * Os textos de duração, com os modelos de `strings.xml` ("%1$d h %2$d min"). A tela pega com [durationTexts]; quem
 * está fora da composição (PDF, mensagem, testes), com [loadDurationTexts].
 */
class DurationTexts(
    private val hoursAndMinutes: String,
    private val hoursOnly: String,
    private val minutesOnly: String,
    private val decimalHoursTemplate: String,
) {
    /**
     * Minutos por extenso em horas e minutos, ex.: "6 h 30 min", "45 min", "3 h". É o formato pro cliente (PDF,
     * copiar/colar): "6,5 h" é conta de planilha, "6 h 30 min" é como se fala. Arredonda pro minuto mais próximo.
     */
    fun duration(minutes: Double): String {
        val parts = minutes.toDurationParts()
        return when {
            parts.hours == 0L -> minutesOnly.fill(parts.minutes)
            parts.minutes == 0L -> hoursOnly.fill(parts.hours)
            else -> hoursAndMinutes.fill(parts.hours, parts.minutes)
        }
    }

    /** Minutos em horas com uma casa decimal e o separador do idioma ([AppLocale]), ex.: "12,5 h". */
    fun decimalHours(minutes: Double): String {
        val tenths = kotlin.math.round(minutes / 60.0 * 10).toLong()
        val whole = tenths / 10
        val decimal = kotlin.math.abs(tenths % 10)
        val number = if (decimal == 0L) "$whole" else "$whole${AppLocale.decimalSeparator}$decimal"
        return decimalHoursTemplate.fill(number)
    }

    private fun String.fill(vararg args: Any): String = fillTemplate(this, *args)
}

/** Os modelos de duração em [DurationTexts], na tela. */
@Composable
fun durationTexts(): DurationTexts = DurationTexts(
    hoursAndMinutes = stringResource(Res.string.duration_hours_minutes),
    hoursOnly = stringResource(Res.string.duration_hours),
    minutesOnly = stringResource(Res.string.duration_minutes),
    decimalHoursTemplate = stringResource(Res.string.duration_decimal_hours),
)

/** Os mesmos modelos, fora da composição. */
suspend fun loadDurationTexts(): DurationTexts = DurationTexts(
    hoursAndMinutes = getString(Res.string.duration_hours_minutes),
    hoursOnly = getString(Res.string.duration_hours),
    minutesOnly = getString(Res.string.duration_minutes),
    decimalHoursTemplate = getString(Res.string.duration_decimal_hours),
)

/** "6 h 30 min" na tela (ver [DurationTexts.duration]). */
@Composable
fun Double.minutesToDurationText(): String = durationTexts().duration(this)

/** "12,5 h" na tela (ver [DurationTexts.decimalHours]). */
@Composable
fun Double.minutesToHoursText(): String = durationTexts().decimalHours(this)

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
    val minutesText = minutes.toInputText(AppLocale.decimalSeparator, maxDecimals = 3)
    return when {
        hours == 0L -> minutesText
        minutesText == "0" -> "${hours}h"
        else -> "${hours}h$minutesText"
    }
}
