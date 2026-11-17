package com.threedreport.app.platform

import kotlin.math.pow

/**
 * Brilho médio de uma lista de pixels ARGB (`0xAARRGGBB`), em luminância relativa (WCAG): 0 é preto, 1 é branco, com o
 * verde pesando mais que o vermelho e o azul, como o olho vê. Cada pixel pesa pela opacidade dele: o fundo
 * transparente de uma foto recortada não conta, e a borda suavizada conta pela metade. Foto sem transparência conta
 * inteira. `null` quando nada é visível (lista vazia ou toda transparente).
 *
 * É o que decide o fundo automático da imagem pro cliente ([com.threedreport.core.model.ImageBackground.resolve]).
 */
fun averageLuminance(argbPixels: IntArray): Double? {
    var weighted = 0.0
    var weight = 0.0
    for (pixel in argbPixels) {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha == 0) continue
        val luminance = 0.2126 * LINEAR[(pixel shr 16) and 0xFF] + 0.7152 * LINEAR[(pixel shr 8) and 0xFF] + 0.0722 * LINEAR[pixel and 0xFF]
        weighted += luminance * alpha
        weight += alpha
    }
    return if (weight == 0.0) null else weighted / weight
}

/**
 * Brilho médio da imagem [bytes] ([averageLuminance] sobre os pixels dela), ou `null` se não abrir ou for toda
 * transparente. Imagem grande é lida por amostragem, que basta pra uma média.
 */
expect fun averageImageLuminance(bytes: ByteArray): Double?

/** Cada valor de canal sRGB (0 a 255) em luz linear, a conta da luminância relativa. */
private val LINEAR = DoubleArray(256) { value ->
    val channel = value / 255.0
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
