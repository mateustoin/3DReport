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
     * [sourceLink] e [client] opcionais. Nasce com [OrderStatus.ORCADO].
     */
    fun save(
        name: String,
        quote: Quote,
        services: List<Service>,
        photo: PickedFile?,
        sourceLink: String?,
        client: Client? = null,
    ): SavedQuote

    fun delete(id: String)

    /** Atualiza o andamento do pedido [id] pra [status]. Não faz nada se [id] não existir. */
    fun updateStatus(id: String, status: OrderStatus)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?
}
