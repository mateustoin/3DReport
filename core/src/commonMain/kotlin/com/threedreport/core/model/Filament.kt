package com.threedreport.core.model

import kotlinx.serialization.Serializable
import kotlin.math.PI

/**
 * Filamento usado na impressão. Um catálogo de filamentos é salvo pela UI;
 * cada orçamento escolhe um deles.
 *
 * @property id identificador único, atribuído por quem cria o filamento (UI).
 * @property name nome livre para identificação (ex.: "PLA", "ABS barato").
 * @property pricePerKg preço do quilo do filamento, em R$.
 * @property densityGPerCm3 densidade do material, em g/cm³ (ex.: PLA ≈ 1,24; ABS ≈ 1,04; PETG ≈ 1,27).
 * @property diameterMm diâmetro nominal do filamento, em mm.
 * @property brand marca/fabricante do filamento, se houver (ex.: "Voolt").
 * @property materialType tipo do material, se houver (ex.: "PLA", "PETG-CF").
 *   Texto livre, igual [brand] — a UI oferece uma lista de tipos comuns (com
 *   densidade padrão sugerida) mais uma opção de digitar um tipo que não
 *   esteja nela, mas o modelo não força nenhum valor específico.
 * @property colors variantes de cor deste filamento (mesma marca/preço/
 *   densidade, rolos diferentes) — ver [FilamentColor]. Existe pra evitar
 *   cadastro duplicado de filamentos idênticos que só diferem na cor.
 *   Controle de estoque é **manual e por cor** (decisão: dedução automática
 *   pelo consumo é imprecisa na prática — falhas de impressão, testes e
 *   sobras consomem material sem virar um orçamento salvo). Uma cor com
 *   `inStock = false` ("Acabou") continua na lista (histórico de orçamentos
 *   antigos continua coerente) mas não aparece pra seleção na tela de
 *   Orçamento. Nunca fica vazia depois de salvo pela UI — um filamento sem
 *   necessidade de diferenciar cor tem uma única entrada sem nome/hex.
 * @property archived arquivado (decisão 115): some das escolhas de um orçamento novo, mas continua no
 *   cadastro pra quem já usou. Pedidos reabertos e produtos do catálogo continuam achando ele.
 */
@Serializable
data class Filament(
    val id: String,
    val name: String,
    val pricePerKg: Double,
    val densityGPerCm3: Double,
    val diameterMm: Double = DEFAULT_DIAMETER_MM,
    val brand: String? = null,
    val materialType: String? = null,
    val colors: List<FilamentColor> = listOf(FilamentColor(id = "default")),
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(pricePerKg >= 0) { "pricePerKg não pode ser negativo: $pricePerKg" }
        require(densityGPerCm3 > 0) { "densityGPerCm3 deve ser positivo: $densityGPerCm3" }
        require(diameterMm > 0) { "diameterMm deve ser positivo: $diameterMm" }
    }

    /** Se há alguma cor em estoque — controla se o filamento aparece pra seleção na tela de Orçamento. */
    val hasStockAvailable: Boolean
        get() = colors.any { it.inStock }

    /** Área da seção transversal do filamento, em mm² (π · r²). */
    val crossSectionAreaMm2: Double
        get() = PI * (diameterMm / 2) * (diameterMm / 2)

    /**
     * Massa, em gramas, de [lengthMeters] metros deste filamento.
     *
     * volume (cm³) = comprimento (mm) · área (mm²) / 1000
     * massa (g)    = volume (cm³) · densidade (g/cm³)
     */
    fun weightGrams(lengthMeters: Double): Double = filamentWeightGrams(lengthMeters, diameterMm, densityGPerCm3)

    /** Comprimento, em metros, que pesa [grams] gramas: o inverso de [weightGrams]. */
    fun lengthMeters(grams: Double): Double = grams / weightGrams(1.0)

    companion object {
        const val DEFAULT_DIAMETER_MM = 1.75
    }
}

private const val MM_PER_METER = 1000.0
private const val MM3_PER_CM3 = 1000.0

/**
 * Massa, em gramas, de [lengthMeters] metros de um filamento de [diameterMm] e [densityGPerCm3].
 * Uma fórmula só pro cadastro ([Filament]) e pro retrato guardado no orçamento ([FilamentSnapshot]).
 *
 * volume (cm³) = comprimento (mm) · área (mm²) / 1000
 * massa (g)    = volume (cm³) · densidade (g/cm³)
 */
internal fun filamentWeightGrams(lengthMeters: Double, diameterMm: Double, densityGPerCm3: Double): Double {
    val crossSectionAreaMm2 = PI * (diameterMm / 2) * (diameterMm / 2)
    val volumeCm3 = lengthMeters * MM_PER_METER * crossSectionAreaMm2 / MM3_PER_CM3
    return volumeCm3 * densityGPerCm3
}

/**
 * Retrato de um [Filament] dentro de um orçamento salvo: só o que a conta e o histórico usam (preço,
 * densidade, diâmetro e identificação). Fica de fora a lista de cores com estoque do cadastro, que
 * muda o tempo todo e não diz nada sobre o que foi orçado (a cor usada fica em [FilamentUsage.color]).
 *
 * @property id o [Filament.id] de origem, pra reabrir e recalcular com o cadastro atual.
 */
@Serializable
data class FilamentSnapshot(
    val id: String,
    val name: String,
    val pricePerKg: Double,
    val densityGPerCm3: Double,
    val diameterMm: Double = Filament.DEFAULT_DIAMETER_MM,
    val brand: String? = null,
    val materialType: String? = null,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
        require(pricePerKg >= 0) { "pricePerKg não pode ser negativo: $pricePerKg" }
        require(densityGPerCm3 > 0) { "densityGPerCm3 deve ser positivo: $densityGPerCm3" }
        require(diameterMm > 0) { "diameterMm deve ser positivo: $diameterMm" }
    }

    /** Massa, em gramas, de [lengthMeters] metros deste filamento (mesma fórmula de [Filament.weightGrams]). */
    fun weightGrams(lengthMeters: Double): Double = filamentWeightGrams(lengthMeters, diameterMm, densityGPerCm3)

    companion object {
        fun of(filament: Filament) = FilamentSnapshot(
            id = filament.id,
            name = filament.name,
            pricePerKg = filament.pricePerKg,
            densityGPerCm3 = filament.densityGPerCm3,
            diameterMm = filament.diameterMm,
            brand = filament.brand,
            materialType = filament.materialType,
        )
    }
}
