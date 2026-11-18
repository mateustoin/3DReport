package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.payment_method_card
import _3dreport.composeapp.generated.resources.payment_method_cash
import _3dreport.composeapp.generated.resources.payment_method_other
import _3dreport.composeapp.generated.resources.payment_method_pix
import _3dreport.composeapp.generated.resources.payments_action_register
import _3dreport.composeapp.generated.resources.payments_date_pick
import _3dreport.composeapp.generated.resources.payments_date_today
import _3dreport.composeapp.generated.resources.payments_date_yesterday
import _3dreport.composeapp.generated.resources.payments_delete_description
import _3dreport.composeapp.generated.resources.payments_delete_message
import _3dreport.composeapp.generated.resources.payments_delete_title
import _3dreport.composeapp.generated.resources.payments_deposit
import _3dreport.composeapp.generated.resources.payments_empty
import _3dreport.composeapp.generated.resources.payments_field_amount
import _3dreport.composeapp.generated.resources.payments_field_date
import _3dreport.composeapp.generated.resources.payments_field_method
import _3dreport.composeapp.generated.resources.payments_paid
import _3dreport.composeapp.generated.resources.payments_receipt_description
import _3dreport.composeapp.generated.resources.payments_register_confirm
import _3dreport.composeapp.generated.resources.payments_register_title
import _3dreport.composeapp.generated.resources.payments_remaining
import _3dreport.composeapp.generated.resources.payments_status
import _3dreport.composeapp.generated.resources.payments_title
import _3dreport.composeapp.generated.resources.payments_total
import _3dreport.composeapp.generated.resources.quote_deadline_use
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.threedreport.app.platform.epochDayToUtcMillis
import com.threedreport.app.platform.formatDate
import com.threedreport.app.platform.utcMillisToEpochDay
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.InfoRow
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.components.SegmentedChoice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.core.model.PaymentMethod
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** O nome da forma de pagamento, o mesmo na lista, no formulário e no recibo. */
fun PaymentMethod.labelRes(): StringResource = when (this) {
    PaymentMethod.PIX -> Res.string.payment_method_pix
    PaymentMethod.CARD -> Res.string.payment_method_card
    PaymentMethod.CASH -> Res.string.payment_method_cash
    PaymentMethod.OTHER -> Res.string.payment_method_other
}

/**
 * "Pagamentos" de um pedido (decisão 146, item 13): total, pago, falta e situação, o sinal editável, a lista de
 * pagamentos (com "Recibo" e excluir) e o "Registrar pagamento", a única ação principal. Só desenha o [PaymentsDialog].
 */
@Composable
fun PaymentsDialogView(dialog: PaymentsDialog, viewModel: QuoteHistoryViewModel, today: Long) {
    val quote = dialog.quote
    val currency = quote.currency
    AppDialog(
        title = stringResource(Res.string.payments_title),
        onDismiss = viewModel::closePayments,
        buttons = {
            AppButton(stringResource(Res.string.common_close), viewModel::closePayments)
            AppButton(stringResource(Res.string.payments_action_register), viewModel::startRegisteringPayment, kind = ButtonKind.Primary, icon = AppIcons.Add)
        },
    ) {
        InfoRow(stringResource(Res.string.payments_total), quote.totalWithServices.toCurrencyText(currency))
        InfoRow(stringResource(Res.string.payments_paid), quote.paidTotal.toCurrencyText(currency))
        InfoRow(stringResource(Res.string.payments_remaining), quote.remaining.toCurrencyText(currency))
        InfoRow(stringResource(Res.string.payments_status), stringResource(quote.paymentStatus.labelRes()))
        AppNumberField(
            label = stringResource(Res.string.payments_deposit),
            value = dialog.depositText,
            onValueChange = viewModel::setDeposit,
            kind = NumberKind.AMOUNT,
            prefix = currency.symbol,
            placeholder = "0",
            error = dialog.depositError?.asString(),
        )
        if (quote.payments.isEmpty()) {
            DialogText(stringResource(Res.string.payments_empty), muted = true)
        }
        quote.payments.forEach { payment ->
            val amount = payment.amount.toCurrencyText(currency)
            ListRow(
                title = formatDate(payment.epochDay),
                details = listOf(stringResource(payment.method.labelRes())),
                value = amount,
                actions = listOf(
                    RowAction(AppIcons.ReceiptLong, stringResource(Res.string.payments_receipt_description, amount), onClick = { viewModel.generateReceipt(payment.id) }),
                    RowAction(AppIcons.Delete, stringResource(Res.string.payments_delete_description, amount), { viewModel.requestDeletePayment(payment) }, danger = true),
                ),
            )
        }
    }
    dialog.form?.let { PaymentFormDialog(it, currency.symbol, viewModel, today) }
    dialog.deleting?.let { payment ->
        ConfirmDialog(
            title = stringResource(Res.string.payments_delete_title),
            message = stringResource(Res.string.payments_delete_message, payment.amount.toCurrencyText(currency)),
            onConfirm = viewModel::confirmDeletePayment,
            onDismiss = viewModel::cancelDeletePayment,
            confirmLabel = stringResource(Res.string.common_delete),
        )
    }
}

