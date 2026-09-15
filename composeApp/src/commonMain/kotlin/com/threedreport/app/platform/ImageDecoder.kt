package com.threedreport.app.platform

import androidx.compose.ui.graphics.ImageBitmap

/** Decodifica bytes de imagem (PNG/JPEG/etc.) para exibição num `Image` do Compose. */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap
