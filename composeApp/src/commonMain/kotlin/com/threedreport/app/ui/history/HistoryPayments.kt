package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.payments_error_amount
import _3dreport.composeapp.generated.resources.payments_notice_deleted
import _3dreport.composeapp.generated.resources.payments_notice_receipt
import _3dreport.composeapp.generated.resources.payments_notice_registered
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.roundToCents
import kotlin.math.abs

/**
 * O diálogo "Pagamentos" (decisão 146, item 13): a lista, o sinal editável e o "Registrar pagamento". O diálogo vive em
 * [HistoryUiState.payments]; aqui ficam os eventos dele. As contas (pago, falta, situação) são do `core` ([SavedQuote]).
 */
internal class HistoryPayments(
    private val repository: QuoteHistoryRepository,
    private val state: () -> HistoryUiState,
    private val change: ((HistoryUiState) -> HistoryUiState) -> Unit,
    private val notify: (UiNotice) -> Unit,
    private val today: () -> Long,
    /** A moeda do app: o campo de número lê com o separador dela. */
    private val appCurrency: () -> Currency,
    private val generateReceipt: (SavedQuote, String) -> Unit,
) {
    /** A forma do último pagamento registrado nesta sessão, que o próximo formulário já traz. */
    private var lastMethod: PaymentMethod? = null

    private fun editing(transform: (PaymentsDialog) -> PaymentsDialog) = change { s -> s.copy(payments = s.payments?.let(transform)) }

    fun open(quote: SavedQuote) {
        val current = repository.savedQuotes.value.find { it.id == quote.id } ?: quote
        if (!current.isOrder) return
        val deposit = current.depositAmount.takeIf { it > 0.0 }?.toInputText(appCurrency().decimalSeparator).orEmpty()
        change { it.copy(payments = PaymentsDialog(current, deposit)) }
    }

    /** Fecha o diálogo, gravando antes o sinal digitado ([commitDeposit]). */
    fun close() {
        commitDeposit()
        change { it.copy(payments = null) }
    }

    /** O sinal como a pessoa digita: só confere o número; a gravação fica pro [commitDeposit] (sair do campo ou fechar). */
    fun setDeposit(text: String) {
        val error = when (val value = parseDeposit(text)) {
            null -> uiText(Res.string.common_error_number_invalid)
            else -> if (value < 0) uiText(Res.string.common_error_negative) else null
        }
        editing { it.copy(depositText = text, depositError = error) }
    }

    /**
     * Grava o sinal do campo, se for um número válido e diferente do gravado. O que o pedido limita ao total volta no
     * campo já limitado. Chamado ao sair do campo, ao fechar o diálogo e antes de abrir o "Registrar pagamento" (que
     * parte do sinal pra sugerir o valor).
     */
    fun commitDeposit() {
        val open = state().payments ?: return
        val value = parseDeposit(open.depositText) ?: return
        if (value < 0) return
        val current = repository.savedQuotes.value.find { it.id == open.quote.id } ?: return
        if (abs(current.depositAmount - value) > 0.004) repository.updateDeposit(open.quote.id, value)
        val saved = repository.savedQuotes.value.find { it.id == open.quote.id }?.depositAmount ?: value
        if (abs(saved - value) > 0.004) editing { it.copy(depositText = saved.toInputText(appCurrency().decimalSeparator), depositError = null) }
    }

    private fun parseDeposit(text: String): Double? = if (text.isBlank()) 0.0 else parseDecimal(text, NumberKind.AMOUNT, appCurrency().decimalSeparator)

    /** Abre o formulário já com o valor do que falta (do sinal, ou do pedido), a última forma usada e hoje. */
    fun startRegistering() {
        commitDeposit()
        val open = state().payments ?: return
        val quote = open.quote
        val amount = quote.amountDue?.amount?.toInputText(appCurrency().decimalSeparator).orEmpty()
        val method = lastMethod ?: quote.payments.lastOrNull()?.method ?: PaymentMethod.PIX
        editing { it.copy(form = PaymentForm(amount, method, today())) }
    }

    fun updateForm(transform: (PaymentForm) -> PaymentForm) =
        editing { dialog -> dialog.copy(form = dialog.form?.let { transform(it).copy(error = null) }) }

    fun cancelForm() = editing { it.copy(form = null) }

    fun register() {
        val open = state().payments ?: return
        val form = open.form ?: return
        // Em centavos, como o repositório grava: "0,004" arredonda a zero e é valor inválido, não pagamento.
        val amount = parseDecimal(form.amountText, NumberKind.AMOUNT, appCurrency().decimalSeparator)?.let(::roundToCents)
        if (amount == null || !Payment.isValidAmount(amount)) {
            editing { dialog -> dialog.copy(form = form.copy(error = uiText(Res.string.payments_error_amount))) }
            return
        }
        val payment = repository.addPayment(open.quote.id, amount, form.method, form.epochDay) ?: return
        lastMethod = form.method
        editing { it.copy(form = null) }
        val id = open.quote.id
        notify(
            UiNotice(
                uiText(Res.string.payments_notice_registered, amount.toCurrencyText(open.quote.currency)),
                actionLabel = uiText(Res.string.payments_notice_receipt),
                action = { repository.savedQuotes.value.find { it.id == id }?.let { generateReceipt(it, payment.id) } },
            ),
        )
    }

    fun requestDelete(payment: Payment) = editing { it.copy(deleting = payment) }

    fun cancelDelete() = editing { it.copy(deleting = null) }

    fun confirmDelete() {
        val open = state().payments ?: return
        val payment = open.deleting ?: return
        repository.deletePayment(open.quote.id, payment.id)
        editing { it.copy(deleting = null) }
        notify(UiNotice(uiText(Res.string.payments_notice_deleted)))
    }

    /** "Recibo" de um pagamento da lista: o PDF do pedido como está agora. */
    fun receipt(paymentId: String) {
        val quote = state().payments?.quote ?: return
        generateReceipt(quote, paymentId)
    }
}
