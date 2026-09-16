package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Orçamento salvo no histórico: um retrato congelado de [quote] (e dos
 * [services] escolhidos, com o preço deles no momento) — editar depois o
 * filamento/impressora/configurações/serviços usados não muda os valores
 * aqui, porque o que já foi cotado para o cliente não deve mudar
 * retroativamente.
 *
 * @property id identificador único, atribuído ao salvar.
 * @property name nome do orçamento; nunca vazio (a UI gera um nome genérico
 *   automaticamente se o usuário deixar em branco ao salvar).
 * @property quote retrato do cálculo no momento em que foi salvo.
 * @property services serviços opcionais escolhidos para esta peça (retrato
 *   do nome/preço no momento em que foi salvo — ver [Service]).
 * @property photoFileName nome do arquivo da foto do produto, se houver
 *   (resolvido pela camada de persistência da UI — não é um caminho
 *   absoluto). Entra no PDF e fica disponível pra download no histórico;
 *   nunca entra no texto de copiar/colar.
 * @property sourceLink link de onde o modelo 3D foi obtido, se houver. Uso
 *   **só interno**: nunca aparece em nenhum export (PDF ou copiar/colar).
 * @property savedAtEpochMillis quando foi salvo (epoch millis).
 * @property client cliente vinculado a este orçamento, se houver. Mesmo
 *   tratamento do [sourceLink] — uso só interno, nunca exportado.
 * @property status andamento do pedido, editável no Histórico. Todo
 *   orçamento nasce [OrderStatus.ORCADO] (o ato de salvar já é o orçamento
 *   "feito"). Uso só interno, nunca exportado.
 */
@Serializable
data class SavedQuote(
    val id: String,
    val name: String,
    val quote: Quote,
    val services: List<Service> = emptyList(),
    val photoFileName: String? = null,
    val sourceLink: String? = null,
    val savedAtEpochMillis: Long,
    val client: Client? = null,
    val status: OrderStatus = OrderStatus.ORCADO,
) {
    /** Total de fato cobrado do cliente: valor de venda + soma dos serviços escolhidos. */
    val totalWithServices: Double
        get() = quote.salePrice + services.sumOf { it.price }
}
