package com.threedreport.app.ui.settings

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.OrderSettingsRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.AppLocaleState
import com.threedreport.app.platform.DesktopLocale
import com.threedreport.app.platform.UiLanguage
import com.threedreport.app.ui.quote.QuoteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.Locale
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Trocar o idioma com um rascunho aberto passa os números que não são dinheiro pro separador novo (decisão 153): senão,
 * "1,250" g escrito em inglês (mil duzentos e cinquenta) seria lido como 1,25 g em português.
 */
class LanguageDraftSeparatorTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val original = Locale.getDefault()

    @BeforeTest
    fun setUp() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        Locale.setDefault(original)
        System.clearProperty("threedreport.dataDir")
    }

    private val english = AppLocaleState(UiLanguage.ENGLISH, "US")

    private fun quoteViewModel() = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository(),
    ).apply { setPrintTimeMinutes("60") }

    @Test
    fun aWeightTypedInEnglishKeepsItsValueInPortuguese() {
        DesktopLocale.use(english)
        val viewModel = quoteViewModel()
        viewModel.setWeightGrams("1,250")
        val meters = viewModel.uiState.value.result.quote!!.prints.single().job.filaments.single().lengthMeters

        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)

        val row = viewModel.uiState.value.input.prints.single().filaments.single()
        assertEquals("1250", row.weightText)
        assertEquals(meters, viewModel.uiState.value.result.quote!!.prints.single().job.filaments.single().lengthMeters, 1e-9)
        assertTrue(',' in row.lengthText, row.lengthText)
    }

    @Test
    fun aLengthTypedInPortugueseIsRewrittenWithTheDotInEnglish() {
        val viewModel = quoteViewModel()
        viewModel.setLengthMeters("12,5")

        DesktopLocale.use(english)

        assertEquals("12.5", viewModel.uiState.value.input.prints.single().filaments.single().lengthText)
        assertEquals(12.5, viewModel.uiState.value.result.quote!!.prints.single().job.filaments.single().lengthMeters, 1e-9)
    }

    @Test
    fun aReopenedQuoteKeepsItsFrozenPriceWhenTheLanguageChanges() {
        val history = QuoteHistoryRepository()
        val settings = SettingsRepository()
        val viewModel = QuoteViewModel(FilamentRepository(), PrinterRepository(), settings, ServiceRepository(), SalesChannelRepository(), history)
        viewModel.setLengthMeters("12,5")
        viewModel.setPrintTimeMinutes("190")
        assertTrue(viewModel.saveCurrentQuote())
        val saved = history.savedQuotes.value.single()
        settings.update(settings.settings.value.copy(energyPricePerKwh = 5.0))
        viewModel.loadForEditing(saved)
        assertTrue(viewModel.uiState.value.result.keepsOriginalPrice)

        DesktopLocale.use(english)

        assertEquals("12.5", viewModel.uiState.value.input.prints.single().filaments.single().lengthText)
        assertTrue(viewModel.uiState.value.result.keepsOriginalPrice, "trocar o idioma não muda o preço de um orçamento reaberto")
        assertEquals(saved.quote, viewModel.uiState.value.result.quote)
        assertFalse(viewModel.hasUnsavedEdits, "trocar o idioma não é uma edição")
    }

    @Test
    fun theDepositPercentFollowsTheLanguageWithoutLookingEdited() {
        val repository = OrderSettingsRepository()
        repository.update(repository.settings.value.copy(defaultDepositRate = 0.125))
        val viewModel = OrderSettingsViewModel(repository, scope)
        assertEquals("12,5", viewModel.uiState.value.depositPercentText)

        DesktopLocale.use(english)

        assertEquals("12.5", viewModel.uiState.value.depositPercentText)
        assertFalse(viewModel.hasUnsavedChanges)
        viewModel.save()
        assertEquals(0.125, repository.settings.value.defaultDepositRate, 1e-9)
    }

    @Test
    fun theOpenChannelFormRewritesItsPercentages() {
        val viewModel = SalesChannelViewModel(SalesChannelRepository(), scope)
        viewModel.startAdd()
        viewModel.setName("Etsy")
        viewModel.setFeeRate("6,5")

        DesktopLocale.use(english)

        assertEquals("6.5", viewModel.uiState.value.form!!.feeRatePercentText)
    }
}
