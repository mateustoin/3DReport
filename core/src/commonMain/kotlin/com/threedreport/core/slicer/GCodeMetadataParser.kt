package com.threedreport.core.slicer

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Metadados lidos dos comentários de um G-code exportado por um fatiador.
 *
 * Qualquer um dos campos pode vir `null` se o fatiador usado não gravar esse
 * dado num formato reconhecido — nesse caso o campo correspondente na tela de
 * Orçamento simplesmente não é preenchido, continua editável/escolhível na
 * mão. [layerHeightMm]/[infillPercentage]/[infillPattern]/[supportsEnabled]
 * só são reconhecidos no bloco de configuração completo que PrusaSlicer/
 * Bambu Studio/OrcaSlicer gravam no fim do arquivo — o Cura não grava esse
 * bloco por padrão, então esses quatro campos ficam sempre `null` pra G-codes
 * exportados dele (mesma limitação de [thumbnail]).
 */
data class GCodeMetadata(
    val filamentLengthMeters: Double?,
    val printTimeMinutes: Double?,
    val thumbnail: GCodeThumbnail?,
    val layerHeightMm: Double? = null,
    val infillPercentage: Double? = null,
    val infillPattern: String? = null,
    val supportsEnabled: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = filamentLengthMeters == null && printTimeMinutes == null && thumbnail == null &&
            layerHeightMm == null && infillPercentage == null && infillPattern == null && supportsEnabled == null
}

/**
 * Miniatura do modelo (prévia renderizada pelo fatiador) embutida no G-code,
 * já decodificada de base64 — pronta pra usar como foto do orçamento.
 */
data class GCodeThumbnail(val bytes: ByteArray, val fileExtension: String)

/**
 * Lê os comentários de metadados que a maioria dos fatiadores (PrusaSlicer,
 * Bambu Studio/OrcaSlicer, Cura) grava no cabeçalho/rodapé do G-code —
 * consumo de filamento, tempo estimado de impressão e, quando presente, uma
 * miniatura do modelo. Não interpreta nenhum comando de movimento, só os
 * comentários de texto.
 */
object GCodeMetadataParser {

    @OptIn(ExperimentalEncodingApi::class)
    fun parse(text: String): GCodeMetadata = GCodeMetadata(
        filamentLengthMeters = parseFilamentLengthMeters(text),
        printTimeMinutes = parsePrintTimeMinutes(text),
        thumbnail = parseThumbnail(text),
        layerHeightMm = layerHeightRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull(),
        infillPercentage = infillDensityRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull(),
        infillPattern = infillPatternRegex.find(text)?.groupValues?.get(1)?.trim(),
        supportsEnabled = supportsRegex.find(text)?.groupValues?.get(1)?.let(::parseBooleanFlag),
    )

    // PrusaSlicer/Bambu Studio/OrcaSlicer, ex.: "; filament used [mm] = 1234.56"
    // ou "; total filament length [mm] : 1234.56" (múltiplos extrusores separados por vírgula).
    private val filamentMillimetersRegex =
        Regex("""(?im)^;\s*(?:total\s+)?filament (?:used|length)\s*\[mm]\s*[:=]\s*(.+)$""")

    // Cura, ex.: ";Filament used: 2.5m" (já em metros; múltiplos extrusores separados por vírgula).
    private val filamentMetersRegex =
        Regex("""(?im)^;\s*filament used\s*[:=]\s*(.+)$""")

    private fun parseFilamentLengthMeters(text: String): Double? {
        filamentMillimetersRegex.find(text)?.let { match ->
            sumNumbers(match.groupValues[1])?.let { return it / 1000.0 }
        }
        filamentMetersRegex.find(text)?.let { match ->
            sumMeterValues(match.groupValues[1])?.let { return it }
        }
        return null
    }

    // PrusaSlicer/Bambu Studio/OrcaSlicer, ex.:
    // "; estimated printing time (normal mode) = 1h 23m 45s" ou "; total estimated time: 1h23m45s".
    private val durationLabelRegex =
        Regex("""(?im)^;\s*(?:estimated printing time|total estimated time)\s*(?:\([^)]*\))?\s*[:=]\s*(.+)$""")

    // Cura, ex.: ";TIME:12345" (segundos).
    private val secondsRegex = Regex("""(?im)^;\s*TIME\s*:\s*(\d+)\s*$""")

