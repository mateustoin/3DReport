package com.threedreport.app.platform

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A arte em si é julgada a olho; o que dá pra travar em teste é o formato (quadrado 1080, PNG
 * válido) e o fato de nada quebrar quando falta foto ou quando o nome é longo demais pra caber.
 */
class QuoteImageExporterTest {

    @Test
    fun generatesASquarePngEvenWithoutAPhoto() {
        val bytes = renderQuoteImage("Peça sem foto", "R$ 42,00", null, null, "Minha marca")

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(1080, image.width)
        assertEquals(1080, image.height)
    }

    @Test
    fun aVeryLongNameDoesNotBreakTheRendering() {
        val bytes = renderQuoteImage("Nome absurdamente comprido ".repeat(20), "R$ 1,00", null, null, null)

        assertTrue(ImageIO.read(ByteArrayInputStream(bytes)) != null)
    }

    @Test
    fun anUnreadablePhotoFallsBackToTheTextOnlyArt() {
        val bytes = renderQuoteImage("Peça", "R$ 10,00", null, byteArrayOf(1, 2, 3), null)

        assertEquals(1080, ImageIO.read(ByteArrayInputStream(bytes)).width)
    }
}
