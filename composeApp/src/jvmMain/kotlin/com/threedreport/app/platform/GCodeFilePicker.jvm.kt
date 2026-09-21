package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private val GCODE_EXTENSIONS = setOf("gcode", "gco", "g")

actual fun pickGCodeFile(): PickedFile? {
    val dialog = FileDialog(null as Frame?, "Escolher arquivo G-code", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in GCODE_EXTENSIONS }
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    return PickedFile(fileName = file.name, bytes = file.readBytes())
}
