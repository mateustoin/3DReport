package com.threedreport.app.platform

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Tudo vem do AppLocale: o idioma e os padrões de data.
private val LOCALE: Locale = Locale.forLanguageTag(AppLocale.languageTag)
private val FORMATTER = DateTimeFormatter.ofPattern(AppLocale.dateTimePattern, LOCALE)
private val DATE_FORMATTER = DateTimeFormatter.ofPattern(AppLocale.datePattern, LOCALE)
private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern(AppLocale.shortDatePattern, LOCALE)

actual fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(FORMATTER)

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

actual fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DATE_FORMATTER)

actual fun formatShortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(SHORT_DATE_FORMATTER)

// "sexta-feira" vira "sexta": é como se fala em português, e cabe melhor ao lado da data.
actual fun weekdayName(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE).substringBefore('-')
