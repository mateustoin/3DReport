package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.history_notice_notify_client
import _3dreport.composeapp.generated.resources.payments_error_amount
import _3dreport.composeapp.generated.resources.payments_notice_receipt
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.ClientRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.LocalStorage
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.appDataDir
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.localEpochDayOf
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Client
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PaymentMethod
import com.threedreport.core.model.PaymentStatus
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pagamentos, mensagens prontas, pedido parado, Pix e rastreio em Pedidos (Leva 11): o que o ViewModel mostra nas
 * linhas e o que cada evento faz.
 */
class QuoteHistoryLeva11Test {

    private lateinit var dataDir: File
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    /** O relógio do repositório de pedidos: dá pra "passar os dias" sem esperar. */
    private var nowMillis = DAY_MILLIS * 20_000L

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = Quote(
        prints = listOf(
            QuotedPrint(
                job = PrintJob(
                    filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
                    filamentLengthMeters = 12.0,
                    printTimeMinutes = 190.0,
                ),
                printerId = "printer",
                printerName = "Impressora",
                cost = PrintCost(material = 3.58, energy = 1.48, maintenance = 0.54, finishing = 0.36, investmentReturn = 1.78, fixedCost = 0.0),
            ),
        ),
        costs = CostBreakdown(
            material = 3.58, energy = 1.48, maintenance = 0.54, failures = 0.36, finishing = 0.36,
            investmentReturn = 1.78, administrative = 0.0, labor = 0.0, fixedCost = 0.0,
        ),
        salePrice = 100.0,
    )

    private class Setup(
        val repository: QuoteHistoryRepository,
        val branding: BrandingRepository,
        val orderSettings: OrderSettingsRepository,
        val platform: FakePlatform,
        val viewModel: QuoteHistoryViewModel,
    ) {
        val state get() = viewModel.uiState.value
        fun row(id: String) = state.rows.first { it.quote.id == id }
    }

    /** "Hoje" do ViewModel; muda junto com [nowMillis] quando um teste passa os dias. */
    private var today = localEpochDayOf(nowMillis)

