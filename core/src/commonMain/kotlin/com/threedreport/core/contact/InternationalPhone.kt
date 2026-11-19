package com.threedreport.core.contact

/** Uma sequência com cara de telefone: dígitos com espaço, ponto, hífen ou parênteses no meio, e "+" opcional. */
private val PHONE_CANDIDATE = Regex("""\+?\(?\d[\d\s().-]{6,}\d""")

/**
 * Telefone digitado à mão como número internacional, só dígitos e com o código do país (`5511999990000`), ou `null`
 * se não dá pra reconhecer. É a leitura do link do WhatsApp (decisão 80), aqui no `core` pra a chave Pix de telefone
 * (decisão 145) usar a mesma regra. Pega a primeira sequência com cara de telefone, então "Maria (11) 99999-0000 /
 * e-mail" funciona.
 *
 * - Com "+", o número já é internacional e vai como está.
 * - Com 10 ou 11 dígitos, é o formato com DDD que se escreve no dia a dia ("(11) 99999-0000"): ganha o 55.
 * - Com o 0 da ligação interurbana ("0 21 11 99999-0000" ou "011 99999-0000"), sai o 0 e, se houver, o
 *   código da operadora.
 * - 12 a 15 dígitos sem "+" já são tratados como internacionais.
 * - Sem DDD (8 ou 9 dígitos) não dá pra saber a cidade: `null`.
 */
fun internationalPhoneDigits(contact: String): String? {
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
