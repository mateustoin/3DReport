package com.threedreport.app.ui.format

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Como ler o separador que **não** é o decimal da moeda quando ele aparece sozinho (decisão 107).
 *
 * Em pt-BR, "2.700" é dois mil e setecentos; "27.5" é vinte e sete e meio, escrito com ponto. Quem
 * digita um preço usa o ponto como milhar; quem digita uma densidade ou um comprimento em metros usa o
 * ponto como decimal. Cada campo diz qual dos dois ele espera.
 */
enum class NumberKind {
    /**
     * Valores que costumam passar de mil (dinheiro, watts, minutos, gramas): um ponto seguido de
     * exatamente três dígitos ("2.700", "12.500") é milhar.
     */
    AMOUNT,

    /** Medidas pequenas com casas decimais (densidade, diâmetro, metros, kWh, percentual): o ponto é sempre decimal. */
    MEASURE,
}

/**
 * Converte o que a pessoa digitou em número, ou `null` se não for um número (decisão 107).
 *
 * Aceita vírgula ou ponto decimal, separador de milhar ("2.700,50", "1,234.56"), símbolo de moeda e
 * de percentual ("R$ 50", "20%") e espaços. Recusa o que não é número finito ("NaN", "Infinity",
 * "1e999") e texto com letras. Antes, "2.700" virava 2,7 (o preço da máquina ficava mil vezes menor
 * em silêncio) e "2.700,50" era recusado.
 *
 * @param decimalSeparator o separador decimal da moeda em uso ([com.threedreport.core.model.Currency]).
 */
