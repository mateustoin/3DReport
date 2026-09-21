package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.Service
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
     */
    fun save(
        name: String,
        quote: Quote,
        services: List<Service>,
        photo: PickedFile?,
        stlFile: PickedFile? = null,
        sourceLink: String?,
        client: Client? = null,
    ): SavedQuote

    fun delete(id: String)

    /**
     * Reabre e salva de novo o orçamento [id] (edição explícita, pela aba Orçamento) — troca o
     * retrato por um novo com os valores atuais, preservando [SavedQuote.savedAtEpochMillis] (data
     * de criação original) e marcando [SavedQuote.lastEditedEpochMillis]. `null` de [photo]/[stlFile]
     * remove o anexo existente; não-nulo substitui (mesmo que os bytes sejam os mesmos de antes).
     * Retorna `null` sem fazer nada se [id] não existir.
     */
    fun update(
        id: String,
        name: String,
        quote: Quote,
        services: List<Service>,
        photo: PickedFile?,
        stlFile: PickedFile?,
        sourceLink: String?,
        client: Client?,
    ): SavedQuote?

    /** Atualiza o andamento do pedido [id] pra [status]. Não faz nada se [id] não existir. */
    fun updateStatus(id: String, status: OrderStatus)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?

    /** Bytes do arquivo STL de [savedQuote], ou `null` se não houver STL salvo. */
    fun stlBytes(savedQuote: SavedQuote): ByteArray?
}
