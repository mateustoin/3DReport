package com.threedreport.app.platform

import com.threedreport.core.model.SavedQuote

/**
 * Gera um PDF simples do orçamento salvo, pra mandar pro cliente: nome, valor
 * de venda e a foto do produto (se houver). Não inclui produção/lucro (uso
 * interno) nem o link do modelo (uso interno).
 *
 * @param watermarkText marca d'água opcional (personalização do criador,
 *   ver [com.threedreport.core.model.BrandingSettings]); `null`/vazio não desenha nenhuma.
 */
expect fun renderSavedQuotePdf(savedQuote: SavedQuote, photoBytes: ByteArray?, watermarkText: String?): ByteArray
