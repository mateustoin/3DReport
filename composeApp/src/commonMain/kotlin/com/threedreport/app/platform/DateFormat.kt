package com.threedreport.app.platform

/** Formata um instante (epoch millis) para exibição, ex.: "15/09/2026 18:42". */
expect fun formatDateTime(epochMillis: Long): String

/**
 * Hoje no fuso do computador, em dias desde 01/01/1970 — a mesma unidade de
 * `SavedQuote.deliveryDateEpochDay`. Datas de calendário andam em dias, e não em millis, pra não
 * mudarem de dia conforme o fuso.
 */
expect fun todayEpochDay(): Long

/** Formata uma data de calendário (dias desde 01/01/1970), ex.: "30/09/2026". */
expect fun formatDate(epochDay: Long): String

/** Versão curta de [formatDate] pra caber num card, ex.: "30/09". */
expect fun formatShortDate(epochDay: Long): String

/** Dia da semana por extenso e em minúsculas, ex.: "sexta". */
expect fun weekdayName(epochDay: Long): String

/**
 * Converte o instante escolhido num seletor de data (meia-noite UTC, que é como o `DatePicker` do
 * Material3 devolve) em dias desde 01/01/1970, e o contrário. Divisão exata, sem fuso: o seletor
 * já trabalha em UTC, então somar o fuso local aqui deslocaria a data em um dia.
 */
fun utcMillisToEpochDay(utcMillis: Long): Long = utcMillis.floorDiv(MILLIS_PER_DAY)

fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

private const val MILLIS_PER_DAY = 86_400_000L
