package com.threedreport.app.platform

import com.threedreport.core.model.ImageBackground
import com.threedreport.core.model.ImageTone
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Medidas e cores da imagem. Cada cor é um papel do sistema de design (docs/design-system.md); as medidas vêm do
// esboço aprovado (Leva 10B, parte 7), onde a largura da imagem é 100 e aqui são 1080 pixels.
// ---------------------------------------------------------------------------------------------

private const val SIZE = 1080

/** Margem lateral do texto (6% da largura). */
private const val SIDE_PADDING = 64

/** Respiro acima e abaixo do texto da faixa (4,4%). */
private const val BAND_PADDING_Y = 48

/** Altura da faixa inferior (30%). Cresce quando o texto não cabe: nome de duas linhas e preço por unidade. */
private const val BAND_MIN_HEIGHT = 324

/** Nome da peça, preço, preço por unidade, entrega, contato e assinatura, em pixels. */
private const val NAME_SIZE = 58f
private const val PRICE_SIZE = 130f
private const val UNIT_PRICE_SIZE = 36f
private const val DELIVERY_SIZE = 41f
private const val CONTACT_SIZE = 32f
private const val SIGNATURE_SIZE = 24f

/** O preço encolhe até aqui, no mínimo, antes de a imagem desistir de caber (um preço enorme nunca é cortado). */
private const val MIN_PRICE_SIZE = 48f

/** Altura da linha do nome em relação ao corpo, e o respiro entre o nome e o preço e entre o preço e o preço por unidade. */
private const val NAME_LINE_HEIGHT = 1.15f
private const val NAME_PRICE_GAP = 28
private const val PRICE_UNIT_GAP = 18

/** Altura das maiúsculas em relação ao corpo (Inter e Space Grotesk ficam perto de 0,72) e a descida abaixo da linha de base. */
private const val CAP_HEIGHT = 0.72f
private const val DESCENT = 0.2f

/** O nome da peça tem até duas linhas; depois disso, reticências. */
private const val NAME_MAX_LINES = 2

/** Largura máxima do bloco da direita (entrega, contato, assinatura) e o vão mínimo até o nome. Linha mais longa quebra. */
private const val RIGHT_MAX_WIDTH = 420
private const val COLUMN_GAP = 40
private const val RIGHT_GAP = 10

/** Peça com transparência (miniatura do G-code, captura do 3D): fica inteira dentro da área, com esta margem. */
private const val PIECE_MARGIN_X = 86
private const val PIECE_MARGIN_Y = 54

/** Tom claro: o cartão da peça. Margem (5%), raio dos cantos (2,4%) e espessura da borda. Ele desce até [CARD_TEXT_GAP] acima do texto. */
private const val CARD_MARGIN = 54
private const val CARD_TEXT_GAP = 48
private const val CARD_RADIUS = 26
private const val CARD_BORDER = 2
private const val CARD_PIECE_MARGIN = 64

/** A assinatura "Gerado com 3DReport" é bem apagada: 55% de opacidade. */
private const val SIGNATURE_ALPHA = 140

/** Nome do app na assinatura; a frase em volta vem dos recursos ([ClientDocumentTexts.signatureLine]). */
private const val APP_NAME = "3DReport"

/** As cores de um dos dois tons. */
private class Palette(
    val background: Color,
    val band: Color?,
    val name: Color,
    val price: Color,
    val delivery: Color,
    val muted: Color,
)

/** Tom escuro, o do azul-marinho: `navy` no fundo e `navy` do tema escuro na faixa, `onNavy` e `onNavyMuted` no texto. Só o preço em `amber`. */
private val DARK_PALETTE = Palette(
    background = DocumentColors.navy,
    band = DocumentColors.navyDeep,
    name = DocumentColors.onNavy,
    price = DocumentColors.amber,
    delivery = DocumentColors.onNavy,
    muted = DocumentColors.onNavyMuted,
)

/** Tom claro: `background` no fundo, `text` no nome e no preço, `textMuted` no contato e a entrega no âmbar de texto (o `amber` não tem contraste no claro). */
private val LIGHT_PALETTE = Palette(
    background = DocumentColors.screenBackground,
    band = null,
    name = DocumentColors.text,
    price = DocumentColors.text,
    delivery = DocumentColors.amberText,
    muted = DocumentColors.textMuted,
)

/** `card` e `line`: o cartão branco com borda onde a peça fica no tom claro. */
private val CARD_COLOR = DocumentColors.card
private val CARD_LINE = DocumentColors.line

private fun loadFont(bytes: ByteArray): Font = Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(bytes))

