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
) {
    /** Instagram sempre com "@" na frente, que é como ele é reconhecido; `null` se vazio. */
    val instagramHandle: String?
        get() = contactInstagram?.trim()?.removePrefix("@")?.trim()?.takeIf { it.isNotEmpty() }?.let { "@$it" }

    /**
     * Contatos prontos pra exibir, na ordem WhatsApp, e-mail, Instagram, pulando os vazios. É o
     * mesmo texto no cabeçalho do PDF e na prévia em Configurações.
     */
    val contactLines: List<String>
        get() = listOfNotNull(
            contactWhatsApp?.trim()?.takeIf { it.isNotEmpty() }?.let { "WhatsApp $it" },
            contactEmail?.trim()?.takeIf { it.isNotEmpty() },
            instagramHandle,
        )

    /** Se há algo pro cabeçalho do PDF (logo ou contato). Sem isso, a página fica como sempre foi. */
    val hasIdentity: Boolean
        get() = logoFileName != null || contactLines.isNotEmpty()
}
