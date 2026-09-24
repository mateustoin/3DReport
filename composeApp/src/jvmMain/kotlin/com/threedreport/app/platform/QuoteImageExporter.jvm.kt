package com.threedreport.app.platform

import androidx.compose.ui.graphics.toAwtImage
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private const val SIZE = 1080
private const val MARGIN = 64

/**
 * Altura mínima da faixa inferior com nome e preço; o resto da imagem é a foto. Cresce quando há
 * linhas a mais (preço unitário e prazo juntos), pra o texto nunca encostar na borda de baixo.
 */
private const val MIN_BAND_HEIGHT = 300

/** Espaço livre entre a última linha de texto e a borda de baixo da faixa. */
private const val BAND_BOTTOM_PADDING = 56

// Distância entre linhas de base: título → preço → preço unitário → prazo.
private const val TITLE_OFFSET = 90
private const val PRICE_OFFSET = 96
private const val UNIT_PRICE_OFFSET = 52
private const val DELIVERY_OFFSET = 56

// Mesma paleta do app (azul petróleo + âmbar, decisão 35), pra imagem e interface falarem a mesma
// língua visual sem depender de nenhum recurso externo.
private val BACKGROUND = Color(0x0F, 0x2B, 0x33)
private val ACCENT = Color(0xF5, 0xA6, 0x23)
private val TEXT = Color.WHITE
private val TEXT_MUTED = Color(0xB5, 0xC4, 0xC9)

actual fun renderQuoteImage(
    title: String,
    priceText: String,
    unitPriceText: String?,
    photoBytes: ByteArray?,
    brandText: String?,
    deliveryText: String?,
): ByteArray {
    val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

    graphics.color = BACKGROUND
    graphics.fillRect(0, 0, SIZE, SIZE)

    val lastBaseline = TITLE_OFFSET + PRICE_OFFSET +
        (if (unitPriceText != null) UNIT_PRICE_OFFSET else 0) +
        (if (deliveryText != null) DELIVERY_OFFSET else 0)
    val bandHeight = maxOf(MIN_BAND_HEIGHT, lastBaseline + BAND_BOTTOM_PADDING)

    // A mesma decodificação por Skia usada no PDF (decisão 65): o ImageIO do Java devolve null em
    // silêncio pra WebP, e a foto sumiria da imagem sem nenhum erro aparecer.
    val photo = photoBytes?.let { runCatching { decodeImageBitmap(it).toAwtImage() }.getOrNull() }
    if (photo != null) {
        drawPhotoFilling(graphics, photo, SIZE - bandHeight)
        graphics.color = BACKGROUND
        graphics.fillRect(0, SIZE - bandHeight, SIZE, bandHeight)
        graphics.color = ACCENT
        graphics.fillRect(0, SIZE - bandHeight, SIZE, 6)
    }

    // Sem foto não há o que separar: a faixa viraria um retângulo vazio com uma linha solta no
    // meio do nada, então o texto vai centralizado e a arte fica com cara de proposital.
    val bandTop = if (photo != null) SIZE - bandHeight else (SIZE - bandHeight) / 2
    var cursorY = bandTop + TITLE_OFFSET
    graphics.color = TEXT
    graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 46)
    graphics.drawString(fitToWidth(graphics, title, SIZE - MARGIN * 2), MARGIN, cursorY)

    cursorY += PRICE_OFFSET
    graphics.color = ACCENT
    graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 84)
    graphics.drawString(priceText, MARGIN, cursorY)

    if (unitPriceText != null) {
        cursorY += UNIT_PRICE_OFFSET
        graphics.color = TEXT_MUTED
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 34)
        graphics.drawString(unitPriceText, MARGIN, cursorY)
    }

    // Em âmbar e negrito, mas menor que o preço: é a segunda coisa que o cliente procura.
    if (deliveryText != null) {
        cursorY += DELIVERY_OFFSET
        graphics.color = ACCENT
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 38)
        graphics.drawString(fitToWidth(graphics, deliveryText, SIZE - MARGIN * 2), MARGIN, cursorY)
    }

    if (!brandText.isNullOrBlank()) {
        graphics.color = TEXT_MUTED
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 30)
        val text = fitToWidth(graphics, brandText, SIZE - MARGIN * 2)
        val width = graphics.fontMetrics.stringWidth(text)
        val brandY = if (photo != null) SIZE - bandHeight - 32 else SIZE - MARGIN
        if (photo != null) {
            // Por cima da foto, texto claro some numa foto clara: uma pílula escura translúcida
            // atrás garante a leitura do nome e do contato em qualquer foto.
            val metrics = graphics.fontMetrics
            graphics.color = Color(BACKGROUND.red, BACKGROUND.green, BACKGROUND.blue, 190)
            graphics.fillRoundRect(
                SIZE - MARGIN - width - 18,
                brandY - metrics.ascent - 10,
                width + 36,
                metrics.ascent + metrics.descent + 20,
                24,
                24,
            )
            graphics.color = TEXT
        }
        graphics.drawString(text, SIZE - MARGIN - width, brandY)
    }

    graphics.dispose()

    val output = ByteArrayOutputStream()
    ImageIO.write(image, "png", output)
    return output.toByteArray()
}

/**
 * Desenha a foto cobrindo toda a área disponível, cortando o excesso do lado mais comprido em vez
 * de espremer a peça: uma foto distorcida num anúncio passa a impressão errada do produto.
 */
private fun drawPhotoFilling(graphics: java.awt.Graphics2D, photo: BufferedImage, areaHeight: Int) {
    val scale = maxOf(SIZE.toDouble() / photo.width, areaHeight.toDouble() / photo.height)
    val drawWidth = (photo.width * scale).toInt()
    val drawHeight = (photo.height * scale).toInt()
    val x = (SIZE - drawWidth) / 2
    val y = (areaHeight - drawHeight) / 2
    graphics.drawImage(photo, x, y, drawWidth, drawHeight, null)
}

/** Corta o texto com reticências quando ele não cabe na largura, pra nunca vazar pra fora da arte. */
private fun fitToWidth(graphics: java.awt.Graphics2D, text: String, maxWidth: Int): String {
    val metrics = graphics.fontMetrics
    if (metrics.stringWidth(text) <= maxWidth) return text

    var end = text.length
    while (end > 1 && metrics.stringWidth(text.take(end) + "...") > maxWidth) end--
    return text.take(end).trimEnd() + "..."
}
