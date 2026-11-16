package com.threedreport.app.platform

import com.threedreport.core.model.ImageBackground
import com.threedreport.core.model.ImageTone
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Brilho médio da peça na foto, pro fundo automático da imagem (Leva 10B, parte 7). Só os pixels que não são
 * transparentes contam; foto sem transparência conta inteira.
 */
class ImageBrightnessTest {

    @Test
    fun theLuminanceOfAPixelIsRelativeLuminance() {
        assertEquals(0.0, averageLuminance(intArrayOf(argb(255, 0, 0, 0)))!!, 1e-9)
        assertEquals(1.0, averageLuminance(intArrayOf(argb(255, 255, 255, 255)))!!, 1e-9)
        assertEquals(0.2159, averageLuminance(intArrayOf(argb(255, 128, 128, 128)))!!, 1e-4)
        // Verde pesa mais que vermelho, que pesa mais que azul.
        assertEquals(0.7152, averageLuminance(intArrayOf(argb(255, 0, 255, 0)))!!, 1e-4)
    }

    @Test
    fun aBlackPieceOnATransparentBackgroundIsDark() {
        val luminance = assertNotNull(averageImageLuminance(png(piece = argb(255, 0, 0, 0), background = argb(0, 255, 255, 255))))

        assertEquals(0.0, luminance, 1e-9)
        assertEquals(ImageTone.LIGHT, ImageBackground.AUTO.resolve(luminance))
    }

    @Test
    fun aWhitePieceOnATransparentBackgroundIsBright() {
        val luminance = assertNotNull(averageImageLuminance(png(piece = argb(255, 255, 255, 255), background = argb(0, 0, 0, 0))))

        assertEquals(1.0, luminance, 1e-9)
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(luminance))
    }

    @Test
    fun aGreyPieceCountsOnlyItsOwnPixels() {
        val luminance = assertNotNull(averageImageLuminance(png(piece = argb(255, 128, 128, 128), background = argb(0, 0, 0, 0))))

        assertEquals(0.2159, luminance, 1e-3)
        assertEquals(ImageTone.DARK, ImageBackground.AUTO.resolve(luminance))
    }

    @Test
    fun aFullyTransparentImageHasNothingToMeasure() {
        assertNull(averageImageLuminance(png(piece = argb(0, 0, 0, 0), background = argb(0, 0, 0, 0))))
        assertNull(averageLuminance(IntArray(0)))
    }

    @Test
    fun anOpaquePhotoCountsAsAWhole() {
        // Foto enviada pela pessoa (JPEG, sem transparência): peça preta no meio de um fundo branco, metade de cada.
        val photo = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until 10) for (y in 0 until 10) photo.setRGB(x, y, if (x < 5) 0x000000 else 0xFFFFFF)
        val bytes = ByteArrayOutputStream().also { ImageIO.write(photo, "jpg", it) }.toByteArray()

        assertEquals(0.5, assertNotNull(averageImageLuminance(bytes)), 0.05)
    }

    @Test
    fun whatIsNotAnImageHasNothingToMeasure() {
        assertNull(averageImageLuminance(byteArrayOf(1, 2, 3)))
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    /** Imagem 20 × 20 com a peça num quadrado 10 × 10 no meio e o resto em [background]. */
    private fun png(piece: Int, background: Int): ByteArray {
        val image = BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 20) for (y in 0 until 20) image.setRGB(x, y, if (x in 5 until 15 && y in 5 until 15) piece else background)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }
}
