package com.threedreport.app.ui.history

import com.threedreport.app.platform.ClientDocumentTexts
import com.threedreport.core.contact.internationalPhoneDigits
import com.threedreport.core.model.Currency
import com.threedreport.core.model.SavedQuote

/**
 * Link `wa.me` que abre a conversa com o cliente já com o orçamento escrito, pronto pra enviar.
 *
 * A maior parte da venda de impressão 3D acontece em conversa, não em documento anexado: o texto é
 * o mesmo do copiar/colar (ver [toCopyPasteText]), só que sem o passo de colar na mão. Com [pixCode], a mensagem
 * termina com o Pix copia e cola.
 */
internal fun SavedQuote.toWhatsAppLink(
    texts: ClientDocumentTexts,
    currency: Currency = this.currency,
    showPrintTime: Boolean = false,
    pixCode: String? = null,
): String = whatsAppLink(client?.contact, toCopyPasteText(texts, currency, showPrintTime, pixCode))

/**
 * O link `wa.me` de [text] pro telefone lido de [contact]. Quando não dá pra reconhecer um telefone (vazio, e-mail, @ de
 * rede social), o link sai **sem número**: o WhatsApp abre perguntando pra quem enviar, com o texto já preenchido.
 */
internal fun whatsAppLink(contact: String?, text: String): String {
    val phone = contact?.let(::toInternationalPhone)
    return "https://wa.me/${phone.orEmpty()}?text=${percentEncode(text)}"
}

/**
 * O contato digitado à mão como número que o `wa.me` aceita (decisão 80). A leitura mora no `core`
 * ([internationalPhoneDigits]), que a chave Pix de telefone também usa.
 */
internal fun toInternationalPhone(contact: String): String? = internationalPhoneDigits(contact)

private const val HEX = "0123456789ABCDEF"

/**
 * Codificação percentual do texto pra caber numa URL. Feita à mão porque o Kotlin Multiplatform não
 * tem um codificador de URL comum, e o texto do orçamento tem acento e quebra de linha, que quebram
 * o link se forem embutidos crus.
 */
private fun percentEncode(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        val char = value.toChar()
        val isUnreserved = char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-_.~"
        if (value < 128 && isUnreserved) {
            append(char)
        } else {
            append('%').append(HEX[value shr 4]).append(HEX[value and 0x0F])
        }
    }
}
