package com.threedreport.app.platform

import androidx.compose.ui.graphics.toAwtImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Decodifica uma imagem guardada pelo Skia (lê WebP, que o `ImageIO` do Java não lê e devolve `null` em
 * silêncio, decisão 65) e converte pro formato do AWT. `null` se os bytes não forem uma imagem legível.
 */
internal fun decodeToBufferedImage(bytes: ByteArray): BufferedImage? =
    runCatching { decodeImageBitmap(bytes).toAwtImage() }.getOrNull()

/**
 * A mesma imagem com o lado maior em no máximo [maxPixels], sem distorcer. Menor que isso, volta como
 * está. [keepAlpha] mantém a transparência (logo); sem ela, o fundo transparente vira branco, que é o
 * que um JPEG faria de qualquer jeito.
 */
internal fun BufferedImage.scaledToFit(maxPixels: Int, keepAlpha: Boolean = true): BufferedImage {
    val largestSide = maxOf(width, height)
    val alreadyFits = largestSide <= maxPixels
    val targetType = if (keepAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    if (alreadyFits && type == targetType) return this
    val scale = if (alreadyFits) 1.0 else maxPixels.toDouble() / largestSide
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return BufferedImage(targetWidth, targetHeight, targetType).also { scaled ->
        val graphics = scaled.createGraphics()
        if (!keepAlpha) {
            graphics.color = java.awt.Color.WHITE
            graphics.fillRect(0, 0, targetWidth, targetHeight)
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
        graphics.dispose()
    }
}

/** Lado da grade de amostragem: até 64 × 64 pontos decidem a transparência, sem varrer a foto pixel a pixel. */
private const val TRANSPARENCY_GRID = 64

/** Alfa abaixo disto (de 255) conta como transparente; o que fica acima é opaco pra todo efeito visível. */
private const val TRANSPARENT_ALPHA_BELOW = 250

/** Fração mínima dos pontos amostrados transparentes pra a imagem ser "com transparência" (2%). */
private const val TRANSPARENT_SHARE = 0.02

/**
 * Se a imagem tem transparência de verdade (miniatura do G-code, captura do 3D, logo recortada), decidido por amostragem
 * numa grade fixa e por um limiar: uma foto opaca com cantos arredondados, ou com um pixel translúcido, conta como opaca.
 * É a regra única do PDF e da imagem pra escolher entre "preencher a área" e "mostrar inteira".
 */
internal fun BufferedImage.hasTransparency(): Boolean {
    if (!colorModel.hasAlpha()) return false
    val columns = minOf(width, TRANSPARENCY_GRID)
    val rows = minOf(height, TRANSPARENCY_GRID)
    var transparent = 0
    for (row in 0 until rows) {
        val y = ((row + 0.5) * height / rows).toInt()
        for (column in 0 until columns) {
            val x = ((column + 0.5) * width / columns).toInt()
            if ((getRGB(x, y) ushr 24) < TRANSPARENT_ALPHA_BELOW) transparent++
        }
    }
    return transparent >= columns * rows * TRANSPARENT_SHARE
}

internal fun BufferedImage.encodePng(): ByteArray =
    ByteArrayOutputStream().also { ImageIO.write(this, "png", it) }.toByteArray()
