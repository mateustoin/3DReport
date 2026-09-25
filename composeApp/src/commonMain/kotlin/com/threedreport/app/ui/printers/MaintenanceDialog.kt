package com.threedreport.app.ui.printers

import com.threedreport.app.ui.format.toInputText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.threedreport.app.platform.epochDayToUtcMillis
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.SubsectionTitle
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.minutesToHoursText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.report.ComponentStatus
import com.threedreport.core.report.MaintenanceState

/**
 * Nomes comuns pra não ter que digitar. Só nomes: o intervalo varia de máquina pra máquina, e o
 * app não inventa número (mesma disciplina das decisões 55 e 56).
 */
private val COMPONENT_SUGGESTIONS = listOf("Bico", "Lubrificar eixos", "Correias", "Limpeza da mesa")

private val USAGE_REASON_SUGGESTIONS = listOf("Calibração", "Reimpressão de falha", "Uso próprio")

/** Horas como texto curto, ex.: "12,5 h". */
internal fun Double.hoursText(): String = (this * 60).minutesToHoursText()

/**
 * Manutenção de uma impressora (decisão 96): componentes com intervalo e contador, diário do que
 * foi feito e horas de uso fora de orçamento. Aberto pelo botão "Manutenção" na aba Impressoras,
 * que é onde a fila de cada máquina já mora (decisão 68), sem aba nova.
 */