/** O formulário "Registrar pagamento": valor (já preenchido), forma e dia. */
@Composable
private fun PaymentFormDialog(form: PaymentForm, symbol: String, viewModel: QuoteHistoryViewModel, today: Long) {
    val methods = PaymentMethod.entries
    AppDialog(
        title = stringResource(Res.string.payments_register_title),
        onDismiss = viewModel::cancelPaymentForm,
        dismissOnClickOutside = false,
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), viewModel::cancelPaymentForm)
            AppButton(stringResource(Res.string.payments_register_confirm), viewModel::registerPayment, kind = ButtonKind.Primary)
        },
    ) {
        AppNumberField(
            label = stringResource(Res.string.payments_field_amount),
            value = form.amountText,
            onValueChange = { text -> viewModel.updatePaymentForm { it.copy(amountText = text) } },
            kind = NumberKind.AMOUNT,
            prefix = symbol,
            error = form.error?.asString(),
        )
        AppChoiceField(
            label = stringResource(Res.string.payments_field_method),
            options = methods.map { stringResource(it.labelRes()) },
            selectedIndex = methods.indexOf(form.method),
            onSelect = { index -> viewModel.updatePaymentForm { it.copy(method = methods[index]) } },
            fillWidth = true,
        )
        Text(stringResource(Res.string.payments_field_date), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PaymentDatePicker(form.epochDay, today) { day -> viewModel.updatePaymentForm { it.copy(epochDay = day) } }
    }
}

/**
 * O dia do pagamento: "Hoje", "Ontem" ou "Escolher data" (o calendário, até hoje: recibo de dia que ainda não
 * chegou não existe). Mostra a data escolhida quando não é um dos atalhos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDatePicker(epochDay: Long, today: Long, onChange: (Long) -> Unit) {
    var showCalendar by remember { mutableStateOf(false) }
    val selected = when (epochDay) {
        today -> 0
        today - 1 -> 1
        else -> 2
    }
    SegmentedChoice(
        options = listOf(
            stringResource(Res.string.payments_date_today),
            stringResource(Res.string.payments_date_yesterday),
            if (selected == 2) formatDate(epochDay) else stringResource(Res.string.payments_date_pick),
        ),
        selectedIndex = selected,
        onSelect = { index ->
            when (index) {
                0 -> onChange(today)
                1 -> onChange(today - 1)
                else -> showCalendar = true
            }
        },
    )
    if (showCalendar) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = epochDayToUtcMillis(epochDay),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcMillisToEpochDay(utcTimeMillis) <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            shape = MaterialTheme.shapes.large,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.appColors.card),
            confirmButton = {
                AppButton(
                    stringResource(Res.string.quote_deadline_use),
                    onClick = {
                        state.selectedDateMillis?.let { onChange(utcMillisToEpochDay(it)) }
                        showCalendar = false
                    },
                    kind = ButtonKind.Primary,
                    enabled = state.selectedDateMillis != null,
                )
            },
            dismissButton = { AppButton(stringResource(Res.string.common_cancel), onClick = { showCalendar = false }) },
        ) {
            DatePicker(state = state, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.appColors.card))
        }
    }
}
