package com.threedreport.app.ui.history

import androidx.compose.runtime.Composable
import com.threedreport.app.ui.components.OverdueExportDialog
import com.threedreport.app.ui.quote.DeliveryDateDialog
import com.threedreport.app.ui.quote.PrintSettingsDialog
import com.threedreport.core.model.PrintSettings

/**
 * Os diálogos que Pedidos e o Catálogo dividem, abertos a partir do [state]: prazo de entrega, configurações de impressão,
 * detalhes, atualizar preço e o aviso de prazo vencido. Cada um é o do kit ([com.threedreport.app.ui.components.AppDialog]).
 */
@Composable
fun HistoryDialogs(state: HistoryUiState, viewModel: QuoteHistoryViewModel) {
    state.deliveryDateEditing?.let { quote ->
        DeliveryDateDialog(
            quoteName = quote.name,
            initial = quote.deliveryDateEpochDay,
            daysLate = viewModel::deliveryDaysLate,
            today = state.today,
            onDismiss = viewModel::cancelEditingDeliveryDate,
            onSave = viewModel::saveDeliveryDate,
        )
    }
    state.details?.let { editing -> QuoteDetailsDialog(editing, viewModel, state.clients, state.today) }
    state.printSettingsEditing?.let { quote ->
        PrintSettingsDialog(
            initial = quote.printSettings ?: PrintSettings(),
            onDismiss = viewModel::cancelEditingPrintSettings,
            onSave = viewModel::savePrintSettings,
        )
    }
    state.pendingExport?.let { pending ->
        OverdueExportDialog(
            overdue = pending.overdue,
            onChangeDate = viewModel::changeDateOfPendingExport,
            onSendAnyway = viewModel::confirmPendingExport,
            onCancel = viewModel::dismissPendingExport,
        )
    }
    state.repricing?.let { RepriceDialog(it, viewModel) }
}
