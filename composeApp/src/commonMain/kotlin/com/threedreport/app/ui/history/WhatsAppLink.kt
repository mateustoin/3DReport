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
internal fun SavedQuote.toWhatsAppLink(currency: Currency = this.currency, showPrintTime: Boolean = false): String {
    val phone = client?.contact?.let(::toInternationalPhone)
    val text = percentEncode(toCopyPasteText(currency, showPrintTime))
    return "https://wa.me/${phone.orEmpty()}?text=$text"
}

/** Uma sequência com cara de telefone: dígitos com espaço, ponto, hífen ou parênteses no meio, e "+" opcional. */
private val PHONE_CANDIDATE = Regex("""\+?\(?\d[\d\s().-]{6,}\d""")

/**
 * Converte o contato digitado à mão num número que o `wa.me` aceita (só dígitos, com código do
 * país). Pega a primeira sequência com cara de telefone, então "Maria (11) 99999-0000 / e-mail" funciona.
 *
 * - Com "+", o número já é internacional e vai como está.
 * - Com 10 ou 11 dígitos, é o formato com DDD que se escreve no dia a dia ("(11) 99999-0000"): ganha o 55.
 * - Com o 0 da ligação interurbana ("0 21 11 99999-0000" ou "011 99999-0000"), sai o 0 e, se houver, o
 *   código da operadora.
 * - 12 a 15 dígitos sem "+" já são tratados como internacionais.
 * - Sem DDD (8 ou 9 dígitos) não dá pra saber a cidade: o link sai sem número.
 */
internal fun toInternationalPhone(contact: String): String? {
    val candidate = PHONE_CANDIDATE.find(contact)?.value ?: return null
    val digits = candidate.filter { it.isDigit() }
    if (candidate.startsWith("+")) return digits.takeIf { it.length in 8..15 }
    if (digits.startsWith("0")) {
        val national = digits.trimStart('0')
        return when (national.length) {
            in 10..11 -> "55$national"
            // Operadora (2 dígitos) + DDD + número.
            in 12..13 -> "55${national.drop(2)}"
            else -> null
        }
    }
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
