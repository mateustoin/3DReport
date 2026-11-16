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
 * resultado, nunca escolha. Função pura, fora do ViewModel (decisão 108): cada impressão do pedido
 * importa o seu G-code (decisão 114).
 */
object GCodeImporter {

    /**
     * @param inStock filamentos com alguma cor em estoque (os que o casamento pode escolher).
     * @param photoApplied se a miniatura do arquivo vai virar a foto (o resultado diz).
     * @param newRowId gera o id de cada linha de filamento nova.
     */
    fun apply(
        current: PrintInput,
        metadata: GCodeMetadata,
        allFilaments: List<Filament>,
        printers: List<PrinterProfile>,
        photoApplied: Boolean,
        newRowId: () -> Int,
        decimalSeparator: Char = ',',
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
                    lengthText = formatLength(extruder.lengthMeters!!, decimalSeparator),
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
                    lengthText = metadata.filamentLengthMeters?.let { formatLength(it, decimalSeparator) } ?: first.lengthText,
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
            beforeGCode = current.beforeGCode ?: current.copy(gcodeImport = null),
            gcodeImport = result(metadata, photoApplied, printerMatch, extruders, perExtruder, keptManualRows, decimalSeparator),
            settings = settings,
            // A miniatura é do G-code desta impressão: um arquivo sem miniatura não fica com a do anterior.
            thumbnail = thumbnail,
        )
    }

    /**
     * Arquivos que parecem G-code mas o app ainda não lê, com o caminho pra resolver. `null`
     * quando dá pra tentar ler.
     */
    fun unsupported(fileName: String): GCodeImportResult.Unsupported? {
        val name = fileName.lowercase()
        val kind = when {
            name.endsWith(".bgcode") -> UnsupportedGCode.BINARY_GCODE
            name.endsWith(".gcode.3mf") -> UnsupportedGCode.SLICED_3MF
            name.endsWith(".3mf") -> UnsupportedGCode.PROJECT_3MF
            GCODE_EXTENSIONS.none { name.endsWith(it) } -> UnsupportedGCode.NOT_GCODE
            else -> return null
        }
        return GCodeImportResult.Unsupported(kind, fileName)
    }

    /** Metros com até 2 casas, no formato do campo ("27,53"), com o separador decimal da moeda em uso. */
    fun formatLength(meters: Double, decimalSeparator: Char = ','): String = (round(meters * 100) / 100).toInputText(decimalSeparator)

    /** Tempo arredondado pro minuto, no formato do campo ("3h20"): os segundos do fatiador não mudam o preço. */
    fun formatTime(minutes: Double): String = round(minutes).toDurationInputText()

    private fun result(
        metadata: GCodeMetadata,
        photoApplied: Boolean,
        printerMatch: PrinterMatch,
        extruders: List<ExtruderMatch>,
        perExtruder: Boolean,
        keptManualRows: Boolean,
        decimalSeparator: Char,
    ): GCodeImportResult.Read {
        val filled = buildList {
            when {
                perExtruder -> add(GCodeFilled.FILAMENT_PER_EXTRUDER)
                keptManualRows -> Unit
                metadata.filamentLengthMeters != null -> add(GCodeFilled.FILAMENT_LENGTH)
            }
            if (metadata.printTimeMinutes != null) add(GCodeFilled.PRINT_TIME)
            if (photoApplied) add(GCodeFilled.PHOTO)
            if (metadata.layerHeightMm != null || metadata.infillPercentage != null ||
                metadata.infillPattern != null || metadata.supportsEnabled != null
            ) {
                add(GCodeFilled.SETTINGS)
            }
        }
        val filaments = when {
            perExtruder -> GCodeFilamentsNote.PerExtruder(extruders.map { note(it.match) })

            keptManualRows -> GCodeFilamentsNote.KeptRows(extruders.size, metadata.filamentLengthMeters?.let { formatLength(it, decimalSeparator) })

            extruders.size > 1 -> GCodeFilamentsNote.MergedRows(extruders.size)

            else -> extruders.singleOrNull()?.let { note(it.match) }?.takeIf { it != GCodeFilamentNote.Unknown }
                ?.let { GCodeFilamentsNote.Single(it) }
        }
        return GCodeImportResult.Read(
            filled = filled,
            // As linhas mantidas já têm os comprimentos; a nota delas explica o resto.
            nothingFound = filled.isEmpty() && !keptManualRows,
            printer = printerNote(printerMatch),
            filaments = filaments,
            photoKept = metadata.thumbnail != null && !photoApplied,
        )
    }

    private fun printerNote(match: PrinterMatch): GCodePrinterNote? = when (match) {
        is PrinterMatch.Found -> GCodePrinterNote.Found(match.printer.name)
        is PrinterMatch.Similar -> GCodePrinterNote.Similar(match.gcodeName, match.printer.name)
        is PrinterMatch.NotRegistered -> GCodePrinterNote.NotRegistered(match.gcodeName)
        PrinterMatch.Unknown -> null
    }

    private fun note(match: FilamentMatch): GCodeFilamentNote = when (match) {
        is FilamentMatch.Found -> GCodeFilamentNote.Found(match.filament.name, match.color?.displayLabel())
        is FilamentMatch.Ambiguous -> GCodeFilamentNote.Ambiguous(match.type, match.count)
        is FilamentMatch.NotRegistered -> GCodeFilamentNote.NotRegistered(match.type, match.vendor)
        FilamentMatch.Unknown -> GCodeFilamentNote.Unknown
    }

    /** Extensões de G-code em texto que os fatiadores exportam. */
    private val GCODE_EXTENSIONS = listOf(".gcode", ".gco", ".g")
}
