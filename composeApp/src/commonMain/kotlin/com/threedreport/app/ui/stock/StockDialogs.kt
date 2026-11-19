package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_discard
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.stock_action_adjust_confirm
import _3dreport.composeapp.generated.resources.stock_action_stop
import _3dreport.composeapp.generated.resources.stock_action_track_confirm
import _3dreport.composeapp.generated.resources.stock_action_use_confirm
import _3dreport.composeapp.generated.resources.stock_action_weigh_confirm
import _3dreport.composeapp.generated.resources.stock_adjust_current
import _3dreport.composeapp.generated.resources.stock_delete_movement_message
import _3dreport.composeapp.generated.resources.stock_delete_movement_title
import _3dreport.composeapp.generated.resources.stock_delete_purchase_message
import _3dreport.composeapp.generated.resources.stock_delete_purchase_title
import _3dreport.composeapp.generated.resources.stock_dialog_adjust_title
import _3dreport.composeapp.generated.resources.stock_dialog_track_title
import _3dreport.composeapp.generated.resources.stock_dialog_use_title
import _3dreport.composeapp.generated.resources.stock_dialog_weigh_title
import _3dreport.composeapp.generated.resources.stock_discard_message
import _3dreport.composeapp.generated.resources.stock_discard_title
import _3dreport.composeapp.generated.resources.stock_field_current_balance
import _3dreport.composeapp.generated.resources.stock_field_new_balance
import _3dreport.composeapp.generated.resources.stock_field_note
import _3dreport.composeapp.generated.resources.stock_field_reason
import _3dreport.composeapp.generated.resources.stock_field_scale
import _3dreport.composeapp.generated.resources.stock_field_spool
import _3dreport.composeapp.generated.resources.stock_field_use_amount
import _3dreport.composeapp.generated.resources.stock_help_spool
import _3dreport.composeapp.generated.resources.stock_stop_message
import _3dreport.composeapp.generated.resources.stock_stop_title
import _3dreport.composeapp.generated.resources.stock_unit_g
import _3dreport.composeapp.generated.resources.stock_unit_un
import _3dreport.composeapp.generated.resources.stock_weigh_new
import _3dreport.composeapp.generated.resources.stock_weigh_was
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.StockItemRef
import org.jetbrains.compose.resources.stringResource

/**
 * O diálogo aberto da tela Estoque: pesar o carretel, consumo avulso, corrigir o saldo, controlar saldo e as três confirmações
 * (parar de controlar, excluir um movimento, excluir uma compra). O ViewModel valida ao confirmar e deixa o erro no campo.
 */
