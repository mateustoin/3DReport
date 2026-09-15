package com.threedreport.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual fun saveBytesToFile(bytes: ByteArray, suggestedFileName: String, initialDirectory: String?): Boolean {
    val dialog = FileDialog(null as Frame?, "Salvar como", FileDialog.SAVE)
    dialog.file = suggestedFileName
    initialDirectory?.let { dialog.directory = it }
    dialog.isVisible = true

    val directory = dialog.directory ?: return false
    val fileName = dialog.file ?: return false
    File(directory, fileName).writeBytes(bytes)
    return true
}

actual fun defaultDocumentsDirectory(): String? {
    val home = System.getProperty("user.home") ?: return null
    return listOf("Documents", "Documentos")
        .map { File(home, it) }
        .firstOrNull { it.isDirectory }
        ?.path
        ?: home
}
