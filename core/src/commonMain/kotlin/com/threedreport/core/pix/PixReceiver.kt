package com.threedreport.core.pix

import com.threedreport.core.model.SavedQuote

/**
 * O que falta (ou está errado) nos dados do Pix em Configurações. A tela escolhe a frase de cada um.
 * [KEY_INVALID_CHARACTERS]: a chave tem acento ou outro caractere fora do ASCII, que o BR Code não aceita.
 */
enum class PixSetupIssue { KEY_MISSING, KEY_INVALID_CHARACTERS, KEY_TOO_LONG, NAME_MISSING, CITY_MISSING }

/**
 * Quem recebe o Pix, já pronto pro código: chave normalizada, nome e cidade limpos (ver [PixPayload.sanitizeName]).
 * Vem de [com.threedreport.core.model.BrandingSettings.pixReceiver], que só existe com os três preenchidos.
 */
data class PixReceiver(val key: PixKey, val name: String, val city: String) {

    /** O código copia e cola de [amount] ([PixPayload]), com o identificador do pedido [orderNumber] (`0` = sem número). */
    fun code(amount: Double?, orderNumber: Int = 0): String =
        PixPayload.build(key = key.value, merchantName = name, merchantCity = city, amount = amount, txid = PixPayload.txidFor(orderNumber))

    /**
     * O código do pedido [order] pelo valor que ele cobra agora ([SavedQuote.pixCharge]: o sinal pendente ou o que
     * falta), ou `null` quando o pedido não tem Pix (pago, outra moeda, produto do catálogo).
     */
    fun codeFor(order: SavedQuote): String? = order.pixCharge?.let { code(it.amount, order.number) }

    companion object {
        /** O que falta pra montar o Pix com [rawKey], [rawName] e [rawCity] como estão em Configurações. Vazio = pronto. */
        fun issuesOf(rawKey: String?, rawName: String?, rawCity: String?): Set<PixSetupIssue> = buildSet {
            val key = PixKey.parse(rawKey)
            if (key == null) {
                add(PixSetupIssue.KEY_MISSING)
            } else if (!key.isAscii) {
                add(PixSetupIssue.KEY_INVALID_CHARACTERS)
            } else if (!key.fitsInPayload) {
                add(PixSetupIssue.KEY_TOO_LONG)
            }
            if (PixPayload.sanitizeName(rawName.orEmpty()).isEmpty()) add(PixSetupIssue.NAME_MISSING)
            if (PixPayload.sanitizeCity(rawCity.orEmpty()).isEmpty()) add(PixSetupIssue.CITY_MISSING)
        }

        /** Quem recebe, ou `null` se falta alguma coisa ([issuesOf]). */
        fun of(rawKey: String?, rawName: String?, rawCity: String?): PixReceiver? {
            if (issuesOf(rawKey, rawName, rawCity).isNotEmpty()) return null
            return PixReceiver(
                key = PixKey.parse(rawKey)!!,
                name = PixPayload.sanitizeName(rawName.orEmpty()),
                city = PixPayload.sanitizeCity(rawCity.orEmpty()),
            )
        }
    }
}
