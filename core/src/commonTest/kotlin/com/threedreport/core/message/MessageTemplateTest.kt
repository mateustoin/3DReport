package com.threedreport.core.message

import com.threedreport.core.costsOf
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.Payment
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.quotedPrint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Preenchimento dos modelos de mensagem (decisão 148, item 21) e qual modelo abre em cada andamento. */
class MessageTemplateTest {

    private fun fill(template: String, vararg values: Pair<MessagePlaceholder, String?>) = fillMessageTemplate(template, values.toMap())

    @Test
    fun replacesEveryMarker() {
        val text = fill(
            "Oi, {cliente}! Seu pedido {pedido} ({peca}) fica pronto até {prazo}. Total {valor}, falta {falta}. {loja}",
            MessagePlaceholder.CLIENT to "Maria",
            MessagePlaceholder.ORDER to "#0042",
            MessagePlaceholder.PIECE to "Vaso",
            MessagePlaceholder.DEADLINE to "20/11/2026",
            MessagePlaceholder.VALUE to "R$ 174,34",
            MessagePlaceholder.REMAINING to "R$ 124,34",
            MessagePlaceholder.STORE to "Ateliê 3D",
        )

        assertEquals("Oi, Maria! Seu pedido #0042 (Vaso) fica pronto até 20/11/2026. Total R$ 174,34, falta R$ 124,34. Ateliê 3D", text)
    }

    @Test
    fun anEmptyMarkerLeavesNoDoubleSpace() {
        assertEquals("Seu pedido está pronto.", fill("Seu pedido {pedido} está pronto.", MessagePlaceholder.ORDER to null))
        assertEquals("Seu pedido está pronto.", fill("Seu pedido {pedido} está pronto.", MessagePlaceholder.ORDER to "  "))
    }

    @Test
    fun anEmptyMarkerBeforePunctuationTakesTheSpaceAndTheCommaWithIt() {
        assertEquals("Oi! Tudo certo.", fill("Oi, {cliente}! Tudo certo.", MessagePlaceholder.CLIENT to null))
        assertEquals("Oi, tudo bem?", fill("Oi, {cliente}, tudo bem?", MessagePlaceholder.CLIENT to null))
    }

    @Test
    fun emptyParenthesesGoAway() {
        assertEquals("Seu pedido #0042 foi aprovado.", fill("Seu pedido {pedido} ({peca}) foi aprovado.", MessagePlaceholder.ORDER to "#0042"))
    }

    @Test
    fun anEmptyMarkerAtTheStartOrEndOfALine() {
        assertEquals("ficou pronto!\nAbraço", fill("{peca} ficou pronto!\nAbraço {loja}", MessagePlaceholder.PIECE to null))
    }

    @Test
    fun twoEmptyMarkersInARow() {
        assertEquals("Obrigado!", fill("Obrigado, {cliente} {loja}!"))
    }

    @Test
    fun keepsTheLineBreaksAndUnknownBraces() {
        assertEquals("Linha 1\n\nLinha {x}", fill("Linha 1\n\nLinha {x}"))
    }

    @Test
    fun aValueIsNotReadAsAMarker() {
        // O nome do cliente com "{peca}" escrito não é preenchido de novo.
        assertEquals("Oi, {peca}! Vaso", fill("Oi, {cliente}! {peca}", MessagePlaceholder.CLIENT to "{peca}", MessagePlaceholder.PIECE to "Vaso"))
    }

    @Test
    fun anOptionalPartGoesAwayWhenAMarkerInsideHasNoValue() {
        val template = "Seu pedido {pedido} foi aprovado[ e fica pronto até {prazo}]."

        assertEquals("Seu pedido #0042 foi aprovado.", fill(template, MessagePlaceholder.ORDER to "#0042"))
        assertEquals("Vaso saiu pra entrega.", fill("{peca} saiu pra entrega. [Rastreio: {rastreio}]", MessagePlaceholder.PIECE to "Vaso"))
        assertEquals("Oi! Tudo certo.", fill("Oi[, {cliente}]! Tudo certo."))
        // Basta um marcador vazio pra o trecho todo sair.
        assertEquals("Pronto.", fill("Pronto[ ({peca} {pedido})].", MessagePlaceholder.PIECE to "Vaso"))
    }

