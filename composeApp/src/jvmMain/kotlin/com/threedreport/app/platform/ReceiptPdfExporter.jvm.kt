package com.threedreport.app.platform

import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.history.clientTitle
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.ByteArrayOutputStream

// O recibo em PDF (Leva 11, decisão 147): o mesmo visual do orçamento (cabeçalho, tinta e cinzas, quadro do valor grande,
// rodapé e assinatura), uma página só. O nome da marca aparece no cabeçalho e no rodapé, nada mais: sem marca d'água.

/** O corpo da frase "Recebi de...", um pouco maior que a linha de detalhe. */
private val BODY_SIZE = cqw(2.9f)
private const val BODY_LEADING = 1.5f

actual fun renderReceiptPdf(
    receipt: ReceiptData,
    texts: ClientDocumentTexts,
    brandName: String?,
    footerText: String?,
    options: PdfLayoutOptions,
): ByteArray {
    PDDocument().use { document ->
        val context = PdfContext(document, options, texts)
        val order = receipt.order
        val payment = receipt.payment
        val currency = order.currency
        val contentWidth = PAGE_WIDTH - SIDE_MARGIN * 2

        val writer = context.startPage(withHeader = true)
        val content = writer.content
        // "Emitido em" em cima e o título grande embaixo, como no orçamento.
        writer.y -= cqw(2f) + META_SIZE
        content.text(context.bodyFont, META_SIZE, SIDE_MARGIN, writer.y, texts.issued(formatDate(options.issuedOnEpochDay ?: todayEpochDay())), INK_MUTED)
        writer.y -= cqw(0.8f)
        wrapText(context.displayFont, TITLE_SIZE, texts.receiptTitle(receipt.number), contentWidth, maxLines = 2).forEach { line ->
            writer.y -= TITLE_SIZE
            content.text(context.displayFont, TITLE_SIZE, SIDE_MARGIN, writer.y, line)
            writer.y -= TITLE_SIZE * 0.15f
        }
        writer.y -= GAP

        // O valor recebido, no estilo do total do orçamento: rótulo e valor grande à esquerda, data e forma à direita.
        val block = PriceBlock(
            label = texts.receiptReceived,
            price = payment.amount.toCurrencyText(currency),
            lines = listOf(
                BlockLine(texts.receiptDate, formatDate(payment.epochDay), VALUE_SIZE, gapBelow = GROUP_GAP),
                BlockLine(texts.receiptMethod, texts.paymentMethod(payment.method), VALUE_SIZE, gapBelow = 0f),
            ),
        )
        val layout = priceLayout(context, block, contentWidth)
        drawPriceBlock(context, writer, block, layout, SIDE_MARGIN, contentWidth)
        writer.y -= GAP

        val body = texts.receiptBody(
            client = order.client?.name?.trim()?.takeIf { it.isNotEmpty() },
            amount = payment.amount.toCurrencyText(currency),
            method = texts.paymentMethod(payment.method),
            date = formatDate(payment.epochDay),
            orderNumber = order.displayNumber,
            piece = order.clientTitle(texts, withNumber = false),
        )
        wrapText(context.bodyFont, BODY_SIZE, body, contentWidth, maxLines = 12).forEach { line ->
            writer.y -= BODY_SIZE * BODY_LEADING
            content.text(context.bodyFont, BODY_SIZE, SIDE_MARGIN, writer.y, line)
        }
        writer.y -= GAP

        // O quadro do pedido: total, pago até agora e falta pagar. Sem custo nem lucro.
        val rows = listOf(
            texts.receiptTotal to order.totalWithServices.toCurrencyText(currency),
            texts.receiptPaid to order.paidTotal.toCurrencyText(currency),
            texts.receiptRemaining to order.remaining.toCurrencyText(currency),
        )
        val panelHeight = BLOCK_PAD_Y * 2 + rows.size * ROW_HEIGHT
        val panelBottom = writer.y - panelHeight
        content.fillPanel(SIDE_MARGIN, panelBottom, contentWidth, panelHeight)
        rows.forEachIndexed { index, (label, value) ->
            val rowTop = writer.y - BLOCK_PAD_Y - index * ROW_HEIGHT
            val baseline = rowTop - ROW_HEIGHT / 2f - ROW_SIZE * PDF_CAP_HEIGHT / 2f
            content.text(context.bodyFont, ROW_SIZE, SIDE_MARGIN + BLOCK_PAD_X, baseline, label, INK_MUTED)
            content.textRight(context.strongFont, ROW_SIZE, SIDE_MARGIN + contentWidth - BLOCK_PAD_X, baseline, value)
        }
        writer.y = panelBottom

        // Sem marca d'água. O recibo sempre diz quem recebeu: o nome da marca, quando existe, vai no rodapé mesmo com o rodapé
        // do orçamento desligado e sem logo nem contato no cabeçalho (no máximo duas vezes: cabeçalho e rodapé).
        context.finishPage(writer, null, options.brandName?.takeIf { it.isNotBlank() } ?: footerText)
        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}
