package com.threedreport.app.platform

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Os formatadores de um idioma, montados uma vez por troca de idioma e não a cada data. */
private class DateFormatters(val state: AppLocaleState) {
    val locale: Locale = Locale.forLanguageTag(state.languageTag)
    val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern(state.dateTimePattern, locale)
    val date: DateTimeFormatter = DateTimeFormatter.ofPattern(state.datePattern, locale)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern(state.shortDatePattern, locale)
}

@Volatile
private var cached = DateFormatters(AppLocale.current.value)

// Tudo vem do AppLocale, lido na hora: trocar o idioma com o app aberto muda a próxima data escrita.
private fun formatters(): DateFormatters {
    val state = AppLocale.current.value
    return cached.takeIf { it.state == state } ?: DateFormatters(state).also { cached = it }
}

actual fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatters().dateTime)

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

actual fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(formatters().date)

actual fun formatShortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(formatters().shortDate)

// "sexta-feira" vira "sexta": é como se fala em português, e cabe melhor ao lado da data. Em inglês não há hífen.
actual fun weekdayName(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.FULL, formatters().locale).substringBefore('-')

actual fun localEpochDayOf(epochMillis: Long): Long = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
