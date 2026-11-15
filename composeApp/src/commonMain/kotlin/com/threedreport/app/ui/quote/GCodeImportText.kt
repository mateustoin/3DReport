package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_import_extruder_ambiguous
import _3dreport.composeapp.generated.resources.quote_import_extruder_item
import _3dreport.composeapp.generated.resources.quote_import_extruder_not_registered
import _3dreport.composeapp.generated.resources.quote_import_extruder_not_registered_vendor
import _3dreport.composeapp.generated.resources.quote_import_extruder_unknown
import _3dreport.composeapp.generated.resources.quote_import_filament_ambiguous
import _3dreport.composeapp.generated.resources.quote_import_filament_color
import _3dreport.composeapp.generated.resources.quote_import_filament_found
import _3dreport.composeapp.generated.resources.quote_import_filament_found_color
import _3dreport.composeapp.generated.resources.quote_import_filament_not_registered
import _3dreport.composeapp.generated.resources.quote_import_filament_not_registered_vendor
import _3dreport.composeapp.generated.resources.quote_import_filled
import _3dreport.composeapp.generated.resources.quote_import_filled_length
import _3dreport.composeapp.generated.resources.quote_import_filled_per_extruder
import _3dreport.composeapp.generated.resources.quote_import_filled_photo
import _3dreport.composeapp.generated.resources.quote_import_filled_settings
import _3dreport.composeapp.generated.resources.quote_import_filled_time
import _3dreport.composeapp.generated.resources.quote_import_kept_rows
import _3dreport.composeapp.generated.resources.quote_import_kept_rows_total
import _3dreport.composeapp.generated.resources.quote_import_merged_rows
import _3dreport.composeapp.generated.resources.quote_import_nothing_found
import _3dreport.composeapp.generated.resources.quote_import_per_extruder
import _3dreport.composeapp.generated.resources.quote_import_photo_kept
import _3dreport.composeapp.generated.resources.quote_import_printer_found
import _3dreport.composeapp.generated.resources.quote_import_printer_not_registered
import _3dreport.composeapp.generated.resources.quote_import_printer_similar
import _3dreport.composeapp.generated.resources.quote_import_unreadable
import _3dreport.composeapp.generated.resources.quote_import_unsupported_3mf
import _3dreport.composeapp.generated.resources.quote_import_unsupported_bgcode
import _3dreport.composeapp.generated.resources.quote_import_unsupported_other
import _3dreport.composeapp.generated.resources.quote_import_unsupported_sliced_3mf
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** O texto que a tela mostra sobre a importação de G-code: a escolha da frase é daqui, o conteúdo é do [GCodeImportResult]. */
@Composable
fun GCodeImportResult.asText(): String = when (this) {
    is GCodeImportResult.PickFailed -> message

    GCodeImportResult.Unreadable -> stringResource(Res.string.quote_import_unreadable)

    is GCodeImportResult.Unsupported -> when (kind) {
        UnsupportedGCode.BINARY_GCODE -> stringResource(Res.string.quote_import_unsupported_bgcode)
        UnsupportedGCode.SLICED_3MF -> stringResource(Res.string.quote_import_unsupported_sliced_3mf)
        UnsupportedGCode.PROJECT_3MF -> stringResource(Res.string.quote_import_unsupported_3mf)
        UnsupportedGCode.NOT_GCODE -> stringResource(Res.string.quote_import_unsupported_other, fileName)
    }

    is GCodeImportResult.Read -> readText()
}

@Composable
private fun GCodeImportResult.Read.readText(): String {
    val sentences = buildList {
        when {
            filled.isNotEmpty() -> add(stringResource(Res.string.quote_import_filled, filled.map { it.asText() }.joinToString(", ")))
            nothingFound -> add(stringResource(Res.string.quote_import_nothing_found))
        }
        printer?.let { add(it.asText()) }
        filaments?.let { add(it.asText()) }
        if (photoKept) add(stringResource(Res.string.quote_import_photo_kept))
    }
    return sentences.joinToString(" ")
}

@Composable
private fun GCodeFilled.asText(): String = stringResource(
    when (this) {
        GCodeFilled.FILAMENT_PER_EXTRUDER -> Res.string.quote_import_filled_per_extruder
        GCodeFilled.FILAMENT_LENGTH -> Res.string.quote_import_filled_length
        GCodeFilled.PRINT_TIME -> Res.string.quote_import_filled_time
        GCodeFilled.PHOTO -> Res.string.quote_import_filled_photo
        GCodeFilled.SETTINGS -> Res.string.quote_import_filled_settings
    },
)

@Composable
private fun GCodePrinterNote.asText(): String = when (this) {
    is GCodePrinterNote.Found -> stringResource(Res.string.quote_import_printer_found, name)
    is GCodePrinterNote.Similar -> stringResource(Res.string.quote_import_printer_similar, gcodeName, printerName)
    is GCodePrinterNote.NotRegistered -> stringResource(Res.string.quote_import_printer_not_registered, gcodeName)
}

@Composable
private fun GCodeFilamentsNote.asText(): String = when (this) {
    is GCodeFilamentsNote.PerExtruder ->
        stringResource(
            Res.string.quote_import_per_extruder,
            extruders.mapIndexed { index, note -> stringResource(Res.string.quote_import_extruder_item, index + 1, note.extruderText()) }
                .joinToString("; "),
        )

    is GCodeFilamentsNote.KeptRows ->
        if (totalMeters == null) {
            pluralStringResource(Res.plurals.quote_import_kept_rows, extruderCount, extruderCount)
        } else {
            pluralStringResource(Res.plurals.quote_import_kept_rows_total, extruderCount, extruderCount, totalMeters)
        }

    is GCodeFilamentsNote.MergedRows -> pluralStringResource(Res.plurals.quote_import_merged_rows, extruderCount, extruderCount)

    is GCodeFilamentsNote.Single -> note.singleText()
}

/** "PLA, cor Verde" ou só o nome, quando o filamento não tem cor. */
@Composable
private fun GCodeFilamentNote.Found.label(): String =
    if (colorLabel == null) name else stringResource(Res.string.quote_import_filament_color, name, colorLabel)

@Composable
private fun GCodeFilamentNote.extruderText(): String = when (this) {
    is GCodeFilamentNote.Found -> label()

    is GCodeFilamentNote.Ambiguous -> stringResource(Res.string.quote_import_extruder_ambiguous, type, count)

    is GCodeFilamentNote.NotRegistered ->
        if (vendor == null) {
            stringResource(Res.string.quote_import_extruder_not_registered, type)
        } else {
            stringResource(Res.string.quote_import_extruder_not_registered_vendor, type, vendor)
        }

    GCodeFilamentNote.Unknown -> stringResource(Res.string.quote_import_extruder_unknown)
}

@Composable
private fun GCodeFilamentNote.singleText(): String = when (this) {
    is GCodeFilamentNote.Found ->
        if (colorLabel == null) {
            stringResource(Res.string.quote_import_filament_found, name)
        } else {
            stringResource(Res.string.quote_import_filament_found_color, name, colorLabel)
        }

    is GCodeFilamentNote.Ambiguous -> pluralStringResource(Res.plurals.quote_import_filament_ambiguous, count, count, type)

    is GCodeFilamentNote.NotRegistered ->
        if (vendor == null) {
            stringResource(Res.string.quote_import_filament_not_registered, type)
        } else {
            stringResource(Res.string.quote_import_filament_not_registered_vendor, type, vendor)
        }

    GCodeFilamentNote.Unknown -> ""
}
