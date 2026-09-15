package com.threedreport.app.ui.format

/** Converte texto digitado pelo usuário (aceita `,` ou `.` como separador decimal) em [Double]. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** [parseDecimal] que falha com uma mensagem amigável quando o texto é inválido. */
fun String.toRequiredDouble(fieldLabel: String): Double = parseDecimal(this) ?: error("$fieldLabel inválido")

/** Converte texto digitado pelo usuário em [Int], com mensagem amigável quando inválido. */
fun String.toRequiredInt(fieldLabel: String): Int = trim().toIntOrNull() ?: error("$fieldLabel inválido")
