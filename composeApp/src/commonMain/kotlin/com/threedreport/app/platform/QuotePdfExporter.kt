package com.threedreport.app.platform

import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.SavedQuote

/** Um orçamento salvo a incluir na exportação em PDF, com sua foto (se houver) já carregada em memória. */
data class QuoteExportItem(val savedQuote: SavedQuote, val photoBytes: ByteArray?)

/**
 * Opções de apresentação do PDF que vêm de `BrandingSettings`, já resolvidas pelo chamador. Ficam
 * num objeto à parte, com padrão, em vez de somar mais um parâmetro solto a cada leva: com os
 * padrões, o PDF sai idêntico ao de antes dessas opções existirem.
 *
 * @property showPrintTime mostra "Tempo de impressão" abaixo do prazo (ver
 *   `BrandingSettings.showPrintTime`).
 * @property logoBytes logo do vendedor, pro cabeçalho. Imagem que não decodifica é ignorada.
 * @property brandName nome da marca, mostrado no cabeçalho junto da logo/contato.
 * @property contactLines contato público do vendedor, uma linha por item (ver
 *   `BrandingSettings.contactLines`).
 * @property showBorder borda fina em volta da página.
 * @property showAppSignature "Gerado com 3DReport" no canto inferior direito, com "3DReport" como link
 *   pro site. Nos exports do app vem sempre ligada ([resolvePdfBranding], decisão 91).
 *   Padrão desligado aqui (só quem resolve a partir de `BrandingSettings` liga), pra quem chama
 *   sem opções continuar recebendo o PDF de sempre.
 * @property validityDays "Válido até" a data de emissão mais esses dias; `null` ou zero tira a linha.
 * @property issuedOnEpochDay data de emissão; `null` é hoje (os testes fixam a data).
 * @property showClientName "Para: nome do cliente" abaixo do título.
 *
 * O cabeçalho só aparece com logo ou contato: o nome sozinho já tem a marca d'água e o rodapé.
 */
data class PdfLayoutOptions(
    val showPrintTime: Boolean = false,
    val logoBytes: ByteArray? = null,
    val brandName: String? = null,
    val contactLines: List<String> = emptyList(),
    val showBorder: Boolean = false,
    val showAppSignature: Boolean = false,
    val validityDays: Int? = null,
    val issuedOnEpochDay: Long? = null,
    val showClientName: Boolean = false,
)

/**
 * Gera um PDF pra mandar pro cliente: uma página por item de [items] — nome,
 * valor de venda, o prazo de entrega em destaque (se houver) e a foto do
 * produto (se houver). Não inclui produção/lucro
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
 * @param brandName texto da marca d'água diagonal e translúcida, desenhada
 *   por cima de todo o conteúdo (inclusive a foto, pra continuar visível ali).
 * @param footerText texto do rodapé (linha fina + texto centralizado no fim da página).
 *
 * Cada orçamento sai na moeda em que foi salvo ([SavedQuote.currency], decisão 106).
 */
expect fun renderSavedQuotesPdf(
    items: List<QuoteExportItem>,
    brandName: String?,
    footerText: String?,
    options: PdfLayoutOptions = PdfLayoutOptions(),
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
 * @param brandName ver [renderSavedQuotesPdf].
 * @param footerText ver [renderSavedQuotesPdf].
 */
expect fun renderCatalogPdf(
    items: List<QuoteExportItem>,
    brandName: String?,
    footerText: String?,
    options: PdfLayoutOptions = PdfLayoutOptions(),
): ByteArray

/**
 * Primeira página de [pdf] como PNG, pra "Ver como fica" em Configurações mostrar o PDF de
 * verdade em vez de uma imitação desenhada na tela, que poderia divergir do documento real.
 */
expect fun renderPdfFirstPagePng(pdf: ByteArray, dpi: Float = 90f): ByteArray

/**
 * Tudo o que o PDF precisa de [BrandingSettings], resolvido num lugar só pra o Histórico e a
 * prévia de Configurações nunca montarem o documento de jeitos diferentes. A logo vem à parte
 * porque mora num arquivo, não na configuração.
 */
data class ResolvedPdfBranding(val brandName: String?, val footerText: String?, val options: PdfLayoutOptions)

fun BrandingSettings.resolvePdfBranding(logoBytes: ByteArray?): ResolvedPdfBranding {
    val brandName = brandName?.takeIf { it.isNotBlank() }
    return ResolvedPdfBranding(
        brandName = brandName?.takeIf { showWatermark },
        footerText = brandName?.takeIf { showFooter },
        options = PdfLayoutOptions(
            showPrintTime = showPrintTime,
            logoBytes = logoBytes,
            brandName = brandName,
            contactLines = contactLines,
            showBorder = showBorder,
            // Sempre ligada nos exports do app (decisão 91): opcional, todo mundo desligaria.
            showAppSignature = true,
            validityDays = quoteValidityDays.takeIf { it > 0 },
            showClientName = showClientName,
        ),
    )
}
