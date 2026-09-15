package com.threedreport.app.platform

import com.threedreport.core.model.SavedQuote

/**
 * Gera um PDF simples do orçamento salvo, pra mandar pro cliente: nome, valor
 * de venda e a foto do produto (se houver). Não inclui produção/lucro (uso
 * interno) nem o link do modelo (uso interno).
 *
 * A marca d'água diagonal e o rodapé são independentes um do outro — cada um
 * só é desenhado se seu respectivo texto não for `null`/vazio (ver
 * [com.threedreport.core.model.BrandingSettings.showWatermark]/`showFooter`,
 * resolvidos pelo chamador antes de invocar esta função).
 *
 * @param watermarkText texto da marca d'água diagonal e translúcida, desenhada
 *   por cima de todo o conteúdo (inclusive a foto, pra continuar visível ali).
 * @param footerText texto do rodapé (linha fina + texto centralizado no fim da página).
 */
expect fun renderSavedQuotePdf(
    savedQuote: SavedQuote,
    photoBytes: ByteArray?,
    watermarkText: String?,
    footerText: String?,
): ByteArray
