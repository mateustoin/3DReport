package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private val GCODE_EXTENSIONS = setOf("gcode", "gco", "g")

actual fun pickGCodeFile(): PickedFile? {
    val dialog = FileDialog(null as Frame?, "Escolher arquivo G-code", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in GCODE_EXTENSIONS }
    // No Windows, o FileDialog nativo ignora `filenameFilter` (peer não chama o callback Java) —
    // só filtra de fato quando o padrão vem em `file` com wildcard (bug antigo do AWT, não tem
    // fix por parte da JDK). As duas linhas juntas cobrem Windows e as demais plataformas.
    dialog.file = GCODE_EXTENSIONS.joinToString(";") { "*.$it" }
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    return PickedFile(fileName = file.name, bytes = file.readBytes())
}
