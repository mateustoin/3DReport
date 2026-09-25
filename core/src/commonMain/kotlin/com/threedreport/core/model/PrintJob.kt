package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Quanto de um filamento uma impressão consome. Uma peça multicolor (AMS, MMU) tem um por
 * filamento; a maioria das peças tem um só.
 *
 * @property filament retrato do filamento usado (com preço e densidade no momento do orçamento).
 * @property lengthMeters comprimento consumido numa rodada da impressão, em metros. Quando vem do
 *   G-code, já inclui a purga e a torre de limpeza, que são custo de verdade (conferido em arquivos
 *   reais, decisão 105).
 * @property color qual [FilamentColor] de [filament] foi usado, se o filamento tiver mais de uma
 *   cor cadastrada. Não afeta o cálculo (preço/densidade são do filamento, não da cor): é registro
 *   e é o que diferencia duas cores do mesmo filamento numa peça multicolor.
 */
@Serializable
data class FilamentUsage(
    val filament: Filament,
    val lengthMeters: Double,
    val color: FilamentColor? = null,
) {
    init {
        require(lengthMeters >= 0) { "lengthMeters não pode ser negativo: $lengthMeters" }
    }

    /** Massa consumida numa rodada, em gramas. */
    val weightGrams: Double
        get() = filament.weightGrams(lengthMeters)
}

/**
 * Uma impressão: uma mesa que a impressora roda, como o fatiador informa. Um pedido pode ter
 * várias (uma action figure em cabeça, corpo e base), cada uma na sua impressora (ver
 * [QuotedPrint]); o que é do pedido inteiro (trabalho, quantidade, canal) fica no [Quote].
 *
 * O custo é da mesa, não de cada parte que está nela: uma mesa com três partes é uma impressão só.
 *
 * @property filaments o que esta impressão consome, um item por filamento. Nunca vazio.
 * @property printTimeMinutes tempo de uma rodada, em minutos.
 * @property runs quantas vezes a mesma mesa roda num pedido (4 mesas iguais de peças pequenas).
 *   Diferente de [Quote.quantity], que é quantos pedidos iguais: cada impressão multiplica pelos
 *   próprios [runs] e depois pela quantidade.
 * @property name nome opcional pra identificar a impressão ("Cabeça"). Uso interno.
 */
@Serializable
data class PrintJob(
    val filaments: List<FilamentUsage>,
    val printTimeMinutes: Double,
    val runs: Int = 1,
    val name: String? = null,
) {
    /** Atalho pra impressão de um filamento só, que é a maioria. */
    constructor(
        filament: Filament,
        filamentLengthMeters: Double,
        printTimeMinutes: Double,
        filamentColor: FilamentColor? = null,
    ) : this(filaments = listOf(FilamentUsage(filament, filamentLengthMeters, filamentColor)), printTimeMinutes = printTimeMinutes)

    init {
        require(filaments.isNotEmpty()) { "uma impressão precisa de pelo menos um filamento" }
        require(printTimeMinutes >= 0) { "printTimeMinutes não pode ser negativo: $printTimeMinutes" }
        require(runs >= 1) { "runs deve ser pelo menos 1: $runs" }
    }

    /** Tempo de uma rodada, em horas. */
    val printTimeHours: Double
        get() = printTimeMinutes / 60.0

    /** Comprimento somado de todos os filamentos numa rodada, em metros. */
    val totalLengthMeters: Double
        get() = filaments.sumOf { it.lengthMeters }

    /** Massa somada de todos os filamentos numa rodada, em gramas. */
    val weightGrams: Double
        get() = filaments.sumOf { it.weightGrams }

    /**
     * Tempo de máquina de todas as rodadas, em minutos: o que esta impressão ocupa por unidade do
     * pedido. Todo total do [Quote] parte daqui (e de [allRunsWeightGrams]) e só multiplica pela
     * quantidade, pra nenhuma conta esquecer as rodadas.
     */
    val allRunsPrintTimeMinutes: Double
        get() = printTimeMinutes * runs

    /** Massa de todas as rodadas, em gramas (ver [allRunsPrintTimeMinutes]). */
    val allRunsWeightGrams: Double
        get() = weightGrams * runs
}
