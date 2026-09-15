package com.threedreport.app.ui.history

import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.saveBytesToFile
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.StateFlow

/** ViewModel da tela de Histórico: lista os orçamentos salvos e permite excluir ou baixar a foto. */
class QuoteHistoryViewModel(private val repository: QuoteHistoryRepository) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

    fun delete(id: String) = repository.delete(id)

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        saveBytesToFile(bytes, savedQuote.photoFileName ?: "foto.png")
    }
}