    private fun parsePrintTimeMinutes(text: String): Double? {
        durationLabelRegex.find(text)?.let { match ->
            parseDurationToMinutes(match.groupValues[1])?.let { return it }
        }
        secondsRegex.find(text)?.let { match ->
            return match.groupValues[1].toDouble() / 60.0
        }
        return null
    }

    private fun sumNumbers(value: String): Double? {
        val numbers = Regex("""[0-9]+(?:\.[0-9]+)?""").findAll(value).map { it.value.toDouble() }.toList()
        return numbers.takeIf { it.isNotEmpty() }?.sum()
    }

    private fun sumMeterValues(value: String): Double? {
        val numbers = Regex("""([0-9]+(?:\.[0-9]+)?)\s*m""").findAll(value)
            .map { it.groupValues[1].toDouble() }
            .toList()
        return numbers.takeIf { it.isNotEmpty() }?.sum()
    }

    private fun parseDurationToMinutes(value: String): Double? {
        val days = Regex("""(\d+)d""").find(value)?.groupValues?.get(1)?.toDouble() ?: 0.0
        val hours = Regex("""(\d+)h""").find(value)?.groupValues?.get(1)?.toDouble() ?: 0.0
        val minutes = Regex("""(\d+)m""").find(value)?.groupValues?.get(1)?.toDouble() ?: 0.0
        val seconds = Regex("""(\d+)s""").find(value)?.groupValues?.get(1)?.toDouble() ?: 0.0
        if (days == 0.0 && hours == 0.0 && minutes == 0.0 && seconds == 0.0) return null
        return days * 24 * 60 + hours * 60 + minutes + seconds / 60.0
    }

    // PrusaSlicer/Bambu Studio/OrcaSlicer, ex.: "; layer_height = 0.2" (bloco de configuração
    // completo gravado no fim do arquivo — mesma limitação de fatiador do thumbnail, ver KDoc).
    private val layerHeightRegex = Regex("""(?im)^;\s*layer_height\s*=\s*([0-9.]+)\s*$""")

    // PrusaSlicer usa "fill_density"; OrcaSlicer/Bambu Studio (bifurcação mais recente do Prusa)
    // usam "sparse_infill_density". Ambos gravam como porcentagem, com ou sem o "%" no valor.
    private val infillDensityRegex = Regex("""(?im)^;\s*(?:fill_density|sparse_infill_density)\s*=\s*([0-9.]+)%?\s*$""")

    private val infillPatternRegex = Regex("""(?im)^;\s*(?:fill_pattern|sparse_infill_pattern)\s*=\s*(\S+)\s*$""")

    // PrusaSlicer usa "support_material"; OrcaSlicer/Bambu Studio usam "enable_support". Ambos
    // gravam "1"/"0", mas aceita "true"/"false" também por segurança.
    private val supportsRegex = Regex("""(?im)^;\s*(?:support_material|enable_support)\s*=\s*(\S+)\s*$""")

    private fun parseBooleanFlag(value: String): Boolean? = when (value.trim().lowercase()) {
        "1", "true" -> true
        "0", "false" -> false
        else -> null
    }

    // PrusaSlicer/SuperSlicer/OrcaSlicer/Bambu Studio embutem uma ou mais prévias do modelo (em
    // tamanhos diferentes) como PNG ou JPEG em base64, delimitadas por comentários "thumbnail
    // begin"/"thumbnail end" — cada linha do bloco é ";" + um pedaço da string base64. Cura não usa
    // esse formato em G-code puro, por isso não tem equivalente aqui.
    private val thumbnailBlockRegex =
        Regex("""(?is);\s*thumbnail(_png|_jpg)?\s+begin\s+(\d+)x(\d+)\s+\d+(.*?);\s*thumbnail(?:_png|_jpg)?\s+end""")

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseThumbnail(text: String): GCodeThumbnail? =
        thumbnailBlockRegex.findAll(text)
            .mapNotNull { match ->
                val width = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                val height = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                val isJpeg = match.groupValues[1].contains("jpg", ignoreCase = true)
                val base64 = match.groupValues[4].lineSequence()
                    .map { it.trim().removePrefix(";").trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("")
                val bytes = runCatching { Base64.decode(base64) }.getOrNull() ?: return@mapNotNull null
                Triple(width * height, bytes, if (isJpeg) "jpg" else "png")
            }
            .maxByOrNull { it.first }
            ?.let { (_, bytes, extension) -> GCodeThumbnail(bytes, extension) }
}
