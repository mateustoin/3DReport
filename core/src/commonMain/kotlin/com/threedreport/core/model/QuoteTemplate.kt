package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Foto nomeada de uma configuração de marca d'água/rodapé, pra guardar e
 * voltar a ela depois sem reconfigurar o texto toda vez. Criado a partir do
 * [BrandingSettings] ativo em Configurações ("Salvar como template") e
 * "Carregar" copia estes campos de volta pra dentro dele (ver
 * [com.threedreport.app.data.TemplateRepository], módulo `composeApp`) — o
 * template em si não é usado direto nos exports.
 *
 * @property id identificador único, atribuído ao criar.
 * @property name nome pra identificar o template (ex.: "Formal", "Simples").
 * @property brandName nome da marca na marca d'água/rodapé, ou `null`/vazio pra nenhum.
 * @property showWatermark se este preset liga a marca d'água diagonal.
 * @property showFooter se este preset liga o rodapé.
 * @property showPrintTime se este preset mostra o tempo de impressão (ver [BrandingSettings.showPrintTime]).
 * @property showBorder se este preset desenha a borda da página.
 *
 * Logo e contato do vendedor **não** fazem parte do template: são a identidade de quem vende, uma
 * só, e aplicar um template não deve apagá-los (ver [BrandingSettings]).
 */
@Serializable
data class QuoteTemplate(
    val id: String,
    val name: String,
    val brandName: String? = null,
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
    val showBorder: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
    }
}
