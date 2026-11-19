package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_delete
import _3dreport.composeapp.generated.resources.payment_method_card
import _3dreport.composeapp.generated.resources.payment_method_cash
import _3dreport.composeapp.generated.resources.payment_method_other
import _3dreport.composeapp.generated.resources.payment_method_pix
import _3dreport.composeapp.generated.resources.payments_action_register
import _3dreport.composeapp.generated.resources.payments_back
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
import _3dreport.composeapp.generated.resources.payments_list_title
import _3dreport.composeapp.generated.resources.payments_paid_of
import _3dreport.composeapp.generated.resources.payments_receipt_description
import _3dreport.composeapp.generated.resources.payments_register_confirm
import _3dreport.composeapp.generated.resources.payments_register_title
import _3dreport.composeapp.generated.resources.payments_remaining
import _3dreport.composeapp.generated.resources.payments_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.threedreport.app.platform.formatDate
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.DateShortcut
import com.threedreport.app.ui.components.DateShortcutPicker
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
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
 * "Pagamentos" de um pedido (decisão 146, item 13). Um [AppDialog] só, de largura fixa, com três passos que trocam o
 * título, o corpo e os botões: a lista (o que falta em destaque, o sinal, os pagamentos), o "Registrar pagamento" (com
 * "Voltar") e a confirmação de excluir. Só o calendário abre por cima. A ação principal é registrar, a não ser que não
 * falte nada: aí ela é secundária.
 */
@Composable
fun PaymentsDialogView(dialog: PaymentsDialog, viewModel: QuoteHistoryViewModel, today: Long) {
    val form = dialog.form
    val deleting = dialog.deleting
    val quote = dialog.quote
    val currency = quote.currency
    val amountOf = { value: Double -> value.toCurrencyText(currency) }
    AppDialog(
        title = stringResource(
            when {
                deleting != null -> Res.string.payments_delete_title
                form != null -> Res.string.payments_register_title
                else -> Res.string.payments_title
            },
        ),
        onDismiss = when {
            deleting != null -> viewModel::cancelDeletePayment
            form != null -> viewModel::cancelPaymentForm
            else -> viewModel::closePayments
        },
        dismissOnClickOutside = form == null && deleting == null,
        width = AppDimens.paymentsDialogWidth,
        buttons = {
            when {
                deleting != null -> {
                    AppButton(stringResource(Res.string.payments_back), viewModel::cancelDeletePayment)
                    AppButton(stringResource(Res.string.common_delete), viewModel::confirmDeletePayment, kind = ButtonKind.Danger)
                }

                form != null -> {
                    AppButton(stringResource(Res.string.payments_back), viewModel::cancelPaymentForm)
                    AppButton(stringResource(Res.string.payments_register_confirm), viewModel::registerPayment, kind = ButtonKind.Primary)
                }

                else -> {
                    AppButton(stringResource(Res.string.common_close), viewModel::closePayments)
                    AppButton(
                        stringResource(Res.string.payments_action_register),
                        viewModel::startRegisteringPayment,
                        kind = if (quote.remaining > 0.0) ButtonKind.Primary else ButtonKind.Secondary,
                        icon = AppIcons.Add,
                    )
                }
            }
        },
    ) {
        when {
            deleting != null -> DialogText(stringResource(Res.string.payments_delete_message, amountOf(deleting.amount)))

            form != null -> PaymentFormFields(form, currency.symbol, viewModel, today)

            else -> {
                PaymentsSummary(dialog, amountOf)
                AppNumberField(
                    label = stringResource(Res.string.payments_deposit),
                    value = dialog.depositText,
                    onValueChange = viewModel::setDeposit,
                    kind = NumberKind.AMOUNT,
                    prefix = currency.symbol,
                    placeholder = "0",
                    error = dialog.depositError?.asString(),
                    // Grava ao sair do campo, e não a cada tecla.
                    modifier = Modifier.width(AppDimens.moneyFieldWidth).onFocusChanged { if (!it.hasFocus) viewModel.commitDeposit() },
                )
                // 24 antes da lista: o respiro do diálogo (8) mais este espaço e mais 8.
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Text(stringResource(Res.string.payments_list_title), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (quote.payments.isEmpty()) DialogText(stringResource(Res.string.payments_empty), muted = true)
                quote.payments.forEach { payment ->
                    val amount = amountOf(payment.amount)
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
        }
    }
}

/** O que falta como número em destaque, a situação como selo, e o que já foi pago de apoio. */
@Composable
private fun PaymentsSummary(dialog: PaymentsDialog, amountOf: (Double) -> String) {
    val quote = dialog.quote
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(stringResource(Res.string.payments_remaining), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumericText(amountOf(quote.remaining), style = MaterialTheme.appType.statValue, color = MaterialTheme.colorScheme.onSurface)
        }
        PaymentBadgeView(quote.paymentStatus)
    }
    Text(
        stringResource(Res.string.payments_paid_of, amountOf(quote.paidTotal), amountOf(quote.totalWithServices)),
        style = MaterialTheme.appType.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** O passo "Registrar pagamento": valor (já preenchido), forma e dia. */
@Composable
private fun PaymentFormFields(form: PaymentForm, symbol: String, viewModel: QuoteHistoryViewModel, today: Long) {
    val methods = PaymentMethod.entries
    val labels = methods.associateWith { stringResource(it.labelRes()) }
    AppNumberField(
        label = stringResource(Res.string.payments_field_amount),
        value = form.amountText,
        onValueChange = { text -> viewModel.updatePaymentForm { it.copy(amountText = text) } },
        kind = NumberKind.AMOUNT,
        prefix = symbol,
        error = form.error?.asString(),
        modifier = Modifier.width(AppDimens.moneyFieldWidth),
    )
    AppDropdownField(
        label = stringResource(Res.string.payments_field_method),
        items = methods,
        selected = form.method,
        itemLabel = { labels.getValue(it) },
        displayText = { labels.getValue(it) },
        onSelect = { method -> viewModel.updatePaymentForm { it.copy(method = method) } },
        modifier = Modifier.width(AppDimens.moneyFieldWidth),
    )
    Text(stringResource(Res.string.payments_field_date), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    DateShortcutPicker(
        epochDay = form.epochDay,
        shortcuts = listOf(
            DateShortcut(stringResource(Res.string.payments_date_today), today),
            DateShortcut(stringResource(Res.string.payments_date_yesterday), today - 1),
        ),
        today = today,
        onChange = { day -> if (day != null) viewModel.updatePaymentForm { it.copy(epochDay = day) } },
        allowFuture = false,
    )
}
