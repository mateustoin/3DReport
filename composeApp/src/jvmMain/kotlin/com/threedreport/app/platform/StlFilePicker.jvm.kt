package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private val STL_EXTENSIONS = setOf("stl")

actual fun pickStlFile(): PickedFile? {
    val dialog = FileDialog(null as Frame?, "Escolher arquivo STL", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in STL_EXTENSIONS }
    dialog.isVisible = true

    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val file = File(directory, fileName)
    return PickedFile(fileName = file.name, bytes = file.readBytes())
}