private val INTER_REGULAR: Font by lazy { loadFont(ClientDocumentFonts.interRegular) }
private val INTER_MEDIUM: Font by lazy { loadFont(ClientDocumentFonts.interMedium) }
private val INTER_SEMIBOLD: Font by lazy { loadFont(ClientDocumentFonts.interSemiBold) }
private val SPACE_GROTESK_BOLD: Font by lazy { loadFont(ClientDocumentFonts.spaceGroteskBold) }

/** Uma linha do bloco da direita, já quebrada pra caber. */
private class RightLine(val text: String, val font: Font, val color: Color)

actual fun renderQuoteImage(
    texts: ClientDocumentTexts,
    title: String,
    priceText: String,
    unitPriceText: String?,
    photoBytes: ByteArray?,
    brandText: String?,
    deliveryText: String?,
    background: ImageBackground,
): ByteArray {
    // A mesma decodificação por Skia usada no PDF (decisão 65): o ImageIO do Java devolve null em
    // silêncio pra WebP, e a foto sumiria da imagem sem nenhum erro aparecer. Uma vez só: o tom e o desenho usam a mesma.
    val photo = photoBytes?.let(::decodeToBufferedImage)
    val tone = background.resolve(photo?.let(::averageImageLuminance))
    val palette = if (tone == ImageTone.LIGHT) LIGHT_PALETTE else DARK_PALETTE

    val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

    graphics.color = palette.background
    graphics.fillRect(0, 0, SIZE, SIZE)

    // O bloco da direita primeiro: a largura dele diz quanto sobra pro nome e pro preço.
    val signatureFont = INTER_REGULAR.deriveFont(SIGNATURE_SIZE)
    val rightLines = buildList {
        deliveryText?.takeIf { it.isNotBlank() }?.let { addAll(rightLinesOf(graphics, it, INTER_SEMIBOLD.deriveFont(DELIVERY_SIZE), palette.delivery)) }
        brandText?.takeIf { it.isNotBlank() }?.let { addAll(rightLinesOf(graphics, it, INTER_REGULAR.deriveFont(CONTACT_SIZE), palette.muted)) }
        val signatureColor = Color(palette.muted.red, palette.muted.green, palette.muted.blue, SIGNATURE_ALPHA)
        addAll(rightLinesOf(graphics, texts.signatureLine(APP_NAME), signatureFont, signatureColor))
    }
    val rightWidth = rightLines.maxOf { graphics.getFontMetrics(it.font).stringWidth(it.text) }
    val leftWidth = SIZE - SIDE_PADDING * 2 - rightWidth - COLUMN_GAP

    // A coluna da esquerda: nome (até duas linhas), preço (encolhe se preciso) e, com mais de uma peça, o preço por unidade.
    val nameFont = INTER_MEDIUM.deriveFont(NAME_SIZE)
    val nameLines = wrapLines(graphics.getFontMetrics(nameFont), title, leftWidth, NAME_MAX_LINES)
    val priceFont = fitFont(graphics, SPACE_GROTESK_BOLD, priceText, leftWidth, PRICE_SIZE)
    val unitFont = INTER_REGULAR.deriveFont(UNIT_PRICE_SIZE)
    val unitLine = unitPriceText?.takeIf { it.isNotBlank() }?.let { wrapLines(graphics.getFontMetrics(unitFont), it, leftWidth, 1).first() }

    // Linhas de base, de baixo pra cima: a última do bloco fica a `BAND_PADDING_Y` da borda, mais a descida do preço.
    val bottomBaseline = SIZE - BAND_PADDING_Y - DESCENT * priceFont.size2D
    val priceBaseline = if (unitLine != null) bottomBaseline - UNIT_PRICE_SIZE - PRICE_UNIT_GAP else bottomBaseline
    val lastNameBaseline = priceBaseline - CAP_HEIGHT * priceFont.size2D - NAME_PRICE_GAP
    val nameLineStep = NAME_LINE_HEIGHT * NAME_SIZE
    val firstNameBaseline = lastNameBaseline - nameLineStep * (nameLines.size - 1)
    val leftTop = firstNameBaseline - CAP_HEIGHT * NAME_SIZE
    // Distância da linha de base de cada linha da direita até a de cima: a descida de cima, o respiro e a altura das maiúsculas.
    val rightStep = rightLines.indices.map { index ->
        if (index == 0) 0f else DESCENT * rightLines[index - 1].font.size2D + RIGHT_GAP + CAP_HEIGHT * rightLines[index].font.size2D
    }
    val rightTop = bottomBaseline - rightStep.sum() - CAP_HEIGHT * rightLines.first().font.size2D
    val infoTop = min(leftTop, rightTop)

    val hasPhoto = photo != null
    val bandTop = min(SIZE - BAND_MIN_HEIGHT, floor(infoTop - BAND_PADDING_Y).toInt())

    if (hasPhoto) {
        if (tone == ImageTone.LIGHT) {
            drawCard(graphics, photo!!, floor(infoTop - CARD_TEXT_GAP).toInt())
        } else {
            drawPieceOnBackground(graphics, photo!!, bandTop)
            graphics.color = palette.band
            graphics.fillRect(0, bandTop, SIZE, SIZE - bandTop)
        }
    }

    // Sem foto não há o que separar da peça: o texto vai pro meio da imagem, e a arte fica com cara de proposital.
    if (!hasPhoto) graphics.translate(0.0, SIZE / 2.0 - (infoTop + (SIZE - BAND_PADDING_Y)) / 2.0)

    graphics.color = palette.name
    graphics.font = nameFont
    nameLines.forEachIndexed { index, line -> graphics.drawString(line, SIDE_PADDING.toFloat(), firstNameBaseline + nameLineStep * index) }

    graphics.color = palette.price
    graphics.font = priceFont
    graphics.drawString(priceText, SIDE_PADDING.toFloat(), priceBaseline)

    if (unitLine != null) {
        graphics.color = palette.muted
        graphics.font = unitFont
        graphics.drawString(unitLine, SIDE_PADDING.toFloat(), bottomBaseline)
    }

    // O bloco da direita sobe a partir da mesma linha de base do preço; sem prazo, contato ou marca ele só fica menor.
    var baseline = bottomBaseline
    for (index in rightLines.indices.reversed()) {
        val line = rightLines[index]
        graphics.color = line.color
        graphics.font = line.font
        val width = graphics.fontMetrics.stringWidth(line.text)
        graphics.drawString(line.text, (SIZE - SIDE_PADDING - width).toFloat(), baseline)
        if (index > 0) baseline -= rightStep[index]
    }

    graphics.dispose()

    val output = ByteArrayOutputStream()
    ImageIO.write(image, "png", output)
    return output.toByteArray()
}

