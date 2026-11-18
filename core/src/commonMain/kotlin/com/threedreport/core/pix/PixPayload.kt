package com.threedreport.core.pix

import kotlin.math.roundToLong

/**
 * O "Pix copia e cola" estático (BR Code, padrão EMV do Banco Central), montado sem internet (decisão 145, item 2).
 * O mesmo texto vira o QR Code no `composeApp`.
 *
 * Cada campo é `ID` (2 dígitos) + tamanho (2 dígitos) + valor, nesta ordem: `00` versão (`01`); `26` conta Pix
 * (`00` = `br.gov.bcb.pix`, `01` = chave); `52` categoria (`0000`); `53` moeda (`986`, real); `54` valor, só se maior
 * que zero; `58` país (`BR`); `59` nome; `60` cidade; `62` com `05` = identificador (txid); `63` CRC16.
 */
object PixPayload {
    /**
     * Maior chave que cabe no campo `26`, em bytes (99, menos os 18 do `00` com `br.gov.bcb.pix` e os 4 do `01`). Como os
     * campos são só ASCII ([isAscii]), byte e caractere dão o mesmo número.
     */
    const val MAX_KEY_LENGTH = 77

    /** Limite do nome de quem recebe (campo `59`). */
    const val MAX_NAME_LENGTH = 25

    /** Limite da cidade (campo `60`). */
    const val MAX_CITY_LENGTH = 15

    /** Identificador sem número de pedido. */
    const val NO_TXID = "***"

    private const val GUI = "br.gov.bcb.pix"

    /**
     * Monta o código com os campos como vieram: quem chama já passa a chave normalizada ([PixKey]), o nome e a cidade
     * limpos ([sanitizeName], [sanitizeCity]) e o [txid] ([txidFor]). [amount] `null` ou zero deixa o valor em aberto
     * (o cliente digita no banco).
     */
    fun build(key: String, merchantName: String, merchantCity: String, amount: Double?, txid: String): String {
        require(listOf(key, merchantName, merchantCity, txid).all(::isAscii)) { "o BR Code só aceita ASCII" }
        require(key.isNotEmpty() && key.length <= MAX_KEY_LENGTH) { "chave fora do tamanho do BR Code: ${key.length}" }
        require(merchantName.isNotEmpty() && merchantName.length <= MAX_NAME_LENGTH) { "nome fora do tamanho do BR Code" }
        require(merchantCity.isNotEmpty() && merchantCity.length <= MAX_CITY_LENGTH) { "cidade fora do tamanho do BR Code" }
        require(txid.isNotEmpty() && txid.length <= 25) { "txid fora do tamanho do BR Code" }
        val body = buildString {
            append(field("00", "01"))
            append(field("26", field("00", GUI) + field("01", key)))
            append(field("52", "0000"))
            append(field("53", "986"))
            formatAmount(amount)?.let { append(field("54", it)) }
            append(field("58", "BR"))
            append(field("59", merchantName))
            append(field("60", merchantCity))
            append(field("62", field("05", txid)))
            append("6304")
        }
        return body + crc16(body)
    }

    /** Identificador do Pix de um pedido: `3DR` + número com 4 dígitos (`3DR0042`), ou [NO_TXID] sem número. */
    fun txidFor(orderNumber: Int): String = if (orderNumber > 0) "3DR" + orderNumber.toString().padStart(4, '0') else NO_TXID

    /** Nome pro código: sem acento, maiúsculas, espaços simples, até [MAX_NAME_LENGTH] caracteres. */
    fun sanitizeName(raw: String): String = sanitize(raw, MAX_NAME_LENGTH)

    /** Cidade pro código: sem acento, maiúsculas, espaços simples, até [MAX_CITY_LENGTH] caracteres. */
    fun sanitizeCity(raw: String): String = sanitize(raw, MAX_CITY_LENGTH)

    /** CRC16-CCITT (polinômio 0x1021, início 0xFFFF) dos bytes de [data], em 4 hex maiúsculos. */
    fun crc16(data: String): String {
        var crc = 0xFFFF
        for (byte in data.encodeToByteArray()) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }

    /** Se [text] só tem caracteres ASCII imprimíveis (de espaço a `~`), os que o BR Code aceita. */
    fun isAscii(text: String): Boolean = text.all { it in ' '..'~' }

    /** Um campo `ID` + tamanho + valor. O tamanho é em bytes UTF-8, os mesmos que o CRC e o QR leem. */
    private fun field(id: String, value: String): String {
        val size = value.encodeToByteArray().size
        require(size <= 99) { "campo $id maior que 99 bytes" }
        return id + size.toString().padStart(2, '0') + value
    }

    /** Valor com ponto e duas casas ("174.34"), ou `null` se não há valor a cobrar. */
    private fun formatAmount(amount: Double?): String? {
        if (amount == null || amount.isNaN()) return null
        val cents = (amount * 100.0).roundToLong()
        if (cents <= 0) return null
        return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    }

    private fun sanitize(raw: String, maxLength: Int): String {
        val ascii = buildString {
            for (char in raw.uppercase()) {
                val plain = ACCENTS[char] ?: char
                if (plain in ' '..'~') {
                    append(plain)
                } else if (plain.isWhitespace()) {
                    append(' ')
                }
            }
        }
        return ascii.trim().replace(MULTIPLE_SPACES, " ").take(maxLength).trimEnd()
    }

    private val MULTIPLE_SPACES = Regex(" {2,}")

    /** Letras acentuadas (já em maiúscula) e a letra sem acento. O resto fora do ASCII sai. */
    private val ACCENTS: Map<Char, Char> = buildMap {
        "ÀÁÂÃÄÅ".forEach { put(it, 'A') }
        "ÈÉÊË".forEach { put(it, 'E') }
        "ÌÍÎÏ".forEach { put(it, 'I') }
        "ÒÓÔÕÖ".forEach { put(it, 'O') }
        "ÙÚÛÜ".forEach { put(it, 'U') }
        put('Ç', 'C')
        put('Ñ', 'N')
        put('Ý', 'Y')
    }
}
