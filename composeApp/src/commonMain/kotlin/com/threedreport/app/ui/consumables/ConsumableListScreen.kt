package com.threedreport.app.ui.consumables

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_archive
import _3dreport.composeapp.generated.resources.common_charge_per_order
import _3dreport.composeapp.generated.resources.common_charge_per_piece
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.common_edit
import _3dreport.composeapp.generated.resources.common_field_charge
import _3dreport.composeapp.generated.resources.common_help_charge
import _3dreport.composeapp.generated.resources.common_restore
import _3dreport.composeapp.generated.resources.consumables_action_new
import _3dreport.composeapp.generated.resources.consumables_delete_message
import _3dreport.composeapp.generated.resources.consumables_delete_title
import _3dreport.composeapp.generated.resources.consumables_delete_what
import _3dreport.composeapp.generated.resources.consumables_empty
import _3dreport.composeapp.generated.resources.consumables_field_cost
import _3dreport.composeapp.generated.resources.consumables_field_name
import _3dreport.composeapp.generated.resources.consumables_field_name_hint
import _3dreport.composeapp.generated.resources.consumables_form_edit
import _3dreport.composeapp.generated.resources.consumables_form_new
import _3dreport.composeapp.generated.resources.consumables_help_charge
import _3dreport.composeapp.generated.resources.consumables_help_cost
import _3dreport.composeapp.generated.resources.consumables_title
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
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.core.model.Consumable
import org.jetbrains.compose.resources.stringResource

/**
 * Tela de Insumos (decisão 122): argola, ímã, parafuso, caixa, saquinho, o que entra em cada peça e custa dinheiro
 * além do filamento. É o molde das telas de lista: cabeçalho, linhas, painel de formulário à direita.
 */
@Composable
fun ConsumableListScreen(viewModel: ConsumableListViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    ObserveNotices(viewModel.notices)

    val form = state.form
    val panel: (@Composable () -> Unit)? = form?.let { open ->
        { ConsumableFormPanel(open, viewModel) }
    }
    ListScreenLayout(panel = panel, modifier = modifier) {
        ScreenHeader(stringResource(Res.string.consumables_title)) {
            if (!state.isEmpty) {
                AppButton(
                    stringResource(Res.string.consumables_action_new),
                    onClick = viewModel::startAdd,
                    kind = if (form == null) ButtonKind.Primary else ButtonKind.Secondary,
                    icon = AppIcons.Add,
                )
            }
        }
        if (state.active.isEmpty()) {
            EmptyState(
                stringResource(Res.string.consumables_empty),
                actionLabel = stringResource(Res.string.consumables_action_new).takeIf { state.isEmpty },
                onAction = viewModel::startAdd.takeIf { state.isEmpty },
            )
        }
        state.active.forEach { key(it.id) { ConsumableRow(it, viewModel) } }
        ArchivedSection(state.archived.size) { state.archived.forEach { key(it.id) { ConsumableRow(it, viewModel) } } }
    }

    state.pendingDelete?.let { pending ->
        DeleteOrArchiveDialog(
            title = stringResource(Res.string.consumables_delete_title),
            what = stringResource(Res.string.consumables_delete_what, pending.name),
            usageCount = pending.usageCount,
            deleteMessage = stringResource(Res.string.consumables_delete_message, pending.name),
            onArchive = { viewModel.setArchived(pending.id, true) }.takeIf { pending.canArchive },
            onDelete = { viewModel.delete(pending.id) },
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun ConsumableRow(consumable: Consumable, viewModel: ConsumableListViewModel) {
    val archiveOrRestore = if (consumable.archived) {
        RowAction(AppIcons.Unarchive, stringResource(Res.string.common_restore), { viewModel.setArchived(consumable.id, false) })
    } else {
        RowAction(AppIcons.Archive, stringResource(Res.string.common_archive), { viewModel.setArchived(consumable.id, true) })
    }
    ListRow(
        title = consumable.name,
        details = listOf(stringResource(if (consumable.chargedPerOrder) Res.string.common_charge_per_order else Res.string.common_charge_per_piece)),
        value = consumable.unitCost.toMoney(),
        muted = consumable.archived,
        onClick = { viewModel.startEdit(consumable.id) },
        actions = listOf(
            RowAction(AppIcons.Edit, stringResource(Res.string.common_edit), { viewModel.startEdit(consumable.id) }),
            archiveOrRestore,
            RowAction(AppIcons.Delete, stringResource(Res.string.common_delete), { viewModel.requestDelete(consumable.id) }, danger = true),
        ),
    )
}

@Composable
private fun ConsumableFormPanel(form: ConsumableFormState, viewModel: ConsumableListViewModel) {
    FormPanel(
        title = stringResource(if (form.id == null) Res.string.consumables_form_new else Res.string.consumables_form_edit),
        onSave = viewModel::save,
        onCancel = viewModel::cancelEdit,
    ) {
        AppTextField(
            label = stringResource(Res.string.consumables_field_name),
            value = form.name,
            onValueChange = { text -> viewModel.updateForm { it.copy(name = text) } },
            placeholder = stringResource(Res.string.consumables_field_name_hint),
            error = form.nameError?.asString(),
        )
        AppTextField(
            label = stringResource(Res.string.consumables_field_cost, LocalCurrency.current.symbol),
            value = form.unitCostText,
            onValueChange = { text -> viewModel.updateForm { it.copy(unitCostText = text) } },
            error = form.unitCostError?.asString(),
            help = HelpText(stringResource(Res.string.consumables_help_cost)),
        )
        AppChoiceField(
            label = stringResource(Res.string.common_field_charge),
            options = listOf(stringResource(Res.string.common_charge_per_piece), stringResource(Res.string.common_charge_per_order)),
            selectedIndex = if (form.chargedPerOrder) 1 else 0,
            onSelect = { index -> viewModel.updateForm { it.copy(chargedPerOrder = index == 1) } },
            help = HelpText(stringResource(Res.string.common_help_charge), stringResource(Res.string.consumables_help_charge)),
        )
    }
}
