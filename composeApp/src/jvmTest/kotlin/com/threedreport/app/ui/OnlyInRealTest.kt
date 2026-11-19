package com.threedreport.app.ui

import com.threedreport.app.data.MaintenanceRepository
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.ui.history.defaultPaymentMethod
import com.threedreport.app.ui.history.paymentMethodsFor
import com.threedreport.app.ui.printers.PRINTER_PRESETS
import com.threedreport.app.ui.printers.PrinterListViewModel
import com.threedreport.app.ui.settings.OrderSettingsViewModel
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** O que só vale pro Brasil some fora do real (decisão 16b da Leva 11B): preço de referência, Pix e rastreio dos Correios. */
class OnlyInRealTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun theReferencePriceOnlyFillsTheFormInReal() {
        val preset = PRINTER_PRESETS.first { it.model == "A1 Combo" }
        fun form(currency: Currency) = PrinterListViewModel(PrinterRepository(), QuoteHistoryRepository(), MaintenanceRepository(), scope, currency = { currency })
            .apply { startAddFromPreset(preset) }.uiState.value.form!!

        assertEquals("4498,50", form(Currency.BRL).machinePriceText)
        assertEquals("", form(Currency.USD).machinePriceText)
        assertEquals("95", form(Currency.USD).printerPowerWattsText, "o consumo continua")
    }

    @Test
    fun pixIsOnlyOfferedInReal() {
        assertTrue(PaymentMethod.PIX in paymentMethodsFor(Currency.BRL))
        assertFalse(PaymentMethod.PIX in paymentMethodsFor(Currency.USD))
        assertEquals(PaymentMethod.PIX, defaultPaymentMethod(null, Currency.BRL))
        assertEquals(PaymentMethod.CARD, defaultPaymentMethod(null, Currency.USD))
        assertEquals(PaymentMethod.CARD, defaultPaymentMethod(PaymentMethod.PIX, Currency.EUR))
        assertEquals(PaymentMethod.CASH, defaultPaymentMethod(PaymentMethod.CASH, Currency.USD))
    }

    @Test
    fun theDefaultTrackingSiteFollowsTheDefaultCurrency() {
        val inReal = OrderSettingsViewModel(OrderSettingsRepository(), scope, currency = { Currency.BRL })
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, inReal.uiState.value.trackingUrlText)

        val inDollars = OrderSettingsViewModel(OrderSettingsRepository(), scope, currency = { Currency.USD })
        assertEquals("", inDollars.uiState.value.trackingUrlText)
        inDollars.useDefaultTracking()
        assertEquals("", inDollars.uiState.value.trackingUrlText, "o padrão fora do real é vazio")
    }

    @Test
    fun savingOtherSettingsInDollarsDoesNotWriteAnEmptyTrackingSite() {
        var currency = Currency.USD
        val repository = OrderSettingsRepository()
        val inDollars = OrderSettingsViewModel(repository, scope, currency = { currency })
        inDollars.setDepositPercent("30")
        inDollars.save()
        assertEquals(null, repository.settings.value.trackingUrlTemplate, "o site que só aparecia vazio continua o padrão do app")

        currency = Currency.BRL
        val inReal = OrderSettingsViewModel(repository, scope, currency = { currency })
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, inReal.uiState.value.trackingUrlText, "o link dos Correios volta")
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE.replace(OrderSettings.CODE_MARKER, "AA1"), repository.settings.value.trackingUrl("AA1", Currency.BRL))
    }

    @Test
    fun erasingTheCorreiosOnPurposeStaysErasedAfterSavingInDollars() {
        var currency = Currency.BRL
        val repository = OrderSettingsRepository()
        val inReal = OrderSettingsViewModel(repository, scope, currency = { currency })
        inReal.setTrackingUrl("")
        inReal.save()
        assertEquals("", repository.settings.value.trackingUrlTemplate)

        currency = Currency.USD
        val inDollars = OrderSettingsViewModel(repository, scope, currency = { currency })
        inDollars.setDepositPercent("10")
        inDollars.save()
        assertEquals("", repository.settings.value.trackingUrlTemplate, "o site apagado de propósito continua apagado")
    }

    @Test
    fun aSiteTypedInDollarsAndThenErasedGoesBackToTheDefault() {
        val repository = OrderSettingsRepository()
        val inDollars = OrderSettingsViewModel(repository, scope, currency = { Currency.USD })
        inDollars.setTrackingUrl("https://x.test/?c={code}")
        inDollars.save()
        assertEquals("https://x.test/?c={code}", repository.settings.value.trackingUrlTemplate)
        inDollars.setTrackingUrl("")
        inDollars.save()
        assertEquals(null, repository.settings.value.trackingUrlTemplate, "em dólar não há padrão pra apagar")
    }

    @Test
    fun theUseDefaultButtonOnlyAppearsInRealAndFollowsTheCurrency() {
        val currency = kotlinx.coroutines.flow.MutableStateFlow(Currency.BRL)
        val viewModel = OrderSettingsViewModel(OrderSettingsRepository(), scope, currency = { currency.value }, currencyChanges = currency)
        assertFalse(viewModel.uiState.value.canUseDefaultTracking, "já está nos Correios")
        viewModel.setTrackingUrl("https://x.test/{codigo}")
        assertTrue(viewModel.uiState.value.canUseDefaultTracking)
        viewModel.discard()

        currency.value = Currency.USD
        assertEquals("", viewModel.uiState.value.trackingUrlText, "sem edição, o campo mostra o padrão da moeda nova")
        viewModel.setTrackingUrl("https://x.test/{code}")
        assertFalse(viewModel.uiState.value.canUseDefaultTracking, "fora do real não há site padrão")
    }
}