/**
 * [text] em até duas linhas de [RIGHT_MAX_WIDTH], na ordem de leitura. Quando a linha "marca · @contato" não cabe numa linha, quebra
 * no separador (marca numa linha, contato na outra, sem o "·") em vez de no meio de uma palavra.
 */
private fun rightLinesOf(graphics: Graphics2D, text: String, font: Font, color: Color): List<RightLine> {
    val metrics = graphics.getFontMetrics(font)
    val parts = text.split(" · ").map { it.trim() }.filter { it.isNotEmpty() }
    val lines = if (parts.size > 1 && metrics.stringWidth(text) > RIGHT_MAX_WIDTH) {
        parts.flatMap { wrapLines(metrics, it, RIGHT_MAX_WIDTH, maxLines = 1) }.take(2)
    } else {
        wrapLines(metrics, text, RIGHT_MAX_WIDTH, maxLines = 2)
    }
    return lines.map { RightLine(it, font, color) }
}

/** A fonte de [base] no maior corpo, de [startSize] pra baixo, em que [text] cabe em [maxWidth] (nunca abaixo de [MIN_PRICE_SIZE]). */
private fun fitFont(graphics: Graphics2D, base: Font, text: String, maxWidth: Int, startSize: Float): Font {
    val full = base.deriveFont(startSize)
    val width = graphics.getFontMetrics(full).stringWidth(text)
    if (width <= maxWidth) return full
    return base.deriveFont(maxOf(MIN_PRICE_SIZE, floor(startSize * maxWidth / width)))
}

/**
 * Quebra [text] em até [maxLines] linhas que cabem em [maxWidth]; se sobrar texto, a última termina em reticências.
 * Uma palavra mais larga que a linha também é cortada com reticências, em vez de vazar pra fora da arte.
 */
private fun wrapLines(metrics: FontMetrics, text: String, maxWidth: Int, maxLines: Int): List<String> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val lines = mutableListOf<String>()
    var current = ""
    var index = 0
    while (index < words.size && lines.size < maxLines) {
        val candidate = if (current.isEmpty()) words[index] else "$current ${words[index]}"
        if (current.isEmpty() || metrics.stringWidth(candidate) <= maxWidth) {
            current = candidate
            index++
        } else {
            lines += current
            current = ""
        }
    }
    if (current.isNotEmpty() && lines.size < maxLines) lines += current
    val truncated = index < words.size
    return lines.ifEmpty { listOf("") }.mapIndexed { position, line ->
        ellipsizeIfNeeded(metrics, line, maxWidth, force = truncated && position == lines.lastIndex)
    }
}

