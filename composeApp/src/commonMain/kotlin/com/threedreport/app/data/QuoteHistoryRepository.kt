package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.StateFlow

/**
 * Histórico de orçamentos salvos. Cada [SavedQuote] é um retrato congelado
 * do [Quote] no momento em que foi salvo (ver KDoc de [SavedQuote]).
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma): metadados em JSON e a foto (se houver) como arquivo à parte.
 */
expect class QuoteHistoryRepository() {
    val savedQuotes: StateFlow<List<SavedQuote>>

    /** Salva [quote] com [name] (gera um nome genérico se vazio), [photo] e [sourceLink] opcionais. */
    fun save(name: String, quote: Quote, photo: PickedFile?, sourceLink: String?): SavedQuote

    fun delete(id: String)

    /** Bytes da foto de [savedQuote], ou `null` se não houver foto salva. */
    fun photoBytes(savedQuote: SavedQuote): ByteArray?
}
