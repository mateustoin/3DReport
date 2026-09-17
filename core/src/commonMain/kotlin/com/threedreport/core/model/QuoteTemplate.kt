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
 * @property watermarkText texto da marca d'água/rodapé, ou `null`/vazio pra nenhum.
 * @property showWatermark se este preset liga a marca d'água diagonal.
 * @property showFooter se este preset liga o rodapé.
 */
@Serializable
data class QuoteTemplate(
    val id: String,
    val name: String,
    val watermarkText: String? = null,
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
) {
    init {
        require(name.isNotBlank()) { "name não pode ser vazio" }
    }
}
