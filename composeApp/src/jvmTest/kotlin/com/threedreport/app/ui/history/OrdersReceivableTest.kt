package com.threedreport.app.ui.history

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlin.test.Test
import kotlin.test.assertEquals

/** "A receber" no resumo de Pedidos soma o que falta de cada pedido de Aprovado a Pronto (decisão 146, item 15). */
class OrdersReceivableTest {

    private fun order(id: String, total: Double, status: OrderStatus, paid: List<Double> = emptyList(), currency: Currency = Currency.BRL) = SavedQuote(
        id = id,
        name = "Peça $id",
        quote = Quote(
            prints = listOf(
                QuotedPrint(
                    job = PrintJob(
                        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                        filamentLengthMeters = 1.0,
                        printTimeMinutes = 60.0,
                    ),
                    printerId = "p",
                    printerName = "Impressora",
                    cost = PrintCost(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                ),
            ),
            costs = CostBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            salePrice = total,
        ),
        savedAtEpochMillis = 0L,
        status = status,
        payments = paid.mapIndexed { index, amount -> Payment("$id-$index", amount, PaymentMethod.PIX, 0L) },
        currency = currency,
    )

    @Test
    fun withoutPaymentsTheReceivableIsTheTotalAsBefore() {
        val summary = ordersSummary(listOf(order("a", 100.0, OrderStatus.APROVADO), order("b", 50.0, OrderStatus.PRONTO)), today = 0)

        assertEquals(150.0, summary.receivable.single().amount, 1e-9)
    }

    @Test
    fun theReceivableDiscountsWhatWasPaid() {
        val summary = ordersSummary(
            listOf(
                order("a", 100.0, OrderStatus.APROVADO, paid = listOf(30.0)),
                order("b", 50.0, OrderStatus.EM_IMPRESSAO, paid = listOf(50.0)),
                order("c", 80.0, OrderStatus.PRONTO, paid = listOf(100.0)),
                // Orçado e Entregue continuam fora, mesmo com falta.
                order("d", 40.0, OrderStatus.ORCADO),
                order("e", 40.0, OrderStatus.ENTREGUE, paid = listOf(10.0)),
            ),
            today = 0,
        )

        assertEquals(70.0, summary.receivable.single().amount, 1e-9)
        assertEquals(3, summary.inProgress)
    }

    @Test
    fun theReceivableStaysByCurrency() {
        val summary = ordersSummary(
            listOf(order("a", 100.0, OrderStatus.APROVADO, paid = listOf(40.0)), order("b", 20.0, OrderStatus.APROVADO, paid = listOf(5.0), currency = Currency.USD)),
            today = 0,
        )

        assertEquals(listOf(Currency.BRL to 60.0, Currency.USD to 15.0), summary.receivable.map { it.currency to it.amount })
    }
}