fun parseDecimal(text: String, kind: NumberKind = NumberKind.AMOUNT, decimalSeparator: Char = ','): Double? {
    var cleaned = text.trim()
        .replace("R$", "")
        .filterNot { it.isWhitespace() || it == '$' || it == '€' || it == '£' || it == '%' || it == ' ' }
    if (cleaned.isEmpty()) return null
    val negative = cleaned.startsWith('-')
    if (negative) cleaned = cleaned.drop(1)
    if (cleaned.isEmpty() || cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null

    val lastDot = cleaned.lastIndexOf('.')
    val lastComma = cleaned.lastIndexOf(',')
    val normalized = when {
        lastDot >= 0 && lastComma >= 0 -> {
            // Os dois aparecem: o último é o decimal, e o outro só pode ser milhar.
            val decimal = if (lastDot > lastComma) '.' else ','
            val grouping = if (decimal == '.') ',' else '.'
            val (integer, fraction) = cleaned.substring(0, cleaned.lastIndexOf(decimal)) to cleaned.substring(cleaned.lastIndexOf(decimal) + 1)
            if (decimal in integer || !isValidGrouping(integer, grouping) || fraction.any { !it.isDigit() }) return null
            integer.replace(grouping.toString(), "") + "." + fraction
        }
        lastDot < 0 && lastComma < 0 -> cleaned
        else -> {
            val separator = if (lastDot >= 0) '.' else ','
            val occurrences = cleaned.count { it == separator }
            when {
                occurrences > 1 -> if (isValidGrouping(cleaned, separator)) cleaned.replace(separator.toString(), "") else return null
                separator == decimalSeparator -> cleaned.replace(separator, '.')
                kind == NumberKind.AMOUNT && isValidGrouping(cleaned, separator) -> cleaned.replace(separator.toString(), "")
                else -> cleaned.replace(separator, '.')
            }
        }
    }
    val value = normalized.toDoubleOrNull() ?: return null
    if (!value.isFinite()) return null
    return if (negative) -value else value
}

/**
 * Se [text] é um número com [separator] separando grupos de três ("2.700", "1.234.567"): primeiro grupo
 * de 1 a 3 dígitos sem zero à esquerda, os outros com exatamente 3.
 */
private fun isValidGrouping(text: String, separator: Char): Boolean {
    val groups = text.split(separator)
    if (groups.size < 2) return groups.single().all(Char::isDigit)
    val first = groups.first()
    if (first.isEmpty() || first.length > 3 || !first.all(Char::isDigit) || (first.length > 1 && first.startsWith('0')) || first == "0") return false
    return groups.drop(1).all { it.length == 3 && it.all(Char::isDigit) }
}

/** [parseDecimal] que falha com uma mensagem amigável quando o texto é inválido. */
fun String.toRequiredDouble(fieldLabel: String, kind: NumberKind = NumberKind.AMOUNT, decimalSeparator: Char = ','): Double =
    parseDecimal(this, kind, decimalSeparator) ?: error("Confira o campo \"$fieldLabel\": não é um número.")

/** [toRequiredDouble] maior que zero, com a mensagem dizendo qual campo (e não a do `require` do core). */
fun String.toRequiredPositive(fieldLabel: String, kind: NumberKind = NumberKind.AMOUNT): Double =
    toRequiredDouble(fieldLabel, kind).also { if (it <= 0) error("\"$fieldLabel\" precisa ser maior que zero.") }

/** [toRequiredDouble] que não aceita negativo. */
fun String.toRequiredNonNegative(fieldLabel: String, kind: NumberKind = NumberKind.AMOUNT): Double =
    toRequiredDouble(fieldLabel, kind).also { if (it < 0) error("\"$fieldLabel\" não pode ser negativo.") }

/** [toRequiredInt] maior que zero. */
fun String.toRequiredPositiveInt(fieldLabel: String): Int =
    toRequiredInt(fieldLabel).also { if (it <= 0) error("\"$fieldLabel\" precisa ser maior que zero.") }

/** Converte texto digitado pelo usuário em [Int], com mensagem amigável quando inválido. */
fun String.toRequiredInt(fieldLabel: String): Int =
    parseWholeNumber(this) ?: error("Confira o campo \"$fieldLabel\": use um número inteiro.")

/** Número inteiro digitado ("1.000" e "1000" valem mil), ou `null` se não for inteiro. */
fun parseWholeNumber(text: String): Int? {
    val value = parseDecimal(text, NumberKind.AMOUNT) ?: return null
    if (value != kotlin.math.floor(value) || abs(value) > Int.MAX_VALUE) return null
    return value.toInt()
}

/**
 * Um número pra pôr de volta num campo de texto (decisão 107): com o separador decimal da moeda, sem
 * ".0" à toa, sem separador de milhar (que o [parseDecimal] leria de volta igual) e sem o ruído do
 * ponto flutuante — 7% guardado como 0,07 voltava pro campo como "7.000000000000001".
 */
fun Double.toInputText(decimalSeparator: Char = ',', maxDecimals: Int = 6): String {
    if (!isFinite()) return ""
    val factor = 10.0.pow(maxDecimals)
    if (abs(this) * factor >= Long.MAX_VALUE.toDouble()) return toString()
    val scaled = (this * factor).roundToLong()
    val negative = scaled < 0
    val absolute = abs(scaled)
    val factorLong = factor.toLong()
    val integerPart = (absolute / factorLong).toString()
    val fraction = (absolute % factorLong).toString().padStart(maxDecimals, '0').trimEnd('0')
    val sign = if (negative && (absolute != 0L)) "-" else ""
    return if (fraction.isEmpty()) "$sign$integerPart" else "$sign$integerPart$decimalSeparator$fraction"
}

/**
 * O que o app entendeu de [text], pra mostrar embaixo do campo quando há como ler de dois jeitos (um
 * ponto numa moeda de vírgula): "= 2.700". `null` quando não há ambiguidade a mostrar.
 */
fun interpretationHint(text: String, kind: NumberKind, decimalSeparator: Char = ','): String? {
    val otherSeparator = if (decimalSeparator == ',') '.' else ','
    if (otherSeparator !in text) return null
    val value = parseDecimal(text, kind, decimalSeparator) ?: return null
    val grouping = otherSeparator
    val integer = abs(value).toLong()
    val groupedInteger = integer.toString().reversed().chunked(3).joinToString(grouping.toString()).reversed()
    val fraction = abs(value).toInputText(decimalSeparator).substringAfter(decimalSeparator, "")
    val sign = if (value < 0) "-" else ""
    return "= $sign$groupedInteger" + if (fraction.isNotEmpty()) "$decimalSeparator$fraction" else ""
}
