package com.threedreport.app.platform

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Tem transparência de verdade" é decidido por amostragem e limiar, não por um pixel translúcido qualquer. */
class ImageTransparencyTest {

    private fun image(width: Int, height: Int, draw: (java.awt.Graphics2D) -> Unit): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            val graphics = image.createGraphics()
            draw(graphics)
            graphics.dispose()
        }

    @Test
    fun anOpaquePhotoWithRoundedCornersCountsAsOpaque() {
        // Foto do celular em PNG com os cantos recortados: só uma fatia mínima de pixels é transparente.
        val photo = image(400, 300) { graphics ->
            graphics.color = Color(0x80, 0x60, 0x40)
            graphics.fillRoundRect(0, 0, 400, 300, 24, 24)
        }

        assertFalse(photo.hasTransparency())
    }

    @Test
    fun aGCodeThumbnailWithATransparentBackgroundCountsAsTransparent() {
        val thumbnail = image(300, 300) { graphics ->
            graphics.color = Color(0x40, 0x40, 0x40)
            graphics.fillRect(100, 100, 100, 100)
        }

        assertTrue(thumbnail.hasTransparency())
    }

    @Test
    fun aSingleTranslucentPixelIsNotEnough() {
        val photo = image(200, 200) { graphics ->
            graphics.color = Color.GRAY
            graphics.fillRect(0, 0, 200, 200)
        }
        photo.setRGB(100, 100, 0x00000000)

        assertFalse(photo.hasTransparency())
    }

    @Test
    fun aPhotoWithoutAlphaChannelIsOpaque() {
        val photo = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)

        assertFalse(photo.hasTransparency())
    }

    @Test
    fun almostOpaquePixelsAboveTheAlphaCutDoNotCount() {
        val photo = image(100, 100) { graphics ->
            graphics.color = Color(10, 20, 30, 252)
            graphics.fillRect(0, 0, 100, 100)
        }

        assertFalse(photo.hasTransparency())
    }
}
