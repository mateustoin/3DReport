package com.threedreport.app.platform

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.client_doc_catalog_other
import _3dreport.composeapp.generated.resources.client_doc_catalog_title
import _3dreport.composeapp.generated.resources.client_doc_contact_whatsapp
import _3dreport.composeapp.generated.resources.client_doc_delivery
import _3dreport.composeapp.generated.resources.client_doc_delivery_rush
import _3dreport.composeapp.generated.resources.client_doc_deposit
import _3dreport.composeapp.generated.resources.client_doc_image_delivery
import _3dreport.composeapp.generated.resources.client_doc_image_delivery_rush
import _3dreport.composeapp.generated.resources.client_doc_image_unit_price
import _3dreport.composeapp.generated.resources.client_doc_issued
import _3dreport.composeapp.generated.resources.client_doc_kit
import _3dreport.composeapp.generated.resources.client_doc_label_delivery
import _3dreport.composeapp.generated.resources.client_doc_label_print_time
import _3dreport.composeapp.generated.resources.client_doc_label_shipping
import _3dreport.composeapp.generated.resources.client_doc_label_shipping_free
import _3dreport.composeapp.generated.resources.client_doc_label_total
import _3dreport.composeapp.generated.resources.client_doc_label_valid_until
import _3dreport.composeapp.generated.resources.client_doc_label_value
import _3dreport.composeapp.generated.resources.client_doc_line_amount
import _3dreport.composeapp.generated.resources.client_doc_order_number
import _3dreport.composeapp.generated.resources.client_doc_pieces
import _3dreport.composeapp.generated.resources.client_doc_pix_code_label
import _3dreport.composeapp.generated.resources.client_doc_pix_copy_paste
import _3dreport.composeapp.generated.resources.client_doc_pix_deposit
import _3dreport.composeapp.generated.resources.client_doc_pix_key
import _3dreport.composeapp.generated.resources.client_doc_pix_title
import _3dreport.composeapp.generated.resources.client_doc_pix_value
import _3dreport.composeapp.generated.resources.client_doc_print_time
import _3dreport.composeapp.generated.resources.client_doc_print_time_pieces
import _3dreport.composeapp.generated.resources.client_doc_receipt_body
import _3dreport.composeapp.generated.resources.client_doc_receipt_body_anonymous
import _3dreport.composeapp.generated.resources.client_doc_receipt_date
import _3dreport.composeapp.generated.resources.client_doc_receipt_method
import _3dreport.composeapp.generated.resources.client_doc_receipt_paid
import _3dreport.composeapp.generated.resources.client_doc_receipt_received
import _3dreport.composeapp.generated.resources.client_doc_receipt_reference_numbered
import _3dreport.composeapp.generated.resources.client_doc_receipt_reference_plain
import _3dreport.composeapp.generated.resources.client_doc_receipt_remaining
import _3dreport.composeapp.generated.resources.client_doc_receipt_title
import _3dreport.composeapp.generated.resources.client_doc_receipt_title_numbered
import _3dreport.composeapp.generated.resources.client_doc_receipt_total
import _3dreport.composeapp.generated.resources.client_doc_rush
import _3dreport.composeapp.generated.resources.client_doc_service_per_piece
import _3dreport.composeapp.generated.resources.client_doc_shipping
import _3dreport.composeapp.generated.resources.client_doc_shipping_free
import _3dreport.composeapp.generated.resources.client_doc_signature
import _3dreport.composeapp.generated.resources.client_doc_title_auto_order
import _3dreport.composeapp.generated.resources.client_doc_title_auto_order_numbered
import _3dreport.composeapp.generated.resources.client_doc_title_auto_product
import _3dreport.composeapp.generated.resources.client_doc_title_numbered
import _3dreport.composeapp.generated.resources.client_doc_to
import _3dreport.composeapp.generated.resources.client_doc_total
import _3dreport.composeapp.generated.resources.client_doc_valid_until
import _3dreport.composeapp.generated.resources.client_doc_value
import _3dreport.composeapp.generated.resources.client_doc_value_quantity
import _3dreport.composeapp.generated.resources.duration_decimal_hours
import _3dreport.composeapp.generated.resources.duration_hours
import _3dreport.composeapp.generated.resources.duration_hours_minutes
import _3dreport.composeapp.generated.resources.duration_minutes
import _3dreport.composeapp.generated.resources.payment_method_card
import _3dreport.composeapp.generated.resources.payment_method_cash
import _3dreport.composeapp.generated.resources.payment_method_other
import _3dreport.composeapp.generated.resources.payment_method_pix
import com.threedreport.app.ui.format.DurationTexts
import com.threedreport.core.model.ContactChannel
import com.threedreport.core.model.ContactEntry
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Os textos do que vai pro cliente (PDF do orçamento, catálogo, imagem quadrada e mensagem do WhatsApp), lidos de
 * `strings.xml` fora da tela (Leva 10B, parte 7). O PDF e a imagem são desenhados fora de corrotina e sem
 * `@Composable`, então os textos chegam prontos: quem gera o documento chama [loadClientDocumentTexts] antes e passa
 * o resultado adiante.
 *
 * Cada modelo fica como está no recurso ("Valor: %1$s") e é preenchido na hora com [fill], do mesmo jeito que o
 * `getString` do Compose preenche. O plural ("3 peças") não dá pra deixar pra depois, porque a forma depende do
 * número e da língua: [loadClientDocumentTexts] resolve de antemão as quantidades que os orçamentos usam.
 */
