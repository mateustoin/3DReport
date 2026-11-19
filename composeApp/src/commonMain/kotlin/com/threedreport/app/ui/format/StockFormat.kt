package com.threedreport.app.ui.format

import com.threedreport.app.platform.AppLocale
import kotlin.math.abs
import kotlin.math.round

/** Gramas sem casa decimal ("640 g", "-40 g"): na tela do estoque, meio grama não muda nada. */
fun Double.toWholeGramsText(): String = "${round(this).toLong()} g"

/**
 * Gramas de uma compra: "500 g", e a partir de mil em quilos ("1 kg", "1,5 kg"), como o carretel é comprado. Duas casas no
 * máximo, sem zero sobrando.
 */
fun Double.toPurchaseGramsText(): String =
    if (abs(this) >= GRAMS_PER_KG) "${(this / GRAMS_PER_KG).toInputText(AppLocale.decimalSeparator, 2)} kg" else toWholeGramsText()

/** Unidades de insumo com até duas casas, sem zero sobrando ("8", "2,5"), no separador do idioma. O "un" vem do recurso. */
fun Double.toUnitsNumberText(): String = toInputText(AppLocale.decimalSeparator, 2)

private const val GRAMS_PER_KG = 1000.0