    private fun setup(depositRate: Double = 0.0, main: CoroutineDispatcher = Dispatchers.Unconfined): Setup {
        val storage = LocalStorage(appDataDir(), Clock { nowMillis })
        val orderSettings = storage.orderSettings()
        val repository = storage.quoteHistory { depositRate }
        val branding = BrandingRepository()
        val platform = FakePlatform()
        val viewModel = QuoteHistoryViewModel(
            repository, branding, FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), PreferencesRepository(),
            today = { today }, clientRepository = ClientRepository(), clock = { nowMillis }, platform = platform, scope = scope,
            main = main,
            orderSettingsRepository = orderSettings,
        )
        return Setup(repository, branding, orderSettings, platform, viewModel)
    }

    private fun Setup.order(name: String = "Peça", contact: String? = "(11) 99999-0000", status: OrderStatus = OrderStatus.ORCADO): SavedQuote {
        val saved = repository.save(
            name = name, quote = quote, services = emptyList(), photo = null, sourceLink = null,
            client = Client(name = "Maria", contact = contact), currency = Currency.BRL,
        )
        if (status != OrderStatus.ORCADO) repository.updateStatus(saved.id, status)
        return repository.savedQuotes.value.first { it.id == saved.id }
    }

    private fun Setup.withPix() = branding.update(BrandingSettings(brandName = "Sua Loja", pixKey = "loja@example.com", pixReceiverName = "Sua Loja", pixCity = "Sao Paulo"))

    private fun Setup.awaitMessage(): MessageEditing = runBlocking { withTimeout(AWAIT_MILLIS) { viewModel.uiState.first { it.message != null }.message!! } }

    private fun Setup.awaitNotice(): UiNotice = runBlocking { withTimeout(AWAIT_MILLIS) { viewModel.notices.first() } }

    // ---- selos

    @Test
    fun anOrderWithoutPaymentsOrDepositHasNoPaymentBadge() {
        val s = setup()
        val saved = s.order()

        assertNull(s.row(saved.id).paymentStatus)
    }

    @Test
    fun theBadgeFollowsTheDepositAndThePayments() {
        val s = setup(depositRate = 0.3)
        val saved = s.order(status = OrderStatus.APROVADO)
        assertEquals(PaymentStatus.NOT_PAID, s.row(saved.id).paymentStatus)

        s.repository.addPayment(saved.id, 10.0, PaymentMethod.PIX, 1L)
        assertEquals(PaymentStatus.PARTIALLY_PAID, s.row(saved.id).paymentStatus, "entrou menos que o sinal")

        s.repository.addPayment(saved.id, 20.0, PaymentMethod.CASH, 2L)
        assertEquals(PaymentStatus.DEPOSIT_PAID, s.row(saved.id).paymentStatus)

        s.repository.addPayment(saved.id, 70.0, PaymentMethod.CARD, 3L)
        assertEquals(PaymentStatus.PAID, s.row(saved.id).paymentStatus)
    }

    // ---- orçamento parado

    @Test
    fun aQuoteBecomesStaleAfterTheConfiguredDaysAndFollowingUpClearsIt() {
        val s = setup()
        val saved = s.order()
        assertNull(s.row(saved.id).staleDays)

        // Três dias depois, no padrão de Configurações.
        nowMillis += 3 * DAY_MILLIS
        today += 3
        s.viewModel.onShown()
        assertEquals(3, s.row(saved.id).staleDays)

        s.viewModel.followUp(saved)
        s.awaitMessage()
        s.viewModel.copyMessage()

        assertNull(s.row(saved.id).staleDays, "o contato some com o selo até passar o prazo de novo")
    }

    @Test
    fun zeroDaysTurnsTheStaleBadgeOff() {
        val s = setup()
        val saved = s.order()
        today += 30
        s.viewModel.onShown()
        assertNotNull(s.row(saved.id).staleDays)

        s.orderSettings.update(OrderSettings(staleQuoteDays = 0))

        assertNull(s.row(saved.id).staleDays)
    }

    @Test
    fun onlyAQuotedOrderIsStale() {
        val s = setup()
        val approved = s.order(status = OrderStatus.APROVADO)
        today += 30
        s.viewModel.onShown()

        assertNull(s.row(approved.id).staleDays)
    }

    @Test
    fun markCancelledCancelsWithUndo() {
        val s = setup()
        val saved = s.order()

        s.viewModel.markCancelled(saved.id)

        assertEquals(OrderStatus.CANCELADO, s.repository.savedQuotes.value.first { it.id == saved.id }.status)
        assertNotNull(s.awaitNotice().action)
    }

    // ---- mensagem pro cliente

    @Test
    fun theMenuMessageOpensInTheModelOfTheCurrentStatus() {
        val s = setup()
        val quoted = s.order("Suporte")
        val ready = s.order("Vaso", status = OrderStatus.PRONTO)
        val charged = s.order("Luminária", status = OrderStatus.PRONTO)
        s.repository.updateDeposit(charged.id, 30.0)

        assertEquals(MessageTemplateKind.FOLLOW_UP, s.row(quoted.id).messageKind)
        assertEquals(MessageTemplateKind.PAYMENT_REMINDER, s.row(charged.id).messageKind, "pronto, com sinal e sem pagar: lembrete")
        assertEquals(MessageTemplateKind.READY, s.row(ready.id).messageKind, "sem acompanhamento de pagamento não há cobrança")

        s.viewModel.openMessage(quoted)
        val message = s.awaitMessage()
        assertEquals(MessageTemplateKind.FOLLOW_UP, message.kind)
        assertTrue("Conseguiu ver o orçamento de Suporte?" in message.text, message.text)
        assertTrue(message.text.startsWith("Oi, Maria!"))
    }

    @Test
    fun aCancelledOrderOffersNoMessageNorPix() {
        val s = setup()
        s.withPix()
        val saved = s.order(status = OrderStatus.CANCELADO)

        assertNull(s.row(saved.id).messageKind)
        assertFalse(s.row(saved.id).hasPix)
        s.viewModel.openMessage(saved)
        assertNull(s.state.message)
    }

    @Test
    fun changingTheModelRefillsTheTextAndEditingKeepsTheSavedModel() {
        val s = setup()
        val saved = s.order("Suporte")
        s.viewModel.openMessage(saved)
        s.awaitMessage()

        s.viewModel.editMessage("Texto só desta vez")
        assertEquals("Texto só desta vez", s.state.message!!.text)

        s.viewModel.changeMessageKind(MessageTemplateKind.READY)
        val refilled = runBlocking { withTimeout(AWAIT_MILLIS) { s.viewModel.uiState.first { it.message?.kind == MessageTemplateKind.READY }.message!! } }
        assertEquals("Oi, Maria! Suporte ficou pronto!", refilled.text)
        assertTrue(s.branding.branding.value.messageTemplates.isEmpty(), "editar na janela não muda o modelo salvo")
    }

    @Test
    fun anEditedTemplateInSettingsIsUsed() {
        val s = setup()
        s.branding.update(BrandingSettings(messageTemplates = mapOf("followUp" to "Oi {cliente}, e o orçamento de {peca}?")))
        val saved = s.order("Suporte")

        s.viewModel.followUp(saved)

        assertEquals("Oi Maria, e o orçamento de Suporte?", s.awaitMessage().text)
    }

    @Test
    fun sendingTheFollowUpOnWhatsAppOpensTheLinkAndRecordsTheContact() {
        val s = setup()
        val saved = s.order("Suporte")
        s.viewModel.followUp(saved)
        val message = s.awaitMessage()

        s.viewModel.sendMessageOnWhatsApp()

        val link = s.platform.opened.single()
        assertTrue(link.startsWith("https://wa.me/5511999990000?text="), link)
        assertNotNull(s.repository.savedQuotes.value.first { it.id == saved.id }.lastFollowUpEpochMillis)
        assertNull(s.state.message, "a janela fecha depois de enviar")
        assertEquals(MessageTemplateKind.FOLLOW_UP, message.kind)
    }

    @Test
    fun sendingAnotherModelDoesNotRecordTheFollowUp() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.PRONTO)
        s.viewModel.openMessage(saved, MessageTemplateKind.READY)
        s.awaitMessage()

        s.viewModel.copyMessage()

        assertNull(s.repository.savedQuotes.value.first { it.id == saved.id }.lastFollowUpEpochMillis)
        assertEquals(listOf("Oi, Maria! Suporte ficou pronto!"), s.platform.copied)
    }

    @Test
    fun whenTheBrowserFailsTheMessageGoesToTheClipboard() {
        val s = setup()
        s.platform.browserWorks = false
        val saved = s.order("Suporte")
        s.viewModel.followUp(saved)
        s.awaitMessage()

        s.viewModel.sendMessageOnWhatsApp()

        assertEquals(1, s.platform.copied.size)
        assertTrue(s.awaitNotice().isError)
    }

    @Test
    fun thePixStaysOutOfTheEditableTextAndGoesLastWhenIncluded() {
        val s = setup()
        s.withPix()
        val saved = s.order("Suporte", status = OrderStatus.PRONTO)
        // O pedido acompanha pagamento (sinal do total), então o pronto abre no lembrete.
        s.repository.updateDeposit(saved.id, 100.0)

        s.viewModel.openMessage(saved)
        val message = s.awaitMessage()

        assertEquals(MessageTemplateKind.PAYMENT_REMINDER, message.kind)
        assertTrue("falta o pagamento de R$ 100,00 do pedido #0001." in message.text, message.text)
        assertFalse("Pix copia e cola" in message.text, "o Pix não entra no texto editável")
        assertTrue(message.includePix, "nasce ligado no lembrete de pagamento")
        val lines = message.finalText.lines()
        assertEquals("Pix copia e cola (R$ 100,00):", lines[lines.size - 2])
        assertTrue(lines.last().startsWith("000201"), "o código vai sozinho na última linha")

        s.viewModel.setIncludePix(false)
        assertFalse("000201" in s.state.message!!.finalText)
        s.viewModel.copyMessage()
        assertFalse("000201" in s.platform.copied.single())
    }

    @Test
    fun thePixCheckboxStartsOnlyInApprovedAndTheReminderAndOnlyWithAChargeToMake() {
        val s = setup()
        s.withPix()
        val quoted = s.order("Suporte")
        s.viewModel.openMessage(quoted)
        assertFalse(s.awaitMessage().includePix, "Retomar orçamento nasce desligado")
        assertNotNull(s.state.message!!.pix, "há Pix a cobrar sem estar marcado")

        s.viewModel.changeMessageKind(MessageTemplateKind.APPROVED)
        runBlocking { withTimeout(AWAIT_MILLIS) { s.viewModel.uiState.first { it.message?.kind == MessageTemplateKind.APPROVED } } }
        assertTrue(s.state.message!!.includePix)

        s.repository.addPayment(quoted.id, 100.0, PaymentMethod.PIX, 1L)
        s.viewModel.openMessage(s.repository.savedQuotes.value.first { it.id == quoted.id }, MessageTemplateKind.APPROVED)
        val paid = runBlocking { withTimeout(AWAIT_MILLIS) { s.viewModel.uiState.first { it.message?.quote?.paidTotal == 100.0 }.message!! } }
        assertNull(paid.pix, "pedido pago: sem marcação")
        assertFalse(paid.includePix)
    }

    @Test
    fun theDeliveryMessageCarriesTheTrackingCodeOnlyWhenThereIsOne() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.ENTREGUE)
        s.viewModel.openMessage(saved, MessageTemplateKind.OUT_FOR_DELIVERY)
        assertEquals("Oi, Maria! Suporte saiu pra entrega.", s.awaitMessage().text)

        s.viewModel.startEditingDetails(saved)
        s.viewModel.updateDetails { it.copy(trackingCode = "  AB123456789BR ") }
        s.viewModel.saveDetails()
        s.viewModel.openMessage(s.repository.savedQuotes.value.first { it.id == saved.id }, MessageTemplateKind.OUT_FOR_DELIVERY)

        val text = runBlocking { withTimeout(AWAIT_MILLIS) { s.viewModel.uiState.first { it.message?.text?.contains("Rastreio") == true }.message!!.text } }
        assertEquals("Oi, Maria! Suporte saiu pra entrega. Rastreio: AB123456789BR", text)
    }

    @Test
    fun changingTheStatusOffersToNotifyTheClientOnlyForStatusesWithAMessage() {
        val s = setup()
        val saved = s.order("Suporte")

        s.viewModel.updateStatus(saved.id, OrderStatus.PRONTO)
        val notice = s.awaitNotice()
        assertEquals(UiText.Res(Res.string.history_notice_notify_client), notice.actionLabel)
        assertNull(s.state.message, "nada abre sozinho")
        notice.action!!.invoke()
        assertEquals("Oi, Maria! Suporte ficou pronto!", s.awaitMessage().text)
    }

    @Test
    fun movingBackToQuotedOffersNoMessage() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.APROVADO)

        s.viewModel.updateStatus(saved.id, OrderStatus.ORCADO)

        assertNull(runBlocking { kotlinx.coroutines.withTimeoutOrNull(200) { s.viewModel.notices.first() } })
    }

    // ---- Pix e rastreio

    @Test
    fun copyingThePixCodeNeedsAKeyAndAnOpenCharge() {
        val s = setup()
        val saved = s.order("Suporte")
        assertFalse(s.row(saved.id).hasPix, "sem chave, nada de Pix")
        s.viewModel.copyPixCode(saved)
        assertTrue(s.platform.copied.isEmpty())

        s.withPix()
        assertTrue(s.row(saved.id).hasPix)
        s.viewModel.copyPixCode(saved)
        assertTrue(s.platform.copied.single().startsWith("000201"))

        s.repository.addPayment(saved.id, 100.0, PaymentMethod.PIX, 1L)
        assertFalse(s.row(saved.id).hasPix, "pedido pago não tem Pix")
    }

    @Test
    fun trackingOpensTheConfiguredSiteWithTheCode() {
        val s = setup()
        val saved = s.order("Suporte")
        s.viewModel.startEditingDetails(saved)
        s.viewModel.updateDetails { it.copy(trackingCode = "AB123456789BR", notes = "Cliente prefere retirar") }
        s.viewModel.saveDetails()
        val updated = s.repository.savedQuotes.value.first { it.id == saved.id }
        assertEquals("AB123456789BR", updated.trackingCode)
        assertEquals("Cliente prefere retirar", updated.notes)
        assertNotNull(s.row(saved.id).trackingUrl)

        s.viewModel.openTracking(updated)
        assertEquals("https://rastreamento.correios.com.br/app/index.php?objetos=AB123456789BR", s.platform.opened.single())

        s.viewModel.copyTrackingCode(updated)
        assertEquals(listOf("AB123456789BR"), s.platform.copied)

        s.orderSettings.update(OrderSettings(trackingUrlTemplate = ""))
        assertNull(s.row(saved.id).trackingUrl, "sem site, sem link")
    }

    // ---- pagamentos

    @Test
    fun registeringAPaymentStartsFilledWithWhatIsMissingAndOffersTheReceipt() {
        val s = setup(depositRate = 0.3)
        val saved = s.order("Suporte")
        s.viewModel.openPayments(saved)

        s.viewModel.startRegisteringPayment()
        val form = s.state.payments!!.form!!
        assertEquals("30", form.amountText, "o que falta do sinal")
        assertEquals(PaymentMethod.PIX, form.method)

        s.viewModel.updatePaymentForm { it.copy(method = PaymentMethod.CASH) }
        s.viewModel.registerPayment()

        val payments = s.repository.savedQuotes.value.first { it.id == saved.id }.payments
        assertEquals(30.0, payments.single().amount, 1e-9)
        assertEquals(PaymentMethod.CASH, payments.single().method)
        assertNull(s.state.payments!!.form, "o formulário fecha")
        assertEquals(UiText.Res(Res.string.payments_notice_receipt), s.awaitNotice().actionLabel)

        s.viewModel.startRegisteringPayment()
        assertEquals("70", s.state.payments!!.form!!.amountText, "depois do sinal, o que falta do pedido")
        assertEquals(PaymentMethod.CASH, s.state.payments!!.form!!.method, "a última forma usada")
    }

    @Test
    fun theFormShowsTheAmountInCentsEvenWhenTheTotalHasLooseDecimals() {
        val s = setup()
        val saved = s.repository.save(
            name = "Peça", quote = quote.copy(salePrice = 126.98999999999999), services = emptyList(), photo = null, sourceLink = null,
            client = Client(name = "Maria"), currency = Currency.BRL,
        )
        s.viewModel.openPayments(saved)

        s.viewModel.startRegisteringPayment()

        assertEquals("126,99", s.state.payments!!.form!!.amountText)
    }

    @Test
    fun paymentBadgesAndTheColumnOnlyAppearFromApprovedOn() {
        val s = setup(depositRate = 0.3)
        val quoted = s.order("Orçado")
        assertNull(s.row(quoted.id).paymentStatus, "em Orçado nenhum selo de pagamento")
        assertFalse(s.state.showPaymentColumn)

        s.viewModel.updateStatus(quoted.id, OrderStatus.APROVADO)

        assertEquals(PaymentStatus.NOT_PAID, s.row(quoted.id).paymentStatus)
        assertTrue(s.state.showPaymentColumn)
    }

    @Test
    fun anInvalidAmountShowsAnErrorAndRegistersNothing() {
        val s = setup()
        val saved = s.order("Suporte")
        s.viewModel.openPayments(saved)
        s.viewModel.startRegisteringPayment()

        s.viewModel.updatePaymentForm { it.copy(amountText = "0") }
        s.viewModel.registerPayment()

        assertEquals(UiText.Res(Res.string.payments_error_amount), s.state.payments!!.form!!.error)
        assertTrue(s.repository.savedQuotes.value.first { it.id == saved.id }.payments.isEmpty())
    }

    /** Todos os modelos editados em Configurações: o preenchimento não lê recurso e só anda quando o [scheduler] avança. */
    private fun Setup.withEditedTemplates() = branding.update(
        MessageTemplateKind.entries.fold(BrandingSettings()) { settings, kind ->
            settings.withMessageTemplate(kind, text = "Modelo ${kind.key}", defaultText = "")
        },
    )

    @Test
    fun closingTheMessageBeforeItIsReadyDoesNotReopenIt() {
        val scheduler = TestCoroutineScheduler()
        val s = setup(main = StandardTestDispatcher(scheduler))
        s.withEditedTemplates()
        val saved = s.order("Vaso", status = OrderStatus.APROVADO)

        s.viewModel.openMessage(saved)
        s.viewModel.closeMessage()
        scheduler.advanceUntilIdle()

        assertNull(s.state.message)
    }

    @Test
    fun changingTheTemplateQuicklyShowsTheLastOneChosen() {
        val scheduler = TestCoroutineScheduler()
        val s = setup(main = StandardTestDispatcher(scheduler))
        s.withEditedTemplates()
        val saved = s.order("Vaso", status = OrderStatus.APROVADO)
        s.viewModel.openMessage(saved, MessageTemplateKind.READY)
        scheduler.advanceUntilIdle()
        assertEquals(MessageTemplateKind.READY, s.state.message!!.kind)

        s.viewModel.changeMessageKind(MessageTemplateKind.APPROVED)
        s.viewModel.changeMessageKind(MessageTemplateKind.PRINTING)
        scheduler.advanceUntilIdle()

        val message = s.state.message!!
        assertEquals(MessageTemplateKind.PRINTING, message.kind)
        assertEquals("Modelo printing", message.text)
    }

    @Test
    fun anOpenRequestForAnotherOrderReplacesTheFirst() {
        val scheduler = TestCoroutineScheduler()
        val s = setup(main = StandardTestDispatcher(scheduler))
        s.withEditedTemplates()
        val first = s.order("Vaso", status = OrderStatus.APROVADO)
        val second = s.order("Suporte", status = OrderStatus.EM_IMPRESSAO)

        s.viewModel.openMessage(first)
        s.viewModel.openMessage(second)
        scheduler.advanceUntilIdle()

        assertEquals(second.id, s.state.message!!.quote.id)
        assertEquals(MessageTemplateKind.PRINTING, s.state.message!!.kind)
    }

    @Test
    fun theDepositIsEditableAndLimitedToTheTotal() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.APROVADO)
        s.viewModel.openPayments(saved)

        // Digitar não grava; sair do campo grava.
        s.viewModel.setDeposit("4")
        s.viewModel.setDeposit("40")
        assertEquals(0.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount)
        s.viewModel.commitDeposit()
        assertEquals(40.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount, 1e-9)
        assertEquals(PaymentStatus.NOT_PAID, s.row(saved.id).paymentStatus)

        s.viewModel.setDeposit("500")
        s.viewModel.commitDeposit()
        assertEquals(100.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount, 1e-9)
        assertEquals("100", s.state.payments!!.depositText, "o campo mostra o sinal já limitado")

        s.viewModel.setDeposit("abc")
        s.viewModel.commitDeposit()
        assertEquals(UiText.Res(Res.string.common_error_number_invalid), s.state.payments!!.depositError)
        assertEquals(100.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount, 1e-9)
    }

    @Test
    fun theTypedDepositIsSavedWhenClosingOrRegistering() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.APROVADO)

        s.viewModel.openPayments(saved)
        s.viewModel.setDeposit("30")
        s.viewModel.closePayments()
        assertEquals(30.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount, 1e-9)

        s.viewModel.openPayments(s.repository.savedQuotes.value.first { it.id == saved.id })
        s.viewModel.setDeposit("20")
        s.viewModel.startRegisteringPayment()
        // O formulário já parte do sinal novo.
        assertEquals("20", s.state.payments!!.form!!.amountText)
        assertEquals(20.0, s.repository.savedQuotes.value.first { it.id == saved.id }.depositAmount, 1e-9)
    }

    @Test
    fun anAmountThatRoundsToZeroIsAnInvalidAmount() {
        val s = setup()
        val saved = s.order("Suporte", status = OrderStatus.APROVADO)
        s.viewModel.openPayments(saved)
        s.viewModel.startRegisteringPayment()

        s.viewModel.updatePaymentForm { it.copy(amountText = "0,004") }
        s.viewModel.registerPayment()

        assertEquals(UiText.Res(Res.string.payments_error_amount), s.state.payments!!.form!!.error)
        assertTrue(s.repository.savedQuotes.value.first { it.id == saved.id }.payments.isEmpty())
    }

    @Test
    fun deletingAPaymentAsksFirstAndKeepsTheOtherNumbers() {
        val s = setup()
        val saved = s.order("Suporte")
        s.repository.addPayment(saved.id, 10.0, PaymentMethod.PIX, 1L)
        s.repository.addPayment(saved.id, 20.0, PaymentMethod.PIX, 2L)
        s.viewModel.openPayments(saved)
        val first = s.state.payments!!.quote.payments.first()

        s.viewModel.requestDeletePayment(first)
        assertEquals(2, s.repository.savedQuotes.value.first { it.id == saved.id }.payments.size, "só confirma depois")
        s.viewModel.confirmDeletePayment()

        val left = s.repository.savedQuotes.value.first { it.id == saved.id }.payments
        assertEquals(1, left.size)
        assertEquals(2, left.single().number, "excluir não renumera")
        assertNull(s.state.payments!!.deleting)
    }

    @Test
    fun theReceiptIsGeneratedForTheChosenPayment() {
        val s = setup()
        val saved = s.order("Suporte")
        s.platform.nextSaveLocation = dataDir.path + "/"
        val payment = s.repository.addPayment(saved.id, 10.0, PaymentMethod.PIX, 1L)!!
        s.viewModel.openPayments(saved)

        s.viewModel.generateReceipt(payment.id)

        val written = s.platform.awaitWritten(1)
        assertEquals("Recibo #0001-1 - Suporte.pdf", written.keys.single().substringAfterLast('/'))
        assertTrue(written.values.single().decodeToString(0, 4) == "%PDF")
    }

    @Test
    fun thePaymentsDialogClosesWhenTheOrderDisappears() {
        val s = setup()
        val saved = s.order("Suporte")
        s.viewModel.openPayments(saved)

        s.repository.delete(saved.id)

        assertNull(s.state.payments)
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
        const val AWAIT_MILLIS = 10_000L
    }
}
