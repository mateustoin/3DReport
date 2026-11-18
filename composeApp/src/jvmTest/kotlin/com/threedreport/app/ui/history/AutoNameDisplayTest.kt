package com.threedreport.app.ui.history

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.AppLocaleState
import com.threedreport.app.platform.DesktopLocale
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.UiLanguage
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O nome automático (decisão 139) aparece no idioma do app em tudo o que mostra o nome, não só nas listas (item 7b da
 * Leva 11B): busca, aviso de prazo vencido e avisos de andamento e de exclusão. A busca acha pelo nome mostrado e também
 * pelo texto gravado.
 */
class AutoNameDisplayTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val original = Locale.getDefault()

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        DesktopLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        Locale.setDefault(original)
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = Quote(
        prints = listOf(
            QuotedPrint(
                job = PrintJob(filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24), filamentLengthMeters = 12.0, printTimeMinutes = 190.0),
                printerId = "printer",
                printerName = "Impressora",
                cost = PrintCost(material = 3.58, energy = 1.48, maintenance = 0.54, finishing = 0.36, investmentReturn = 1.78, fixedCost = 0.0),
            ),
        ),
        costs = CostBreakdown(3.58, 1.48, 0.54, 0.36, 0.36, 1.78, 0.0, 0.0, 0.0),
        salePrice = 16.19,
    )

    private fun viewModelOf(repository: QuoteHistoryRepository) = QuoteHistoryViewModel(
        repository, BrandingRepository(), FilamentRepository(), PrinterRepository(), SettingsRepository(), SalesChannelRepository(), PreferencesRepository(),
        today = { TODAY }, clock = { 0L }, platform = FakePlatform(), scope = scope,
    )

    /** Salvo em português com o nome em branco: o texto gravado é "Orçamento - dd/MM/yyyy HH:mm". */
    private fun QuoteHistoryRepository.saveAutoNamed(deadline: Long? = null): SavedQuote {
        val saved = save(name = "", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = deadline)
        assertTrue(saved.autoNamed && saved.name.startsWith("Orçamento - "), saved.name)
        return saved
    }

    private fun inEnglish() = DesktopLocale.use(AppLocaleState(UiLanguage.ENGLISH, "US"))

    private fun QuoteHistoryViewModel.awaitNotice(): UiNotice = runBlocking { withTimeout(5_000) { notices.first() } }

    @Test
    fun theSearchFindsTheShownNameAndTheStoredOne() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveAutoNamed()
        repository.save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        inEnglish()
        val viewModel = viewModelOf(repository)

        viewModel.setSearchQuery("quote -")
        assertEquals(listOf(saved.id), viewModel.uiState.value.rows.map { it.quote.id }, "pelo nome mostrado em inglês")

        viewModel.setSearchQuery("Orçamento")
        assertEquals(listOf(saved.id), viewModel.uiState.value.rows.map { it.quote.id }, "pelo texto gravado")
    }

    @Test
    fun theOverdueWarningShowsTheNameInTheLanguageOfTheApp() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveAutoNamed(deadline = TODAY - 1)
        inEnglish()
        val viewModel = viewModelOf(repository)

        viewModel.copyQuoteToClipboard(repository.savedQuotes.value.single())

        val shown = viewModel.uiState.value.pendingExport!!.overdue.single().name
        assertEquals(repository.autoName(saved.savedAtEpochMillis), shown)
        assertTrue(shown.startsWith("Quote - "), shown)
    }

    @Test
    fun theDeleteNoticeShowsTheNameInTheLanguageOfTheApp() {
        val repository = QuoteHistoryRepository()
        val saved = repository.saveAutoNamed()
        inEnglish()
        val viewModel = viewModelOf(repository)

        viewModel.delete(saved.id)

        val args = (viewModel.awaitNotice().message as UiText.Res).args
        assertEquals(listOf<Any>(repository.autoName(saved.savedAtEpochMillis)), args)
        assertTrue((args.single() as String).startsWith("Quote - "), args.toString())
    }

    @Test
    fun aTypedNameIsShownAsTyped() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "Orçamento da Maria", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        inEnglish()
        val viewModel = viewModelOf(repository)

        viewModel.delete(saved.id)

        assertEquals(listOf<Any>("Orçamento da Maria"), (viewModel.awaitNotice().message as UiText.Res).args)
    }
}

private const val TODAY = 100L
