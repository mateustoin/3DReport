package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual fun saveBytesToFile(bytes: ByteArray, suggestedFileName: String): Boolean {
    val dialog = FileDialog(null as Frame?, "Salvar como", FileDialog.SAVE)
    dialog.file = suggestedFileName
    dialog.isVisible = true

    val directory = dialog.directory ?: return false
    val fileName = dialog.file ?: return false
    File(directory, fileName).writeBytes(bytes)
    return true
}
