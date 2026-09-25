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
 *
 * @property printerModel modelo da impressora como o fatiador grava: `printer_model` na família
 *   PrusaSlicer/Orca/Bambu, e `machine_name` (ou, na falta dele, a `definition`) no bloco
 *   `;SETTING_3` do Cura. Conferido em arquivos reais (decisão 89).
 * @property printerSettingsName nome do perfil de impressora (`printer_settings_id`), já sem o
 *   bico ("0.4 nozzle") e sem o "- Copy" que o fatiador acrescenta a perfis copiados: às vezes é
 *   mais legível que [printerModel], que pode ser um identificador interno.
 * @property filamentLengthMeters consumo total, somando todos os extrusores, em metros.
 * @property filaments um item por extrusor/slot **usado**, na ordem do fatiador, com o consumo de
 *   cada um quando o fatiador informa (ver [GCodeFilament.lengthMeters]). Slot declarado e sem
 *   consumo (`0.00`) fica de fora: não é material da peça. No Cura só vem o consumo, sem tipo nem
 *   marca.
 */
data class GCodeMetadata(
    val filamentLengthMeters: Double?,
    val printTimeMinutes: Double?,
    val thumbnail: GCodeThumbnail?,
    val layerHeightMm: Double? = null,
    val infillPercentage: Double? = null,
    val infillPattern: String? = null,
    val supportsEnabled: Boolean? = null,
    val printerModel: String? = null,
    val printerSettingsName: String? = null,
    val filaments: List<GCodeFilament> = emptyList(),
) {
    val isEmpty: Boolean
        get() = filamentLengthMeters == null && printTimeMinutes == null && thumbnail == null &&
            layerHeightMm == null && infillPercentage == null && infillPattern == null && supportsEnabled == null &&
            printerModel == null && printerSettingsName == null && filaments.isEmpty()
}

/**
 * Filamento de um extrusor, como o fatiador grava (`filament_type`, `filament_vendor`,
 * `filament_colour`). [vendor] vem `null` quando o fatiador grava um marcador genérico no lugar
 * da marca ("Generic", "(Undefined)", "(Unknown)", vistos em arquivos reais): isso não é uma marca
 * e não pode ser comparado com a marca cadastrada.
 *
 * @property lengthMeters consumo deste extrusor, em metros, ou `null` quando o fatiador não informa
 *   por extrusor. Já inclui a purga e a torre de limpeza: num G-code real do OrcaSlicer com torre, a
 *   extrusão somada de cada ferramenta bate com o valor do cabeçalho (decisão 105).
 */
data class GCodeFilament(val type: String?, val vendor: String?, val colorHex: String?, val lengthMeters: Double? = null)

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
// Opções em vez de flags embutidas como "(?im)", que o JavaScript não aceita: o `core` também
// roda no navegador (calculadora do site, decisão 98).
private val IGNORE_CASE_MULTILINE = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)

object GCodeMetadataParser {

    @OptIn(ExperimentalEncodingApi::class)
    fun parse(text: String): GCodeMetadata {
        val lengthsMeters = parseFilamentLengthsMeters(text)
        return GCodeMetadata(
            filamentLengthMeters = lengthsMeters?.sum(),
            printTimeMinutes = parsePrintTimeMinutes(text),
            thumbnail = parseThumbnail(text),
            layerHeightMm = layerHeightRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull(),
            infillPercentage = infillDensityRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull(),
            infillPattern = infillPatternRegex.find(text)?.groupValues?.get(1)?.trim(),
            supportsEnabled = supportsRegex.find(text)?.groupValues?.get(1)?.let(::parseBooleanFlag),
            printerModel = parsePrinterModel(text),
            printerSettingsName = printerSettingsRegex.find(text)?.groupValues?.get(1)?.let(::cleanPrinterSettingsName),
            filaments = parseFilaments(text, lengthsMeters),
        )
    }

    // PrusaSlicer/OrcaSlicer, ex.: "; filament used [mm] = 1384.73, 1805.62", e Bambu Studio, ex.:
    // "; total filament length [mm] : 9035.47,11467.79": um valor por extrusor, separados por
    // vírgula, na mesma ordem de "filament_type" (conferido em arquivos reais, decisão 105).
    private val filamentMillimetersRegex =
        Regex("""^;\s*(?:total\s+)?filament (?:used|length)\s*\[mm\]\s*[:=]\s*(.+)$""", IGNORE_CASE_MULTILINE)

    // Cura, ex.: ";Filament used: 2.5m, 0m" (já em metros; um valor por extrusor).
    private val filamentMetersRegex =
        Regex("""^;\s*filament used\s*[:=]\s*(.+)$""", IGNORE_CASE_MULTILINE)

