package com.threedreport.app.platform

import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.ImageBackground
import com.threedreport.core.model.ImageTone

/**
 * Gera uma imagem quadrada (1080×1080 PNG) do orçamento pra mandar numa conversa de WhatsApp ou
 * postar no status/stories, em vez de anexar um PDF.
 *
 * Existe porque a maior parte da venda de impressão 3D acontece em conversa: um PDF formal é o
 * documento certo pra fechar, mas é pesado demais pro "quanto sai isso aqui?" do dia a dia.
 *
 * Mostra o mesmo que o PDF mostra ao cliente (foto, nome e valor cobrado) e **nada do que é uso
 * interno** — custo de produção, lucro, link do modelo e cliente continuam de fora. Os valores
 * chegam já formatados na moeda escolhida, porque formatação é responsabilidade de quem chama.
 *
 * Leva sempre, no canto inferior direito e bem apagada, a linha "Gerado com 3DReport" (decisão 91),
 * a mesma dos PDFs, só que sem link, porque PNG não tem link.
 *
 * @param texts de onde sai a assinatura "Gerado com 3DReport" (os outros textos já chegam escritos).
 * @param photoBytes foto do produto, se houver. Sem foto, a imagem sai só com o texto, no azul-marinho (ou no fundo
 *   claro, se for o escolhido), que continua servindo pra mandar o preço. A foto opaca (enviada pela pessoa)
 *   preenche a área; a com transparência (miniatura do G-code, captura do 3D) fica inteira, com margem.
 * @param unitPriceText linha secundária com o preço por unidade, quando o pedido tem mais de uma
 *   peça; `null` esconde a linha.
 * @param deliveryText prazo de entrega já escrito ("Entrega até 30/09"), em destaque abaixo do preço;
 *   `null` esconde a linha.
 * @param brandText nome da marca do vendedor com o contato ("Minha Loja · @minhaloja"), escrito discreto na faixa
 *   de baixo, à direita, quando configurado.
 * @param background o fundo escolhido em Documentos pro cliente ([BrandingSettings.imageBackground]). Em
 *   [ImageBackground.AUTO] a imagem escolhe o tom pelo brilho da foto ([resolveImageTone]).
 */
expect fun renderQuoteImage(
    texts: ClientDocumentTexts,
    title: String,
    priceText: String,
    unitPriceText: String?,
    photoBytes: ByteArray?,
    brandText: String?,
    deliveryText: String? = null,
    background: ImageBackground = ImageBackground.AUTO,
): ByteArray

/**
 * O tom da imagem: o fundo escolhido, resolvido pelo brilho médio da peça quando for automático. Sem foto, ou com uma
 * foto que não abre, não há o que medir e o tom é o escuro.
 */
fun resolveImageTone(background: ImageBackground, photoBytes: ByteArray?): ImageTone =
    background.resolve(photoBytes?.let(::averageImageLuminance))

/**
 * Linha de marca da imagem quadrada: "Minha Loja · @minhaloja". Instagram primeiro, porque a imagem
 * é feita pra status e redes; sem Instagram, o WhatsApp. A logo não vai pra imagem: o fundo pode ser
 * o azul-marinho, e a logo típica (escura, com fundo transparente) sumiria nele (decisão 86).
 */
fun BrandingSettings.imageBrandLine(): String? =
    listOfNotNull(
        brandName?.trim()?.takeIf { it.isNotEmpty() },
        instagramHandle ?: contactWhatsApp?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ").ifEmpty { null }
