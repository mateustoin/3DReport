package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.settings_orders_error_deposit
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.message.MessageTemplateKind
import com.threedreport.core.model.BrandingSettings
import com.threedreport.core.model.Currency
import com.threedreport.core.model.OrderSettings
import com.threedreport.core.pix.PixKeyType
import com.threedreport.core.pix.PixSetupIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pix e mensagens prontas em "Documentos pro cliente", e a seção Pedidos de Configurações (Leva 11). */
class Leva11SettingsViewModelTest {

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

    private fun branding(repository: BrandingRepository = BrandingRepository()) = BrandingViewModel(repository, TemplateRepository(), scope)

    // ---- Pix

    @Test
    fun theFormStartsEmptyAndSavingKeepsThePixFields() {
        val repository = BrandingRepository()
        val viewModel = branding(repository)
        assertEquals("", viewModel.uiState.value.pixKeyInput)
        assertNull(viewModel.uiState.value.pixKey)

        viewModel.setPixKey("  loja@Example.com ")
        viewModel.setPixName("Sua Loja")
        viewModel.setPixCity("São Paulo")
        assertTrue(viewModel.hasUnsavedChanges)
        viewModel.save()

        val saved = repository.branding.value
        assertEquals("loja@Example.com", saved.pixKey)
        assertEquals("Sua Loja", saved.pixReceiverName)
        assertEquals("São Paulo", saved.pixCity)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun theKeyTypeIsRecognizedAndAnUnknownKeyIsStillAccepted() {
        val viewModel = branding()

        viewModel.setPixKey("529.982.247-25")
        assertEquals(PixKeyType.CPF, viewModel.uiState.value.pixKey?.type)

        viewModel.setPixKey("qualquer coisa")
        assertEquals(PixKeyType.UNKNOWN, viewModel.uiState.value.pixKey?.type)
        viewModel.setPixName("Loja")
        viewModel.setPixCity("Rio")
        assertFalse(PixSetupIssue.KEY_MISSING in viewModel.uiState.value.pixIssues, "chave estranha é aceita")
    }

    @Test
    fun theNameIsSuggestedByTheBrandOnTheFirstKeyOnly() {
        val viewModel = branding()
        viewModel.update("Sua Loja 3D")

        viewModel.setPixKey("a@b.com")
        assertEquals("Sua Loja 3D", viewModel.uiState.value.pixNameInput)

        viewModel.setPixName("Outro nome")
        viewModel.setPixKey("c@d.com")
        assertEquals("Outro nome", viewModel.uiState.value.pixNameInput, "não sobrescreve o que a pessoa digitou")
    }

    @Test
    fun whatIsMissingFollowsTheFields() {
        val viewModel = branding()
        viewModel.setPixKey("a@b.com")
        assertEquals(setOf(PixSetupIssue.NAME_MISSING, PixSetupIssue.CITY_MISSING), viewModel.uiState.value.pixIssues)

        viewModel.setPixName("Loja")
        viewModel.setPixCity("Rio")
        assertTrue(viewModel.uiState.value.pixIssues.isEmpty())

        // O BR Code só leva ASCII: chave com acento é apontada, e não vira Pix.
        viewModel.setPixKey("joão@b.com")
        assertEquals(setOf(PixSetupIssue.KEY_INVALID_CHARACTERS), viewModel.uiState.value.pixIssues)
    }

    // ---- mensagens

    @Test
    fun onlyAChangedTemplateIsSavedAndTheDefaultComesBack() {
        val repository = BrandingRepository()
        val viewModel = branding(repository)
        val default = "Oi[, {cliente}]! {peca} ficou pronto!"

        viewModel.setMessageTemplate(MessageTemplateKind.READY, "Chegou! {peca}", default)
        viewModel.save()
        assertEquals(mapOf("ready" to "Chegou! {peca}"), repository.branding.value.messageTemplates)

        viewModel.setMessageTemplate(MessageTemplateKind.READY, default, default)
        viewModel.save()
        assertTrue(repository.branding.value.messageTemplates.isEmpty(), "igual ao padrão não é gravado")
    }

    @Test
    fun resetGoesBackToTheDefaultAndDiscardDropsTheDraft() {
        val repository = BrandingRepository().also { it.update(BrandingSettings(messageTemplates = mapOf("ready" to "X", "futureKind" to "Y"))) }
        val viewModel = branding(repository)
        assertEquals("X", viewModel.uiState.value.messageEdits["ready"])

        viewModel.resetMessageTemplate(MessageTemplateKind.READY)
        assertTrue(viewModel.hasUnsavedChanges)
        viewModel.save()
        assertEquals(mapOf("futureKind" to "Y"), repository.branding.value.messageTemplates, "o que esta versão não conhece fica")

        viewModel.setMessageTemplate(MessageTemplateKind.PRINTING, "Imprimindo {peca}", "padrão")
        viewModel.discard()
        assertFalse(viewModel.hasUnsavedChanges)
        assertFalse("printing" in viewModel.uiState.value.messageEdits)
    }

    @Test
    fun savingOtherFieldsNeverLosesThePixOrTheTemplates() {
        val repository = BrandingRepository().also {
            it.update(BrandingSettings(pixKey = "a@b.com", pixReceiverName = "Loja", pixCity = "Rio", messageTemplates = mapOf("ready" to "X")))
        }
        val viewModel = branding(repository)

        viewModel.update("Nova marca")
        viewModel.save()

        val saved = repository.branding.value
        assertEquals("a@b.com", saved.pixKey)
        assertEquals("Loja", saved.pixReceiverName)
        assertEquals("Rio", saved.pixCity)
        assertEquals(mapOf("ready" to "X"), saved.messageTemplates)
    }

    // ---- Pedidos

    private fun orders(repository: OrderSettingsRepository = OrderSettingsRepository()) = OrderSettingsViewModel(repository, scope) to repository

    @Test
    fun theOrdersFormStartsWithTheDefaultsAndNothingToSave() {
        val (viewModel, _) = orders()

        val state = viewModel.uiState.value
        assertEquals("0", state.depositPercentText)
        assertEquals("3", state.staleDaysText)
        assertEquals(OrderSettings.DEFAULT_TRACKING_URL_TEMPLATE, state.trackingUrlText)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun editsStayInTheDraftUntilSaved() {
        val (viewModel, repository) = orders()

        viewModel.setDepositPercent("30")
        assertTrue(viewModel.hasUnsavedChanges)
        assertEquals(0.0, repository.settings.value.defaultDepositRate, 1e-9, "ainda não gravou")

        viewModel.save()
        assertEquals(0.3, repository.settings.value.defaultDepositRate, 1e-9)
        assertFalse(viewModel.hasUnsavedChanges)

        viewModel.setDepositPercent("50")
        viewModel.discard()
        assertEquals("30", viewModel.uiState.value.depositPercentText)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun anInvalidFieldShowsItsErrorAndSavesNothing() {
        val (viewModel, repository) = orders()
        viewModel.setDepositPercent("abc")
        viewModel.setStaleDays("9999")

        assertFalse(viewModel.validate())
        assertEquals(UiText.Res(Res.string.common_error_number_invalid), viewModel.uiState.value.errors[OrderSettingsField.DEPOSIT])
        assertTrue(OrderSettingsField.STALE_DAYS in viewModel.uiState.value.errors)
        viewModel.save()
        assertEquals(OrderSettings(), repository.settings.value)

        viewModel.setDepositPercent("150")
        viewModel.validate()
        assertEquals(UiText.Res(Res.string.settings_orders_error_deposit), viewModel.uiState.value.errors[OrderSettingsField.DEPOSIT])
        viewModel.setDepositPercent("10")
        assertNull(viewModel.uiState.value.errors[OrderSettingsField.DEPOSIT])
    }

    @Test
    fun staleDaysAcceptZeroAndRejectNonsense() {
        val (viewModel, repository) = orders()

        viewModel.setStaleDays("0")
        viewModel.save()
        assertEquals(0, repository.settings.value.staleQuoteDays)
        viewModel.setStaleDays("2,5")
        assertFalse(viewModel.validate())
    }

    @Test
    fun theTrackingSiteIsDefaultWhenUntouchedEmptyWhenCleared() {
        val (viewModel, repository) = orders()
        assertNull(repository.settings.value.trackingUrlTemplate, "nada gravado é o padrão do app")

        viewModel.setTrackingUrl("https://exemplo.com/?c={codigo}")
        viewModel.save()
        assertEquals("https://exemplo.com/?c=AB1", repository.settings.value.trackingUrl("AB1", Currency.BRL))

        viewModel.setTrackingUrl("")
        viewModel.save()
        assertEquals("", repository.settings.value.trackingUrlTemplate)
        assertNull(repository.settings.value.trackingUrl("AB1", Currency.BRL), "apagar o campo é sem link")

        viewModel.useDefaultTracking()
        viewModel.save()
        assertNull(repository.settings.value.trackingUrlTemplate)
    }

    @Test
    fun theSharedSaveBarSavesTheOrdersToo() {
        val (orderViewModel, repository) = orders()
        val pricing = SettingsViewModel(SettingsRepository(), scope)
        val branding = branding()
        orderViewModel.setStaleDays("7")

        assertTrue(hasUnsavedSettings(pricing, branding, orderViewModel))
        assertNull(saveAllSettings(pricing, branding, orderViewModel))
        assertEquals(7, repository.settings.value.staleQuoteDays)

        orderViewModel.setStaleDays("zzz")
        assertEquals(SettingsSection.ORDERS, saveAllSettings(pricing, branding, orderViewModel))
    }

    @Test
    fun whatIsSavedElsewhereShowsInTheCleanForm() {
        val (viewModel, repository) = orders()

        repository.update(OrderSettings(defaultDepositRate = 0.5, staleQuoteDays = 7))

        assertEquals("50", viewModel.uiState.value.depositPercentText)
        assertEquals("7", viewModel.uiState.value.staleDaysText)
    }
}
