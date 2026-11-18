package com.threedreport.core.model

import com.threedreport.core.costsOf
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Situação do pagamento, sinal e valor do Pix de um pedido (decisões 145 e 146). */
class SavedQuotePaymentsTest {

    private val filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)

    /** Pedido de [total] (preço da peça, sem serviço nem frete). */
    private fun order(
        total: Double = 100.0,
        deposit: Double = 0.0,
        payments: List<Double> = emptyList(),
        currency: Currency = Currency.BRL,
        kind: QuoteKind = QuoteKind.ORDER,
    ) = SavedQuote(
        id = "1",
        name = "Peça",
        quote = Quote(
            prints = listOf(quotedPrint(PrintJob(filament = filament, filamentLengthMeters = 1.0, printTimeMinutes = 60.0))),
            costs = costsOf(),
            salePrice = total,
        ),
        savedAtEpochMillis = 0L,
        depositAmount = deposit,
        payments = payments.mapIndexed { index, amount -> Payment("p$index", amount, PaymentMethod.PIX, 0L) },
        currency = currency,
        kind = kind,
        number = 42,
    )

    @Test
    fun anOrderWithoutPaymentsOwesItsWholeTotal() {
        val saved = order(total = 174.34)

        assertEquals(0.0, saved.paidTotal)
        assertEquals(174.34, saved.remaining)
        assertEquals(PaymentStatus.NOT_PAID, saved.paymentStatus)
        assertFalse(saved.hasPaymentTracking, "pedido sem sinal nem pagamento fica como antes")
    }

    @Test
    fun aDepositAloneShowsAsNotPaid() {
        val saved = order(deposit = 50.0)

        assertEquals(PaymentStatus.NOT_PAID, saved.paymentStatus)
        assertTrue(saved.hasPaymentTracking)
        assertEquals(50.0, saved.pendingDeposit)
    }

    @Test
    fun theDepositPaidLeavesTheRestToPay() {
        val saved = order(deposit = 50.0, payments = listOf(50.0))

        assertEquals(PaymentStatus.DEPOSIT_PAID, saved.paymentStatus)
        assertEquals(0.0, saved.pendingDeposit)
        assertEquals(50.0, saved.remaining)
    }

    @Test
    fun payingLessThanTheDepositIsPartial() {
        val saved = order(deposit = 50.0, payments = listOf(20.0))

        assertEquals(PaymentStatus.PARTIALLY_PAID, saved.paymentStatus)
        assertEquals(30.0, saved.pendingDeposit, 1e-9)
    }

    @Test
    fun payingPartWithoutDepositIsPartial() {
        assertEquals(PaymentStatus.PARTIALLY_PAID, order(payments = listOf(30.0)).paymentStatus)
    }

    @Test
    fun paymentsThatAddUpToTheTotalArePaidEvenWithFloatingPointNoise() {
        // 0,1 + 0,2 não dá 0,3 exato em Double: a falta não pode ficar em 0,0000000001.
        val saved = order(total = 0.3, payments = listOf(0.1, 0.2))

        assertEquals(0.0, saved.remaining)
        assertEquals(PaymentStatus.PAID, saved.paymentStatus)
    }

    @Test
    fun overpayingIsAcceptedAndNothingIsLeft() {
        val saved = order(total = 100.0, payments = listOf(105.0))

        assertEquals(0.0, saved.remaining)
        assertEquals(105.0, saved.paidTotal)
        assertEquals(PaymentStatus.PAID, saved.paymentStatus)
    }

    @Test
    fun remainingIsOverTheTotalWithServicesAndShipping() {
        val saved = order(total = 100.0, payments = listOf(100.0)).copy(
            services = listOf(QuoteService(id = "s", name = "Pintura", price = 10.0, chargedPerOrder = true)),
            shippingCost = 15.0,
        )

        assertEquals(25.0, saved.remaining, 1e-9)
        assertEquals(PaymentStatus.PARTIALLY_PAID, saved.paymentStatus)
    }

    @Test
    fun theDepositIsNeverMoreThanTheTotal() {
        val saved = order(total = 40.0, deposit = 50.0)

        assertEquals(40.0, saved.requestedDeposit)
        assertEquals(40.0, saved.pendingDeposit)
    }

    @Test
    fun thePixChargesThePendingDepositThenTheRest() {
        assertEquals(AmountDue(50.0, isDeposit = true), order(deposit = 50.0).pixCharge)
        assertEquals(AmountDue(30.0, isDeposit = true), order(deposit = 50.0, payments = listOf(20.0)).pixCharge)
        assertEquals(AmountDue(50.0, isDeposit = false), order(deposit = 50.0, payments = listOf(50.0)).pixCharge)
        assertEquals(AmountDue(100.0, isDeposit = false), order().pixCharge)
        assertEquals(100.0, order().pixAmount)
    }

    @Test
    fun noPixForAPaidOrderAnotherCurrencyOrAProduct() {
        assertNull(order(payments = listOf(100.0)).pixCharge)
        assertNull(order(currency = Currency.USD).pixCharge)
        assertNull(order(kind = QuoteKind.PRODUCT).pixCharge)
        assertNull(order(currency = Currency.USD).pixAmount)
    }

    @Test
    fun theSuggestedPaymentIgnoresTheCurrency() {
        // O formulário de pagamento vale pra qualquer moeda; só o Pix é em real.
        assertEquals(AmountDue(100.0, isDeposit = false), order(currency = Currency.USD).amountDue)
        assertNull(order(payments = listOf(100.0)).amountDue)
    }

    @Test
    fun theReceiptNumberIsTheOrderNumberAndThePaymentPosition() {
        val saved = order(payments = listOf(10.0, 20.0))

        assertEquals("#0042-1", saved.receiptNumber("p0"))
        assertEquals("#0042-2", saved.receiptNumber("p1"))
        assertNull(saved.receiptNumber("outro"))
        assertNull(saved.copy(number = 0).receiptNumber("p0"))
    }

    @Test
    fun aNewOrderDepositIsTheTotalTimesTheRateRoundedToCents() {
        assertEquals(52.30, SavedQuote.initialDeposit(total = 174.34, rate = 0.3), 1e-9)
        assertEquals(0.0, SavedQuote.initialDeposit(total = 174.34, rate = 0.0))
        assertEquals(174.34, SavedQuote.initialDeposit(total = 174.34, rate = 1.5), 1e-9)
    }

    @Test
    fun reopeningKeepsTheDepositUnlessThereWasNone() {
        // Mantém o que já estava, mesmo com o % de hoje diferente.
        assertEquals(50.0, order(deposit = 50.0).withDepositAfterEdit(rate = 0.3).depositAmount)
        // Sem sinal e % maior que zero: ganha o do % padrão.
        assertEquals(30.0, order().withDepositAfterEdit(rate = 0.3).depositAmount, 1e-9)
        // Sem sinal e % zero: continua sem.
        assertEquals(0.0, order().withDepositAfterEdit(rate = 0.0).depositAmount)
        // Total que caiu abaixo do sinal: o sinal cai junto.
        assertEquals(40.0, order(total = 40.0, deposit = 50.0).withDepositAfterEdit(rate = 0.0).depositAmount)
    }

    @Test
    fun aProductNeverGetsADeposit() {
        assertEquals(0.0, order(kind = QuoteKind.PRODUCT).withDepositAfterEdit(rate = 0.5).depositAmount)
    }

    @Test
    fun settingTheDepositKeepsItBetweenZeroAndTheTotal() {
        assertEquals(60.0, order().withDeposit(60.0).depositAmount)
        assertEquals(100.0, order().withDeposit(250.0).depositAmount)
        assertEquals(0.0, order().withDeposit(-5.0).depositAmount)
        assertEquals(33.33, order().withDeposit(33.333).depositAmount, 1e-9)
    }

    @Test
    fun invalidValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { Payment("x", 0.0, PaymentMethod.CASH, 0L) }
        assertFailsWith<IllegalArgumentException> { Payment("x", -1.0, PaymentMethod.CASH, 0L) }
        assertFailsWith<IllegalArgumentException> { order(deposit = -1.0) }
        assertFalse(Payment.isValidAmount(Double.NaN))
    }
}
