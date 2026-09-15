package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.copyToClipboard
import com.threedreport.app.platform.renderSavedQuotePdf
import com.threedreport.app.platform.saveBytesToFile
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel da tela de Histórico: lista os orçamentos salvos e permite excluir, baixar a foto ou exportar. */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

    private val copiedIdState = MutableStateFlow<String?>(null)
    val copiedId: StateFlow<String?> = copiedIdState.asStateFlow()

    fun delete(id: String) = repository.delete(id)

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        saveBytesToFile(bytes, savedQuote.photoFileName ?: "foto.png")
    }

    fun exportPdf(savedQuote: SavedQuote) {
        val watermarkText = brandingRepository.branding.value.watermarkText
        val pdfBytes = renderSavedQuotePdf(savedQuote, photoBytes(savedQuote), watermarkText)
        saveBytesToFile(pdfBytes, "${sanitizeFileName(savedQuote.name)}.pdf")
    }

    fun copyQuoteToClipboard(savedQuote: SavedQuote) {
        copyToClipboard(savedQuote.toCopyPasteText())
        copiedIdState.value = savedQuote.id
    }

    private fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("")
}
