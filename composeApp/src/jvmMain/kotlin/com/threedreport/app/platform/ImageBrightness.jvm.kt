package com.threedreport.app.platform

/** Lado da grade de amostragem: até 256 × 256 pixels entram na média, o que basta e não pesa numa foto de 12 MP. */
private const val SAMPLE_GRID = 256

actual fun averageImageLuminance(bytes: ByteArray): Double? {
    val image = decodeToBufferedImage(bytes) ?: return null
    val stepX = maxOf(1, image.width / SAMPLE_GRID)
    val stepY = maxOf(1, image.height / SAMPLE_GRID)
    val pixels = buildList {
        for (y in 0 until image.height step stepY) {
            for (x in 0 until image.width step stepX) add(image.getRGB(x, y))
        }
    }
    return averageLuminance(pixels.toIntArray())
}
