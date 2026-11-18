package com.threedreport.app.platform

import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.SavedQuote
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import java.awt.Color

// O bloco "Pague com Pix" e a linha do sinal do PDF do orçamento (Leva 11, decisões 145 e 146). Mesmas medidas e
// cores do resto do documento; o catálogo nunca passa por aqui.

/** O QR do Pix com cerca de 3,5 cm de lado, margem branca incluída (1 cm = 72 / 2,54 pontos). */
private const val QR_SIZE = 3.5f * 72f / 2.54f

/** O código copia e cola, em fonte menor que o resto do texto: ele é longo e só serve pra copiar. */
private val CODE_SIZE = cqw(1.7f)

/** O que o bloco do Pix mostra: o valor cobrado, a chave, o código e a matriz do QR. */
internal class PixBlock(val amountText: String, val isDeposit: Boolean, val key: String, val code: String, val qr: QrMatrix)

/** O bloco do Pix de [savedQuote], ou `null` sem Pix configurado ou sem Pix neste pedido (pago, outra moeda, produto). */
internal fun pixBlockFor(savedQuote: SavedQuote, options: PdfLayoutOptions): PixBlock? {
    val receiver = options.pixReceiver ?: return null
    val charge = savedQuote.pixCharge ?: return null
    val code = receiver.codeFor(savedQuote) ?: return null
    return PixBlock(charge.amount.toCurrencyText(savedQuote.currency), charge.isDeposit, receiver.key.value, code, pixQrMatrix(code))
}

/** Uma linha do texto do bloco; [overline] é o rótulo em maiúsculas. */
private class PixLine(val text: String, val font: PDFont, val size: Float, val color: Color, val overline: Boolean = false, val gapAbove: Float = 0f)

/** Quebra [text] em linhas de até [width], letra a letra: o código copia e cola não tem espaço onde quebrar. */
internal fun wrapChars(font: PDFont, size: Float, text: String, width: Float): List<String> {
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (char in text) {
        if (current.isNotEmpty() && font.widthOf(current.toString() + char, size) > width) {
            lines += current.toString()
            current = StringBuilder()
        }
        current.append(char)
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}

private fun textWidthOf(width: Float): Float = width - BLOCK_PAD_X * 2 - QR_SIZE - GROUP_GAP

private fun pixLines(context: PdfContext, block: PixBlock, width: Float): List<PixLine> {
    val texts = context.texts
    val textWidth = textWidthOf(width)
    return buildList {
        add(PixLine(texts.pixTitle, context.bodyFont, LABEL_SIZE, INK_MUTED, overline = true))
        add(PixLine(texts.pixAmount(block.amountText, block.isDeposit), context.strongFont, VALUE_SIZE, INK, gapAbove = LABEL_GAP))
        wrapChars(context.bodyFont, ROW_SIZE, texts.pixKey(block.key), textWidth).forEachIndexed { index, line ->
            add(PixLine(line, context.bodyFont, ROW_SIZE, INK_MUTED, gapAbove = if (index == 0) LABEL_GAP else 0f))
        }
        add(PixLine(texts.pixCodeLabel, context.bodyFont, LABEL_SIZE, INK_MUTED, overline = true, gapAbove = GROUP_GAP))
        wrapChars(context.bodyFont, CODE_SIZE, block.code, textWidth).forEachIndexed { index, line ->
            add(PixLine(line, context.bodyFont, CODE_SIZE, INK, gapAbove = if (index == 0) LABEL_GAP else 0f))
        }
    }
}

private fun PixLine.height(): Float = gapAbove + size * (PDF_CAP_HEIGHT + 0.35f)

/** A altura do bloco com a largura [width]: a maior entre o QR e o texto, mais o respiro de cima e de baixo. */
internal fun pixBlockHeight(context: PdfContext, block: PixBlock, width: Float): Float {
    val text = pixLines(context, block, width).sumOf { it.height().toDouble() }.toFloat()
    return BLOCK_PAD_Y * 2 + maxOf(QR_SIZE, text)
}

/** Desenha o bloco com o alto em [top]. O bloco nunca é partido: quem chama garante que cabe e já desceu o cursor. */
internal fun drawPixBlock(context: PdfContext, content: PDPageContentStream, block: PixBlock, x: Float, top: Float, width: Float) {
    val height = pixBlockHeight(context, block, width)
    content.fillPanel(x, top - height, width, height)
    content.drawQr(block.qr, x + BLOCK_PAD_X, top - BLOCK_PAD_Y, QR_SIZE)

    val textX = x + BLOCK_PAD_X + QR_SIZE + GROUP_GAP
    var cursor = top - BLOCK_PAD_Y
    pixLines(context, block, width).forEach { line ->
        cursor -= line.gapAbove + line.size * PDF_CAP_HEIGHT
        if (line.overline) {
            content.overline(line.font, line.size, textX, cursor, line.text, line.color)
        } else {
            content.text(line.font, line.size, textX, cursor, line.text, line.color)
        }
        cursor -= line.size * 0.35f
    }
}

/** O QR como retângulos (vetor, nítido em qualquer zoom) num quadrado branco de [size] com o canto de cima à esquerda em [x], [top]. */
private fun PDPageContentStream.drawQr(qr: QrMatrix, x: Float, top: Float, size: Float) {
    saveGraphicsState()
    setNonStrokingColor(DocumentColors.qrBackground)
    addRect(x, top - size, size, size)
    fill()
    setNonStrokingColor(INK)
    val module = size / qr.size
    for (row in 0 until qr.size) {
        var column = 0
        while (column < qr.size) {
            if (!qr[column, row]) {
                column++
                continue
            }
            val start = column
            while (column < qr.size && qr[column, row]) column++
            // Um fio a mais na altura: sem ele, linhas vizinhas deixam uma frestinha clara em alguns leitores de PDF.
            addRect(x + start * module, top - (row + 1) * module - 0.1f, (column - start) * module, module + 0.2f)
        }
    }
    fill()
    restoreGraphicsState()
}