class ClientDocumentTexts internal constructor(
    private val templates: Map<StringResource, String>,
    private val piecesByCount: Map<Int, String>,
) {
    private fun fill(resource: StringResource, vararg args: Any): String = fillTemplate(templates.getValue(resource), *args)

    /** Os modelos de duração, montados uma vez só (o PDF pede a duração de cada orçamento). */
    private val durationTexts: DurationTexts by lazy {
        DurationTexts(
            hoursAndMinutes = templates.getValue(Res.string.duration_hours_minutes),
            hoursOnly = templates.getValue(Res.string.duration_hours),
            minutesOnly = templates.getValue(Res.string.duration_minutes),
            decimalHoursTemplate = templates.getValue(Res.string.duration_decimal_hours),
        )
    }

    /** "3 peças", "1 peça". Só quantidades resolvidas em [loadClientDocumentTexts]. */
    fun pieces(count: Int): String = piecesByCount[count] ?: error("quantidade $count não foi carregada")

    /** "Valor: R$ 16,19". */
    fun value(amount: String) = fill(Res.string.client_doc_value, amount)

    /** "3 × R$ 16,19 = R$ 48,57". */
    fun valueTimesQuantity(quantity: Int, unit: String, total: String) = fill(Res.string.client_doc_value_quantity, quantity, unit, total)

    /** "Pintura (× 3)": serviço cobrado por peça num pedido de mais de uma. */
    fun servicePerPiece(name: String, quantity: Int) = fill(Res.string.client_doc_service_per_piece, name, quantity)

    /** "Pintura: R$ 15,00". */
    fun lineAmount(label: String, amount: String) = fill(Res.string.client_doc_line_amount, label, amount)

    /** "Frete: R$ 12,00". */
    fun shipping(amount: String) = fill(Res.string.client_doc_shipping, amount)

    /** "Frete: grátis". */
    val shippingFree: String get() = fill(Res.string.client_doc_shipping_free)

    /** "Total: R$ 78,57". */
    fun total(amount: String) = fill(Res.string.client_doc_total, amount)

    /** "Prazo de entrega: até 30/09/2026". */
    fun delivery(date: String) = fill(Res.string.client_doc_delivery, date)

    /** "Prazo de entrega: até 30/09/2026 (entrega expressa)". */
    fun deliveryRush(delivery: String) = fill(Res.string.client_doc_delivery_rush, delivery)

    /** "Entrega expressa", sem prazo. */
    val rush: String get() = fill(Res.string.client_doc_rush)

    /** "6 h 30 min", pro tempo de impressão (ver [DurationTexts.duration]). */
    fun duration(minutes: Double): String = durationTexts.duration(minutes)

    /** "Tempo de impressão: 6 h 30 min". */
    fun printTime(duration: String) = fill(Res.string.client_doc_print_time, duration)

    /** "Tempo de impressão: 6 h 30 min (3 peças)". */
    fun printTimeForPieces(printTime: String, quantity: Int) = fill(Res.string.client_doc_print_time_pieces, printTime, pieces(quantity))

    /** "Suporte · #0042". */
    fun titleNumbered(name: String, number: String) = fill(Res.string.client_doc_title_numbered, name, number)

    /** Título de um pedido de nome automático: "Orçamento", ou "Orçamento #0042" com o número. */
    fun autoOrderTitle(number: String?) =
        if (number == null) fill(Res.string.client_doc_title_auto_order) else fill(Res.string.client_doc_title_auto_order_numbered, number)

    /** Título de um produto de nome automático: "Produto". */
    val autoProductTitle: String get() = fill(Res.string.client_doc_title_auto_product)

    /** "Para: Maria". */
    fun to(clientName: String) = fill(Res.string.client_doc_to, clientName)

    /** "Kit com 2 peças". */
    fun kit(printCount: Int) = fill(Res.string.client_doc_kit, pieces(printCount))

    /** "Pedido #0042". */
    fun orderNumber(number: String) = fill(Res.string.client_doc_order_number, number)

    /** "Emitido em 25/09/2026". */
    fun issued(date: String) = fill(Res.string.client_doc_issued, date)

    /** "Válido até 02/10/2026". */
    fun validUntil(date: String) = fill(Res.string.client_doc_valid_until, date)

    /**
     * "Gerado com 3DReport", em três pedaços: o texto antes do nome do app, o nome e o que vem depois. O PDF desenha
     * a linha inteira e põe o link só em cima do nome (decisão 91), onde quer que a língua coloque o nome.
     */
    fun signature(appName: String): Triple<String, String, String> {
        val template = templates.getValue(Res.string.client_doc_signature)
        val at = template.indexOf(APP_NAME_PLACEHOLDER)
        if (at < 0) return Triple(template, "", "")
        return Triple(template.substring(0, at), appName, template.substring(at + APP_NAME_PLACEHOLDER.length))
    }

    /** "Gerado com 3DReport" numa linha só, pra imagem (que não tem link). */
    fun signatureLine(appName: String) = fill(Res.string.client_doc_signature, appName)

    /** "Catálogo de produtos". */
    val catalogTitle: String get() = fill(Res.string.client_doc_catalog_title)

    /** A seção do catálogo dos produtos sem categoria: "Outros". */
    val catalogOther: String get() = fill(Res.string.client_doc_catalog_other)

    /** Prazo na imagem, curto: "Entrega até 30/09". */
    fun imageDelivery(date: String) = fill(Res.string.client_doc_image_delivery, date)

    /** "Entrega até 30/09 · entrega expressa". */
    fun imageDeliveryRush(delivery: String) = fill(Res.string.client_doc_image_delivery_rush, delivery)

    /** "3 peças · R$ 22,19 cada". */
    fun imageUnitPrice(quantity: Int, unitPrice: String) = fill(Res.string.client_doc_image_unit_price, pieces(quantity), unitPrice)

    /** "Valor", rótulo solto do PDF (o preço vai ao lado). */
    val labelValue: String get() = fill(Res.string.client_doc_label_value)

    /** "Total", rótulo solto do PDF quando há serviços, frete ou várias peças. */
    val labelTotal: String get() = fill(Res.string.client_doc_label_total)

    /** "Frete", rótulo solto do PDF. */
    val labelShipping: String get() = fill(Res.string.client_doc_label_shipping)

    /** "grátis", o valor do frete absorvido. */
    val labelShippingFree: String get() = fill(Res.string.client_doc_label_shipping_free)

    /**
     * A linha de contato do cabeçalho do PDF: "WhatsApp (11) 99999-0000" (o nome do canal e o modelo vêm do recurso, pra
     * cada língua escolher a ordem), ou só o e-mail ou o Instagram.
     */
    fun contactLine(contact: ContactEntry): String = when (contact.channel) {
        ContactChannel.WHATSAPP -> fill(Res.string.client_doc_contact_whatsapp, contact.value)
        ContactChannel.EMAIL, ContactChannel.INSTAGRAM -> contact.value
    }

    /** "Entrega até", rótulo solto do PDF (a data vai ao lado). */
    val labelDelivery: String get() = fill(Res.string.client_doc_label_delivery)

    /** "Válido até", rótulo solto do PDF (a data vai ao lado). */
    val labelValidUntil: String get() = fill(Res.string.client_doc_label_valid_until)

    /** "Tempo de impressão", rótulo solto do PDF (a duração vai ao lado). */
    val labelPrintTime: String get() = fill(Res.string.client_doc_label_print_time)

    /** "Sinal de R$ 50,00 pra começar". */
    fun deposit(amount: String) = fill(Res.string.client_doc_deposit, amount)

    /** "Pix copia e cola (R$ 50,00):", a linha antes do código na mensagem. */
    fun pixCopyPaste(amount: String) = fill(Res.string.client_doc_pix_copy_paste, amount)

    /** "Pague com Pix", o título do bloco do PDF. */
    val pixTitle: String get() = fill(Res.string.client_doc_pix_title)

    /** "Sinal: R$ 50,00" (com [isDeposit]) ou "Valor: R$ 174,34", o que o Pix cobra. */
    fun pixAmount(amount: String, isDeposit: Boolean) =
        if (isDeposit) fill(Res.string.client_doc_pix_deposit, amount) else fill(Res.string.client_doc_pix_value, amount)

    /** "Chave: nome@email.com". */
    fun pixKey(key: String) = fill(Res.string.client_doc_pix_key, key)

    /** "Pix copia e cola", o rótulo do código no PDF. */
    val pixCodeLabel: String get() = fill(Res.string.client_doc_pix_code_label)

    /** "Pix", "Cartão", "Dinheiro" ou "Outro". */
    fun paymentMethod(method: PaymentMethod): String = when (method) {
        PaymentMethod.PIX -> fill(Res.string.payment_method_pix)
        PaymentMethod.CARD -> fill(Res.string.payment_method_card)
        PaymentMethod.CASH -> fill(Res.string.payment_method_cash)
        PaymentMethod.OTHER -> fill(Res.string.payment_method_other)
    }

    /** "Recibo", ou "Recibo #0042-1" com o número. */
    fun receiptTitle(number: String?) =
        if (number == null) fill(Res.string.client_doc_receipt_title) else fill(Res.string.client_doc_receipt_title_numbered, number)

    /**
     * A frase do recibo: "Recebi de Maria o valor de R$ 50,00 (Pix) em 17/11/2026, referente ao pedido #0042, Suporte." (em inglês, "Received $5.86 from Sophia L. by card on 09/24/2026 for order #0003 (Headset stand).")
     * Sem [client], "Recebi o valor de...". Sem [orderNumber], "referente a Suporte".
     */
    fun receiptBody(client: String?, amount: String, method: String, date: String, orderNumber: String?, piece: String): String {
        // Em inglês a forma entra no meio da frase ("by card"), então vai em minúscula.
        val methodText = if (AppLocale.current.value.language == UiLanguage.ENGLISH) method.lowercase() else method
        val reference = if (orderNumber != null) {
            fill(Res.string.client_doc_receipt_reference_numbered, orderNumber, piece)
        } else {
            fill(Res.string.client_doc_receipt_reference_plain, piece)
        }
        return if (client != null) {
            fill(Res.string.client_doc_receipt_body, client, amount, methodText, date, reference)
        } else {
            fill(Res.string.client_doc_receipt_body_anonymous, amount, methodText, date, reference)
        }
    }

    /** "Total do pedido", rótulo do quadro do recibo. */
    val receiptTotal: String get() = fill(Res.string.client_doc_receipt_total)

    /** "Valor recebido", o rótulo do valor grande do recibo. */
    val receiptReceived: String get() = fill(Res.string.client_doc_receipt_received)

    /** "Data", rótulo solto do recibo. */
    val receiptDate: String get() = fill(Res.string.client_doc_receipt_date)

    /** "Forma", rótulo solto do recibo. */
    val receiptMethod: String get() = fill(Res.string.client_doc_receipt_method)

    /** "Pago até agora". */
    val receiptPaid: String get() = fill(Res.string.client_doc_receipt_paid)

    /** "Falta pagar". */
    val receiptRemaining: String get() = fill(Res.string.client_doc_receipt_remaining)

    private companion object {
        const val APP_NAME_PLACEHOLDER = "%1\$s"
    }
}

