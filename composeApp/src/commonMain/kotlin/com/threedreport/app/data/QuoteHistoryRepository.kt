package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.QuoteService
import kotlinx.coroutines.flow.StateFlow

/**
 * Histórico de orçamentos salvos. Cada [SavedQuote] é um retrato congelado
 * do [Quote] (e dos serviços escolhidos) no momento em que foi salvo (ver
 * KDoc de [SavedQuote]).
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma): metadados em JSON e a foto (se houver) como arquivo à parte.
 */
expect class QuoteHistoryRepository() {
    val savedQuotes: StateFlow<List<SavedQuote>>

    /**
     * Salva [quote] com [name] (gera um nome genérico se vazio), os
     * [services] escolhidos (retrato do preço no momento), [photo],
     * [stlFile], [sourceLink] e [client] opcionais. Nasce com
     * [OrderStatus.ORCADO].
     *
     * [photoReferenceFileName]/[stlReferenceFileName]: quando não-nulo (ex.: duplicando um
     * orçamento cuja foto/STL não mudou), reaproveita esse arquivo já existente em vez de gravar
     * [photo]/[stlFile] de novo em disco — evita duplicar o mesmo arquivo de imagem/modelo a cada
     * duplicação. Ignorado se o [photo]/[stlFile] correspondente for `null`.
     *
     * [kind] = [QuoteKind.PRODUCT] salva um produto do catálogo (decisão 101): [client],
     * [shippingCost] e [deliveryDateEpochDay] são descartados mesmo se vierem preenchidos, porque
     * produto não tem venda. [sourceProductId] é o produto de onde um pedido nasceu pelo "Vender".
     * [category] é só de produto (decisão 102): num pedido, é descartada.
     */
    fun save(
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        photoReferenceFileName: String? = null,
        stlFile: PickedFile? = null,
        stlReferenceFileName: String? = null,
        sourceLink: String?,
        client: Client? = null,
        printSettings: PrintSettings? = null,
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        kind: QuoteKind = QuoteKind.ORDER,
        sourceProductId: String? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
    ): SavedQuote

    fun delete(id: String)

    /**
     * Reabre e salva de novo o orçamento [id] (edição explícita, pela aba Orçamento) — troca o
     * retrato por um novo com os valores atuais, preservando [SavedQuote.savedAtEpochMillis] (data
     * de criação original) e marcando [SavedQuote.lastEditedEpochMillis]. `null` de [photo]/[stlFile]
     * remove o anexo existente; não-nulo substitui. [photoReferenceFileName]/[stlReferenceFileName]
     * (ver [save]) evitam regravar o arquivo em disco quando o anexo não mudou desde que foi
     * carregado pra edição. Retorna `null` sem fazer nada se [id] não existir. O
     * [SavedQuote.kind] não muda por aqui (produto continua produto, com cliente, frete e prazo
     * descartados); mudar de tipo é pelo [convertToOrder].
     */
    fun update(
        id: String,
        name: String,
        quote: Quote,
        services: List<QuoteService>,
        photo: PickedFile?,
        photoReferenceFileName: String? = null,
        stlFile: PickedFile?,
        stlReferenceFileName: String? = null,
        sourceLink: String?,
        client: Client?,
        printSettings: PrintSettings? = null,
        shippingCost: Double = 0.0,
        deliveryDateEpochDay: Long? = null,
        category: String? = null,
        soldAtCatalogPrice: Boolean = false,
    ): SavedQuote?

    /**
     * "Transformar em pedido" (decisão 101): o produto [id] passa a ser pedido, nascendo
     * [OrderStatus.ORCADO] como qualquer orçamento salvo, com
     * [SavedQuote.savedAtEpochMillis] no momento da conversão. Pra quem salvou no lugar errado; vender
     * um produto de verdade é outro caminho, que cria um pedido novo e deixa o produto no catálogo.
     * Não faz nada se [id] não existir ou já for pedido.
     */
    fun convertToOrder(id: String)

    /**
     * "Atualizar preço" de um produto do catálogo (decisão 102): troca só o [quote] recalculado com
     * os cadastros de hoje e marca [SavedQuote.lastEditedEpochMillis], que passa a ser a data do
     * preço. Serviços, anexos e o resto ficam como estão. Não faz nada se [id] não existir ou for
     * pedido, que é retrato congelado da venda.
     */
    fun updateQuote(id: String, quote: Quote)

    /** Atualiza o andamento do pedido [id] pra [status]. Não faz nada se [id] não existir. */
    fun updateStatus(id: String, status: OrderStatus)

    /**
     * Atualiza só as configurações de impressão do pedido [id] pra [printSettings] (`null` remove),
     * sem tocar em mais nada — edição rápida direto no Histórico, mesmo tratamento de [updateStatus].
     * Não faz nada se [id] não existir.
     */
    fun updatePrintSettings(id: String, printSettings: PrintSettings?)

    /**
     * Troca só o prazo de entrega do pedido [id] (`null` remove), sem tocar em mais nada — mesmo
     * tratamento de [updatePrintSettings]. É o caminho pra atualizar um prazo que venceu enquanto
     * o cliente pensava, sem abrir a edição completa. Não faz nada se [id] não existir.
     */
    fun updateDeliveryDate(id: String, deliveryDateEpochDay: Long?)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?

    /** Bytes do arquivo STL de [savedQuote], ou `null` se não houver STL salvo. */
    fun stlBytes(savedQuote: SavedQuote): ByteArray?
}
