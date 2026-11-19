package com.threedreport.app.ui.format

import com.threedreport.app.platform.AppLocale
import kotlin.math.abs
import kotlin.math.round

/** O sinal de menos tipográfico (U+2212), que não se confunde com um hífen: "−40 g". */
private const val MINUS = "−"

private const val GRAMS_PER_KG = 1000.0

/**
 * Só o número de gramas, sem casa decimal e com separador de milhar do idioma do app ("2.400", "-40" com o sinal U+2212).
 * A unidade vem de recurso (`stock_amount_grams`), junto da tela que escreve.
 */
fun Double.toWholeGramsNumber(): String = groupedWhole(round(this).toLong())

/** Gramas a partir de mil contam em quilos ([toKilogramsNumber]); abaixo disso, em gramas. */
val Double.isKilogramAmount: Boolean get() = abs(this) >= GRAMS_PER_KG

/** Só o número de quilos de [this] gramas, com duas casas no máximo e sem zero sobrando ("1", "1,5"). */
fun Double.toKilogramsNumber(): String = signed((this / GRAMS_PER_KG).toInputText(AppLocale.decimalSeparator, 2))

/** Unidades de insumo com até duas casas, sem zero sobrando e com milhar do idioma ("8", "2,5", "1.200"). O "un" vem do recurso. */
fun Double.toUnitsNumberText(): String {
    val text = toInputText(AppLocale.decimalSeparator, 2).removePrefix("-")
    val negative = this < 0 && text != "0"
    val whole = text.substringBefore(AppLocale.decimalSeparator)
    val rest = text.removePrefix(whole)
    return (if (negative) MINUS else "") + groupDigits(whole) + rest
}

private fun groupedWhole(value: Long): String = (if (value < 0) MINUS else "") + groupDigits(abs(value).toString())

private fun signed(text: String): String = if (text.startsWith("-")) MINUS + text.drop(1) else text

private fun groupDigits(digits: String): String {
    val separator = if (AppLocale.decimalSeparator == ',') '.' else ','
    return digits.reversed().chunked(3).joinToString(separator.toString()).reversed()
}
