package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.filaments.displayLabel
import com.threedreport.app.ui.format.toDurationInputText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.slicer.CatalogMatcher
import com.threedreport.core.slicer.ExtruderMatch
import com.threedreport.core.slicer.FilamentMatch
import com.threedreport.core.slicer.GCodeMetadata
import com.threedreport.core.slicer.PrinterMatch
import kotlin.math.round

/**
 * Monta uma impressão a partir dos metadados de um G-code (decisão 89): comprimento, tempo e
 * configurações de impressão, e escolhe a impressora e o filamento que o fatiador gravou **quando batem
 * com os cadastrados** ([CatalogMatcher]); o que só parece ou não está cadastrado vira explicação na
 * mensagem, nunca escolha. Função pura, fora do ViewModel (decisão 108): cada impressão do pedido
 * importa o seu G-code (decisão 114).
 */
object GCodeImporter {

    /**
     * @param inStock filamentos com alguma cor em estoque (os que o casamento pode escolher).
     * @param photoApplied se a miniatura do arquivo vai virar a foto (a mensagem diz).
     * @param newRowId gera o id de cada linha de filamento nova.
     */
    fun apply(
        current: PrintInput,
        metadata: GCodeMetadata,
        allFilaments: List<Filament>,
        printers: List<PrinterProfile>,
        photoApplied: Boolean,
        newRowId: () -> Int,
    ): PrintInput {
        val inStock = allFilaments.filter { it.hasStockAvailable }
        val preferredFilamentIds = current.filaments.mapNotNull { it.filamentId }.ifEmpty { listOfNotNull(inStock.firstOrNull()?.id) }
        val printerMatch = CatalogMatcher.matchPrinter(metadata, printers)
        val extruders = CatalogMatcher.matchFilaments(metadata, allFilaments, preferredFilamentIds)
        val chosenPrinter = (printerMatch as? PrinterMatch.Found)?.printer
        // Peça multicolor: uma linha por extrusor, cada uma com o próprio consumo (decisão 105). O que
        // não casou fica sem filamento, pra escolher: cobrar a linha pelo filamento errado mudaria o
        // preço sem ninguém ver.
        val perExtruder = extruders.size > 1 && extruders.all { it.lengthMeters != null }
        val rows = if (perExtruder) {
            extruders.map { extruder ->
                val found = extruder.match as? FilamentMatch.Found
                FilamentInput(
                    filamentId = found?.filament?.id,
                    colorId = found?.color?.id,
                    lengthText = formatLength(extruder.lengthMeters!!),
                    id = newRowId(),
                )
            }
        } else if (extruders.size > 1 && current.filaments.size > 1) {
            // Vários extrusores sem o consumo de cada um, e a pessoa já tinha separado as linhas à mão:
            // juntar tudo numa linha cobraria tudo pelo primeiro filamento. As linhas ficam como estão.
            current.filaments
        } else {
            val first = current.filaments.first()
            val single = extruders.singleOrNull()?.match as? FilamentMatch.Found
            // Vários slots que casam com o mesmo filamento (ex.: quatro PLA no AMS) ainda dão um
            // filamento só, sem cor, que não dá pra saber.
            val common = extruders.map { (it.match as? FilamentMatch.Found)?.filament }.distinct().singleOrNull()
            val filament = single?.filament ?: common
            listOf(
                first.copy(
                    filamentId = filament?.id ?: first.filamentId,
                    colorId = if (filament != null) single?.color?.id else first.colorId,
                    lengthText = metadata.filamentLengthMeters?.let(::formatLength) ?: first.lengthText,
                    weightText = null,
                    missingFilamentName = if (filament != null) null else first.missingFilamentName,
                ),
            )
        }
        val keptManualRows = rows === current.filaments
        // As configurações ficam na impressão (decisão 106): o que o arquivo não diz continua como estava.
        val settings = current.settings.copy(
            layerHeightMm = metadata.layerHeightMm ?: current.settings.layerHeightMm,
            infillPercentage = metadata.infillPercentage ?: current.settings.infillPercentage,
            infillPattern = metadata.infillPattern ?: current.settings.infillPattern,
            supportsEnabled = metadata.supportsEnabled ?: current.settings.supportsEnabled,
        )
        val thumbnail = metadata.thumbnail?.let { PickedFile("miniatura_do_gcode.${it.fileExtension}", it.bytes) }

        return current.copy(
            printerId = chosenPrinter?.id ?: current.printerId,
            missingPrinterName = if (chosenPrinter != null) null else current.missingPrinterName,
            filaments = rows,
            printTimeText = metadata.printTimeMinutes?.let(::formatTime) ?: current.printTimeText,
            beforeGCode = current.beforeGCode ?: current.copy(gcodeImportMessage = null),
            gcodeImportMessage = message(metadata, photoApplied, printerMatch, extruders, perExtruder, keptManualRows),
            settings = settings,
            // A miniatura é do G-code desta impressão: um arquivo sem miniatura não fica com a do anterior.
            thumbnail = thumbnail,
        )
    }