private fun ellipsizeIfNeeded(metrics: FontMetrics, line: String, maxWidth: Int, force: Boolean): String {
    if (!force && metrics.stringWidth(line) <= maxWidth) return line
    var end = line.length
    while (end > 1 && metrics.stringWidth(line.take(end).trimEnd() + "…") > maxWidth) end--
    return line.take(end).trimEnd() + "…"
}

/**
 * A peça acima da faixa ([areaHeight]): a foto opaca (enviada pela pessoa) preenche a área inteira, cortando o excesso
 * do lado mais comprido em vez de espremer a peça (uma foto distorcida num anúncio passa a impressão errada do produto).
 * Foto com transparência (miniatura do G-code, captura do 3D) fica inteira, centralizada e com margem, sem cortar a peça.
 */
private fun drawPieceOnBackground(graphics: Graphics2D, photo: BufferedImage, areaHeight: Int) {
    if (photo.hasTransparency()) {
        drawContained(graphics, photo, PIECE_MARGIN_X, PIECE_MARGIN_Y, SIZE - PIECE_MARGIN_X * 2, areaHeight - PIECE_MARGIN_Y * 2)
    } else {
        drawFilling(graphics, photo, 0, 0, SIZE, areaHeight)
    }
}

/** O cartão branco do tom claro, com a borda fina, e a peça dentro: a foto opaca preenche o cartão, a transparente fica inteira. */
private fun drawCard(graphics: Graphics2D, photo: BufferedImage, cardBottom: Int) {
    val width = SIZE - CARD_MARGIN * 2
    val height = (cardBottom - CARD_MARGIN).coerceAtLeast(1)
    val shape = RoundRectangle2D.Float(CARD_MARGIN.toFloat(), CARD_MARGIN.toFloat(), width.toFloat(), height.toFloat(), CARD_RADIUS * 2f, CARD_RADIUS * 2f)
    graphics.color = CARD_COLOR
    graphics.fill(shape)

    if (photo.hasTransparency()) {
        drawContained(graphics, photo, CARD_MARGIN + CARD_PIECE_MARGIN, CARD_MARGIN + CARD_PIECE_MARGIN, width - CARD_PIECE_MARGIN * 2, height - CARD_PIECE_MARGIN * 2)
    } else {
        // O recorte do clipe do Java2D não tem serrilhado suave: a foto vai pra uma camada à parte, que ganha os cantos
        // arredondados por máscara, e só então entra na imagem.
        val layer = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val layerGraphics = layer.createGraphics()
        layerGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        layerGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        layerGraphics.color = Color.WHITE
        layerGraphics.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), CARD_RADIUS * 2f, CARD_RADIUS * 2f))
        layerGraphics.composite = AlphaComposite.SrcIn
        drawFilling(layerGraphics, photo, 0, 0, width, height)
        layerGraphics.dispose()
        graphics.drawImage(layer, CARD_MARGIN, CARD_MARGIN, null)
    }

    graphics.color = CARD_LINE
    graphics.stroke = BasicStroke(CARD_BORDER.toFloat())
    graphics.draw(shape)
    graphics.stroke = BasicStroke(1f)
}

/** [photo] cobrindo a caixa ([x], [y], [width] × [height]), cortando o que passar. */
private fun drawFilling(graphics: Graphics2D, photo: BufferedImage, x: Int, y: Int, width: Int, height: Int) {
    val scale = maxOf(width.toDouble() / photo.width, height.toDouble() / photo.height)
    val drawWidth = ceil(photo.width * scale).toInt()
    val drawHeight = ceil(photo.height * scale).toInt()
    val oldClip = graphics.clip
    graphics.clipRect(x, y, width, height)
    graphics.drawImage(photo, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight, null)
    graphics.clip = oldClip
}

/** [photo] inteira dentro da caixa ([x], [y], [width] × [height]), centralizada, sem cortar nem distorcer. */
private fun drawContained(graphics: Graphics2D, photo: BufferedImage, x: Int, y: Int, width: Int, height: Int) {
    val scale = minOf(width.toDouble() / photo.width, height.toDouble() / photo.height)
    val drawWidth = (photo.width * scale).toInt().coerceAtLeast(1)
    val drawHeight = (photo.height * scale).toInt().coerceAtLeast(1)
    graphics.drawImage(photo, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight, null)
}
