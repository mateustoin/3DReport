package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Dados de uma peça a ser orçada, normalmente informados pelo fatiador.
 *
 * Estes são os únicos parâmetros que variam de peça para peça; o restante
 * vem de [PricingSettings].
 *
 * @property filament filamento utilizado (com preço e densidade).
 * @property filamentLengthMeters comprimento de filamento consumido, em metros.
 * @property printTimeMinutes tempo de impressão, em minutos.
 * @property filamentColor qual [FilamentColor] de [filament] foi usado nesta
 *   peça, se o filamento tiver mais de uma cor cadastrada. Não afeta o
 *   cálculo (preço/densidade são do filamento, não da cor) — é só registro.
 */
@Serializable
data class PrintJob(
    val filament: Filament,
    val filamentLengthMeters: Double,
    val printTimeMinutes: Double,
    val filamentColor: FilamentColor? = null,
) {
    init {
        require(filamentLengthMeters >= 0) { "filamentLengthMeters não pode ser negativo: $filamentLengthMeters" }
        require(printTimeMinutes >= 0) { "printTimeMinutes não pode ser negativo: $printTimeMinutes" }
    }

    /** Tempo de impressão convertido para horas. */
    val printTimeHours: Double
        get() = printTimeMinutes / 60.0
}
