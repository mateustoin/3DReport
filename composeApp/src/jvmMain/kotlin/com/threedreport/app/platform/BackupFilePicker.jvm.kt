package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

actual fun pickBackupFile(): PickedFile? {
    val dialog = FileDialog(null as Frame?, "Escolher backup do 3DReport", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".zip", ignoreCase = true) }
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    return PickedFile(fileName = file.name, bytes = file.readBytes())
}
