package com.threedreport.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Um pagamento recebido num pedido (decisão 146): o sinal, uma parcela ou o resto. Fica na lista
 * [SavedQuote.payments], na ordem em que foi registrado; é essa ordem que numera o recibo (`#0042-1`,
 * ver [SavedQuote.receiptNumber]).
 *
 * Não entra no preço, no lucro nem no Dashboard: é só o registro do que já entrou.
 *
 * @property id identificador único, atribuído ao registrar.
 * @property amount valor recebido, na moeda do pedido. Sempre maior que zero.
 * @property method forma do pagamento.
 * @property epochDay dia do pagamento, em dias desde 01/01/1970 (data de calendário, como
 *   [SavedQuote.deliveryDateEpochDay]).
 */
@Serializable
data class Payment(
    val id: String,
    val amount: Double,
    val method: PaymentMethod,
    val epochDay: Long,
) {
    init {
        require(isValidAmount(amount)) { "amount precisa ser maior que zero: $amount" }
    }

    companion object {
        /** Se [amount] serve como valor de pagamento (finito e maior que zero). É o que o formulário confere antes de registrar. */
        fun isValidAmount(amount: Double): Boolean = amount.isFinite() && amount > 0.0
    }
}

/**
 * Forma de um pagamento. O nome gravado no arquivo ([SerialName]) é fixo e em inglês, como em [OrderStatus];
 * o nome que a pessoa lê vem dos recursos do app.
 */
@Serializable
enum class PaymentMethod {
    @SerialName("pix")
    PIX,

    @SerialName("card")
    CARD,

    @SerialName("cash")
    CASH,

    @SerialName("other")
    OTHER,
}

/**
 * Situação do pagamento de um pedido, calculada a partir dos pagamentos e do sinal (nunca gravada, ver
 * [SavedQuote.paymentStatus]).
 */
enum class PaymentStatus {
    /** Nenhum pagamento registrado. */
    NOT_PAID,

    /** Há sinal pedido, o que entrou cobre o sinal e ainda falta pagar. */
    DEPOSIT_PAID,

    /** Entrou alguma coisa, ainda falta, e não é o caso de [DEPOSIT_PAID]. */
    PARTIALLY_PAID,

    /** Não falta nada, com pelo menos um pagamento. */
    PAID,
}

/**
 * O que cobrar agora de um pedido ([SavedQuote.amountDue]): o que falta do sinal enquanto ele não foi pago
 * ([isDeposit]), ou o que falta do pedido.
 */
data class AmountDue(val amount: Double, val isDeposit: Boolean)
