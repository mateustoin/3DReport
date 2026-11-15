package com.threedreport.app.ui.printers

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_restore
import _3dreport.composeapp.generated.resources.printers_action_maintenance
import _3dreport.composeapp.generated.resources.printers_action_new
import _3dreport.composeapp.generated.resources.printers_action_pick
import _3dreport.composeapp.generated.resources.printers_delete_message
import _3dreport.composeapp.generated.resources.printers_delete_title
import _3dreport.composeapp.generated.resources.printers_delete_what
import _3dreport.composeapp.generated.resources.printers_empty
import _3dreport.composeapp.generated.resources.printers_field_days
import _3dreport.composeapp.generated.resources.printers_field_hours
import _3dreport.composeapp.generated.resources.printers_field_maintenance
import _3dreport.composeapp.generated.resources.printers_field_name
import _3dreport.composeapp.generated.resources.printers_field_name_hint
import _3dreport.composeapp.generated.resources.printers_field_payback
import _3dreport.composeapp.generated.resources.printers_field_power
import _3dreport.composeapp.generated.resources.printers_field_price
import _3dreport.composeapp.generated.resources.printers_form_edit
import _3dreport.composeapp.generated.resources.printers_form_new
import _3dreport.composeapp.generated.resources.printers_help_maintenance
import _3dreport.composeapp.generated.resources.printers_help_power
import _3dreport.composeapp.generated.resources.printers_help_power_detail
import _3dreport.composeapp.generated.resources.printers_help_power_preset
import _3dreport.composeapp.generated.resources.printers_help_power_preset_detail
import _3dreport.composeapp.generated.resources.printers_maintenance_attention
import _3dreport.composeapp.generated.resources.printers_maintenance_more_overdue
import _3dreport.composeapp.generated.resources.printers_maintenance_ok
import _3dreport.composeapp.generated.resources.printers_remaining_left
import _3dreport.composeapp.generated.resources.printers_remaining_overdue
import _3dreport.composeapp.generated.resources.printers_row_per_hour
import _3dreport.composeapp.generated.resources.printers_row_power
import _3dreport.composeapp.generated.resources.printers_row_queue
import _3dreport.composeapp.generated.resources.printers_section_cost
import _3dreport.composeapp.generated.resources.printers_section_printer
import _3dreport.composeapp.generated.resources.printers_summary_help
import _3dreport.composeapp.generated.resources.printers_summary_label
import _3dreport.composeapp.generated.resources.printers_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.FormPanel
import com.threedreport.app.ui.components.FormSummary
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.ListScreenLayout
import com.threedreport.app.ui.components.ObserveNotices
import com.threedreport.app.ui.components.PanelSection
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.minutesToHoursText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.report.MaintenanceState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Tela de Impressoras: perfis salvos, escolhidos depois na tela de Orçamento. Mesmo molde da tela de Insumos. */
@Composable
fun PrinterListScreen(viewModel: PrinterListViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveNotices(viewModel.notices)

    val form = state.form
    val panel: (@Composable () -> Unit)? = form?.let { open ->
        { PrinterFormPanel(open, viewModel) }
    }
    ListScreenLayout(panel = panel, modifier = modifier) {
        ScreenHeader(stringResource(Res.string.printers_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                AppButton(stringResource(Res.string.printers_action_pick), onClick = viewModel::openPresetPicker, icon = AppIcons.ViewList)
                if (!state.isEmpty) {
                    AppButton(
                        stringResource(Res.string.printers_action_new),
                        onClick = viewModel::startAdd,
                        kind = if (form == null) ButtonKind.Primary else ButtonKind.Secondary,
                        icon = AppIcons.Add,
                    )
                }
            }
        }
        if (state.active.isEmpty()) {
            EmptyState(
                stringResource(Res.string.printers_empty),
                actionLabel = stringResource(Res.string.printers_action_new).takeIf { state.isEmpty },
                onAction = viewModel::startAdd.takeIf { state.isEmpty },
            )
        }
        state.active.forEach { key(it.printer.id) { PrinterRow(it, viewModel, selectedId = form?.id) } }
        ArchivedSection(state.archived.size) { state.archived.forEach { key(it.printer.id) { PrinterRow(it, viewModel, selectedId = form?.id) } } }
    }

    state.pendingDelete?.let { pending ->
        DeleteOrArchiveDialog(
            title = stringResource(Res.string.printers_delete_title),
            what = stringResource(Res.string.printers_delete_what, pending.name),
            usageCount = pending.usageCount,
            deleteMessage = stringResource(Res.string.printers_delete_message, pending.name),
            onArchive = { viewModel.setArchived(pending.id, true) }.takeIf { pending.canArchive },
            onDelete = { viewModel.delete(pending.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }

    state.maintenance?.let { open ->
        MaintenanceDialog(open, viewModel, onDismiss = viewModel::closeMaintenance)
    }

    if (state.presetPickerOpen) {
        PrinterPresetDialog(onPick = viewModel::startAddFromPreset, onDismiss = viewModel::closePresetPicker)
    }
}

@Composable
private fun PrinterRow(row: PrinterRowState, viewModel: PrinterListViewModel, selectedId: String?) {
    val printer = row.printer
    val archiveOrRestore = if (printer.archived) {
        RowAction(AppIcons.Unarchive, stringResource(Res.string.common_restore), { viewModel.setArchived(printer.id, false) })
    } else {
        RowAction(AppIcons.Archive, stringResource(Res.string.common_archive), { viewModel.setArchived(printer.id, true) })
    }
    ListRow(
        title = printer.name,
        details = listOfNotNull(
            stringResource(Res.string.printers_row_power, printer.printerPowerWatts.toInt()),
            if (row.queuedQuoteCount > 0) {
                stringResource(Res.string.printers_row_queue, row.queuedMinutes.minutesToHoursText())
            } else {
                null
            },
        ),
        value = stringResource(Res.string.printers_row_per_hour, row.machineCostPerHour.toMoney()),
        muted = printer.archived,
        selected = selectedId == printer.id,
        onClick = { viewModel.startEdit(printer.id) },
        actions = listOf(
            RowAction(AppIcons.Build, stringResource(Res.string.printers_action_maintenance), { viewModel.openMaintenance(printer.id) }),
            RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.startEdit(printer.id) }),
            archiveOrRestore,
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDelete(printer.id) }, danger = true),
        ),
        footer = row.maintenance?.let { summary -> { MaintenanceLine(summary) } },
    )
}

