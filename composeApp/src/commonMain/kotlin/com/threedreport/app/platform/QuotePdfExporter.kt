package com.threedreport.app.platform

import com.threedreport.core.model.SavedQuote

/**
 * Gera um PDF simples do orçamento salvo, pra mandar pro cliente: nome, valor
 * de venda e a foto do produto (se houver). Não inclui produção/lucro (uso
 * interno) nem o link do modelo (uso interno).
 *
 * @param watermarkText marca d'água opcional (personalização do criador,
 *   ver [com.threedreport.core.model.BrandingSettings]); `null`/vazio não desenha
 *   marca d'água nem rodapé. Quando presente, aparece duas vezes: diagonal e
 *   translúcida por cima de todo o conteúdo (inclusive a foto, pra continuar
 *   visível ali) e num rodapé discreto no fim da página.
 */
expect fun renderSavedQuotePdf(savedQuote: SavedQuote, photoBytes: ByteArray?, watermarkText: String?): ByteArray