@Composable
internal fun StockDialogView(dialog: StockDialog, viewModel: StockViewModel) {
    val entry = dialog.entry
    val ref = entry?.ref
    val unit = stringResource(if (ref is StockItemRef.ConsumableRef) Res.string.stock_unit_un else Res.string.stock_unit_g)
    when (dialog.kind) {
        StockDialogKind.WEIGH -> FormDialog(
            title = stringResource(Res.string.stock_dialog_weigh_title),
            subtitle = entry?.label(),
            confirmLabel = stringResource(Res.string.stock_action_weigh_confirm),
            viewModel = viewModel,
        ) {
            AppNumberField(
                label = stringResource(Res.string.stock_field_scale),
                value = dialog.amountText,
                onValueChange = viewModel::setDialogAmount,
                kind = NumberKind.AMOUNT,
                error = dialog.error?.asString(),
                money = false,
            )
            AppNumberField(
                label = stringResource(Res.string.stock_field_spool),
                value = dialog.spoolText,
                onValueChange = viewModel::setDialogSpool,
                kind = NumberKind.AMOUNT,
                placeholder = stringResource(Res.string.common_optional),
                help = HelpText(stringResource(Res.string.stock_help_spool)),
                money = false,
            )
            dialog.newBalance?.let { newBalance ->
                if (ref != null) {
                    // O saldo novo em destaque: é o que a pessoa confere antes de gravar.
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs), verticalAlignment = Alignment.Bottom) {
                        NumericText(
                            stringResource(Res.string.stock_weigh_new, stockAmountText(ref, newBalance)),
                            style = MaterialTheme.appType.bodyStrong,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        NumericText(
                            stringResource(Res.string.stock_weigh_was, stockAmountText(ref, dialog.currentBalance)),
                            style = MaterialTheme.appType.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        StockDialogKind.MANUAL_USE -> FormDialog(
            title = stringResource(Res.string.stock_dialog_use_title),
            subtitle = entry?.label(),
            confirmLabel = stringResource(Res.string.stock_action_use_confirm),
            viewModel = viewModel,
        ) {
            AppNumberField(
                label = stringResource(Res.string.stock_field_use_amount, unit),
                value = dialog.amountText,
                onValueChange = viewModel::setDialogAmount,
                kind = NumberKind.AMOUNT,
                error = dialog.error?.asString(),
                money = false,
            )
            val reasons = ManualUseReason.entries
            val reasonLabels = reasons.associateWith { stringResource(it.labelRes()) }
            AppDropdownField(
                label = stringResource(Res.string.stock_field_reason),
                items = reasons,
                selected = dialog.reason,
                itemLabel = { reasonLabels.getValue(it) },
                displayText = { reasonLabels.getValue(it) },
                onSelect = viewModel::setDialogReason,
            )
            AppTextField(
                label = stringResource(Res.string.stock_field_note),
                value = dialog.noteText,
                onValueChange = viewModel::setDialogNote,
                placeholder = stringResource(Res.string.common_optional),
            )
        }

        StockDialogKind.ADJUST -> FormDialog(
            title = stringResource(Res.string.stock_dialog_adjust_title),
            subtitle = entry?.label(),
            confirmLabel = stringResource(Res.string.stock_action_adjust_confirm),
            viewModel = viewModel,
        ) {
            AppNumberField(
                label = stringResource(Res.string.stock_field_new_balance, unit),
                value = dialog.amountText,
                onValueChange = viewModel::setDialogAmount,
                kind = NumberKind.AMOUNT,
                error = dialog.error?.asString(),
                money = false,
            )
            if (ref != null) DialogText(stringResource(Res.string.stock_adjust_current, stockAmountText(ref, dialog.currentBalance)), muted = true)
        }

        StockDialogKind.TRACK -> FormDialog(
            title = stringResource(Res.string.stock_dialog_track_title),
            subtitle = entry?.label(),
            confirmLabel = stringResource(Res.string.stock_action_track_confirm),
            viewModel = viewModel,
        ) {
            AppNumberField(
                label = stringResource(Res.string.stock_field_current_balance, unit),
                value = dialog.amountText,
                onValueChange = viewModel::setDialogAmount,
                kind = NumberKind.AMOUNT,
                error = dialog.error?.asString(),
                money = false,
            )
        }

        StockDialogKind.STOP_TRACKING -> ConfirmDialog(
            title = stringResource(Res.string.stock_stop_title),
            message = stringResource(Res.string.stock_stop_message, entry?.label().orEmpty()),
            confirmLabel = stringResource(Res.string.stock_action_stop),
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog,
        )

        StockDialogKind.DELETE_MOVEMENT -> ConfirmDialog(
            title = stringResource(Res.string.stock_delete_movement_title),
            message = stringResource(Res.string.stock_delete_movement_message),
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog,
        )

        StockDialogKind.DISCARD_PURCHASE -> ConfirmDialog(
            title = stringResource(Res.string.stock_discard_title),
            message = stringResource(Res.string.stock_discard_message),
            confirmLabel = stringResource(Res.string.common_discard),
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog,
        )

        StockDialogKind.DELETE_PURCHASE -> ConfirmDialog(
            title = stringResource(Res.string.stock_delete_purchase_title),
            message = stringResource(Res.string.stock_delete_purchase_message),
            onConfirm = viewModel::confirmDialog,
            onDismiss = viewModel::dismissDialog,
        )
    }
}

/** O molde dos quatro diálogos de formulário: título, o nome do item e os campos, e Cancelar e a ação principal no pé. */
@Composable
private fun FormDialog(
    title: String,
    subtitle: String?,
    confirmLabel: String,
    viewModel: StockViewModel,
    content: @Composable () -> Unit,
) {
    AppDialog(
        title = title,
        onDismiss = viewModel::dismissDialog,
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), viewModel::dismissDialog)
            AppButton(confirmLabel, viewModel::confirmDialog, kind = ButtonKind.Primary)
        },
    ) {
        if (subtitle != null) DialogText(subtitle, muted = true)
        content()
    }
}
