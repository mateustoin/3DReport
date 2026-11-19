package com.threedreport.app.ui.printers

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_add
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.printers_maintenance_component_delete_message
import _3dreport.composeapp.generated.resources.printers_maintenance_component_delete_title
import _3dreport.composeapp.generated.resources.printers_maintenance_component_done
import _3dreport.composeapp.generated.resources.printers_maintenance_component_empty
import _3dreport.composeapp.generated.resources.printers_maintenance_component_new
import _3dreport.composeapp.generated.resources.printers_maintenance_component_progress
import _3dreport.composeapp.generated.resources.printers_maintenance_components
import _3dreport.composeapp.generated.resources.printers_maintenance_field_component
import _3dreport.composeapp.generated.resources.printers_maintenance_field_date
import _3dreport.composeapp.generated.resources.printers_maintenance_field_description
import _3dreport.composeapp.generated.resources.printers_maintenance_field_hours
import _3dreport.composeapp.generated.resources.printers_maintenance_field_hours_since
import _3dreport.composeapp.generated.resources.printers_maintenance_field_interval
import _3dreport.composeapp.generated.resources.printers_maintenance_field_reason
import _3dreport.composeapp.generated.resources.printers_maintenance_field_reset
import _3dreport.composeapp.generated.resources.printers_maintenance_help_hours_since
import _3dreport.composeapp.generated.resources.printers_maintenance_help_known_hours
import _3dreport.composeapp.generated.resources.printers_maintenance_help_known_hours_detail
import _3dreport.composeapp.generated.resources.printers_maintenance_help_log
import _3dreport.composeapp.generated.resources.printers_maintenance_help_reset
import _3dreport.composeapp.generated.resources.printers_maintenance_help_usage
import _3dreport.composeapp.generated.resources.printers_maintenance_known_hours
import _3dreport.composeapp.generated.resources.printers_maintenance_log
import _3dreport.composeapp.generated.resources.printers_maintenance_log_add
import _3dreport.composeapp.generated.resources.printers_maintenance_log_entry
import _3dreport.composeapp.generated.resources.printers_maintenance_log_entry_component
import _3dreport.composeapp.generated.resources.printers_maintenance_log_submit
import _3dreport.composeapp.generated.resources.printers_maintenance_reset_none
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_bed
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_belts
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_calibration
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_nozzle
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_own_use
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_reprint
import _3dreport.composeapp.generated.resources.printers_maintenance_suggestion_rods
import _3dreport.composeapp.generated.resources.printers_maintenance_title
import _3dreport.composeapp.generated.resources.printers_maintenance_today
import _3dreport.composeapp.generated.resources.printers_maintenance_usage
import _3dreport.composeapp.generated.resources.printers_maintenance_usage_entry
import _3dreport.composeapp.generated.resources.printers_maintenance_usage_submit
import _3dreport.composeapp.generated.resources.printers_remaining_left
import _3dreport.composeapp.generated.resources.printers_remaining_overdue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.platform.epochDayToUtcMillis
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DateShortcut
import com.threedreport.app.ui.components.DateShortcutPicker
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.IconAction
import com.threedreport.app.ui.components.RevealActions
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.revealActionsWidth
import com.threedreport.app.ui.components.suggestionsFor
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.minutesToHoursText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.MaintenanceComponent
import com.threedreport.core.report.ComponentStatus
import com.threedreport.core.report.MaintenanceState
import org.jetbrains.compose.resources.stringResource

/** Horas como texto curto, ex.: "12,5 h". */
@Composable
internal fun Double.hoursText(): String = (this * 60).minutesToHoursText()