@Composable
fun MaintenanceDialog(printer: PrinterProfile, viewModel: PrinterListViewModel, onDismiss: () -> Unit) {
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val allMaintenance by viewModel.maintenance.collectAsState()
    val maintenance = allMaintenance.forPrinter(printer.id)
    val hours = viewModel.printerHours(printer.id, savedQuotes, allMaintenance)
    val statuses = viewModel.componentStatuses(printer.id, savedQuotes, allMaintenance)
    var pendingDelete by remember { mutableStateOf<MaintenanceComponent?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.width(680.dp).heightIn(max = 680.dp).padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Manutenção: ${printer.name}", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${hours.hoursText()} de uso que o app conhece: pedidos Prontos ou Entregues nesta impressora, " +
                        "mais as horas avulsas lá embaixo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SubsectionTitle(AppIcons.Build, "Componentes", modifier = Modifier.padding(top = 8.dp))
                if (statuses.isEmpty()) {
                    Text(
                        "Cadastre o que precisa de manutenção de tempos em tempos, cada um com o seu intervalo em horas de uso.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                statuses.forEach { status ->
                    ComponentRow(
                        status = status,
                        onServiced = { viewModel.markServiced(status.component) },
                        onSave = { name, interval -> viewModel.updateComponent(status.component, name, interval) },
                        onDelete = { pendingDelete = status.component },
                    )
                }
                NewComponentForm(onAdd = { name, interval, since -> viewModel.addComponent(printer.id, name, interval, since) })

                HorizontalDivider()
                SubsectionTitle(AppIcons.History, "Diário de manutenção")
                LogForm(
                    components = maintenance.components,
                    onAdd = { date, description, componentId -> viewModel.logService(printer.id, date, description, componentId) },
                )
                if (maintenance.log.any { it.componentId != null }) {
                    Text(
                        "Registrou por engano? Excluir a manutenção mais recente de um componente devolve o contador dele pra onde estava.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val componentNames = maintenance.components.associate { it.id to it.name }
                maintenance.log.sortedByDescending { it.dateEpochDay }.forEach { entry ->
                    EntryRow(
                        text = "${formatDate(entry.dateEpochDay)} · ${entry.description}" +
                            (entry.componentId?.let(componentNames::get)?.takeIf { it != entry.description }?.let { " ($it)" } ?: ""),
                        onDelete = { viewModel.deleteLogEntry(entry.id) },
                    )
                }

                HorizontalDivider()
                SubsectionTitle(AppIcons.Schedule, "Horas fora de orçamento")
                Text(
                    "Calibração, reimpressão de peça que falhou e uso próprio também gastam a máquina. " +
                        "Lance aqui pra o alerta não chegar atrasado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ManualUsageForm(onAdd = { hoursText, reason -> viewModel.addManualUsage(printer.id, hoursText, reason) })
                maintenance.manualUsage.sortedByDescending { it.dateEpochDay }.forEach { entry ->
                    EntryRow(
                        text = "${formatDate(entry.dateEpochDay)} · ${entry.hours.hoursText()} · ${entry.reason}",
                        onDelete = { viewModel.deleteManualUsage(entry.id) },
                    )
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }

    pendingDelete?.let { component ->
        ConfirmDialog(
            title = "Excluir componente?",
            message = "\"${component.name}\" e o contador dele serão removidos. O que já está no diário continua lá.",
            onConfirm = {
                viewModel.deleteComponent(component.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Cor do estado: erro quando vencido, secundária (o âmbar da paleta) quando perto, primária quando em dia. */
@Composable
internal fun MaintenanceState.color(): Color = when (this) {
    MaintenanceState.OVERDUE -> MaterialTheme.colorScheme.error
    MaintenanceState.DUE_SOON -> MaterialTheme.colorScheme.secondary
    MaintenanceState.OK -> MaterialTheme.colorScheme.primary
}

/** Frase curta da situação, ex.: "vencida há 8 h", "faltam 12 h". */
internal fun ComponentStatus.remainingText(): String =
    if (state == MaintenanceState.OVERDUE) "vencida há ${(-remainingHours).hoursText()}" else "faltam ${remainingHours.hoursText()}"

@Composable
private fun ComponentRow(
    status: ComponentStatus,
    onServiced: () -> Unit,
    onSave: (name: String, interval: String) -> String?,
    onDelete: () -> Unit,
) {
    var editing by remember(status.component.id) { mutableStateOf(false) }
    if (editing) {
        ComponentFields(
            initialName = status.component.name,
            initialInterval = status.component.intervalHours.toInputText(),
            showHoursSince = false,
            submitLabel = "Salvar",
            onSubmit = { name, interval, _ -> onSave(name, interval).also { if (it == null) editing = false } },
            onCancel = { editing = false },
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(status.component.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${status.hoursSinceService.hoursText()} de ${status.component.intervalHours.hoursText()} · ${status.remainingText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.state == MaintenanceState.OK) MaterialTheme.colorScheme.onSurfaceVariant else status.state.color(),
                )
            }
            TextButton(onClick = onServiced) { Text("Feito hoje") }
            TextButton(onClick = { editing = true }) { Text("Editar") }
            TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
        }
        LinearProgressIndicator(
            progress = { (status.hoursSinceService / status.component.intervalHours).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = status.state.color(),
            // O trilho padrão é âmbar nesta paleta e, vazio, parece uma barra cheia.
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun NewComponentForm(onAdd: (name: String, interval: String, hoursSince: String) -> String?) {
    var open by remember { mutableStateOf(false) }
    if (!open) {
        TextButton(onClick = { open = true }) { Text("+ Componente") }
        return
    }
    ComponentFields(
        initialName = "",
        initialInterval = "",
        showHoursSince = true,
        submitLabel = "Adicionar",
        onSubmit = { name, interval, since -> onAdd(name, interval, since).also { if (it == null) open = false } },
        onCancel = { open = false },
    )
}

/** Campos de componente, pra cadastrar ou editar. [onSubmit] devolve a mensagem de erro, ou `null`. */
@Composable
private fun ComponentFields(
    initialName: String,
    initialInterval: String,
    showHoursSince: Boolean,
    submitLabel: String,
    onSubmit: (name: String, interval: String, hoursSince: String) -> String?,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var interval by remember { mutableStateOf(initialInterval) }
    var hoursSince by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showHoursSince) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                COMPONENT_SUGGESTIONS.forEach { suggestion ->
                    FilterChip(selected = name == suggestion, onClick = { name = suggestion }, label = { Text(suggestion) })
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text("Componente ou tarefa") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f).tabToNavigate(),
                value = interval,
                onValueChange = { interval = it; error = null },
                label = { Text("A cada quantas horas de uso") },
            )
            if (showHoursSince) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f).tabToNavigate(),
                    value = hoursSince,
                    onValueChange = { hoursSince = it; error = null },
                    label = { Text("Horas desde a última vez") },
                )
            }
        }
        if (showHoursSince) {
            Text(
                "Horas desde a última vez é opcional: deixe em branco se a peça é nova ou acabou de ser trocada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { error = onSubmit(name, interval, hoursSince) }) { Text(submitLabel) }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogForm(components: List<MaintenanceComponent>, onAdd: (date: Long, description: String, componentId: String?) -> String?) {
    var open by remember { mutableStateOf(false) }
    if (!open) {
        TextButton(onClick = { open = true }) { Text("+ Registrar manutenção") }
        return
    }
    val today = todayEpochDay()
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var componentId by remember { mutableStateOf<String?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = description,
            onValueChange = { description = it; error = null },
            label = { Text("O que foi feito (ex.: Trocado bico 0,4 mm)") },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
            FilterChip(selected = date == today, onClick = { date = today }, label = { Text("Hoje") })
            FilterChip(
                selected = date != today,
                onClick = { showCalendar = true },
                label = { Text(if (date == today) "Outra data…" else formatDate(date)) },
            )
        }
        if (components.isNotEmpty()) {
            Text("Zera o contador de:", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = componentId == null, onClick = { componentId = null }, label = { Text("Nenhum") })
                components.forEach { component ->
                    FilterChip(
                        selected = componentId == component.id,
                        onClick = {
                            componentId = component.id
                            if (description.isBlank()) description = component.name
                        },
                        label = { Text(component.name) },
                    )
                }
            }
            if (componentId != null) {
                Text(
                    "O contador recomeça agora, mesmo com data antiga: o app não sabe quanto a máquina rodou desde então.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                error = onAdd(date, description, componentId)
                if (error == null) open = false
            }) { Text("Registrar") }
            TextButton(onClick = { open = false }) { Text("Cancelar") }
        }
    }

    if (showCalendar) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = epochDayToUtcMillis(date),
            // Manutenção registrada é o que já foi feito: o calendário não deixa escolher o futuro.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcMillisToEpochDay(utcTimeMillis) <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { date = utcMillisToEpochDay(it) }
                        showCalendar = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("Usar esta data") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

@Composable
private fun ManualUsageForm(onAdd: (hours: String, reason: String) -> String?) {
    var hours by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            USAGE_REASON_SUGGESTIONS.forEach { suggestion ->
                FilterChip(selected = reason == suggestion, onClick = { reason = suggestion }, label = { Text(suggestion) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.width(140.dp).tabToNavigate(),
                value = hours,
                onValueChange = { hours = it; error = null },
                label = { Text("Horas") },
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f).tabToNavigate(),
                value = reason,
                onValueChange = { reason = it; error = null },
                label = { Text("Motivo") },
            )
            Button(onClick = {
                error = onAdd(hours, reason)
                if (error == null) {
                    hours = ""
                    reason = ""
                }
            }) { Text("Lançar") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EntryRow(text: String, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
    }
}

