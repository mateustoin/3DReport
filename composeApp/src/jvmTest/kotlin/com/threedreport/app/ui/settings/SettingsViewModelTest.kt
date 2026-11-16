package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_error_percent_max
import _3dreport.composeapp.generated.resources.settings_notice_saved
import _3dreport.composeapp.generated.resources.settings_notice_saved_fixed_cost
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

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
    fun whatTheOnboardingSavedIsKeptByTheFirstSaveInSettings() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)

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
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(profitMarginPercentText = "150") }

        repository.update(repository.settings.value.copy(energyPricePerKwh = 0.95))
        viewModel.syncWith(repository.settings.value)

        assertEquals("150", viewModel.uiState.value.profitMarginPercentText)
        assertTrue(viewModel.hasUnsavedChanges)
    }

    @Test
    fun discardGoesBackToWhatIsSaved() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        val saved = viewModel.uiState.value.profitMarginPercentText
        viewModel.update { it.copy(profitMarginPercentText = "150") }
        assertTrue(viewModel.hasUnsavedChanges)

        viewModel.discard()

        assertEquals(saved, viewModel.uiState.value.profitMarginPercentText)
        assertFalse(viewModel.hasUnsavedChanges)
        assertEquals(saved.toDouble() / 100, repository.settings.value.profitMargin, 1e-9)
    }

    @Test
    fun percentagesAcceptTheSignAndFieldsShowCommas() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(taxRatePercentText = "6%", energyPricePerKwhText = "0,95") }

        viewModel.save()

        assertEquals(0.06, repository.settings.value.taxRate, 1e-9)
        assertEquals("6", viewModel.uiState.value.taxRatePercentText)
        assertEquals("0,95", viewModel.uiState.value.energyPricePerKwhText)
    }

    /** O aviso emitido depois da última ação, ou `null` se não houve nenhum. */
    private fun SettingsViewModel.pendingNotice(): UiNotice? = runBlocking { withTimeoutOrNull(200) { notices.first() } }

    @Test
    fun anInvalidFieldNamesTheFieldAndSavesNothing() {
        val repository = SettingsRepository()
        val before = repository.settings.value
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(laborRatePerHourText = "-10") }

        viewModel.save()

        assertEquals(uiText(Res.string.common_error_negative), viewModel.uiState.value.errors[SettingsField.LABOR])
        assertEquals(setOf(SettingsField.LABOR), viewModel.uiState.value.errors.keys)
        assertEquals(before, repository.settings.value)
        assertNull(viewModel.pendingNotice(), "com erro, nada foi salvo e nada é avisado")
    }

    @Test
    fun aTextThatIsNotANumberAndATaxOf100PercentAreErrorsOfTheirOwnFields() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(energyPricePerKwhText = "abc", taxRatePercentText = "100") }

        viewModel.save()

        val errors = viewModel.uiState.value.errors
        assertEquals(uiText(Res.string.common_error_number_invalid), errors[SettingsField.ENERGY])
        assertEquals(uiText(Res.string.settings_error_percent_max), errors[SettingsField.TAX])
    }

    @Test
    fun editingAfterAnErrorClearsTheErrors() {
        val viewModel = SettingsViewModel(SettingsRepository(), scope)
        viewModel.update { it.copy(laborRatePerHourText = "-10") }
        viewModel.save()

        viewModel.update { it.copy(laborRatePerHourText = "10") }

        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun savingAnnouncesOnceAndTheFormIsCleanAgain() {
        val viewModel = SettingsViewModel(SettingsRepository(), scope)
        viewModel.update { it.copy(profitMarginPercentText = "150") }
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.save()

        assertEquals(UiNotice(uiText(Res.string.settings_notice_saved)), viewModel.pendingNotice())
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun savingCostsAndDocumentsTogetherAnnouncesOnlyOnce() {
        val settingsViewModel = SettingsViewModel(SettingsRepository(), scope)
        val brandingViewModel = BrandingViewModel(BrandingRepository(), TemplateRepository(), scope)
        settingsViewModel.update { it.copy(profitMarginPercentText = "150") }
        brandingViewModel.update("Minha Marca")

        val failedSection = saveAllSettings(settingsViewModel, brandingViewModel)

        assertNull(failedSection)
        assertFalse(hasUnsavedSettings(settingsViewModel, brandingViewModel))
        assertEquals(UiNotice(uiText(Res.string.settings_notice_saved)), settingsViewModel.pendingNotice())
        assertNull(runBlocking { withTimeoutOrNull(200) { brandingViewModel.notices.first() } })
    }

    @Test
    fun savingAllTakesTheFirstErrorToItsSection() {
        val settingsViewModel = SettingsViewModel(SettingsRepository(), scope)
        val brandingViewModel = BrandingViewModel(BrandingRepository(), TemplateRepository(), scope)
        brandingViewModel.setValidityDays("dez")

        assertEquals(SettingsSection.DOCUMENTS, saveAllSettings(settingsViewModel, brandingViewModel))

        settingsViewModel.update { it.copy(laborRatePerHourText = "-1") }
        assertEquals(SettingsSection.BUSINESS, saveAllSettings(settingsViewModel, brandingViewModel))
    }

    @Test
    fun minimumPriceAndRushSurchargeRoundTrip() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(minimumOrderPriceText = "25", rushSurchargeRatePercentText = "15") }

        viewModel.save()

        assertEquals(25.0, repository.settings.value.minimumOrderPrice, 1e-9)
        assertEquals(0.15, repository.settings.value.rushSurchargeRate, 1e-9)
        assertEquals("25", viewModel.uiState.value.minimumOrderPriceText)
        assertEquals("15", viewModel.uiState.value.rushSurchargeRatePercentText)
    }

    @Test
    fun blankMinimumPriceAndRushSurchargeSaveAsZero() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(minimumOrderPriceText = "", rushSurchargeRatePercentText = "") }

        viewModel.save()

        assertEquals(0.0, repository.settings.value.minimumOrderPrice)
        assertEquals(0.0, repository.settings.value.rushSurchargeRate)
    }

    @Test
    fun aFixedCostWithoutHoursWarnsThatItIsNotCharged() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        viewModel.update { it.copy(monthlyFixedCostText = "300", productiveHoursPerMonthText = "0") }

        viewModel.save()

        assertTrue(viewModel.uiState.value.fixedCostWithoutHours)
        assertEquals(UiNotice(uiText(Res.string.settings_notice_saved_fixed_cost), isError = true), viewModel.pendingNotice())
        assertNull(viewModel.pendingNotice(), "um aviso só: o de salvo já diz do custo fixo")
    }

    // ---- A1: o número é lido com o separador da moeda que o campo mostra

    @Test
    fun withADotCurrencyAThousandsCommaIsReadAsThousands() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope, currency = { Currency.USD })
        viewModel.update { it.copy(laborRatePerHourText = "1,250", energyPricePerKwhText = "0.15", monthlyFixedCostText = "1,250.50") }

        viewModel.save()

        assertEquals(1250.0, repository.settings.value.laborRatePerHour, 1e-9)
        assertEquals(0.15, repository.settings.value.energyPricePerKwh, 1e-9)
        assertEquals(1250.5, repository.settings.value.monthlyFixedCost, 1e-9)
    }

    @Test
    fun withACommaCurrencyAThousandsDotIsReadAsThousands() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope, currency = { Currency.BRL })
        viewModel.update { it.copy(laborRatePerHourText = "1.250", energyPricePerKwhText = "0,95") }

        viewModel.save()

        assertEquals(1250.0, repository.settings.value.laborRatePerHour, 1e-9)
        assertEquals(0.95, repository.settings.value.energyPricePerKwh, 1e-9)
    }

    @Test
    fun theCurrencyIsReadAtSaveTimeSoChangingItIsFollowed() {
        var currency = Currency.BRL
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope, currency = { currency })
        viewModel.update { it.copy(laborRatePerHourText = "1,250") }
        currency = Currency.USD

        viewModel.save()

        assertEquals(1250.0, repository.settings.value.laborRatePerHour, 1e-9)
    }

    // ---- A2: o aviso de custo fixo descreve o que está gravado

    private fun SettingsRepository.withFixedCostWithoutHours() =
        update(settings.value.copy(monthlyFixedCost = 300.0, productiveHoursPerMonth = 0.0))

    @Test
    fun theFixedCostWarningLastsWhileWhatIsSavedHasFixedCostAndNoHours() {
        val repository = SettingsRepository()
        repository.withFixedCostWithoutHours()
        val viewModel = SettingsViewModel(repository, scope)
        assertTrue(viewModel.uiState.value.fixedCostWithoutHours, "vale ao abrir, não só logo depois de salvar")

        viewModel.update { it.copy(laborRatePerHourText = "20") }
        assertTrue(viewModel.uiState.value.fixedCostWithoutHours, "editar outro campo não apaga")

        viewModel.update { it.copy(productiveHoursPerMonthText = "120") }
        assertTrue(viewModel.uiState.value.fixedCostWithoutHours, "o que está gravado ainda não mudou")

        viewModel.discard()
        assertTrue(viewModel.uiState.value.fixedCostWithoutHours)

        viewModel.update { it.copy(productiveHoursPerMonthText = "120") }
        viewModel.save()
        assertFalse(viewModel.uiState.value.fixedCostWithoutHours, "com as horas gravadas, o aviso sai")
    }

    @Test
    fun theFixedCostWarningFollowsWhatSomethingElseSaved() {
        val repository = SettingsRepository()
        val viewModel = SettingsViewModel(repository, scope)
        assertFalse(viewModel.uiState.value.fixedCostWithoutHours)

        repository.withFixedCostWithoutHours()

        assertTrue(viewModel.uiState.value.fixedCostWithoutHours)
        assertFalse(viewModel.hasUnsavedChanges, "o aviso não conta como alteração por salvar")
    }

    // ---- A4: salvar custos e documentos juntos é tudo ou nada

    private fun pair(): Triple<SettingsViewModel, BrandingViewModel, Pair<SettingsRepository, BrandingRepository>> {
        val settings = SettingsRepository()
        val branding = BrandingRepository()
        return Triple(SettingsViewModel(settings, scope), BrandingViewModel(branding, TemplateRepository(), scope), settings to branding)
    }

    @Test
    fun ifTheCostsFailNothingIsSavedAndThereIsNoSavedNotice() {
        val (settingsViewModel, brandingViewModel, repositories) = pair()
        settingsViewModel.update { it.copy(laborRatePerHourText = "-1") }
        brandingViewModel.update("Minha Marca")

        assertEquals(SettingsSection.BUSINESS, saveAllSettings(settingsViewModel, brandingViewModel))

        assertNull(repositories.second.branding.value.brandName, "os documentos válidos também ficam sem gravar")
        assertTrue(brandingViewModel.hasUnsavedChanges)
        assertNull(settingsViewModel.pendingNotice())
        assertNull(runBlocking { withTimeoutOrNull(200) { brandingViewModel.notices.first() } })
    }

    @Test
    fun ifTheDocumentsFailNothingIsSavedAndThereIsNoSavedNotice() {
        val (settingsViewModel, brandingViewModel, repositories) = pair()
        val before = repositories.first.settings.value
        settingsViewModel.update { it.copy(profitMarginPercentText = "150") }
        brandingViewModel.setValidityDays("dez")

        assertEquals(SettingsSection.DOCUMENTS, saveAllSettings(settingsViewModel, brandingViewModel))

        assertEquals(before, repositories.first.settings.value, "os custos válidos também ficam sem gravar")
        assertTrue(settingsViewModel.hasUnsavedChanges)
        assertNull(settingsViewModel.pendingNotice())
        assertNull(runBlocking { withTimeoutOrNull(200) { brandingViewModel.notices.first() } })
    }

    @Test
    fun ifBothFailTheCostsSectionComesFirstAndBothShowTheirErrors() {
        val (settingsViewModel, brandingViewModel, _) = pair()
        settingsViewModel.update { it.copy(laborRatePerHourText = "-1") }
        brandingViewModel.setValidityDays("dez")

        assertEquals(SettingsSection.BUSINESS, saveAllSettings(settingsViewModel, brandingViewModel))

        assertTrue(settingsViewModel.uiState.value.errors.isNotEmpty())
        assertNotNull(brandingViewModel.uiState.value.validityError)
        assertNull(settingsViewModel.pendingNotice())
    }

    @Test
    fun ifBothPassBothAreSavedWithOneNotice() {
        val (settingsViewModel, brandingViewModel, repositories) = pair()
        settingsViewModel.update { it.copy(profitMarginPercentText = "150") }
        brandingViewModel.update("Minha Marca")

        assertNull(saveAllSettings(settingsViewModel, brandingViewModel))

        assertEquals(1.5, repositories.first.settings.value.profitMargin, 1e-9)
        assertEquals("Minha Marca", repositories.second.branding.value.brandName)
        assertEquals(UiNotice(uiText(Res.string.settings_notice_saved)), settingsViewModel.pendingNotice())
        assertNull(runBlocking { withTimeoutOrNull(200) { brandingViewModel.notices.first() } })
    }
}
