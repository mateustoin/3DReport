package com.threedreport.app.ui.settings

import com.threedreport.app.data.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun whatTheOnboardingSavedIsKeptByTheFirstSaveInSettings() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository)

        // O onboarding grava direto no repositório, com a tela de Configurações já criada.
        repository.update(repository.settings.value.copy(energyPricePerKwh = 0.95, laborRatePerHour = 30.0))
        viewModel.syncWith(repository.settings.value)
        viewModel.save()

        assertEquals(0.95, repository.settings.value.energyPricePerKwh)
        assertEquals(30.0, repository.settings.value.laborRatePerHour)
    }

    @Test
    fun anEditInProgressIsNotReplacedWhenSomethingElseSaves() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository)
        viewModel.update { it.copy(profitMarginPercentText = "150") }

        repository.update(repository.settings.value.copy(energyPricePerKwh = 0.95))
        viewModel.syncWith(repository.settings.value)

        assertEquals("150", viewModel.uiState.value.profitMarginPercentText)
        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun percentagesAcceptTheSignAndFieldsShowCommas() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository)
        viewModel.update { it.copy(taxRatePercentText = "6%", energyPricePerKwhText = "0,95") }

        viewModel.save()

        assertEquals(0.06, repository.settings.value.taxRate, 1e-9)
        assertEquals("6", viewModel.uiState.value.taxRatePercentText)
        assertEquals("0,95", viewModel.uiState.value.energyPricePerKwhText)
    }

    @Test
    fun anInvalidFieldNamesTheFieldAndSavesNothing() {
        val repository = SettingsRepository()
        val before = repository.settings.value
        val viewModel = SettingsViewModel(repository)
        viewModel.update { it.copy(laborRatePerHourText = "-10") }

        viewModel.save()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Valor da sua hora de trabalho"))
        assertEquals(before, repository.settings.value)
    }

    @Test
    fun aFixedCostWithoutHoursWarnsThatItIsNotCharged() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository)
        viewModel.update { it.copy(monthlyFixedCostText = "300", productiveHoursPerMonthText = "0") }

        viewModel.save()

        assertNotNull(viewModel.uiState.value.warningMessage)
    }
}
