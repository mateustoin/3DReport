package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Orçamento salvo no histórico: um retrato congelado de [quote] no momento em
 * que foi salvo — editar depois o filamento/impressora/configurações usados
 * não muda os valores aqui, porque o que já foi cotado para o cliente não
 * deve mudar retroativamente.
 *
 * @property id identificador único, atribuído ao salvar.
 * @property name nome do orçamento; nunca vazio (a UI gera um nome genérico
 *   automaticamente se o usuário deixar em branco ao salvar).
 * @property quote retrato do cálculo no momento em que foi salvo.
 * @property photoFileName nome do arquivo da foto do produto, se houver
 *   (resolvido pela camada de persistência da UI — não é um caminho
 *   absoluto). Entra no PDF e fica disponível pra download no histórico;
 *   nunca entra no texto de copiar/colar.
 * @property sourceLink link de onde o modelo 3D foi obtido, se houver. Uso
 *   **só interno**: nunca aparece em nenhum export (PDF ou copiar/colar).
 * @property savedAtEpochMillis quando foi salvo (epoch millis).
 */
@Serializable
data class SavedQuote(
    val id: String,
    val name: String,
    val quote: Quote,
    val photoFileName: String? = null,
    val sourceLink: String? = null,
    val savedAtEpochMillis: Long,
)
