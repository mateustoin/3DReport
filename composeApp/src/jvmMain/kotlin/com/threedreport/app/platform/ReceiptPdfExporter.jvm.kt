package com.threedreport.app.platform

import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.history.clientTitle
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.ByteArrayOutputStream

// O recibo em PDF (Leva 11, decisão 147): o mesmo visual do orçamento (cabeçalho, tinta e cinzas, rodapé e assinatura),
// uma página só.

/** O corpo da frase "Recebi de...", um pouco maior que a linha de detalhe. */
private val BODY_SIZE = cqw(2.9f)
private const val BODY_LEADING = 1.5f

/** O nome da marca embaixo do quadro. */
private val SIGNER_SIZE = cqw(3.2f)

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
        writer.y -= cqw(2f)
        wrapText(context.displayFont, TITLE_SIZE, texts.receiptTitle(receipt.number), contentWidth, maxLines = 2).forEach { line ->
            writer.y -= TITLE_SIZE
            content.text(context.displayFont, TITLE_SIZE, SIDE_MARGIN, writer.y, line)
            writer.y -= TITLE_SIZE * 0.15f
        }
        writer.y -= GAP * 1.5f

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
        writer.y -= GAP * 1.5f

        // O quadro: total do pedido, pago até agora e falta pagar. Sem custo nem lucro.
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

        options.brandName?.takeIf { it.isNotBlank() }?.let { name ->
            writer.y -= GAP * 1.5f + SIGNER_SIZE
            val shown = wrapText(context.displayFont, SIGNER_SIZE, name, contentWidth, maxLines = 1).first()
            content.text(context.displayFont, SIGNER_SIZE, SIDE_MARGIN, writer.y, shown)
        }

        context.finishPage(writer, brandName, footerText)
        val output = ByteArrayOutputStream()
        document.save(output)
        return output.toByteArray()
    }
}
