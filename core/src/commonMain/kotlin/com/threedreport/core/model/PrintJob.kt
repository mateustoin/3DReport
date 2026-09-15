package com.threedreport.core.model

/**
 * Dados de uma peça a ser orçada, normalmente informados pelo fatiador.
 *
 * Estes são os únicos parâmetros que variam de peça para peça; o restante
 * vem de [PricingSettings].
 *
 * @property filament filamento utilizado (com preço e densidade).
 * @property filamentLengthMeters comprimento de filamento consumido, em metros.
 * @property printTimeMinutes tempo de impressão, em minutos.
 */
data class PrintJob(
    val filament: Filament,
    val filamentLengthMeters: Double,
    val printTimeMinutes: Double,
) {
    init {
        require(filamentLengthMeters >= 0) { "filamentLengthMeters não pode ser negativo: $filamentLengthMeters" }
        require(printTimeMinutes >= 0) { "printTimeMinutes não pode ser negativo: $printTimeMinutes" }
    }

    /** Tempo de impressão convertido para horas. */
    val printTimeHours: Double
        get() = printTimeMinutes / 60.0
}
