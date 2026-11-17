package com.threedreport.app.ui.services

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_charge_per_order
import _3dreport.composeapp.generated.resources.common_charge_per_piece
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_field_charge
import _3dreport.composeapp.generated.resources.common_help_charge
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.common_restore
import _3dreport.composeapp.generated.resources.services_action_new
import _3dreport.composeapp.generated.resources.services_delete_message
import _3dreport.composeapp.generated.resources.services_delete_title
import _3dreport.composeapp.generated.resources.services_delete_what
import _3dreport.composeapp.generated.resources.services_empty
import _3dreport.composeapp.generated.resources.services_field_minutes
import _3dreport.composeapp.generated.resources.services_field_minutes_hint
import _3dreport.composeapp.generated.resources.services_field_name
import _3dreport.composeapp.generated.resources.services_field_name_hint
import _3dreport.composeapp.generated.resources.services_field_price
import _3dreport.composeapp.generated.resources.services_form_edit
import _3dreport.composeapp.generated.resources.services_form_new
import _3dreport.composeapp.generated.resources.services_help_charge
import _3dreport.composeapp.generated.resources.services_help_minutes
import _3dreport.composeapp.generated.resources.services_help_price
import _3dreport.composeapp.generated.resources.services_help_price_detail
import _3dreport.composeapp.generated.resources.services_row_no_price
import _3dreport.composeapp.generated.resources.services_title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.components.FormPanel
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.ListScreenLayout
import com.threedreport.app.ui.components.ObserveNotices
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.ScreenHeader
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.core.model.Service
import org.jetbrains.compose.resources.stringResource

/**
 * Tela de Serviços: catálogo salvo (pintura, lixamento, montagem etc.), escolhido depois na tela de Orçamento.
 * Mesmo molde da tela de Insumos.
 */
@Composable
fun ServiceListScreen(viewModel: ServiceListViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveNotices(viewModel.notices)

    val form = state.form
    val panel: (@Composable () -> Unit)? = form?.let { open ->
        { ServiceFormPanel(open, viewModel) }
    }
    ListScreenLayout(panel = panel, modifier = modifier) {
        ScreenHeader(stringResource(Res.string.services_title)) {
            if (!state.isEmpty) {
                AppButton(
                    stringResource(Res.string.services_action_new),
                    onClick = viewModel::startAdd,
                    kind = if (form == null) ButtonKind.Primary else ButtonKind.Secondary,
                    icon = AppIcons.Add,
                )
            }
        }
        if (state.active.isEmpty()) {
            EmptyState(
                stringResource(Res.string.services_empty),
                actionLabel = stringResource(Res.string.services_action_new).takeIf { state.isEmpty },
                onAction = viewModel::startAdd.takeIf { state.isEmpty },
            )
        }
        state.active.forEach { key(it.id) { ServiceRow(it, viewModel, selectedId = form?.id) } }
        ArchivedSection(state.archived.size) { state.archived.forEach { key(it.id) { ServiceRow(it, viewModel, selectedId = form?.id) } } }
    }

    state.pendingDelete?.let { pending ->
        DeleteOrArchiveDialog(
            title = stringResource(Res.string.services_delete_title),
            what = stringResource(Res.string.services_delete_what, pending.name),
            usageCount = pending.usageCount,
            deleteMessage = stringResource(Res.string.services_delete_message, pending.name),
            onArchive = { viewModel.setArchived(pending.id, true) }.takeIf { pending.canArchive },
            onDelete = { viewModel.delete(pending.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun ServiceRow(service: Service, viewModel: ServiceListViewModel, selectedId: String?) {
    val archiveOrRestore = if (service.archived) {
        RowAction(AppIcons.Unarchive, stringResource(Res.string.common_restore), { viewModel.setArchived(service.id, false) })
    } else {
        RowAction(AppIcons.Archive, stringResource(Res.string.common_archive), { viewModel.setArchived(service.id, true) })
    }
    ListRow(
        title = service.name,
        details = listOfNotNull(
            stringResource(if (service.chargedPerOrder) Res.string.common_charge_per_order else Res.string.common_charge_per_piece),
            service.laborMinutes?.minutesToDurationText(),
        ),
        value = service.suggestedPrice?.toMoney() ?: stringResource(Res.string.services_row_no_price),
        valueMuted = service.suggestedPrice == null,
        muted = service.archived,
        selected = selectedId == service.id,
        onClick = { viewModel.startEdit(service.id) },
        actions = listOf(
            RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.startEdit(service.id) }),
            archiveOrRestore,
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDelete(service.id) }, danger = true),
        ),
    )
}

@Composable
private fun ServiceFormPanel(form: ServiceFormState, viewModel: ServiceListViewModel) {
    FormPanel(
        title = stringResource(if (form.id == null) Res.string.services_form_new else Res.string.services_form_edit),
        onSave = viewModel::save,
        onCancel = viewModel::cancelEdit,
    ) {
        AppTextField(
            label = stringResource(Res.string.services_field_name),
            value = form.name,
            onValueChange = { text -> viewModel.updateForm { it.copy(name = text) } },
            placeholder = stringResource(Res.string.services_field_name_hint),
            error = form.nameError?.asString(),
        )
        AppTextField(
            label = stringResource(Res.string.services_field_price, LocalCurrency.current.symbol),
            value = form.priceText,
            onValueChange = { text -> viewModel.updateForm { it.copy(priceText = text) } },
            placeholder = stringResource(Res.string.common_optional),
            error = form.priceError?.asString(),
            help = HelpText(stringResource(Res.string.services_help_price), stringResource(Res.string.services_help_price_detail)),
        )
        AppChoiceField(
            label = stringResource(Res.string.common_field_charge),
            options = listOf(stringResource(Res.string.common_charge_per_piece), stringResource(Res.string.common_charge_per_order)),
            selectedIndex = if (form.chargedPerOrder) 1 else 0,
            onSelect = { index -> viewModel.updateForm { it.copy(chargedPerOrder = index == 1) } },
            help = HelpText(stringResource(Res.string.common_help_charge), stringResource(Res.string.services_help_charge)),
        )
        AppTextField(
            label = stringResource(Res.string.services_field_minutes),
            value = form.minutesText,
            onValueChange = { text -> viewModel.updateForm { it.copy(minutesText = text) } },
            placeholder = stringResource(Res.string.services_field_minutes_hint),
            error = form.minutesError?.asString(),
            help = HelpText(stringResource(Res.string.services_help_minutes)),
        )
    }
}
