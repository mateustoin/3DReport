package com.threedreport.app.platform

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val IMAGE_EXTENSIONS = arrayOf("png", "jpg", "jpeg", "gif", "bmp", "webp")

actual fun pickImageFile(): PickedFile? {
    // JFileChooser (Swing), não java.awt.FileDialog: o filtro de tipo do FileDialog nativo não
    // funciona de forma confiável no Windows (nem via FilenameFilter, nem via wildcard em `file`
    // — cai na caixa de nome do arquivo em vez de filtrar a lista). JFileChooser resolve isso com
    // um combo "Files of type" de verdade, em qualquer SO.
    val chooser = JFileChooser().apply {
        dialogTitle = "Escolher foto"
        fileFilter = FileNameExtensionFilter("Imagens (*.png, *.jpg, *.jpeg, *.gif, *.bmp, *.webp)", *IMAGE_EXTENSIONS)
        isAcceptAllFileFilterUsed = false
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null

    val file = chooser.selectedFile ?: return null
    return PickedFile(fileName = file.name, bytes = file.readBytes())
}
