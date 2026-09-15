package com.threedreport.core.model

import kotlin.math.PI

/**
 * Filamento usado na impressão.
 *
 * @property name nome livre para identificação (ex.: "PLA", "ABS barato").
 * @property pricePerKg preço do quilo do filamento, em R$.
 * @property densityGPerCm3 densidade do material, em g/cm³ (ex.: PLA ≈ 1,24; ABS ≈ 1,04; PETG ≈ 1,27).
 * @property diameterMm diâmetro nominal do filamento, em mm.
 */
data class Filament(
    val name: String,
    val pricePerKg: Double,
    val densityGPerCm3: Double,
    val diameterMm: Double = DEFAULT_DIAMETER_MM,
) {
    init {
        require(pricePerKg >= 0) { "pricePerKg não pode ser negativo: $pricePerKg" }
        require(densityGPerCm3 > 0) { "densityGPerCm3 deve ser positivo: $densityGPerCm3" }
        require(diameterMm > 0) { "diameterMm deve ser positivo: $diameterMm" }
    }

    /** Área da seção transversal do filamento, em mm² (π · r²). */
    val crossSectionAreaMm2: Double
        get() = PI * (diameterMm / 2) * (diameterMm / 2)

    /**
     * Massa, em gramas, de [lengthMeters] metros deste filamento.
     *
     * volume (cm³) = comprimento (mm) · área (mm²) / 1000
     * massa (g)    = volume (cm³) · densidade (g/cm³)
     */
    fun weightGrams(lengthMeters: Double): Double {
        val volumeCm3 = lengthMeters * MM_PER_METER * crossSectionAreaMm2 / MM3_PER_CM3
        return volumeCm3 * densityGPerCm3
    }

    companion object {
        const val DEFAULT_DIAMETER_MM = 1.75
        private const val MM_PER_METER = 1000.0
        private const val MM3_PER_CM3 = 1000.0
    }
}
