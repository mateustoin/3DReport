package com.threedreport.app.ui.history

import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.SavedQuote

/**
 * Tudo o que dá pra fazer com um orçamento salvo. Um objeto só, montado por [quoteActionsFor], pra tabela, o kanban, a
 * grade e a lista oferecerem exatamente as mesmas ações. Ação `null` não se aplica a este orçamento e some do menu.
 */
class QuoteActions(
    val onEdit: () -> Unit,
    val onEditDetails: () -> Unit,
    val onDuplicate: () -> Unit,
    val onSell: (() -> Unit)?,
    val onCopyToCatalog: (() -> Unit)?,
    val onConvertToOrder: (() -> Unit)?,
    val onReprice: (() -> Unit)?,
    val onStatusChange: ((OrderStatus) -> Unit)?,
    val onEditDeliveryDate: (() -> Unit)?,
    /** `null` com várias impressões: cada uma tem as suas, e elas se editam no Orçamento (decisão 114). */
    val onPrintSettings: (() -> Unit)?,
    val onDownloadPhoto: (() -> Unit)?,
    val onDownloadModelFile: (() -> Unit)?,
    val onExportPdf: () -> Unit,
    val onCopy: () -> Unit,
    val onOpenWhatsApp: () -> Unit,
    val onSaveImage: () -> Unit,
    /** Entra no modo de seleção com este orçamento marcado; `null` onde não há exportação em lote (o kanban). */
    val onSelect: (() -> Unit)?,
    val onDelete: () -> Unit,
)

/** Pra onde as ações que abrem o Orçamento levam (o navegador do app); as que a tela não usa ficam `null`. */
class HistoryNavigation(
    val onEdit: (SavedQuote) -> Unit,
    val onDuplicate: (SavedQuote) -> Unit,
    val onSell: ((SavedQuote) -> Unit)? = null,
    val onCopyToCatalog: ((SavedQuote) -> Unit)? = null,
)

/** Liga as ações de [row] ao [viewModel] e à [navigation]. Só monta; quem decide o que se aplica é o estado da linha. */
fun quoteActionsFor(row: HistoryRow, viewModel: QuoteHistoryViewModel, navigation: HistoryNavigation, selectable: Boolean): QuoteActions {
    val quote = row.quote
    val isOrder = quote.isOrder
    return QuoteActions(
        onEdit = { navigation.onEdit(quote) },
        onEditDetails = { viewModel.startEditingDetails(quote) },
        onDuplicate = { navigation.onDuplicate(quote) },
        onSell = navigation.onSell?.takeUnless { isOrder }?.let { sell -> { sell(quote) } },
        onCopyToCatalog = navigation.onCopyToCatalog?.takeIf { isOrder }?.let { copy -> { copy(quote) } },
        onConvertToOrder = if (row.canConvertToOrder) ({ viewModel.convertToOrder(quote.id) }) else null,
        onReprice = if (row.canReprice) ({ viewModel.startRepricing(quote) }) else null,
        onStatusChange = if (isOrder) ({ status -> viewModel.updateStatus(quote.id, status) }) else null,
        onEditDeliveryDate = if (isOrder) ({ viewModel.startEditingDeliveryDate(quote) }) else null,
        onPrintSettings = if (row.canEditPrintSettings) ({ viewModel.startEditingPrintSettings(quote) }) else null,
        onDownloadPhoto = quote.photoFileName?.let { { viewModel.downloadPhoto(quote) } },
        onDownloadModelFile = quote.modelFileName?.let { { viewModel.downloadModelFile(quote) } },
        onExportPdf = { viewModel.exportPdf(quote) },
        onCopy = { viewModel.copyQuoteToClipboard(quote) },
        onOpenWhatsApp = { viewModel.openInWhatsApp(quote) },
        onSaveImage = { viewModel.saveShareableImage(quote) },
        onSelect = if (selectable) ({ viewModel.startSelection(quote.id) }) else null,
        onDelete = { viewModel.delete(quote.id) },
    )
}