    /**
     * Arquivos que parecem G-code mas o app ainda não lê, com o caminho pra resolver. `null`
     * quando dá pra tentar ler.
     */
    fun unsupportedMessage(fileName: String): String? {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".bgcode") ->
                "G-code binário (.bgcode) ainda não é lido. No PrusaSlicer, desligue \"G-code binário\" nas " +
                    "configurações da impressora e exporte de novo."
            name.endsWith(".gcode.3mf") ->
                "Esse é o arquivo fatiado do Bambu Studio (.gcode.3mf), que o app ainda não lê. No Bambu Studio, " +
                    "use \"Exportar G-code\" (em vez de \"Exportar arquivo fatiado da placa\") e arraste o .gcode."
            name.endsWith(".3mf") ->
                "Esse é um arquivo de projeto (.3mf), não um G-code. No fatiador, use \"Exportar G-code\" e " +
                    "arraste o arquivo .gcode."
            GCODE_EXTENSIONS.none { name.endsWith(it) } ->
                "\"$fileName\" não é um G-code. Use o arquivo .gcode exportado pelo fatiador."
            else -> null
        }
    }

    /** Metros com até 2 casas, no formato do campo ("27,53"). */
    fun formatLength(meters: Double): String = (round(meters * 100) / 100).toInputText()

    /** Tempo arredondado pro minuto, no formato do campo ("3h20"): os segundos do fatiador não mudam o preço. */
    fun formatTime(minutes: Double): String = round(minutes).toDurationInputText()

    private fun message(
        metadata: GCodeMetadata,
        photoApplied: Boolean,
        printerMatch: PrinterMatch,
        extruders: List<ExtruderMatch>,
        perExtruder: Boolean,
        keptManualRows: Boolean,
    ): String {
        val filled = buildList {
            when {
                perExtruder -> add("consumo de cada filamento")
                keptManualRows -> Unit
                metadata.filamentLengthMeters != null -> add("comprimento de filamento")
            }
            if (metadata.printTimeMinutes != null) add("tempo de impressão")
            if (photoApplied) add("foto do modelo")
            if (metadata.layerHeightMm != null || metadata.infillPercentage != null ||
                metadata.infillPattern != null || metadata.supportsEnabled != null
            ) {
                add("configurações de impressão")
            }
        }
        val sentences = buildList {
            when {
                filled.isNotEmpty() -> add("Preenchido a partir do G-code: ${filled.joinToString(", ")}.")
                // As linhas mantidas já têm os comprimentos; a frase delas explica o resto.
                !keptManualRows -> add("Não encontrei comprimento nem tempo nesse G-code — preencha manualmente.")
            }
            printerSentence(printerMatch)?.let(::add)
            when {
                perExtruder -> add(extrudersSentence(extruders))
                keptManualRows -> add(
                    "O G-code usa ${extruders.size} filamentos, mas não informa o consumo de cada um" +
                        (metadata.filamentLengthMeters?.let { " (${formatLength(it)} m no total)" } ?: "") +
                        ": mantive as suas linhas de filamento como estavam.",
                )
                extruders.size > 1 -> add(
                    "O G-code usa ${extruders.size} filamentos, mas não informa o consumo de cada um: o total ficou numa " +
                        "linha só. Separe em \"+ Adicionar filamento\" pra cobrar cada um pelo seu preço.",
                )
                else -> extruders.singleOrNull()?.let { filamentSentence(it.match) }?.let(::add)
            }
            if (metadata.thumbnail != null && !photoApplied) add("Havia uma foto nesse G-code, mas mantive a que você já tinha escolhido.")
        }
        return sentences.joinToString(" ")
    }

    private fun printerSentence(match: PrinterMatch): String? = when (match) {
        is PrinterMatch.Found -> "Impressora: ${match.printer.name}."
        is PrinterMatch.Similar ->
            "O G-code é de uma \"${match.gcodeName}\"; a mais parecida cadastrada é \"${match.printer.name}\" — escolha na lista se for ela."
        is PrinterMatch.NotRegistered ->
            "O G-code é de uma \"${match.gcodeName}\", que não está cadastrada (Impressoras → Escolher da lista)."
        PrinterMatch.Unknown -> null
    }

    private fun filamentSentence(match: FilamentMatch): String? = when (match) {
        is FilamentMatch.Found -> "Filamento: ${match.filament.name}" + (match.color?.let { ", cor ${it.displayLabel()}" } ?: "") + "."
        is FilamentMatch.Ambiguous -> "Há ${match.count} filamentos ${match.type} em estoque — escolha qual usou."
        is FilamentMatch.NotRegistered ->
            "O G-code usa ${match.type}" + (match.vendor?.let { " da $it" } ?: "") + ", que não está cadastrado em estoque."
        FilamentMatch.Unknown -> null
    }

    /** "Um filamento por extrusor: 1) PLA, cor Verde; 2) PETG (não cadastrado em estoque, escolha)." */
    private fun extrudersSentence(extruders: List<ExtruderMatch>): String =
        "Um filamento por extrusor: " + extruders.mapIndexed { index, extruder ->
            "${index + 1}) " + when (val match = extruder.match) {
                is FilamentMatch.Found -> match.filament.name + (match.color?.let { ", cor ${it.displayLabel()}" } ?: "")
                is FilamentMatch.Ambiguous -> "${match.type} (há ${match.count} em estoque, escolha qual)"
                is FilamentMatch.NotRegistered -> match.type + (match.vendor?.let { " da $it" } ?: "") + " (não cadastrado em estoque, escolha)"
                FilamentMatch.Unknown -> "material não informado (escolha)"
            }
        }.joinToString("; ") + "."

    /** Extensões de G-code em texto que os fatiadores exportam. */
    private val GCODE_EXTENSIONS = listOf(".gcode", ".gco", ".g")
}
