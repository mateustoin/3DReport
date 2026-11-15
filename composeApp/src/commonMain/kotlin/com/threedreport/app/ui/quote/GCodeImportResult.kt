package com.threedreport.app.ui.quote

/**
 * O que a importação de um G-code disse sobre a impressão: dado puro, sem frase. A tela escolhe o texto (ver
 * `asText` em `GCodeImportText.kt`), e o teste compara o tipo e os argumentos.
 */
sealed interface GCodeImportResult {

    /** O arquivo foi lido. */
    data class Read(
        /** O que o arquivo preencheu, na ordem em que a mensagem lista. */
        val filled: List<GCodeFilled>,
        /** Nem comprimento nem tempo foram achados e não há linhas mantidas que os expliquem. */
        val nothingFound: Boolean,
        val printer: GCodePrinterNote?,
        val filaments: GCodeFilamentsNote?,
        /** Havia miniatura no arquivo, mas a foto que já estava escolhida foi mantida. */
        val photoKept: Boolean,
    ) : GCodeImportResult

    /** Um arquivo que parece G-code, mas o app ainda não lê. */
    data class Unsupported(val kind: UnsupportedGCode, val fileName: String) : GCodeImportResult

    /** O arquivo não pôde ser interpretado. */
    data object Unreadable : GCodeImportResult

    /** O sistema não conseguiu abrir o arquivo; [message] já vem escrita pela plataforma. */
    data class PickFailed(val message: String) : GCodeImportResult
}

enum class GCodeFilled { FILAMENT_PER_EXTRUDER, FILAMENT_LENGTH, PRINT_TIME, PHOTO, SETTINGS }

enum class UnsupportedGCode { BINARY_GCODE, SLICED_3MF, PROJECT_3MF, NOT_GCODE }

/** A impressora do arquivo contra as cadastradas. */
sealed interface GCodePrinterNote {
    data class Found(val name: String) : GCodePrinterNote

    data class Similar(val gcodeName: String, val printerName: String) : GCodePrinterNote

    data class NotRegistered(val gcodeName: String) : GCodePrinterNote
}

/** O filamento de um extrusor contra os cadastrados. */
sealed interface GCodeFilamentNote {
    data class Found(val name: String, val colorLabel: String?) : GCodeFilamentNote

    data class Ambiguous(val type: String, val count: Int) : GCodeFilamentNote

    data class NotRegistered(val type: String, val vendor: String?) : GCodeFilamentNote

    data object Unknown : GCodeFilamentNote
}

/** O que aconteceu com as linhas de filamento. */
sealed interface GCodeFilamentsNote {
    /** Uma linha por extrusor. */
    data class PerExtruder(val extruders: List<GCodeFilamentNote>) : GCodeFilamentsNote

    /** Vários extrusores sem consumo de cada um: as linhas da pessoa ficaram como estavam. */
    data class KeptRows(val extruderCount: Int, val totalMeters: String?) : GCodeFilamentsNote

    /** Vários extrusores sem consumo de cada um: o total ficou numa linha só. */
    data class MergedRows(val extruderCount: Int) : GCodeFilamentsNote

    /** Um extrusor só. */
    data class Single(val note: GCodeFilamentNote) : GCodeFilamentsNote
}
