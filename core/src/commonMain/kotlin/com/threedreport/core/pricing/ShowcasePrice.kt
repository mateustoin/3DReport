package com.threedreport.core.pricing

import kotlin.math.round

/**
 * "Arredondar pra R$ 18,90" do preço anunciado (decisão 102): o próximo valor terminado em ,90 a partir
 * de [total], ou `null` quando ele já termina assim (depois de arredondado ao centavo).
 */
fun showcasePriceSuggestion(total: Double): Double? {
    val cents = round(total * 100).toLong()
    val suggestion = ((cents - 90 + 99) / 100) * 100 + 90
    return (suggestion / 100.0).takeIf { suggestion != cents }
}