/** Cor do texto do estado: erro quando vencido, o par de prazo "Hoje" quando perto, discreta quando em dia. */
@Composable
internal fun MaintenanceState.textColor(): Color = when (this) {
    MaintenanceState.OVERDUE -> MaterialTheme.colorScheme.error
    MaintenanceState.DUE_SOON -> MaterialTheme.appColors.deadlineToday.content
    MaintenanceState.OK -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Cor da barra de progresso: erro, o tom de "Hoje" ou, em dia, a cor de destaque do tema. */
@Composable
private fun MaintenanceState.barColor(): Color = when (this) {
    MaintenanceState.OVERDUE -> MaterialTheme.colorScheme.error
    MaintenanceState.DUE_SOON -> MaterialTheme.appColors.deadlineToday.content
    MaintenanceState.OK -> MaterialTheme.colorScheme.primary
}

/**
 * Manutenção de uma impressora (decisão 96): componentes com intervalo e contador, diário do que foi feito e horas de
 * uso fora de orçamento. Aberto pelo botão "Manutenção" na linha da impressora, que é onde a fila de cada máquina já
 * mora (decisão 68), sem aba nova. O que está aberto vem do estado do [viewModel] ([open]).
 */
@Composable
fun MaintenanceDialog(open: MaintenanceDialogState, viewModel: PrinterListViewModel, onDismiss: () -> Unit) {
    val printer = open.printer
    var pendingDelete by remember { mutableStateOf<MaintenanceComponent?>(null) }

    AppDialog(
        title = stringResource(Res.string.printers_maintenance_title, printer.name),
        onDismiss = onDismiss,
        buttons = { AppButton(stringResource(Res.string.common_close), onDismiss) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
            DialogText(stringResource(Res.string.printers_maintenance_known_hours, open.hours.hoursText()), muted = true)
            HelpHint(
                HelpText(
                    stringResource(Res.string.printers_maintenance_help_known_hours),
                    stringResource(Res.string.printers_maintenance_help_known_hours_detail),
                ),
                stringResource(Res.string.printers_maintenance_known_hours, open.hours.hoursText()),
            )
        }

        FormSection(null, stringResource(Res.string.printers_maintenance_components)) {
            if (open.statuses.isEmpty()) DialogText(stringResource(Res.string.printers_maintenance_component_empty), muted = true)
            open.statuses.forEach { status ->
                ComponentRow(
                    status = status,
                    onServiced = { viewModel.markServiced(status.component) },
                    onSave = { name, interval -> viewModel.updateComponent(status.component, name, interval) },
                    onDelete = { pendingDelete = status.component },
                )
            }
            NewComponentForm(onAdd = { name, interval, since -> viewModel.addComponent(printer.id, name, interval, since) })
        }

        FormSection(
            null,
            stringResource(Res.string.printers_maintenance_log),
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            help = HelpText(stringResource(Res.string.printers_maintenance_help_log)).takeIf { open.data.log.any { it.componentId != null } },
        ) {
            LogForm(
                today = open.today,
                components = open.data.components,
                onAdd = { date, description, componentId -> viewModel.logService(printer.id, date, description, componentId) },
            )
            val componentNames = open.data.components.associate { it.id to it.name }
            open.data.log.sortedByDescending { it.dateEpochDay }.forEach { entry ->
                val component = entry.componentId?.let(componentNames::get)?.takeIf { it != entry.description }
                EntryRow(
                    text = if (component != null) {
                        stringResource(Res.string.printers_maintenance_log_entry_component, formatDate(entry.dateEpochDay), entry.description, component)
                    } else {
                        stringResource(Res.string.printers_maintenance_log_entry, formatDate(entry.dateEpochDay), entry.description)
                    },
                    onDelete = { viewModel.deleteLogEntry(entry.id) },
                )
            }
        }

        FormSection(
            null,
            stringResource(Res.string.printers_maintenance_usage),
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            help = HelpText(stringResource(Res.string.printers_maintenance_help_usage)),
        ) {
            ManualUsageForm(onAdd = { hoursText, reason -> viewModel.addManualUsage(printer.id, hoursText, reason) })
            open.data.manualUsage.sortedByDescending { it.dateEpochDay }.forEach { entry ->
                EntryRow(
                    text = stringResource(Res.string.printers_maintenance_usage_entry, formatDate(entry.dateEpochDay), entry.hours.hoursText(), entry.reason),
                    onDelete = { viewModel.deleteManualUsage(entry.id) },
                )
            }
        }
    }

    pendingDelete?.let { component ->
        ConfirmDialog(
            title = stringResource(Res.string.printers_maintenance_component_delete_title),
            message = stringResource(Res.string.printers_maintenance_component_delete_message, component.name),
            onConfirm = {
                viewModel.deleteComponent(component.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ComponentRow(
    status: ComponentStatus,
    onServiced: () -> Unit,
    onSave: (name: String, interval: String) -> UiText?,
    onDelete: () -> Unit,
) {
    var editing by remember(status.component.id) { mutableStateOf(false) }
    if (editing) {
        ComponentFields(
            initialName = status.component.name,
            initialInterval = status.component.intervalHours.toInputText(LocalCurrency.current.decimalSeparator),
            isNew = false,
            onSubmit = { name, interval, _ -> onSave(name, interval).also { if (it == null) editing = false } },
            onCancel = { editing = false },
        )
        return
    }

    val remaining = stringResource(
        if (status.state == MaintenanceState.OVERDUE) Res.string.printers_remaining_overdue else Res.string.printers_remaining_left,
        (if (status.state == MaintenanceState.OVERDUE) -status.remainingHours else status.remainingHours).hoursText(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
        RevealActions(
            actions = listOf(
                RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { editing = true }),
                RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), onDelete, danger = true),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = MaterialTheme.spacing.xs, end = revealActionsWidth(2)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.component.name, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            Res.string.printers_maintenance_component_progress,
                            status.hoursSinceService.hoursText(),
                            status.component.intervalHours.hoursText(),
                            remaining,
                        ),
                        style = MaterialTheme.appType.caption,
                        color = status.state.textColor(),
                    )
                }
                AppButton(
                    stringResource(Res.string.printers_maintenance_component_done),
                    onServiced,
                    icon = AppIcons.Check,
                    height = AppDimens.smallControlHeight,
                )
            }
        }
        LinearProgressIndicator(
            progress = { (status.hoursSinceService / status.component.intervalHours).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = status.state.barColor(),
            // O trilho padrão é âmbar nesta paleta e, vazio, parece uma barra cheia.
            trackColor = MaterialTheme.appColors.column,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun NewComponentForm(onAdd: (name: String, interval: String, hoursSince: String) -> UiText?) {
    var open by remember { mutableStateOf(false) }
    if (!open) {
        AppButton(stringResource(Res.string.printers_maintenance_component_new), { open = true }, icon = AppIcons.Add)
        return
    }
    ComponentFields(
        initialName = "",
        initialInterval = "",
        isNew = true,
        onSubmit = { name, interval, since -> onAdd(name, interval, since).also { if (it == null) open = false } },
        onCancel = { open = false },
    )
}

/** Mensagem de erro de um subformulário do diálogo, embaixo dos campos. */
@Composable
private fun FormError(error: UiText?) {
    if (error != null) Text(error.asString(), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.error)
}

/** Campos de componente, pra cadastrar ou editar. [onSubmit] devolve o erro, ou `null`. */
@Composable
private fun ComponentFields(
    initialName: String,
    initialInterval: String,
    isNew: Boolean,
    onSubmit: (name: String, interval: String, hoursSince: String) -> UiText?,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var interval by remember { mutableStateOf(initialInterval) }
    var hoursSince by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    val suggestions = listOf(
        stringResource(Res.string.printers_maintenance_suggestion_nozzle),
        stringResource(Res.string.printers_maintenance_suggestion_rods),
        stringResource(Res.string.printers_maintenance_suggestion_belts),
        stringResource(Res.string.printers_maintenance_suggestion_bed),
    )

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        if (isNew) {
            AppSuggestField(
                label = stringResource(Res.string.printers_maintenance_field_component),
                value = name,
                onValueChange = { name = it; error = null },
                suggestions = suggestions.suggestionsFor(name),
                suggestionLabel = { it },
                onChoose = { name = it; error = null },
            )
        } else {
            AppTextField(
                label = stringResource(Res.string.printers_maintenance_field_component),
                value = name,
                onValueChange = { name = it; error = null },
            )
        }
        FormGrid {
            cell(if (isNew) 1 else 3) {
                AppTextField(
                    label = stringResource(Res.string.printers_maintenance_field_interval),
                    value = interval,
                    onValueChange = { interval = it; error = null },
                )
            }
            if (isNew) {
                cell(2) {
                    AppTextField(
                        label = stringResource(Res.string.printers_maintenance_field_hours_since),
                        value = hoursSince,
                        onValueChange = { hoursSince = it; error = null },
                        placeholder = stringResource(Res.string.common_optional),
                        help = HelpText(stringResource(Res.string.printers_maintenance_help_hours_since)),
                    )
                }
            }
        }
        FormError(error)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            AppButton(stringResource(if (isNew) Res.string.common_add else Res.string.common_save), { error = onSubmit(name, interval, hoursSince) })
            AppButton(stringResource(Res.string.common_cancel), onCancel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogForm(today: Long, components: List<MaintenanceComponent>, onAdd: (date: Long, description: String, componentId: String?) -> UiText?) {
    var open by remember { mutableStateOf(false) }
    if (!open) {
        AppButton(stringResource(Res.string.printers_maintenance_log_add), { open = true }, icon = AppIcons.Add)
        return
    }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var componentId by remember { mutableStateOf<String?>(null) }
    // Resolvido contra a lista atual: um componente que sumiu (excluído com o formulário aberto) vira "nenhum".
    val component = components.find { it.id == componentId }
    var error by remember { mutableStateOf<UiText?>(null) }
    val noneLabel = stringResource(Res.string.printers_maintenance_reset_none)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        AppTextField(
            label = stringResource(Res.string.printers_maintenance_field_description),
            value = description,
            onValueChange = { description = it; error = null },
        )
        Text(stringResource(Res.string.printers_maintenance_field_date), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Manutenção registrada é o que já foi feito: o calendário não deixa escolher o futuro.
        DateShortcutPicker(
            epochDay = date,
            shortcuts = listOf(DateShortcut(stringResource(Res.string.printers_maintenance_today), today)),
            today = today,
            onChange = { day -> date = day ?: today },
            allowFuture = false,
        )
        if (components.isNotEmpty()) {
            AppDropdownField(
                label = stringResource(Res.string.printers_maintenance_field_reset),
                items = listOf<MaintenanceComponent?>(null) + components,
                selected = component,
                itemLabel = { it?.name ?: noneLabel },
                displayText = { it?.name ?: noneLabel },
                onSelect = { chosen ->
                    componentId = chosen?.id
                    if (chosen != null && description.isBlank()) description = chosen.name
                },
                emptyText = noneLabel,
                help = HelpText(stringResource(Res.string.printers_maintenance_help_reset)),
            )
        }
        FormError(error)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            AppButton(stringResource(Res.string.printers_maintenance_log_submit), {
                error = onAdd(date, description, component?.id)
                if (error == null) open = false
            })
            AppButton(stringResource(Res.string.common_cancel), { open = false })
        }
    }
}

@Composable
private fun ManualUsageForm(onAdd: (hours: String, reason: String) -> UiText?) {
    var hours by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    val suggestions = listOf(
        stringResource(Res.string.printers_maintenance_suggestion_calibration),
        stringResource(Res.string.printers_maintenance_suggestion_reprint),
        stringResource(Res.string.printers_maintenance_suggestion_own_use),
    )

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        FormGrid {
            cell(1) {
                AppTextField(
                    label = stringResource(Res.string.printers_maintenance_field_hours),
                    value = hours,
                    onValueChange = { hours = it; error = null },
                )
            }
            cell(2) {
                AppSuggestField(
                    label = stringResource(Res.string.printers_maintenance_field_reason),
                    value = reason,
                    onValueChange = { reason = it; error = null },
                    suggestions = suggestions.suggestionsFor(reason),
                    suggestionLabel = { it },
                    onChoose = { reason = it; error = null },
                )
            }
        }
        FormError(error)
        AppButton(stringResource(Res.string.printers_maintenance_usage_submit), {
            error = onAdd(hours, reason)
            if (error == null) {
                hours = ""
                reason = ""
            }
        })
    }
}

@Composable
private fun EntryRow(text: String, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        IconAction(AppIcons.Delete, stringResource(Res.string.common_delete), onDelete, tint = MaterialTheme.colorScheme.error)
    }
}