    /** Consumo de cada extrusor, em metros, na ordem do fatiador (inclusive os que ficaram em zero). */
    private fun parseFilamentLengthsMeters(text: String): List<Double>? {
        filamentMillimetersRegex.find(text)?.let { match ->
            numbers(match.groupValues[1])?.let { values -> return values.map { it / 1000.0 } }
        }
        filamentMetersRegex.find(text)?.let { match ->
            meterValues(match.groupValues[1])?.let { return it }
        }
        return null
    }

    // PrusaSlicer/OrcaSlicer, ex.: "; estimated printing time (normal mode) = 1h 23m 45s".
    // Bambu Studio 2.x grava tudo numa linha só, ex.:
    // "; model printing time: 1m 6s; total estimated time: 9m 3s" (conferido em arquivos reais,
    // decisão 89). Antes a busca exigia o rótulo no começo da linha e o tempo do Bambu não era lido;
    // agora aceita o rótulo no meio, e usa o "total estimated time", que inclui o preparo da máquina
    // (é o tempo em que ela fica ocupada).
    private val durationLabelRegex =
        Regex("""^;.*?\b(?:estimated printing time|total estimated time)\s*(?:\([^)]*\))?\s*[:=]\s*([^;\n]+)""", IGNORE_CASE_MULTILINE)

    // Cura, ex.: ";TIME:12345" (segundos).
    private val secondsRegex = Regex("""^;\s*TIME\s*:\s*(\d+)\s*$""", IGNORE_CASE_MULTILINE)

    private fun parsePrintTimeMinutes(text: String): Double? {
        durationLabelRegex.find(text)?.let { match ->
            parseDurationToMinutes(match.groupValues[1])?.let { return it }
        }
        secondsRegex.find(text)?.let { match ->
            return match.groupValues[1].toDouble() / 60.0
        }
        return null
    }

    private fun numbers(value: String): List<Double>? =
        Regex("""[0-9]+(?:\.[0-9]+)?""").findAll(value).map { it.value.toDouble() }.toList().takeIf { it.isNotEmpty() }

    private fun meterValues(value: String): List<Double>? =
        Regex("""([0-9]+(?:\.[0-9]+)?)\s*m""").findAll(value).map { it.groupValues[1].toDouble() }.toList().takeIf { it.isNotEmpty() }

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
    private val layerHeightRegex = Regex("""^;\s*layer_height\s*=\s*([0-9.]+)\s*$""", IGNORE_CASE_MULTILINE)

    // PrusaSlicer usa "fill_density"; OrcaSlicer/Bambu Studio (bifurcação mais recente do Prusa)
    // usam "sparse_infill_density". Ambos gravam como porcentagem, com ou sem o "%" no valor.
    private val infillDensityRegex = Regex("""^;\s*(?:fill_density|sparse_infill_density)\s*=\s*([0-9.]+)%?\s*$""", IGNORE_CASE_MULTILINE)

    private val infillPatternRegex = Regex("""^;\s*(?:fill_pattern|sparse_infill_pattern)\s*=\s*(\S+)\s*$""", IGNORE_CASE_MULTILINE)

    // PrusaSlicer usa "support_material"; OrcaSlicer/Bambu Studio usam "enable_support". Ambos
    // gravam "1"/"0", mas aceita "true"/"false" também por segurança.
    private val supportsRegex = Regex("""^;\s*(?:support_material|enable_support)\s*=\s*(\S+)\s*$""", IGNORE_CASE_MULTILINE)

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
        Regex(""";\s*thumbnail(_png|_jpg)?\s+begin\s+(\d+)x(\d+)\s+\d+([\s\S]*?);\s*thumbnail(?:_png|_jpg)?\s+end""", RegexOption.IGNORE_CASE)

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

    // Família PrusaSlicer/SuperSlicer/OrcaSlicer/Bambu Studio, no bloco de configuração (conferido
    // em arquivos reais de cada um, decisão 89), ex.:
    // "; printer_model = Bambu Lab X1 Carbon" e "; printer_settings_id = Bambu Lab X1 Carbon 0.4 nozzle".
    private val printerModelRegex = Regex("""^;\s*printer_model\s*=\s*(.*)$""", IGNORE_CASE_MULTILINE)
    private val printerSettingsRegex = Regex("""^;\s*printer_settings_id\s*=\s*(.*)$""", IGNORE_CASE_MULTILINE)

