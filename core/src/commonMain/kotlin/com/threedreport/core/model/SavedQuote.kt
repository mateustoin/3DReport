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
 * @property services serviços cobrados neste pedido (retrato do nome, do
 *   valor e da forma de cobrança no momento em que foi salvo, ver [QuoteService]).
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
 * @property deliveryDateEpochDay prazo de entrega prometido ao cliente, em dias desde 01/01/1970
 *   (uma data de calendário, e não um instante: guardar millis faria a data mudar de dia
 *   conforme o fuso). Ao contrário de [client]/[sourceLink], **entra nos exports** (PDF, imagem,
 *   copiar/colar): é informação pro cliente, e não custo nem margem (decisão 19). Data fixa, e não
 *   "N dias após aprovar", porque cada vendedor conta prazo de um jeito; em troca, envelhece, e é
 *   por isso que duplicar um orçamento não copia a data. `null` quando não há prazo.
 * @property kind se é pedido ou produto do catálogo (decisão 101). Produto guarda [client],
 *   [shippingCost] e [deliveryDateEpochDay] vazios e ignora [status]; quem decide o que conta como
 *   pedido é [isOrder].
 * @property sourceProductId `id` do produto de onde este pedido nasceu pelo "Vender", ou `null`.
 *   Continua apontando pro id mesmo se o produto for excluído depois.
 * @property category categoria do produto no catálogo ("Chaveiros", "Decoração"), texto livre, ou
 *   `null` (decisão 102). Só produto guarda: serve pra filtrar a lista e separar o catálogo em PDF
 *   em seções, e pedido não tem catálogo.
 * @property soldAtCatalogPrice se o pedido saiu pelo preço anunciado do produto, pelo "Vender", sem
 *   ninguém digitar outro preço (decisão 103). O [Quote] guarda o anunciado como preço fechado e o
 *   calculado em [Quote.tableSalePrice], mas isso não é negociação com o cliente: fica fora dos
 *   números de desconto do Dashboard (ver [isNegotiatedWithClient]).
 * @property number número sequencial do orçamento ("#0042", ver [displayNumber]), pra conversar com o
 *   cliente e achar o pedido no PDF e no WhatsApp (decisão 106). Atribuído ao salvar; `0` = sem número.
 * @property statusHistory cada mudança de [status] com o momento em que aconteceu (decisão 106), em
 *   ordem. É o que dá a data da venda ([soldAtEpochMillis]) e da entrega, que a data de criação não
 *   dá: um orçamento de agosto aprovado em setembro é venda de setembro.
 * @property currency moeda em que o orçamento foi feito. Os valores são dessa moeda; trocar a moeda
 *   padrão em Configurações não re-rotula o que já foi salvo.
 */
