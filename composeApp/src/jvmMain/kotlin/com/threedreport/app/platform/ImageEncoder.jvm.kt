package com.threedreport.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun encodeImageBitmapToPng(bitmap: ImageBitmap): ByteArray {
    val skiaImage = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    return skiaImage.encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("Falha ao codificar a imagem capturada do visualizador 3D como PNG.")
}