    // Cura não tem esse bloco: a impressora só aparece dentro do JSON de ";SETTING_3", quebrado em
    // várias linhas (inclusive no meio de uma palavra). "machine_name" só vem quando o perfil mexeu
    // no nome; "definition" (ex.: "creality_ender3") vem sempre.
    private val curaSettingsLineRegex = Regex("""^;SETTING_3 (.*)$""", RegexOption.MULTILINE)
    // Termina na próxima quebra escapada do JSON ("\\n", uma barra seguida de "n") ou no fim da linha.
    private val curaMachineNameRegex = Regex("""machine_name = ([^\\\r\n]+)""")
    private val curaDefinitionRegex = Regex("""definition = ([A-Za-z0-9_]+)""")

    private fun parsePrinterModel(text: String): String? {
        printerModelRegex.find(text)?.groupValues?.get(1)?.let(::unquote)?.let { return it }
        val curaSettings = curaSettingsLineRegex.findAll(text).joinToString("") { it.groupValues[1].trimEnd('\r') }
        if (curaSettings.isEmpty()) return null
        curaMachineNameRegex.find(curaSettings)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return curaDefinitionRegex.find(curaSettings)?.groupValues?.get(1)?.replace('_', ' ')
    }

    /**
     * "test_bed.3mf (Voron_v2_300_afterburner 0.4 nozzle - Copy)" vira "Voron_v2_300_afterburner":
     * o fatiador embrulha o perfil no nome do projeto quando ele vem de um .3mf, acrescenta
     * "- Copy" a perfis copiados e sempre termina com o bico. Nada disso é o nome da impressora.
     */
    private fun cleanPrinterSettingsName(raw: String): String? {
        var name = unquote(raw) ?: return null
        Regex("""\(([^()]*)\)\s*$""").find(name)?.let { name = it.groupValues[1] }
        name = name.replace(Regex("""\s*-\s*Copy\s*$""", RegexOption.IGNORE_CASE), "")
        name = name.replace(Regex("""\s+[0-9]+(?:\.[0-9]+)?\s*nozzle.*$""", RegexOption.IGNORE_CASE), "")
        return name.trim().takeIf { it.isNotEmpty() }
    }

    private val filamentTypeRegex = Regex("""^;\s*filament_type\s*=\s*(.*)$""", IGNORE_CASE_MULTILINE)
    private val filamentVendorRegex = Regex("""^;\s*filament_vendor\s*=\s*(.*)$""", IGNORE_CASE_MULTILINE)
    private val filamentColourRegex = Regex("""^;\s*filament_colou?r\s*=\s*(.*)$""", IGNORE_CASE_MULTILINE)

    /** Marcadores que os fatiadores gravam no lugar da marca quando o perfil não tem uma (vistos em arquivos reais). */
    private val placeholderVendors = setOf("generic", "(undefined)", "(unknown)", "undefined", "unknown")

    /**
     * Um valor por extrusor, separados por ";" (ex.: "PLA;PETG"), como a família Prusa grava, casado
     * pela posição com o consumo de cada extrusor. Se as duas listas não tiverem o mesmo tamanho, a
     * posição não é confiável e o consumo por extrusor fica de fora (só o total vale): cobrar o
     * material errado é pior do que pedir pra pessoa preencher.
     */
    private fun parseFilaments(text: String, lengthsMeters: List<Double>?): List<GCodeFilament> {
        val types = splitPerExtruder(filamentTypeRegex.find(text)?.groupValues?.get(1))
        val vendors = splitPerExtruder(filamentVendorRegex.find(text)?.groupValues?.get(1))
        val colours = splitPerExtruder(filamentColourRegex.find(text)?.groupValues?.get(1))
        val declared = maxOf(types.size, vendors.size, colours.size)
        val lengths = lengthsMeters?.takeIf { declared == 0 || it.size == declared }
        val count = maxOf(declared, lengths?.size ?: 0)
        return (0 until count).map { index ->
            GCodeFilament(
                type = types.getOrNull(index),
                vendor = vendors.getOrNull(index)?.takeUnless { it.lowercase() in placeholderVendors },
                colorHex = colours.getOrNull(index)?.takeIf { it.startsWith("#") },
                lengthMeters = lengths?.getOrNull(index),
            )
        }.filter { it.lengthMeters != 0.0 }
            .filter { it.type != null || it.vendor != null || it.colorHex != null || it.lengthMeters != null }
    }

    private fun splitPerExtruder(raw: String?): List<String?> =
        raw?.split(';')?.map { unquote(it) } ?: emptyList()

    private fun unquote(raw: String): String? = raw.trim().trim('"').trim().takeIf { it.isNotEmpty() }
}