@Serializable
data class SavedQuote(
    val id: String,
    val name: String,
    val quote: Quote,
    val services: List<QuoteService> = emptyList(),
    val photoFileName: String? = null,
    val stlFileName: String? = null,
    val sourceLink: String? = null,
    val savedAtEpochMillis: Long,
    val client: Client? = null,
    val status: OrderStatus = OrderStatus.ORCADO,
    val lastEditedEpochMillis: Long? = null,
    val shippingCost: Double = 0.0,
    val deliveryDateEpochDay: Long? = null,
    val kind: QuoteKind = QuoteKind.ORDER,
    val sourceProductId: String? = null,
    val category: String? = null,
    val soldAtCatalogPrice: Boolean = false,
    val number: Int = 0,
    val statusHistory: List<StatusChange> = emptyList(),
    val currency: Currency = Currency.BRL,
) {
    init {
        require(shippingCost >= 0) { "shippingCost não pode ser negativo: $shippingCost" }
        require(number >= 0) { "number não pode ser negativo: $number" }
    }

    /** Número pra mostrar ao cliente ("#0042"), ou `null` sem número. */
    val displayNumber: String?
        get() = if (number > 0) "#" + number.toString().padStart(4, '0') else null

    /**
     * Configurações de fatiamento da primeira impressão (ver [PrintJob.settings]). O Histórico da 2.0
     * edita uma impressão por pedido; com várias impressões, cada uma tem as suas.
     */
    val printSettings: PrintSettings?
        get() = quote.prints.first().job.settings

    /** O mesmo pedido com [settings] na primeira impressão (ver [printSettings]). */
    fun withPrintSettings(settings: PrintSettings?): SavedQuote {
        val first = quote.prints.first()
        val updated = first.copy(job = first.job.copy(settings = settings?.takeUnless { it.isEmpty }))
        return copy(quote = quote.copy(prints = listOf(updated) + quote.prints.drop(1)))
    }

    /**
     * O mesmo pedido em [newStatus], registrando a mudança em [statusHistory] com o momento [atEpochMillis].
     * Sem mudança de status, devolve o próprio pedido (nada a registrar).
     */
    fun withStatus(newStatus: OrderStatus, atEpochMillis: Long): SavedQuote =
        if (newStatus == status) this else copy(status = newStatus, statusHistory = statusHistory + StatusChange(newStatus, atEpochMillis))

    /**
     * Quando o pedido foi fechado: a última vez que ele entrou num status de venda ([OrderStatus.isSold])
     * vindo de fora dele. `null` enquanto não é venda. Pedido sem histórico (salvo já vendido) usa a data
     * de criação.
     */
    val soldAtEpochMillis: Long?
        get() {
            if (!status.isSold) return null
            var soldAt: Long? = null
            var wasSold = false
            for (change in statusHistory) {
                if (change.status.isSold && !wasSold) soldAt = change.atEpochMillis
                wasSold = change.status.isSold
            }
            return soldAt ?: savedAtEpochMillis
        }

    /** Quando a impressão terminou (primeira vez em Pronto ou Entregue), ou `null` se ainda não terminou. */
    val printedAtEpochMillis: Long?
        get() = if (status != OrderStatus.PRONTO && status != OrderStatus.ENTREGUE) {
            null
        } else {
            statusHistory.firstOrNull { it.status == OrderStatus.PRONTO || it.status == OrderStatus.ENTREGUE }?.atEpochMillis
                ?: savedAtEpochMillis
        }

    /** Quando o pedido foi entregue (a última vez que foi pra Entregue), ou `null` se não foi. */
    val deliveredAtEpochMillis: Long?
        get() = if (status != OrderStatus.ENTREGUE) {
            null
        } else {
            statusHistory.lastOrNull { it.status == OrderStatus.ENTREGUE }?.atEpochMillis ?: savedAtEpochMillis
        }

    /** Soma dos serviços cobrados (os por peça vezes a quantidade, os por pedido uma vez, ver [QuoteService.total]). */
    val servicesTotal: Double
        get() = services.sumOf { it.total(quote.quantity) }

    /**
     * Total de fato cobrado do cliente: valor de venda do pedido + serviços (os por peça
     * multiplicados pela quantidade, os por pedido uma vez só, ver [QuoteService.total]) +
     * [shippingCost].
     */
    val totalWithServices: Double
        get() = quote.salePrice + servicesTotal + shippingCost

    /**
     * Se é pedido (e não produto do catálogo). É a única regra de "o que é pedido" (decisão 101),
     * no mesmo espírito de [OrderStatus.isSold]: Kanban, Dashboard, fila de impressão, horas de
     * manutenção, prazo e filtro de status só olham pra pedidos, e todos perguntam aqui.
     */
    val isOrder: Boolean
        get() = kind == QuoteKind.ORDER

    /**
     * Se o preço foi combinado com o cliente, e não só tirado do catálogo. É o que o Dashboard conta
     * como negociação ([Quote.isNegotiated] sozinho também pega o preço anunciado do catálogo).
     */
    val isNegotiatedWithClient: Boolean
        get() = quote.isNegotiated && !soldAtCatalogPrice

    /** [Quote.negotiatedDiscount] só quando houve negociação com o cliente; zero no preço do catálogo. */
    val clientDiscount: Double
        get() = if (isNegotiatedWithClient) quote.negotiatedDiscount else 0.0

    /** Tempo de máquina do pedido inteiro, somando todas as impressões (ver [Quote.totalPrintTimeMinutes]). */
    val totalPrintTimeMinutes: Double
        get() = quote.totalPrintTimeMinutes

    /**
     * Se o prazo já passou em [todayEpochDay] sem o pedido ter sido entregue. Um orçamento ainda
     * [OrderStatus.ORCADO] com prazo vencido não é atraso de produção: é um orçamento parado que
     * precisa de data nova antes de ser reenviado (a tela trata os dois casos de forma diferente).
     */
    fun isDeliveryOverdue(todayEpochDay: Long): Boolean {
        if (!isOrder) return false
        val deadline = deliveryDateEpochDay ?: return false
        return deadline < todayEpochDay && status != OrderStatus.ENTREGUE && status != OrderStatus.CANCELADO
    }

    /** Se [name] foi gerado pelo app porque o campo ficou em branco (ver [AUTO_NAME_PREFIX]). */
    val hasAutoName: Boolean
        get() = AUTO_NAME_PATTERN.matches(name)

    companion object {
        /**
         * Começo do nome que o app dá quando o campo fica em branco, seguido de data e hora
         * ("Orçamento - 24/09/2026 14:30"). Fica aqui, e não só em quem salva, porque o ranking do
         * Dashboard precisa reconhecer esses nomes pra não tratar cada um como uma peça diferente.
         */
        const val AUTO_NAME_PREFIX = "Orçamento - "

        private val AUTO_NAME_PATTERN = Regex("""^Orçamento - \d{2}/\d{2}/\d{4} \d{2}:\d{2}$""")
    }
}
