package com.threedreport.app.platform

import com.threedreport.core.model.ImageBackground
import com.threedreport.core.model.ImageTone
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
        val bytes = renderQuoteImage(testClientTexts, "Peça sem foto", "R$ 42,00", null, null, "Minha marca")

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(1080, image.width)
        assertEquals(1080, image.height)
    }

    @Test
    fun aVeryLongNameDoesNotBreakTheRendering() {
        val bytes = renderQuoteImage(testClientTexts, "Nome absurdamente comprido ".repeat(20), "R$ 1,00", null, null, null)

        assertTrue(ImageIO.read(ByteArrayInputStream(bytes)) != null)
    }

    @Test
    fun anUnreadablePhotoFallsBackToTheTextOnlyArt() {
        val bytes = renderQuoteImage(testClientTexts, "Peça", "R$ 10,00", null, byteArrayOf(1, 2, 3), null)

        assertEquals(1080, ImageIO.read(ByteArrayInputStream(bytes)).width)
    }

    @Test
    fun deliveryDateWithUnitPriceStillRendersASquarePng() {
        // Preço unitário e prazo juntos são o caso de mais linhas: a faixa cresce em vez de estourar.
        val bytes = renderQuoteImage(
            texts = testClientTexts,
            title = "Chaveiro",
            priceText = "R$ 80,00",
            unitPriceText = "10 peças · R$ 8,00 cada",
            photoBytes = null,
            brandText = "Minha marca",
            deliveryText = "Entrega até 30/09",
        )

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(1080, image.width)
        assertEquals(1080, image.height)
    }

    // --- O tom e o desenho do fundo (Leva 10B, parte 7) ---

    private val navy = 0x0C2742
    private val navyBand = 0x081526
    private val lightBackground = 0xF6F8FB

    /** Uma peça de [color] no meio de um fundo transparente (miniatura do G-code, captura do 3D). */
    private fun pieceOf(color: Int, width: Int = 400, height: Int = 300): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in height / 4 until height * 3 / 4) for (x in width / 4 until width * 3 / 4) image.setRGB(x, y, 0xFF000000.toInt() or color)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    /** Uma foto sem transparência, toda de [color]. */
    private fun opaquePhotoOf(color: Int, width: Int = 400, height: Int = 300): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) for (x in 0 until width) image.setRGB(x, y, color)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    private fun render(photo: ByteArray?, background: ImageBackground, title: String = "Peça", unit: String? = null, brand: String? = "Minha marca", delivery: String? = "Entrega até 30/09"): BufferedImage =
        ImageIO.read(ByteArrayInputStream(renderQuoteImage(testClientTexts, title, "R$ 42,00", unit, photo, brand, delivery, background)))

    /** A cor do canto de baixo à esquerda: a faixa (tom escuro) ou o fundo (tom claro), nunca coberta pela foto. */
    private fun BufferedImage.bottomCorner() = getRGB(4, 1075) and 0xFFFFFF

    @Test
    fun aBlackPieceOnAutomaticGoesToTheLightBackground() {
        assertEquals(lightBackground, render(pieceOf(0x000000), ImageBackground.AUTO).bottomCorner())
        assertEquals(lightBackground, render(opaquePhotoOf(0x101010), ImageBackground.AUTO).bottomCorner())
    }

    @Test
    fun aWhitePieceOnAutomaticGoesToTheNavyBackground() {
        assertEquals(navyBand, render(pieceOf(0xFFFFFF), ImageBackground.AUTO).bottomCorner())
        assertEquals(navyBand, render(opaquePhotoOf(0xF0F0F0), ImageBackground.AUTO).bottomCorner())
    }

    @Test
    fun theFixedModesIgnoreThePiece() {
        assertEquals(navyBand, render(pieceOf(0x000000), ImageBackground.DARK).bottomCorner())
        assertEquals(lightBackground, render(pieceOf(0xFFFFFF), ImageBackground.LIGHT).bottomCorner())
    }

    @Test
    fun withoutAPhotoAutomaticIsNavyAndTheLightModeStillWorks() {
        assertEquals(navy, render(null, ImageBackground.AUTO).getRGB(4, 4) and 0xFFFFFF)
        assertEquals(lightBackground, render(null, ImageBackground.LIGHT).getRGB(4, 4) and 0xFFFFFF)
        assertEquals(navy, render(byteArrayOf(1, 2, 3), ImageBackground.AUTO).getRGB(4, 4) and 0xFFFFFF)
    }

    @Test
    fun theToneIsChosenFromThePieceBrightness() {
        assertEquals(ImageTone.LIGHT, resolveImageTone(ImageBackground.AUTO, pieceOf(0x000000)))
        assertEquals(ImageTone.DARK, resolveImageTone(ImageBackground.AUTO, pieceOf(0xFFFFFF)))
        assertEquals(ImageTone.DARK, resolveImageTone(ImageBackground.AUTO, pieceOf(0x808080)))
        assertEquals(ImageTone.DARK, resolveImageTone(ImageBackground.AUTO, null))
        assertEquals(ImageTone.LIGHT, resolveImageTone(ImageBackground.LIGHT, null))
    }

    @Test
    fun bothTonesProduceA1080SquareWithAndWithoutAPhoto() {
        for (background in listOf(ImageBackground.DARK, ImageBackground.LIGHT)) {
            for (photo in listOf(null, pieceOf(0x336699), opaquePhotoOf(0x336699))) {
                val image = render(photo, background, unit = "3 peças · R$ 14,00 cada")
                assertEquals(1080, image.width)
                assertEquals(1080, image.height)
            }
        }
    }

    @Test
    fun anOpaquePhotoFillsTheAreaButAPieceWithTransparencyStaysInsideWithAMargin() {
        // Peça larga (4:1) com uma faixa transparente de cada lado (20% da imagem, como numa miniatura do G-code): contida, deixa a margem lateral livre.
        val wide = BufferedImage(800, 200, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 200) for (x in 80 until 720) wide.setRGB(x, y, 0xFF336699.toInt())
        val widePiece = ByteArrayOutputStream().also { ImageIO.write(wide, "png", it) }.toByteArray()

        val contained = render(widePiece, ImageBackground.DARK)
        assertEquals(navy, contained.getRGB(30, 300) and 0xFFFFFF, "a margem lateral fica livre")
        assertEquals(0x336699, contained.getRGB(540, 300) and 0xFFFFFF, "a peça está no meio")

        val filled = render(opaquePhotoOf(0x336699), ImageBackground.DARK)
        assertEquals(0x336699, filled.getRGB(30, 300) and 0xFFFFFF, "a foto opaca preenche a largura")
    }

    @Test
    fun aLongNameAndABigPriceStayInsideTheImage() {
        val longName = "Suporte articulado para celular e tablet com base giratória e ajuste de altura ".repeat(4)
        val bytes = renderQuoteImage(testClientTexts, longName, "R$ 1.234.567,89", "12 peças · R$ 102.880,66 cada", null, "Uma marca com nome bem comprido · @contato.comprido.mesmo", "Entrega até 30/09 · entrega expressa", ImageBackground.DARK)

        val image = ImageIO.read(ByteArrayInputStream(bytes))
        // Nada de texto claro encostando nas bordas: a faixa de 20 px de cada lado continua toda no fundo.
        for (y in 0 until 1080 step 7) {
            assertEquals(navy, image.getRGB(10, y) and 0xFFFFFF)
            assertEquals(navy, image.getRGB(1069, y) and 0xFFFFFF)
        }
    }
}
