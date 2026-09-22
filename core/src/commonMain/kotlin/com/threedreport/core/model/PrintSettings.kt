package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Configurações de fatiamento usadas pra imprimir a peça — não entram no cálculo de preço, servem
 * só pra o criador recuperar depois no Histórico e replicar o mesmo padrão numa venda futura da
 * mesma peça (ex.: "usei 0.2mm com 15% de preenchimento grid e suporte da última vez, era bom"),
 * sem precisar lembrar de cabeça ou reabrir o fatiador pra descobrir de novo. Uso só interno: nunca
 * entra em nenhum export (PDF ou copiar/colar), mesmo tratamento de [SavedQuote.sourceLink]/
 * [SavedQuote.client].
 *
 * Todo campo é opcional e pode vir `null` — tanto de importação automática do G-code (nem todo
 * fatiador grava todos esses dados num formato reconhecido, ver `GCodeMetadataParser`) quanto de
 * preenchimento manual (o criador pode não saber ou não achar relevante registrar algum deles).
 *
 * @property layerHeightMm altura de camada, em milímetros (ex.: 0.2).
 * @property infillPercentage porcentagem de preenchimento, de 0 a 100 (ex.: 15.0 = 15%).
 * @property infillPattern padrão de preenchimento (ex.: "grid", "gyroid", "honeycomb") — texto
 *   livre, sem validação de valores possíveis (varia entre fatiadores).
 * @property supportsEnabled se a peça foi impressa com suporte. `null` = não informado (diferente
 *   de `false`, que significa "informado que não usou suporte").
 */
@Serializable
data class PrintSettings(
    val layerHeightMm: Double? = null,
    val infillPercentage: Double? = null,
    val infillPattern: String? = null,
    val supportsEnabled: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = layerHeightMm == null && infillPercentage == null && infillPattern == null && supportsEnabled == null
}
