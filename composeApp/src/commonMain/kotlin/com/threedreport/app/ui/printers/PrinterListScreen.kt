package com.threedreport.app.ui.printers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.minutesToHoursText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.report.PrinterQueueEntry

/** Tela de Impressoras: perfis salvos, escolhidos depois na tela de Orçamento. */
@Composable
fun PrinterListScreen(viewModel: PrinterListViewModel, modifier: Modifier = Modifier) {
    val printers by viewModel.printers.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val form by viewModel.form.collectAsState()
    var pendingDelete by remember { mutableStateOf<PrinterProfile?>(null) }
    var showPresetPicker by remember { mutableStateOf(false) }
    val queueByPrinterId = remember(printers, savedQuotes) {
        viewModel.printQueue(printers, savedQuotes).associateBy { it.printer.id }
    }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Impressoras", style = MaterialTheme.typography.titleLarge)

        if (printers.isEmpty()) {
            EmptyState("Nenhuma impressora cadastrada ainda. Cadastre a primeira abaixo.")
        }

        printers.forEach { printer ->
            PrinterRow(
                printer = printer,
                queueEntry = queueByPrinterId[printer.id],
                onEdit = { viewModel.startEdit(printer) },
                onDelete = { pendingDelete = printer },
            )
        }

        val currentForm = form
        if (currentForm == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::startAdd) { Text("+ Nova impressora") }
                TextButton(onClick = { showPresetPicker = true }) { Text("Escolher da lista") }
            }
        } else {
            HorizontalDivider()
            PrinterForm(
                form = currentForm,
                onChange = viewModel::updateForm,
                onSave = viewModel::save,
                onCancel = viewModel::cancelEdit,
            )
        }
    }

    pendingDelete?.let { printer ->
        ConfirmDialog(
            title = "Excluir impressora?",
            message = "\"${printer.name}\" será removida do catálogo. Essa ação não pode ser desfeita.",
            onConfirm = {
                viewModel.delete(printer.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showPresetPicker) {
        PrinterPresetDialog(
            onPick = { preset ->
                viewModel.startAddFromPreset(preset)
                showPresetPicker = false
            },
            onDismiss = { showPresetPicker = false },
        )
    }
}

@Composable
private fun PrinterRow(printer: PrinterProfile, queueEntry: PrinterQueueEntry?, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(printer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${printer.printerPowerWatts.toInt()} W · manutenção ${printer.maintenanceCostPerHour.toMoney()}/h · " +
                        "máquina ${printer.machineInvestment.machinePrice.toMoney()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (queueEntry != null && queueEntry.queuedQuoteCount > 0) {
                        "Fila: ${queueEntry.queuedMinutes.minutesToHoursText()} em " +
                            "${queueEntry.queuedQuoteCount} pedido(s) \"Em impressão\""
                    } else {
                        "Sem pedidos em impressão no momento"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PrinterForm(
    form: PrinterFormState,
    onChange: ((PrinterFormState) -> PrinterFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.id == null) "Nova impressora" else "Editar impressora", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome (ex.: Ender 3)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.printerPowerWattsText,
            onValueChange = { text -> onChange { it.copy(printerPowerWattsText = text) } },
            label = { Text("Consumo (W)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.maintenanceCostPerHourText,
            onValueChange = { text -> onChange { it.copy(maintenanceCostPerHourText = text) } },
            label = { Text("Manutenção por hora (${LocalCurrency.current.symbol})") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.machinePriceText,
            onValueChange = { text -> onChange { it.copy(machinePriceText = text) } },
            label = { Text("Valor da máquina (${LocalCurrency.current.symbol})") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.paybackMonthsText,
            onValueChange = { text -> onChange { it.copy(paybackMonthsText = text) } },
            label = { Text("Prazo de retorno (meses)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.printingDaysPerMonthText,
            onValueChange = { text -> onChange { it.copy(printingDaysPerMonthText = text) } },
            label = { Text("Dias de uso por mês") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.printingHoursPerDayText,
            onValueChange = { text -> onChange { it.copy(printingHoursPerDayText = text) } },
            label = { Text("Horas de uso por dia") },
        )

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
