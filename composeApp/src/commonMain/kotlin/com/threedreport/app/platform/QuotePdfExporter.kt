package com.threedreport.app.platform

import com.threedreport.core.model.Currency
import com.threedreport.core.model.SavedQuote

/** Um orçamento salvo a incluir na exportação em PDF, com sua foto (se houver) já carregada em memória. */
data class QuoteExportItem(val savedQuote: SavedQuote, val photoBytes: ByteArray?)

/**
 * Gera um PDF pra mandar pro cliente: uma página por item de [items] — nome,
 * valor de venda e a foto do produto (se houver). Não inclui produção/lucro
 * (uso interno) nem o link do modelo (uso interno). Um único item produz um
 * PDF de uma página, igual ao export individual de um orçamento; vários
 * itens produzem um PDF compilado, um orçamento por página, na ordem dada.
 *
 * A marca d'água diagonal e o rodapé são independentes um do outro — cada um
 * só é desenhado se seu respectivo texto não for `null`/vazio (ver
 * [com.threedreport.core.model.BrandingSettings.showWatermark]/`showFooter`,
 * resolvidos pelo chamador antes de invocar esta função) — e aparecem em
 * todas as páginas quando presentes.
 *
 * @param watermarkText texto da marca d'água diagonal e translúcida, desenhada
 *   por cima de todo o conteúdo (inclusive a foto, pra continuar visível ali).
 * @param footerText texto do rodapé (linha fina + texto centralizado no fim da página).
 */
expect fun renderSavedQuotesPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency = Currency.BRL,
): ByteArray

/**
 * Gera um catálogo em PDF pra divulgação (mandar pro cliente, postar em
 * grupo de venda): uma grade com foto + nome + valor de venda de cada item
 * de [items], vários por página (ao contrário de [renderSavedQuotesPdf],
 * que é um orçamento por página, no formato de documento formal). Mesma
 * regra de conteúdo do export normal: sem produção/lucro (uso interno) nem
 * link do modelo. Item sem foto aparece só com nome + preço, sem quebrar o
 * layout da grade.
 *
 * @param watermarkText ver [renderSavedQuotesPdf].
 * @param footerText ver [renderSavedQuotesPdf].
 */
expect fun renderCatalogPdf(
    items: List<QuoteExportItem>,
    watermarkText: String?,
    footerText: String?,
    currency: Currency = Currency.BRL,
): ByteArray