/**
 * O que a manutenção está pedindo, pelo componente mais urgente: vencido em vermelho, perto em âmbar. Em dia, fica
 * discreto e só diz qual é o próximo.
 */
@Composable
private fun MaintenanceLine(summary: MaintenanceSummary) {
    val remaining = stringResource(
        if (summary.state == MaintenanceState.OVERDUE) Res.string.printers_remaining_overdue else Res.string.printers_remaining_left,
        (if (summary.state == MaintenanceState.OVERDUE) -summary.remainingHours else summary.remainingHours).hoursText(),
    )
    val text = if (summary.state == MaintenanceState.OK) {
        stringResource(Res.string.printers_maintenance_ok, summary.componentName, remaining)
    } else {
        stringResource(Res.string.printers_maintenance_attention, summary.componentName, remaining) +
            if (summary.otherOverdue > 0) " " + pluralStringResource(Res.plurals.printers_maintenance_more_overdue, summary.otherOverdue, summary.otherOverdue) else ""
    }
    Text(
        text,
        style = MaterialTheme.appType.caption,
        color = summary.state.textColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = MaterialTheme.spacing.xxs),
    )
}

@Composable
private fun PrinterFormPanel(form: PrinterFormState, viewModel: PrinterListViewModel) {
    val symbol = LocalCurrency.current.symbol
    FormPanel(
        title = stringResource(if (form.id == null) Res.string.printers_form_new else Res.string.printers_form_edit),
        onSave = viewModel::save,
        onCancel = viewModel::cancelEdit,
    ) {
        PanelSection(1, stringResource(Res.string.printers_section_printer)) {
            AppTextField(
                label = stringResource(Res.string.printers_field_name),
                value = form.name,
                onValueChange = { text -> viewModel.updateForm { it.copy(name = text) } },
                placeholder = stringResource(Res.string.printers_field_name_hint),
                error = form.nameError?.asString(),
            )
            AppTextField(
                label = stringResource(Res.string.printers_field_power),
                value = form.printerPowerWattsText,
                onValueChange = { text -> viewModel.updateForm { it.copy(printerPowerWattsText = text, powerFromPreset = false) } },
                error = form.powerError?.asString(),
                help = if (form.powerFromPreset) {
                    HelpText(
                        stringResource(Res.string.printers_help_power_preset),
                        stringResource(Res.string.printers_help_power_preset_detail),
                        warning = true,
                    )
                } else {
                    HelpText(stringResource(Res.string.printers_help_power), stringResource(Res.string.printers_help_power_detail))
                },
            )
        }
        PanelSection(2, stringResource(Res.string.printers_section_cost)) {
            AppTextField(
                label = stringResource(Res.string.printers_field_maintenance, symbol),
                value = form.maintenanceCostPerHourText,
                onValueChange = { text -> viewModel.updateForm { it.copy(maintenanceCostPerHourText = text) } },
                error = form.maintenanceError?.asString(),
                help = HelpText(stringResource(Res.string.printers_help_maintenance)),
            )
            AppTextField(
                label = stringResource(Res.string.printers_field_price, symbol),
                value = form.machinePriceText,
                onValueChange = { text -> viewModel.updateForm { it.copy(machinePriceText = text) } },
                error = form.priceError?.asString(),
            )
            AppTextField(
                label = stringResource(Res.string.printers_field_payback),
                value = form.paybackMonthsText,
                onValueChange = { text -> viewModel.updateForm { it.copy(paybackMonthsText = text) } },
                error = form.paybackError?.asString(),
            )
            AppTextField(
                label = stringResource(Res.string.printers_field_days),
                value = form.printingDaysPerMonthText,
                onValueChange = { text -> viewModel.updateForm { it.copy(printingDaysPerMonthText = text) } },
                error = form.daysError?.asString(),
            )
            AppTextField(
                label = stringResource(Res.string.printers_field_hours),
                value = form.printingHoursPerDayText,
                onValueChange = { text -> viewModel.updateForm { it.copy(printingHoursPerDayText = text) } },
                error = form.hoursError?.asString(),
            )
            form.machineCostPerHour?.let { perHour ->
                FormSummary(
                    label = stringResource(Res.string.printers_summary_label),
                    value = stringResource(Res.string.printers_row_per_hour, perHour.toMoney()),
                    help = HelpText(stringResource(Res.string.printers_summary_help)),
                )
            }
        }
    }
}
