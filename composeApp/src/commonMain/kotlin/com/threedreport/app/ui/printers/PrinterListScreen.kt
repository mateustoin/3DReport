package com.threedreport.app.ui.printers

import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
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
import com.threedreport.core.report.ComponentStatus
import com.threedreport.core.report.MaintenanceReport
import com.threedreport.core.report.MaintenanceState
import com.threedreport.core.report.PrinterQueueEntry

/** Tela de Impressoras: perfis salvos, escolhidos depois na tela de Orçamento. */
@Composable
fun PrinterListScreen(viewModel: PrinterListViewModel, modifier: Modifier = Modifier) {
    val printers by viewModel.printers.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val form by viewModel.form.collectAsState()
    val maintenance by viewModel.maintenance.collectAsState()
    var pendingDelete by remember { mutableStateOf<PrinterProfile?>(null) }
    var maintenanceFor by remember { mutableStateOf<PrinterProfile?>(null) }
    var showPresetPicker by remember { mutableStateOf(false) }
    val queueByPrinterId = remember(printers, savedQuotes) {
        viewModel.printQueue(printers, savedQuotes).associateBy { it.printer.id }
    }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Impressoras", style = MaterialTheme.typography.titleLarge)

        val (archived, active) = printers.partition { it.archived }
        if (active.isEmpty()) {
            EmptyState("Nenhuma impressora cadastrada ainda. Cadastre a primeira abaixo.")
        }

        val row: @Composable (PrinterProfile) -> Unit = { printer ->
            PrinterRow(
                printer = printer,
                queueEntry = queueByPrinterId[printer.id],
                maintenanceStatuses = viewModel.componentStatuses(printer.id, savedQuotes, maintenance),
                onMaintenance = { maintenanceFor = printer },
                onEdit = { viewModel.startEdit(printer) },
                onArchiveToggle = { viewModel.setArchived(printer.id, !printer.archived) },
                onDelete = { pendingDelete = printer },
            )
        }
        active.forEach { row(it) }
        ArchivedSection(archived.size) { archived.forEach { row(it) } }

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
        DeleteOrArchiveDialog(
            title = "Excluir impressora?",
            what = "a impressora \"${printer.name}\"",
            usageCount = viewModel.usageCount(printer.id),
            deleteMessage = "\"${printer.name}\" será removida do catálogo, junto com os componentes, o diário de manutenção " +
                "e as horas avulsas dela. Essa ação não pode ser desfeita.",
            onArchive = if (printer.archived) null else ({
                viewModel.setArchived(printer.id, true)
                pendingDelete = null
            }),
            onDelete = {
                viewModel.delete(printer.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    maintenanceFor?.let { printer ->
        MaintenanceDialog(printer = printer, viewModel = viewModel, onDismiss = { maintenanceFor = null })
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
private fun PrinterRow(
    printer: PrinterProfile,
    queueEntry: PrinterQueueEntry?,
    maintenanceStatuses: List<ComponentStatus>,
    onMaintenance: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(printer.name + if (printer.archived) " · arquivada" else "", style = MaterialTheme.typography.titleMedium)
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
                MaintenanceLine(maintenanceStatuses)
            }
            Row {
                TextButton(onClick = onMaintenance) { Text("Manutenção") }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onArchiveToggle) { Text(if (printer.archived) "Restaurar" else "Arquivar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/**
 * O que a manutenção está pedindo, pelo componente mais urgente: vencido em vermelho, perto em
 * âmbar. Em dia, fica discreto e só diz qual é o próximo.
 */
@Composable
private fun MaintenanceLine(statuses: List<ComponentStatus>) {
    val urgent = MaintenanceReport.mostUrgent(statuses)
    val overdueCount = statuses.count { it.state == MaintenanceState.OVERDUE }
    val (text, color) = when {
        urgent == null -> "Manutenção: nenhum componente cadastrado" to MaterialTheme.colorScheme.onSurfaceVariant
        urgent.state == MaintenanceState.OK ->
            "Manutenção em dia · próxima: ${urgent.component.name}, ${urgent.remainingText()}" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> {
            val others = overdueCount - (if (urgent.state == MaintenanceState.OVERDUE) 1 else 0)
            val suffix = if (others > 0) " (e mais $others vencida${if (others > 1) "s" else ""})" else ""
            "Manutenção: ${urgent.component.name}, ${urgent.remainingText()}$suffix" to urgent.state.color()
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
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

        PrinterField("Nome", form.name, "Ex.: Ender 3, P1S da oficina.") { text -> onChange { it.copy(name = text) } }
        PrinterField(
            "Consumo médio (W)",
            form.printerPowerWattsText,
            if (form.powerFromPreset) {
                "O preset traz a potência máxima da fonte. Na média de uma impressão a máquina gasta bem menos: " +
                    "se tiver um medidor de tomada, use o valor medido, senão a energia sai mais cara do que é."
            } else {
                "Quanto a impressora puxa da tomada, em média, durante uma impressão (um medidor de tomada dá o número certo)."
            },
            warning = form.powerFromPreset,
        ) { text -> onChange { it.copy(printerPowerWattsText = text, powerFromPreset = false) } }
        PrinterField(
            "Manutenção por hora (${LocalCurrency.current.symbol})",
            form.maintenanceCostPerHourText,
            "Bicos, correias, PTFE e peças que gastam, divididos pelas horas que duram.",
        ) { text -> onChange { it.copy(maintenanceCostPerHourText = text) } }
        PrinterField(
            "Valor da máquina (${LocalCurrency.current.symbol})",
            form.machinePriceText,
            "Quanto você pagou. Entra no preço como retorno do investimento, no prazo abaixo.",
        ) { text -> onChange { it.copy(machinePriceText = text) } }
        PrinterField("Prazo de retorno (meses)", form.paybackMonthsText, "Em quantos meses a máquina precisa se pagar.") { text ->
            onChange { it.copy(paybackMonthsText = text) }
        }
        PrinterField("Dias de uso por mês", form.printingDaysPerMonthText, "Dias em que ela imprime, de 1 a 31.") { text ->
            onChange { it.copy(printingDaysPerMonthText = text) }
        }
        PrinterField("Horas de uso por dia", form.printingHoursPerDayText, "Horas imprimindo num dia de uso, de 1 a 24.") { text ->
            onChange { it.copy(printingHoursPerDayText = text) }
        }
        form.machineCostPerHour?.let { perHour ->
            Text(
                "Custo da máquina: ${perHour.toMoney()} por hora de impressão (retorno + manutenção), fora a energia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@Composable
private fun PrinterField(label: String, value: String, help: String, warning: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().tabToNavigate(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(help, color = if (warning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
    )
}
