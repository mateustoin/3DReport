package com.threedreport.core.slicer

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.PrinterProfile
import kotlin.math.sqrt

/** O que o G-code diz sobre a impressora, cruzado com o que está cadastrado. */
sealed interface PrinterMatch {
    /** Nome igual a um cadastrado (ignorando maiúsculas, espaços e pontuação): pode escolher. */
    data class Found(val printer: PrinterProfile) : PrinterMatch

    /** Só parecido com um cadastrado: vira sugestão, nunca escolha automática (decisão 89). */
    data class Similar(val gcodeName: String, val printer: PrinterProfile) : PrinterMatch

    data class NotRegistered(val gcodeName: String) : PrinterMatch

    /** O G-code não diz qual é a impressora. */
    data object Unknown : PrinterMatch
}

/** O que o G-code diz sobre o filamento de um extrusor, cruzado com os filamentos em estoque. */
sealed interface FilamentMatch {
    /** [color] é `null` quando não dá pra saber a cor com segurança. */
    data class Found(val filament: Filament, val color: FilamentColor?) : FilamentMatch

    /** Mais de um filamento cadastrado com esse tipo (e marca), e nenhum já escolhido entre eles. */
    data class Ambiguous(val type: String, val count: Int) : FilamentMatch

    data class NotRegistered(val type: String, val vendor: String?) : FilamentMatch

    /** O G-code não diz o material deste extrusor (o Cura só grava o consumo). */
    data object Unknown : FilamentMatch
}

/**
 * Um extrusor usado no G-code: quanto consumiu e com qual filamento cadastrado ele casa.
 *
 * @property lengthMeters consumo deste extrusor, com purga e torre, ou `null` quando o fatiador
 *   só informa o total (ver [GCodeFilament.lengthMeters]).
 */
data class ExtruderMatch(val lengthMeters: Double?, val match: FilamentMatch)

/**
 * Casa o que o fatiador gravou no G-code com os catálogos de impressoras e filamentos, pra montar
 * o orçamento num arrasto só (decisão 89). Função pura, sem estado.
 *
 * A regra é **precisão antes de alcance**: escolher a impressora ou o filamento errado muda o
 * preço sem o vendedor perceber, e isso é pior do que não escolher. Por isso só escolhe sozinho
 * quando o nome bate (ignorando maiúsculas, espaços e pontuação); "Bambu Lab A1" e "Bambu Lab A1
 * mini" são máquinas diferentes, com consumo diferente, e um nome que só contém o outro vira
 * sugestão na mensagem, sem seleção.
 */
object CatalogMatcher {

    fun matchPrinter(metadata: GCodeMetadata, printers: List<PrinterProfile>): PrinterMatch {
        val gcodeNames = listOfNotNull(metadata.printerModel, metadata.printerSettingsName)
        if (gcodeNames.isEmpty()) return PrinterMatch.Unknown
        val displayName = gcodeNames.first()
        val keys = gcodeNames.map(::normalize).filter { it.length >= MIN_KEY_LENGTH }.toSet()

        printers.firstOrNull { normalize(it.name) in keys }?.let { return PrinterMatch.Found(it) }

        val similar = printers
            .filter { printer -> keys.any { key -> looksAlike(printer.name, key) } }
            .minByOrNull { it.name.length }
        return similar?.let { PrinterMatch.Similar(displayName, it) } ?: PrinterMatch.NotRegistered(displayName)
    }

    /**
     * Um item por extrusor usado, na ordem do fatiador (leva 9, decisão 105): uma peça multicolor
     * vira um filamento por extrusor, cada um com o próprio consumo e casado sozinho, pela mesma
     * regra de precisão. O que não bate fica em branco pra escolher.
     *
     * @param preferredFilamentIds filamentos já escolhidos na tela: quando um deles está entre os
     *   candidatos, fica (o G-code confirma a escolha em vez de trocá-la).
     */
    fun matchFilaments(
        metadata: GCodeMetadata,
        filaments: List<Filament>,
        preferredFilamentIds: Collection<String> = emptyList(),
    ): List<ExtruderMatch> =
        metadata.filaments.map { ExtruderMatch(it.lengthMeters, matchFilament(it, filaments, preferredFilamentIds)) }

    private fun matchFilament(gcode: GCodeFilament, filaments: List<Filament>, preferredFilamentIds: Collection<String>): FilamentMatch {
        val type = gcode.type ?: return FilamentMatch.Unknown
        val vendor = gcode.vendor

        val sameType = filaments.filter { it.hasStockAvailable && it.materialType?.let(::normalize) == normalize(type) }
        val candidates = if (vendor != null) sameType.filter { it.brand?.let(::normalize) == normalize(vendor) } else sameType

        val chosen = when {
            candidates.isEmpty() -> return FilamentMatch.NotRegistered(type, vendor)
            candidates.size == 1 -> candidates.single()
            else -> candidates.firstOrNull { it.id in preferredFilamentIds } ?: return FilamentMatch.Ambiguous(type, candidates.size)
        }
        return FilamentMatch.Found(chosen, colorFor(chosen, gcode.colorHex))
    }

    /**
     * A cor só é escolhida quando há uma cor cadastrada parecida em estoque. Um filamento com uma
     * única cor em estoque dispensa a comparação.
     */
    private fun colorFor(filament: Filament, gcodeColor: String?): FilamentColor? {
        val inStock = filament.colors.filter { it.inStock }
        if (inStock.size == 1) return inStock.single()
        val target = gcodeColor?.let(::parseRgb) ?: return null
        return inStock
            .mapNotNull { color -> color.hex?.let(::parseRgb)?.let { color to distance(it, target) } }
            .filter { (_, distance) -> distance <= MAX_COLOR_DISTANCE }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    /** Minúsculas, só letras e dígitos: "Bambu Lab X1-Carbon" e "bambu lab x1 carbon" viram a mesma chave. */
    internal fun normalize(value: String): String = value.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Se um nome contém o outro, depois de normalizados (ex.: "Bambu Lab A1" e "Bambu Lab A1 mini"). É o
     * critério de "parecido" pra sugestão, nunca pra escolha.
     */
    private fun looksAlike(registeredName: String, gcodeKey: String): Boolean {
        val registered = normalize(registeredName)
        if (registered.length < MIN_KEY_LENGTH) return false
        return gcodeKey.contains(registered) || registered.contains(gcodeKey)
    }

    private fun parseRgb(hex: String): Triple<Int, Int, Int>? {
        val digits = hex.trim().removePrefix("#").take(6)
        if (digits.length != 6) return null
        val value = digits.toIntOrNull(16) ?: return null
        return Triple(value shr 16 and 0xFF, value shr 8 and 0xFF, value and 0xFF)
    }

    private fun distance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Double {
        val dr = (a.first - b.first).toDouble()
        val dg = (a.second - b.second).toDouble()
        val db = (a.third - b.third).toDouble()
        return sqrt(dr * dr + dg * dg + db * db)
    }

    /** Nome curto demais ("A1", "K1") casaria com meio catálogo. */
    private const val MIN_KEY_LENGTH = 4

    /** Até onde duas cores ainda são "a mesma" (distância RGB; branco e branco-gelo ficam bem abaixo). */
    private const val MAX_COLOR_DISTANCE = 100.0
}
