package com.threedreport.app.platform

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
 * @param photoBytes foto do produto, se houver. Sem foto, a imagem sai só com o texto sobre a cor
 *   da marca, que continua servindo pra mandar o preço.
 * @param unitPriceText linha secundária com o preço por unidade, quando o pedido tem mais de uma
 *   peça; `null` esconde a linha.
 * @param deliveryText prazo de entrega já escrito ("Entrega até 30/09"), em destaque abaixo do preço;
 *   `null` esconde a linha.
 * @param brandText nome da marca do vendedor (mesmo texto da marca d'água do PDF), escrito discreto
 *   no rodapé quando configurado.
 */
expect fun renderQuoteImage(
    title: String,
    priceText: String,
    unitPriceText: String?,
    photoBytes: ByteArray?,
    brandText: String?,
    deliveryText: String? = null,
): ByteArray
