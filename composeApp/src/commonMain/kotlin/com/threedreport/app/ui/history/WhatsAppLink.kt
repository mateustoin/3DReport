package com.threedreport.app.ui.history

import com.threedreport.core.model.Currency
import com.threedreport.core.model.SavedQuote

/**
 * Link `wa.me` que abre a conversa com o cliente já com o orçamento escrito, pronto pra enviar.
 *
 * A maior parte da venda de impressão 3D acontece em conversa, não em documento anexado: o texto é
 * o mesmo do copiar/colar (ver [toCopyPasteText]), só que sem o passo de colar na mão.
 *
 * Quando não dá pra reconhecer um telefone no contato salvo (está vazio, é um e-mail, é um @ de
 * rede social), o link sai **sem número**: o WhatsApp abre perguntando pra quem enviar, com o texto
 * já preenchido. É melhor do que esconder o botão, porque o trabalho de escrever a mensagem já foi
 * feito de qualquer jeito.
 */
internal fun SavedQuote.toWhatsAppLink(currency: Currency = Currency.BRL): String {
    val phone = client?.contact?.let(::toInternationalPhone)
    val text = percentEncode(toCopyPasteText(currency))
    return "https://wa.me/${phone.orEmpty()}?text=$text"
}

/**
 * Converte o contato digitado à mão num número que o `wa.me` aceita (só dígitos, com código do
 * país). Assume Brasil quando o número tem 10 ou 11 dígitos, que é o formato com DDD que as
 * pessoas escrevem no dia a dia ("(11) 99999-0000"); números maiores já são tratados como
 * internacionais e vão como estão.
 */
internal fun toInternationalPhone(contact: String): String? {
    val digits = contact.filter { it.isDigit() }
    return when (digits.length) {
        in 10..11 -> "55$digits"
        in 12..15 -> digits
        else -> null
    }
}

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
