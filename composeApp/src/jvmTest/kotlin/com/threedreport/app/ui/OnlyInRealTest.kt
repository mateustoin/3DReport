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
}