private val CLIENT_DOCUMENT_TEMPLATES: List<StringResource> = listOf(
    Res.string.client_doc_value,
    Res.string.client_doc_value_quantity,
    Res.string.client_doc_service_per_piece,
    Res.string.client_doc_line_amount,
    Res.string.client_doc_shipping,
    Res.string.client_doc_shipping_free,
    Res.string.client_doc_total,
    Res.string.client_doc_delivery,
    Res.string.client_doc_delivery_rush,
    Res.string.client_doc_rush,
    Res.string.client_doc_print_time,
    Res.string.client_doc_print_time_pieces,
    Res.string.client_doc_title_numbered,
    Res.string.client_doc_title_auto_order,
    Res.string.client_doc_title_auto_order_numbered,
    Res.string.client_doc_title_auto_product,
    Res.string.client_doc_to,
    Res.string.client_doc_kit,
    Res.string.client_doc_order_number,
    Res.string.client_doc_issued,
    Res.string.client_doc_valid_until,
    Res.string.client_doc_signature,
    Res.string.client_doc_catalog_title,
    Res.string.client_doc_catalog_other,
    Res.string.client_doc_image_delivery,
    Res.string.client_doc_image_delivery_rush,
    Res.string.client_doc_image_unit_price,
    Res.string.client_doc_contact_whatsapp,
    Res.string.client_doc_label_value,
    Res.string.client_doc_label_total,
    Res.string.client_doc_label_shipping,
    Res.string.client_doc_label_shipping_free,
    Res.string.client_doc_label_delivery,
    Res.string.client_doc_label_valid_until,
    Res.string.client_doc_label_print_time,
    Res.string.client_doc_deposit,
    Res.string.client_doc_pix_copy_paste,
    Res.string.client_doc_pix_title,
    Res.string.client_doc_pix_deposit,
    Res.string.client_doc_pix_value,
    Res.string.client_doc_pix_key,
    Res.string.client_doc_pix_code_label,
    Res.string.client_doc_receipt_title,
    Res.string.client_doc_receipt_title_numbered,
    Res.string.client_doc_receipt_body,
    Res.string.client_doc_receipt_body_anonymous,
    Res.string.client_doc_receipt_reference_numbered,
    Res.string.client_doc_receipt_reference_plain,
    Res.string.client_doc_receipt_total,
    Res.string.client_doc_receipt_paid,
    Res.string.client_doc_receipt_received,
    Res.string.client_doc_receipt_date,
    Res.string.client_doc_receipt_method,
    Res.string.client_doc_receipt_remaining,
    Res.string.payment_method_pix,
    Res.string.payment_method_card,
    Res.string.payment_method_cash,
    Res.string.payment_method_other,
    Res.string.duration_hours_minutes,
    Res.string.duration_hours,
    Res.string.duration_minutes,
    Res.string.duration_decimal_hours,
)

/**
 * Lê os textos dos documentos pro cliente, com o plural de "peças" já resolvido pra cada quantidade que [quotes]
 * usam (unidades do pedido e impressões do kit).
 */
suspend fun loadClientDocumentTexts(quotes: List<SavedQuote>): ClientDocumentTexts =
    loadClientDocumentTexts(pieceCounts = quotes.flatMap { listOf(it.quote.quantity, it.quote.prints.size) })

/** Os mesmos textos, com o plural de "peças" resolvido pra cada número de [pieceCounts]. */
internal suspend fun loadClientDocumentTexts(pieceCounts: Iterable<Int>): ClientDocumentTexts = ClientDocumentTexts(
    templates = CLIENT_DOCUMENT_TEMPLATES.associateWith { getString(it) },
    piecesByCount = pieceCounts.toSet().associateWith { getPluralString(Res.plurals.client_doc_pieces, it, it) },
)
