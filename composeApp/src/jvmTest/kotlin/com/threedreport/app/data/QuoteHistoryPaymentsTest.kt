package com.threedreport.app.data

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PaymentStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pagamentos, sinal, rastreio, observações e "Retomar conversa" no histórico (Leva 11, decisões 146, 149, 150 e 151):
 * o que cada ação grava, o que reabrir o cálculo preserva e o que duplicar não leva.
 */
class QuoteHistoryPaymentsTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = PricingCalculator.calculate(
        job = PrintJob(
            filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = 190.0,
        ),
        printer = PrinterProfile(
            id = "printer",
            name = "Impressora",
            printerPowerWatts = 380.0,
            maintenanceCostPerHour = 0.17,
            machineInvestment = MachineInvestment(2700.0, 12, 25, 16.0),
        ),
        settings = PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, profitMargin = 1.0),
    )

    private fun QuoteHistoryRepository.saveOrder(shippingCost: Double = 0.0, kind: QuoteKind = QuoteKind.ORDER) =
        save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null, shippingCost = shippingCost, kind = kind)

    private fun QuoteHistoryRepository.reopenAndSave(id: String, shippingCost: Double = 0.0) = update(
        id = id,
        name = "Vaso",
        quote = quote,
        services = emptyList(),
        photo = null,
        stlFile = null,
        sourceLink = null,
        client = null,
        shippingCost = shippingCost,
    )

    @Test
    fun withTheDefaultRateAtZeroANewOrderHasNoDeposit() {
        val saved = QuoteHistoryRepository().saveOrder()

        assertEquals(0.0, saved.depositAmount)
        assertTrue(saved.payments.isEmpty())
    }

    @Test
    fun aNewOrderGetsTheDepositFromTheDefaultRate() {
        val saved = QuoteHistoryRepository(defaultDepositRate = 0.3).saveOrder(shippingCost = 10.0)

        val expected = kotlin.math.round(saved.totalWithServices * 0.3 * 100) / 100
        assertEquals(expected, saved.depositAmount, 1e-9)
        assertEquals(expected, QuoteHistoryRepository().savedQuotes.value.single().depositAmount, 1e-9)
    }

    @Test
    fun aProductNeverGetsADeposit() {
        assertEquals(0.0, QuoteHistoryRepository(defaultDepositRate = 0.5).saveOrder(kind = QuoteKind.PRODUCT).depositAmount)
    }

    @Test
    fun registeringAndDeletingAPaymentSurvivesReopeningTheApp() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()

        val first = repository.addPayment(saved.id, amount = 5.0, method = PaymentMethod.PIX, epochDay = 20_400)
        val second = repository.addPayment(saved.id, amount = 2.0, method = PaymentMethod.CASH, epochDay = 20_401)

        assertNotNull(first)
        assertNotNull(second)
        val reloaded = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(listOf(first, second), reloaded.payments)
        assertEquals(7.0, reloaded.paidTotal)
        assertEquals(PaymentStatus.PARTIALLY_PAID, reloaded.paymentStatus)

        assertEquals(first, repository.deletePayment(saved.id, first.id))
        assertEquals(listOf(second), QuoteHistoryRepository().savedQuotes.value.single().payments)
    }

    @Test
    fun anInvalidPaymentOrAMissingOrderRecordsNothing() {
        val repository = QuoteHistoryRepository()
        val order = repository.saveOrder()
        val product = repository.saveOrder(kind = QuoteKind.PRODUCT)

        assertNull(repository.addPayment(order.id, amount = 0.0, method = PaymentMethod.PIX, epochDay = 1))
        assertNull(repository.addPayment(order.id, amount = Double.NaN, method = PaymentMethod.PIX, epochDay = 1))
        assertNull(repository.addPayment("nao-existe", amount = 10.0, method = PaymentMethod.PIX, epochDay = 1))
        assertNull(repository.addPayment(product.id, amount = 10.0, method = PaymentMethod.PIX, epochDay = 1))
        // Valor que arredonda a zero centavo não é pagamento (e não derruba o app).
        assertNull(repository.addPayment(order.id, amount = 0.004, method = PaymentMethod.PIX, epochDay = 1))
        assertEquals(0.01, repository.addPayment(order.id, amount = 0.006, method = PaymentMethod.PIX, epochDay = 1)?.amount)
        repository.deletePayment(order.id, repository.savedQuotes.value.first { it.id == order.id }.payments.single().id)
        assertTrue(QuoteHistoryRepository().savedQuotes.value.all { it.payments.isEmpty() })
    }

    @Test
    fun theDepositIsEditableAndNeverAboveTheTotal() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()

        repository.updateDeposit(saved.id, 12.346)
        assertEquals(12.35, QuoteHistoryRepository().savedQuotes.value.single().depositAmount, 1e-9)

        repository.updateDeposit(saved.id, 1_000_000.0)
        assertEquals(saved.totalWithServices, QuoteHistoryRepository().savedQuotes.value.single().depositAmount, 0.005)

        repository.updateDeposit(saved.id, 0.0)
        assertEquals(0.0, QuoteHistoryRepository().savedQuotes.value.single().depositAmount)
    }

    @Test
    fun reopeningTheCalculationKeepsPaymentsDepositTrackingNotesAndFollowUp() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        val payment = repository.addPayment(saved.id, 5.0, PaymentMethod.CARD, 20_400)!!
        repository.updateDeposit(saved.id, 8.0)
        repository.recordFollowUp(saved.id)
        repository.updateDetails(saved.id, QuoteDetails("Vaso", null, null, null, null, null, trackingCode = "AA123456789BR", notes = "Cor errada na 1ª"))
        val before = repository.savedQuotes.value.single()

        // O % padrão de hoje é outro: o sinal que já estava fica.
        QuoteHistoryRepository(defaultDepositRate = 0.5).reopenAndSave(saved.id)

        val after = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(listOf(payment), after.payments)
        assertEquals(8.0, after.depositAmount)
        assertEquals("AA123456789BR", after.trackingCode)
        assertEquals("Cor errada na 1ª", after.notes)
        assertEquals(before.lastFollowUpEpochMillis, after.lastFollowUpEpochMillis)
        assertNotNull(after.lastFollowUpEpochMillis)
    }

    @Test
    fun reopeningNeverCreatesADepositNorBringsBackAZeroedOne() {
        // Um pedido salvo sem sinal (como os de antes desta leva) e outro com o sinal zerado de propósito.
        val old = QuoteHistoryRepository().saveOrder()
        val repository = QuoteHistoryRepository(defaultDepositRate = 0.5)
        val zeroed = repository.saveOrder()
        repository.updateDeposit(zeroed.id, 0.0)

        // Com o % padrão de hoje em 50%, reabrir e salvar não cria sinal em nenhum dos dois.
        assertEquals(0.0, repository.reopenAndSave(zeroed.id)!!.depositAmount)
        assertEquals(0.0, repository.reopenAndSave(old.id)!!.depositAmount)
    }

    @Test
    fun theReceiptNumberIsFixedWhenThePaymentIsRegistered() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        val first = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!
        val second = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!
        val third = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!

        repository.deletePayment(saved.id, first.id)
        val fourth = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!

        val reloaded = QuoteHistoryRepository().savedQuotes.value.single()
        val number = saved.displayNumber!!
        assertEquals("$number-2", reloaded.receiptNumber(second.id))
        assertEquals("$number-3", reloaded.receiptNumber(third.id))
        assertEquals("$number-4", reloaded.receiptNumber(fourth.id))
        assertEquals(listOf(2, 3, 4), reloaded.payments.map { it.number })
    }

    @Test
    fun aDeletedReceiptNumberNeverComesBack() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)
        val second = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!
        assertEquals(2, second.number)

        repository.deletePayment(saved.id, second.id)
        val next = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!

        assertEquals(3, next.number, "o 2 já foi entregue num recibo")
        val reloaded = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(3, reloaded.lastPaymentNumber)
        // Reabrir o cálculo e salvar preserva o campo.
        assertEquals(3, repository.reopenAndSave(saved.id, shippingCost = 0.0)!!.lastPaymentNumber)
    }

    @Test
    fun deletingEveryPaymentKeepsTheCounter() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        val only = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!
        repository.deletePayment(saved.id, only.id)

        assertEquals(2, repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!.number)
    }

    @Test
    fun theCounterAlsoCoversPaymentsWithoutAStoredNumber() {
        // Pagamento sem número gravado usa a posição: excluir o último não pode devolver a posição dele.
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        val a = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!
        val b = repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!

        repository.deletePayment(saved.id, b.id)
        repository.deletePayment(saved.id, a.id)

        assertEquals(3, repository.addPayment(saved.id, 1.0, PaymentMethod.PIX, 20_400)!!.number)
    }

    @Test
    fun duplicatingDoesNotCopyTheCounter() {
        val repository = QuoteHistoryRepository()
        val original = repository.saveOrder()
        repository.addPayment(original.id, 1.0, PaymentMethod.PIX, 20_400)

        val copy = repository.save(name = "Cópia", quote = original.quote, services = emptyList(), photo = null, sourceLink = null)

        assertEquals(0, copy.lastPaymentNumber)
    }

    @Test
    fun reopeningWithALowerTotalBringsTheDepositDown() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder(shippingCost = 50.0)
        repository.updateDeposit(saved.id, saved.totalWithServices)

        val edited = repository.reopenAndSave(saved.id, shippingCost = 0.0)!!

        // Limitado ao total novo e arredondado a centavos.
        assertEquals(edited.totalWithServices, edited.depositAmount, 0.005)
    }

    @Test
    fun duplicatingCopiesNoneOfIt() {
        val repository = QuoteHistoryRepository(defaultDepositRate = 0.2)
        val original = repository.saveOrder()
        repository.addPayment(original.id, 10.0, PaymentMethod.PIX, 20_400)
        repository.updateDeposit(original.id, 8.0)
        repository.recordFollowUp(original.id)
        repository.updateDetails(original.id, QuoteDetails("Vaso", null, null, null, null, null, trackingCode = "AA1", notes = "interno"))

        // Duplicar é salvar um pedido novo com o mesmo cálculo (o que a tela faz).
        val copy = repository.saveOrder()

        assertTrue(copy.payments.isEmpty())
        assertEquals(kotlin.math.round(copy.totalWithServices * 20) / 100, copy.depositAmount, 1e-9)
        assertNull(copy.trackingCode)
        assertNull(copy.notes)
        assertNull(copy.lastFollowUpEpochMillis)
    }

    @Test
    fun editDetailsSavesTrackingAndNotesAndBlankClearsThem() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()

        repository.updateDetails(saved.id, QuoteDetails("Vaso", null, null, null, null, null, trackingCode = "  AA1  ", notes = "  linha 1\nlinha 2  "))
        val reloaded = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals("AA1", reloaded.trackingCode)
        assertEquals("linha 1\nlinha 2", reloaded.notes)

        repository.updateDetails(saved.id, QuoteDetails("Vaso", null, null, null, null, null, trackingCode = " ", notes = ""))
        val cleared = QuoteHistoryRepository().savedQuotes.value.single()
        assertNull(cleared.trackingCode)
        assertNull(cleared.notes)
    }

    @Test
    fun aProductHasNoTrackingCodeNorNotes() {
        // A especificação põe os dois campos no "Editar detalhes" de pedido (itens 29 e 32).
        val repository = QuoteHistoryRepository()
        val product = repository.saveOrder(kind = QuoteKind.PRODUCT)

        val updated = repository.updateDetails(product.id, QuoteDetails("Vaso", null, null, null, null, "Vasos", trackingCode = "AA1", notes = "nota"))!!

        assertNull(updated.trackingCode)
        assertNull(updated.notes)
    }

    @Test
    fun theFollowUpIsRecordedWithTheMomentAndClearsTheStaleCount() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveOrder()
        val before = System.currentTimeMillis()

        repository.recordFollowUp(saved.id)

        val followUp = QuoteHistoryRepository().savedQuotes.value.single().lastFollowUpEpochMillis!!
        assertTrue(followUp >= before)
        assertEquals(OrderStatus.ORCADO, repository.savedQuotes.value.single().status)
    }

    @Test
    fun convertingAProductToOrderGetsTheDefaultDeposit() {
        val repository = QuoteHistoryRepository(defaultDepositRate = 0.5)
        val product = repository.saveOrder(kind = QuoteKind.PRODUCT)

        repository.convertToOrder(product.id)

        val order = repository.savedQuotes.value.single()
        assertEquals(kotlin.math.round(order.totalWithServices * 50) / 100, order.depositAmount, 1e-9)
    }
}
