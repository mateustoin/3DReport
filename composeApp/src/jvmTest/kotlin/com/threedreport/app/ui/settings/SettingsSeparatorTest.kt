package com.threedreport.app.ui.settings

import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.Currency
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O texto que vai pro campo usa o mesmo separador decimal com que o campo é lido de volta (Leva 10B, parte 7): em dólar,
 * 12,345 gravado voltava como "12,345" e era lido como doze mil.
 */
class SettingsSeparatorTest {

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
    fun settingsShowThreeDecimalsWithTheDollarSeparatorAndSaveThemUnchanged() {
        val repository = SettingsRepository()
        repository.update(repository.settings.value.copy(laborRatePerHour = 12.345, energyPricePerKwh = 0.125))
        val viewModel = SettingsViewModel(repository, scope, currency = { Currency.USD })

        assertEquals("12.345", viewModel.uiState.value.laborRatePerHourText)
        assertEquals("0.125", viewModel.uiState.value.energyPricePerKwhText)
        assertFalse(viewModel.hasUnsavedChanges)

        viewModel.save()

        assertEquals(12.345, repository.settings.value.laborRatePerHour, 1e-9)
        assertEquals(0.125, repository.settings.value.energyPricePerKwh, 1e-9)
    }

    @Test
    fun aCleanDraftFollowsTheCurrencyWithoutBecomingUnsaved() {
        val repository = SettingsRepository()
        repository.update(repository.settings.value.copy(laborRatePerHour = 12.345))
        val currency = MutableStateFlow(Currency.BRL)
        val viewModel = SettingsViewModel(repository, scope, currency = { currency.value }, currencyChanges = currency)
        assertEquals("12,345", viewModel.uiState.value.laborRatePerHourText)

        currency.value = Currency.USD

        assertEquals("12.345", viewModel.uiState.value.laborRatePerHourText)
        assertFalse(viewModel.hasUnsavedChanges, "trocar de moeda com rascunho limpo não cria alteração")
    }

    @Test
    fun aDirtyDraftStaysAsTheUserTypedItWhenTheCurrencyChanges() {
        val repository = SettingsRepository()
        val currency = MutableStateFlow(Currency.BRL)
        val viewModel = SettingsViewModel(repository, scope, currency = { currency.value }, currencyChanges = currency)
        viewModel.update { it.copy(laborRatePerHourText = "45,5") }

        currency.value = Currency.USD

        assertEquals("45,5", viewModel.uiState.value.laborRatePerHourText)
        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun editingAChannelShowsItsNumbersWithTheCurrencySeparator() {
        val repository = SalesChannelRepository()
        val tiers = listOf(
            ChannelFeeTier(upToUnitPrice = 50.125, feeRate = 0.12345, fixedFeePerItem = 1.5),
            ChannelFeeTier(upToUnitPrice = null, feeRate = 0.2, fixedFeePerItem = 0.0),
        )
        repository.add(SalesChannel(id = "loja", name = "Loja", feeRate = 0.12345, fixedFeePerItem = 1.5, tiers = tiers))
        val viewModel = SalesChannelViewModel(repository, scope, currency = { Currency.USD })

        viewModel.startEdit(repository.channels.value.single())

        val form = viewModel.uiState.value.form!!
        assertEquals("12.345", form.feeRatePercentText)
        assertEquals("1.5", form.fixedFeePerItemText)
        assertEquals("50.125", form.tiers[0].upToUnitPriceText)
        assertEquals("12.345", form.tiers[0].feeRatePercentText)

        viewModel.save()
        assertEquals(0.12345, repository.channels.value.single().feeRate, 1e-9)
        assertEquals(50.125, repository.channels.value.single().tiers[0].upToUnitPrice!!, 1e-9)
    }
}
