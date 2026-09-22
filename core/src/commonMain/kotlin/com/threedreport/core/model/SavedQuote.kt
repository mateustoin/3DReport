package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Orçamento salvo no histórico: um retrato congelado de [quote] (e dos
 * [services] escolhidos, com o preço deles no momento) — editar depois o
 * filamento/impressora/configurações/serviços usados não muda os valores
 * aqui, porque o que já foi cotado para o cliente não deve mudar
 * retroativamente. Reabrir e salvar de novo este mesmo orçamento (edição
 * explícita, pela aba Orçamento) é diferente disso — troca o retrato por
 * um novo, mantendo [id]/[savedAtEpochMillis], e marca [lastEditedEpochMillis].
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
 * @property stlFileName nome do arquivo STL do modelo, se houver (mesmo
 *   tratamento do [photoFileName] — resolvido pela camada de persistência,
 *   não é um caminho absoluto). Guardado pra o criador recuperar depois e
 *   reaproveitar numa venda futura da mesma peça — uso só interno, nunca
 *   entra em nenhum export (PDF ou copiar/colar).
 * @property sourceLink link de onde o modelo 3D foi obtido, se houver. Uso
 *   **só interno**: nunca aparece em nenhum export (PDF ou copiar/colar).
 * @property savedAtEpochMillis quando foi salvo (epoch millis).
 * @property client cliente vinculado a este orçamento, se houver. Mesmo
 *   tratamento do [sourceLink] — uso só interno, nunca exportado.
 * @property status andamento do pedido, editável no Histórico. Todo
 *   orçamento nasce [OrderStatus.ORCADO] (o ato de salvar já é o orçamento
 *   "feito"). Uso só interno, nunca exportado.
 * @property lastEditedEpochMillis quando este orçamento foi editado e salvo
 *   de novo pela última vez (ver KDoc acima), ou `null` se nunca foi
 *   editado desde que foi criado. [savedAtEpochMillis] **não muda** numa
 *   edição — continua sendo a data de criação original.
 * @property shippingCost frete cobrado do cliente neste pedido, em R$.
 *   Entra como linha própria no total e nos exports, **nunca embutido no
 *   preço da peça**: não passa pela margem, não multiplica pela quantidade
 *   e não sofre a taxa do canal, porque é um valor repassado, não um
 *   produto seu. `0.0` quando não há frete (retirada, entrega em mãos).
 * @property printSettings configurações de fatiamento usadas pra imprimir
 *   (altura de camada, preenchimento, suporte), se informadas — ver KDoc de
 *   [PrintSettings]. Editável direto no Histórico, sem precisar reabrir a
 *   edição completa do orçamento (mesmo tratamento de [status]).
 */
@Serializable
data class SavedQuote(
    val id: String,
    val name: String,
    val quote: Quote,
    val services: List<Service> = emptyList(),
    val photoFileName: String? = null,
    val stlFileName: String? = null,
    val sourceLink: String? = null,
    val savedAtEpochMillis: Long,
    val client: Client? = null,
    val status: OrderStatus = OrderStatus.ORCADO,
    val lastEditedEpochMillis: Long? = null,
    val printSettings: PrintSettings? = null,
    val shippingCost: Double = 0.0,
) {
    init {
        require(shippingCost >= 0) { "shippingCost não pode ser negativo: $shippingCost" }
    }

    /**
     * Total de fato cobrado do cliente: valor de venda do pedido + serviços escolhidos (estes
     * multiplicados pela quantidade, são trabalho por peça, ver `QuoteResult.servicesTotal`) +
     * [shippingCost].
     */
    val totalWithServices: Double
        get() = quote.salePrice + services.sumOf { it.price } * quote.quantity + shippingCost
}
