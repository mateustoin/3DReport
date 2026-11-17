package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Personalização do documento exportado (PDF). Separado de [PricingSettings]
 * porque não é um parâmetro de custo — é sobre a apresentação do documento.
 *
 * Duas naturezas de campo convivem aqui: **identidade** do vendedor ([logoFileName] e contatos,
 * uma só por vendedor, que nunca entra num [QuoteTemplate]) e **apresentação** (os `show*`, o que
 * um template guarda e aplica).
 *
 * @property brandName nome da marca do vendedor. Aparece na marca d'água, no rodapé, no cabeçalho
 *   (quando há logo ou contato) e na imagem quadrada.
 * @property showWatermark exibe [brandName] como marca d'água diagonal no PDF.
 * @property showFooter exibe [brandName] no rodapé do PDF.
 * @property showPrintTime mostra o tempo de impressão do pedido no PDF e no texto de
 *   copiar/colar/WhatsApp. Desligado por padrão: é tempo de máquina, não custo nem margem (não
 *   fere a decisão 19), mas nem todo vendedor quer dar ao cliente o argumento "só 2 h de máquina?"
 *   pra barganhar.
 * @property logoFileName nome do arquivo da logo, dentro da pasta de dados do app (resolvido pela
 *   camada de persistência, não é um caminho absoluto). Vai no cabeçalho do PDF.
 * @property contactWhatsApp/[contactEmail]/[contactInstagram] contato **público** do vendedor, pro
 *   cliente responder. Diferente de `Client.contact`, que é do cliente e só de uso interno
 *   (decisão 37). Texto livre, como a pessoa digitou.
 * @property showBorder desenha uma borda fina em volta da página do PDF.
 * @property quoteValidityDays por quantos dias o preço do orçamento vale, a contar da emissão do PDF
 *   ("Válido até 02/10/2026", decisão 108). Zero tira a linha. Protege o vendedor de um cliente que volta
 *   meses depois com o preço antigo, depois de o filamento subir.
 * @property showClientName "Para: nome do cliente" no PDF. Desligado por padrão: o cliente é dado interno
 *   (decisão 19), e só vai pro documento quando o vendedor escolhe.
 * @property imageBackground fundo da imagem quadrada pro cliente ([ImageBackground]). Nasce [ImageBackground.AUTO].
 *   É do vendedor, como a logo: um [QuoteTemplate] não guarda nem muda este campo.
 */
@Serializable
data class BrandingSettings(
    val brandName: String? = null,
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
    val logoFileName: String? = null,
    val contactWhatsApp: String? = null,
    val contactEmail: String? = null,
    val contactInstagram: String? = null,
    val showBorder: Boolean = false,
    val quoteValidityDays: Int = DEFAULT_VALIDITY_DAYS,
    val showClientName: Boolean = false,
    val imageBackground: ImageBackground = ImageBackground.AUTO,
) {
    init {
        require(quoteValidityDays in 0..MAX_VALIDITY_DAYS) { "quoteValidityDays fora de 0..$MAX_VALIDITY_DAYS: $quoteValidityDays" }
    }

    /** Instagram sempre com "@" na frente, que é como ele é reconhecido; `null` se vazio. */
    val instagramHandle: String?
        get() = contactInstagram?.trim()?.removePrefix("@")?.trim()?.takeIf { it.isNotEmpty() }?.let { "@$it" }

    /**
     * Contatos do vendedor pro cabeçalho do PDF, na ordem WhatsApp, e-mail, Instagram, pulando os vazios. Cada
     * um diz o canal e o valor; quem desenha escolhe o texto (o `core` não escreve frase pra interface).
     */
    val contacts: List<ContactEntry>
        get() = listOfNotNull(
            contactWhatsApp?.trim()?.takeIf { it.isNotEmpty() }?.let { ContactEntry(ContactChannel.WHATSAPP, it) },
            contactEmail?.trim()?.takeIf { it.isNotEmpty() }?.let { ContactEntry(ContactChannel.EMAIL, it) },
            instagramHandle?.let { ContactEntry(ContactChannel.INSTAGRAM, it) },
        )

    /** Se há algo pro cabeçalho do PDF (logo ou contato). Sem isso, a página fica como sempre foi. */
    val hasIdentity: Boolean
        get() = logoFileName != null || contacts.isNotEmpty()

    companion object {
        const val DEFAULT_VALIDITY_DAYS = 7
        const val MAX_VALIDITY_DAYS = 365
    }
}

/** Por onde o cliente pode responder ao vendedor. */
enum class ContactChannel { WHATSAPP, EMAIL, INSTAGRAM }

/** Um contato público do vendedor: o canal e o valor como vai pro documento (o Instagram já com "@"). */
data class ContactEntry(val channel: ContactChannel, val value: String)
