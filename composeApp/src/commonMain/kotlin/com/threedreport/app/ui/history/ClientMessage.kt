package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.message_default_approved
import _3dreport.composeapp.generated.resources.message_default_follow_up
import _3dreport.composeapp.generated.resources.message_default_out_for_delivery
import _3dreport.composeapp.generated.resources.message_default_payment_reminder
import _3dreport.composeapp.generated.resources.message_default_printing
import _3dreport.composeapp.generated.resources.message_default_ready
import _3dreport.composeapp.generated.resources.message_kind_approved
import _3dreport.composeapp.generated.resources.message_kind_follow_up
import _3dreport.composeapp.generated.resources.message_kind_out_for_delivery
import _3dreport.composeapp.generated.resources.message_kind_payment_reminder
import _3dreport.composeapp.generated.resources.message_kind_printing
import _3dreport.composeapp.generated.resources.message_kind_ready
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.loadClientDocumentTexts
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.message.fillMessageTemplate
import com.threedreport.core.message.orderMessageValues
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.SavedQuote
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** O nome do modelo, como aparece no seletor e em Configurações. */
fun MessageTemplateKind.labelRes(): StringResource = when (this) {
    MessageTemplateKind.APPROVED -> Res.string.message_kind_approved
    MessageTemplateKind.PRINTING -> Res.string.message_kind_printing
    MessageTemplateKind.READY -> Res.string.message_kind_ready
    MessageTemplateKind.OUT_FOR_DELIVERY -> Res.string.message_kind_out_for_delivery
    MessageTemplateKind.PAYMENT_REMINDER -> Res.string.message_kind_payment_reminder
    MessageTemplateKind.FOLLOW_UP -> Res.string.message_kind_follow_up
}

/** O texto padrão do modelo, nos recursos traduzíveis (segue o idioma do app). */
fun MessageTemplateKind.defaultTextRes(): StringResource = when (this) {
    MessageTemplateKind.APPROVED -> Res.string.message_default_approved
    MessageTemplateKind.PRINTING -> Res.string.message_default_printing
    MessageTemplateKind.READY -> Res.string.message_default_ready
    MessageTemplateKind.OUT_FOR_DELIVERY -> Res.string.message_default_out_for_delivery
    MessageTemplateKind.PAYMENT_REMINDER -> Res.string.message_default_payment_reminder
    MessageTemplateKind.FOLLOW_UP -> Res.string.message_default_follow_up
}

/** O Pix que a mensagem pode levar (decisão 148, item 38): o código e a linha "Pix copia e cola (R$ 50,00):" que vem antes dele. */
class MessagePix(val code: String, val header: String)

/** O Pix a cobrar de [order] pra mensagem, ou `null` sem Pix configurado ou sem nada a cobrar. */
suspend fun messagePixFor(order: SavedQuote, branding: BrandingSettings): MessagePix? {
    val code = branding.pixReceiver?.codeFor(order) ?: return null
    val charge = order.pixCharge ?: return null
    return MessagePix(code, loadClientDocumentTexts(listOf(order)).pixCopyPaste(charge.amount.toCurrencyText(order.currency)))
}

/** O modelo em que a marcação "Incluir o Pix" nasce ligada: o lembrete de pagamento e o aprovado (item 38). */
fun MessageTemplateKind.includesPixByDefault(): Boolean = this == MessageTemplateKind.PAYMENT_REMINDER || this == MessageTemplateKind.APPROVED

/**
 * A mensagem do modelo [kind] pro pedido [order] (decisão 148): o texto editado em Configurações, ou o padrão dos
 * recursos, com os marcadores preenchidos ([fillMessageTemplate]). O Pix não entra aqui: fica fora do texto editável
 * ([MessagePix]) e só vai no fim ao enviar.
 */
suspend fun clientMessageFor(order: SavedQuote, kind: MessageTemplateKind, branding: BrandingSettings): String {
    val template = branding.editedMessageTemplate(kind) ?: getString(kind.defaultTextRes())
    val values = orderMessageValues(order, branding.brandName, { it.toCurrencyText(order.currency) }, ::formatDate)
    return fillMessageTemplate(template, values)
}
