package com.threedreport.core.pix

import com.threedreport.core.contact.internationalPhoneDigits

/** Tipo de uma chave Pix, pelo jeito dela. [UNKNOWN] é aceita do mesmo jeito (quem valida é o banco). */
enum class PixKeyType { PHONE, CPF, CNPJ, EMAIL, RANDOM, UNKNOWN }

/**
 * Chave Pix pronta pra entrar no código (decisão 145, item 1): [value] já normalizado e o [type] reconhecido.
 * Monte com [parse].
 */
data class PixKey(val value: String, val type: PixKeyType) {

    /** Se a chave parece um dos tipos do Pix. Sem isso, a tela mostra um aviso curto, mas a chave é usada. */
    val isRecognized: Boolean
        get() = type != PixKeyType.UNKNOWN

    /** Se a chave cabe no campo do BR Code ([PixPayload.MAX_KEY_LENGTH]). */
    val fitsInPayload: Boolean
        get() = value.length <= PixPayload.MAX_KEY_LENGTH

    companion object {
        private val RANDOM_KEY = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private val NUMERIC = Regex("""^[+\d\s().\-/]+$""")
        private val FORMATTED_CPF = Regex("""^\d{3}\.\d{3}\.\d{3}-\d{2}$""")
        private val CNPJ_CHARS = Regex("""^[A-Za-z0-9.\-/\s]+$""")

        /** CNPJ sem pontuação: 12 letras ou dígitos e 2 dígitos verificadores (o alfanumérico da Receita também). */
        private val CNPJ_BODY = Regex("^[A-Z0-9]{12}[0-9]{2}$")

        /**
         * Lê a chave como foi digitada em Configurações. `null` se vazia.
         *
         * - Aleatória (EVP, 36 caracteres com hífens): como veio.
         * - E-mail (tem "@"): em minúsculas.
         * - CPF e CNPJ: só os dígitos (no CNPJ alfanumérico, letras maiúsculas e dígitos).
         * - Telefone: `+55DDDNUMERO`, com a mesma leitura do link do WhatsApp ([internationalPhoneDigits]).
         *
         * Onze dígitos sem pontuação servem tanto pra CPF quanto pra celular com DDD: vira CPF quando os dígitos
         * verificadores batem, e telefone quando não. Com "+", parênteses ou espaço é sempre telefone.
         */
        fun parse(raw: String?): PixKey? {
            val text = raw?.trim()?.ifEmpty { null } ?: return null
            if (RANDOM_KEY.matches(text)) return PixKey(text, PixKeyType.RANDOM)
            if ('@' in text) return PixKey(text.lowercase(), PixKeyType.EMAIL)
            if (NUMERIC.matches(text)) return parseNumeric(text)
            if (CNPJ_CHARS.matches(text)) {
                val body = text.filter { it.isLetterOrDigit() }.uppercase()
                if (CNPJ_BODY.matches(body) && body.any { it.isLetter() }) return PixKey(body, PixKeyType.CNPJ)
            }
            return PixKey(text, PixKeyType.UNKNOWN)
        }

        private fun parseNumeric(text: String): PixKey {
            val digits = text.filter { it.isDigit() }
            // CPF e CNPJ se escrevem sem espaço; telefone, com "+", parênteses ou em blocos separados por espaço.
            val looksLikePhone = text.startsWith("+") || '(' in text || text.any { it.isWhitespace() }
            return when {
                !looksLikePhone && FORMATTED_CPF.matches(text) -> PixKey(digits, PixKeyType.CPF)
                !looksLikePhone && digits.length == 14 -> PixKey(digits, PixKeyType.CNPJ)
                !looksLikePhone && digits.length == 11 && '/' !in text && isValidCpf(digits) -> PixKey(digits, PixKeyType.CPF)
                else -> phone(text) ?: PixKey(text, PixKeyType.UNKNOWN)
            }
        }

        /** Telefone brasileiro em `+55...`; número de outro país não é chave Pix de telefone. */
        private fun phone(text: String): PixKey? {
            val international = internationalPhoneDigits(text) ?: return null
            return if (international.startsWith("55")) PixKey("+$international", PixKeyType.PHONE) else null
        }

        /** Dígitos verificadores do CPF (módulo 11). Repetições ("111.111.111-11") não são CPF. */
        private fun isValidCpf(digits: String): Boolean {
            if (digits.length != 11 || digits.all { it == digits[0] }) return false
            val numbers = digits.map { it - '0' }
            fun check(count: Int): Int {
                val sum = (0 until count).sumOf { numbers[it] * (count + 1 - it) }
                val rest = sum % 11
                return if (rest < 2) 0 else 11 - rest
            }
            return check(9) == numbers[9] && check(10) == numbers[10]
        }
    }
}