    @Test
    fun anOptionalPartWithAllValuesLosesOnlyTheBrackets() {
        val text = fill(
            "Seu pedido foi aprovado[ e fica pronto até {prazo}]. {peca} saiu pra entrega. [Rastreio: {rastreio}]",
            MessagePlaceholder.DEADLINE to "20/11/2026",
            MessagePlaceholder.PIECE to "Vaso",
            MessagePlaceholder.TRACKING to "AA123456789BR",
        )

        assertEquals("Seu pedido foi aprovado e fica pronto até 20/11/2026. Vaso saiu pra entrega. Rastreio: AA123456789BR", text)
    }

    @Test
    fun bracketsWithoutMarkersKeepTheText() {
        assertEquals("Obrigado pela compra!", fill("Obrigado[ pela compra]!"))
    }

    @Test
    fun anUnclosedBracketStaysAsWritten() {
        assertEquals("Promoção [só hoje Vaso", fill("Promoção [só hoje {peca}", MessagePlaceholder.PIECE to "Vaso"))
        assertEquals("Fim] do texto", fill("Fim] do texto"))
    }

    @Test
    fun aValueWithBracketsIsNotReadAsAnOptionalPart() {
        assertEquals("Oi, [Maria]!", fill("Oi[, {cliente}]!", MessagePlaceholder.CLIENT to "[Maria]"))
    }

    @Test
    fun theTemplateOfAStatusChange() {
        assertEquals(MessageTemplateKind.APPROVED, MessageTemplateKind.forStatusChange(OrderStatus.APROVADO))
        assertEquals(MessageTemplateKind.PRINTING, MessageTemplateKind.forStatusChange(OrderStatus.EM_IMPRESSAO))
        assertEquals(MessageTemplateKind.READY, MessageTemplateKind.forStatusChange(OrderStatus.PRONTO))
        assertEquals(MessageTemplateKind.OUT_FOR_DELIVERY, MessageTemplateKind.forStatusChange(OrderStatus.ENTREGUE))
        assertNull(MessageTemplateKind.forStatusChange(OrderStatus.ORCADO))
        assertNull(MessageTemplateKind.forStatusChange(OrderStatus.CANCELADO))
    }

    private val order = SavedQuote(
        id = "1",
        name = "Vaso",
        quote = Quote(
            prints = listOf(
                quotedPrint(
                    PrintJob(
                        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                        filamentLengthMeters = 1.0,
                        printTimeMinutes = 60.0,
                    ),
                ),
            ),
            costs = costsOf(),
            salePrice = 100.0,
        ),
        savedAtEpochMillis = 0L,
        number = 42,
        client = Client(name = " Maria "),
        deliveryDateEpochDay = 20_000,
        trackingCode = "AA123456789BR",
    )

    @Test
    fun theTemplateOfTheMenuFollowsTheCurrentStatus() {
        assertEquals(MessageTemplateKind.FOLLOW_UP, MessageTemplateKind.forOrder(order))
        assertEquals(MessageTemplateKind.APPROVED, MessageTemplateKind.forOrder(order.copy(status = OrderStatus.APROVADO)))
        assertEquals(MessageTemplateKind.PRINTING, MessageTemplateKind.forOrder(order.copy(status = OrderStatus.EM_IMPRESSAO)))
        assertNull(MessageTemplateKind.forOrder(order.copy(status = OrderStatus.CANCELADO)))
    }

