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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.PrinterProfile

/** Tela de Impressoras: perfis salvos, escolhidos depois na tela de Orçamento. */
@Composable
fun PrinterListScreen(viewModel: PrinterListViewModel, modifier: Modifier = Modifier) {
    val printers by viewModel.printers.collectAsState()
    val form by viewModel.form.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Impressoras", style = MaterialTheme.typography.titleLarge)

        printers.forEach { printer ->
            PrinterRow(
                printer = printer,
                onEdit = { viewModel.startEdit(printer) },
                onDelete = { viewModel.delete(printer.id) },
            )
        }

        val currentForm = form
        if (currentForm == null) {
            Button(onClick = viewModel::startAdd) { Text("+ Nova impressora") }
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
}

@Composable
private fun PrinterRow(printer: PrinterProfile, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(printer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${printer.printerPowerWatts.toInt()} W · manutenção ${printer.maintenanceCostPerHour.toBrl()}/h · " +
                        "máquina ${printer.machineInvestment.machinePrice.toBrl()}",
                    style = MaterialTheme.typography.bodyMedium,
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
            label = { Text("Manutenção por hora (R$)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.machinePriceText,
            onValueChange = { text -> onChange { it.copy(machinePriceText = text) } },
            label = { Text("Valor da máquina (R$)") },
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
