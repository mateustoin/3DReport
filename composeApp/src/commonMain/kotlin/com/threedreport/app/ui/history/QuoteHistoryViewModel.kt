package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.QuoteExportItem
import com.threedreport.app.platform.copyToClipboard
import com.threedreport.app.platform.defaultDocumentsDirectory
import com.threedreport.app.platform.renderSavedQuotesPdf
import com.threedreport.app.platform.saveBytesToFile
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel da tela de Histórico: lista os orçamentos salvos e permite
 * excluir, baixar a foto, exportar um orçamento por vez, ou selecionar
 * vários e exportá-los juntos num PDF só (um orçamento por página).
 */
class QuoteHistoryViewModel(
    private val repository: QuoteHistoryRepository,
    private val brandingRepository: BrandingRepository,
) {

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.savedQuotes

    private val copiedIdState = MutableStateFlow<String?>(null)
    val copiedId: StateFlow<String?> = copiedIdState.asStateFlow()

    private val selectedIdsState = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = selectedIdsState.asStateFlow()

    fun delete(id: String) {
        repository.delete(id)
        selectedIdsState.update { it - id }
    }

    fun photoBytes(savedQuote: SavedQuote): ByteArray? = repository.photoBytes(savedQuote)

    fun downloadPhoto(savedQuote: SavedQuote) {
        val bytes = photoBytes(savedQuote) ?: return
        saveBytesToFile(bytes, savedQuote.photoFileName ?: "foto.png")
    }

    fun exportPdf(savedQuote: SavedQuote) {
        val (watermarkText, footerText) = resolveWatermarkAndFooterText()
        val item = QuoteExportItem(savedQuote, photoBytes(savedQuote))
        val pdfBytes = renderSavedQuotesPdf(listOf(item), watermarkText, footerText)
        saveBytesToFile(pdfBytes, "${sanitizeFileName(savedQuote.name)}.pdf", defaultDocumentsDirectory())
    }

    fun copyQuoteToClipboard(savedQuote: SavedQuote) {
        copyToClipboard(savedQuote.toCopyPasteText())
        copiedIdState.value = savedQuote.id
    }

    fun toggleSelection(id: String) {
        selectedIdsState.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selectedIdsState.value = emptySet()
    }

    fun exportSelectedPdf() {
        val selected = savedQuotes.value.filter { it.id in selectedIdsState.value }
        if (selected.isEmpty()) return

        val (watermarkText, footerText) = resolveWatermarkAndFooterText()
        val items = selected.map { QuoteExportItem(it, photoBytes(it)) }
        val pdfBytes = renderSavedQuotesPdf(items, watermarkText, footerText)

        saveBytesToFile(
            pdfBytes,
            "${sanitizeFileName("Orçamentos (${selected.size} itens)")}.pdf",
            defaultDocumentsDirectory(),
        )
        clearSelection()
    }

    private fun resolveWatermarkAndFooterText(): Pair<String?, String?> {
        val branding: BrandingSettings = brandingRepository.branding.value
        val brandName = branding.watermarkText?.takeIf { it.isNotBlank() }
        return brandName?.takeIf { branding.showWatermark } to brandName?.takeIf { branding.showFooter }
    }

    private fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-') it else '_' }.joinToString("")
}
