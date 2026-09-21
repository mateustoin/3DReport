package com.threedreport.app.platform

import androidx.compose.ui.graphics.ImageBitmap

/** Codifica [bitmap] como PNG, pra salvar/anexar como foto do orçamento. */
expect fun encodeImageBitmapToPng(bitmap: ImageBitmap): ByteArray