    @Test
    fun readyOrDeliveredWithPaymentTrackingAndSomethingToPayOpensTheReminder() {
        val partly = order.copy(payments = listOf(Payment("p", 30.0, PaymentMethod.PIX, 0L)))
        val withDeposit = order.copy(depositAmount = 20.0)
        val paid = order.copy(payments = listOf(Payment("p", 100.0, PaymentMethod.PIX, 0L)))

        assertEquals(MessageTemplateKind.PAYMENT_REMINDER, MessageTemplateKind.forOrder(partly.copy(status = OrderStatus.PRONTO)))
        assertEquals(MessageTemplateKind.PAYMENT_REMINDER, MessageTemplateKind.forOrder(partly.copy(status = OrderStatus.ENTREGUE)))
        assertEquals(MessageTemplateKind.PAYMENT_REMINDER, MessageTemplateKind.forOrder(withDeposit.copy(status = OrderStatus.ENTREGUE)))
        assertEquals(MessageTemplateKind.READY, MessageTemplateKind.forOrder(paid.copy(status = OrderStatus.PRONTO)))
        assertEquals(MessageTemplateKind.OUT_FOR_DELIVERY, MessageTemplateKind.forOrder(paid.copy(status = OrderStatus.ENTREGUE)))
    }

    @Test
    fun withoutPaymentTrackingThereIsNoReminder() {
        // Pedido sem sinal nem pagamento (como todo pedido de antes da Leva 11): nada de cobrança.
        assertEquals(MessageTemplateKind.READY, MessageTemplateKind.forOrder(order.copy(status = OrderStatus.PRONTO)))
        assertEquals(MessageTemplateKind.OUT_FOR_DELIVERY, MessageTemplateKind.forOrder(order.copy(status = OrderStatus.ENTREGUE)))
    }

    @Test
    fun theKeyOfATemplateIsFixed() {
        assertEquals(MessageTemplateKind.OUT_FOR_DELIVERY, MessageTemplateKind.ofKey("outForDelivery"))
        assertNull(MessageTemplateKind.ofKey("nao-existe"))
    }

    @Test
    fun theValuesOfAnOrder() {
        val values = orderMessageValues(order, storeName = "Ateliê", formatMoney = { "R$ ${it.toInt()}" }, formatDate = { "dia $it" })

        assertEquals("Maria", values[MessagePlaceholder.CLIENT])
        assertEquals("#0042", values[MessagePlaceholder.ORDER])
        assertEquals("Vaso", values[MessagePlaceholder.PIECE])
        assertEquals("dia 20000", values[MessagePlaceholder.DEADLINE])
        assertEquals("R$ 100", values[MessagePlaceholder.VALUE])
        assertEquals("R$ 100", values[MessagePlaceholder.REMAINING])
        assertEquals("AA123456789BR", values[MessagePlaceholder.TRACKING])
        assertEquals("Ateliê", values[MessagePlaceholder.STORE])

        val paid = orderMessageValues(order.copy(payments = listOf(Payment("p", 100.0, PaymentMethod.PIX, 0L))), null, { "$it" }, { "$it" })
        assertNull(paid[MessagePlaceholder.REMAINING])
        assertNull(paid[MessagePlaceholder.STORE])
    }

    // Decisão 16e da Leva 11B: cada marcador aceita o nome em português e em inglês.
    @Test
    fun englishAndPortugueseNamesFillTheSame() {
        val values = MessagePlaceholder.entries.associateWith { "<${it.name}>" }
        for (placeholder in MessagePlaceholder.entries) {
            assertEquals("<${placeholder.name}>", fillMessageTemplate(placeholder.token, values))
            assertEquals("<${placeholder.name}>", fillMessageTemplate(placeholder.englishToken, values))
        }
        assertEquals(
            "Hi, Emma! Order #3 (Vase), due 11/20, balance $5.",
            fill(
                "Hi, {customer}! Order {pedido} ({part}), due {due}, balance {balance}.",
                MessagePlaceholder.CLIENT to "Emma",
                MessagePlaceholder.ORDER to "#3",
                MessagePlaceholder.PIECE to "Vase",
                MessagePlaceholder.DEADLINE to "11/20",
                MessagePlaceholder.REMAINING to "$5",
            ),
        )
    }

    @Test
    fun optionalPartsWorkWithEnglishNames() {
        assertEquals("Hi!", fill("Hi[, {customer}]!"))
        assertEquals("Hi, Emma!", fill("Hi[, {customer}]!", MessagePlaceholder.CLIENT to "Emma"))
        assertEquals("{customer}", MessagePlaceholder.CLIENT.tokenFor(english = true))
        assertEquals("{cliente}", MessagePlaceholder.CLIENT.tokenFor(english = false))
    }
}
