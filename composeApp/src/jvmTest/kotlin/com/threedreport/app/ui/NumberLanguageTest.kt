package com.threedreport.app.ui

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.AppLocaleState
import com.threedreport.app.platform.UiLanguage
import com.threedreport.app.ui.filaments.FilamentListViewModel
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.displayName
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.printers.PrinterFormState
import com.threedreport.app.ui.printers.toFormState
import com.threedreport.app.ui.quote.QuoteViewModel
import com.threedreport.app.ui.settings.SettingsViewModel
import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.QuotedPrint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Número que não é dinheiro (peso, densidade, diâmetro, comprimento, horas, percentual) segue o idioma do app, mostrado e
 * digitado (Leva 11B, item 7); dinheiro continua seguindo a moeda (decisão 107). Em português nada muda. O nome
 * automático das listas sai no idioma do app (item 7b).
 */
class NumberLanguageTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val english = AppLocaleState(UiLanguage.ENGLISH, "US")

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun reset() {
        scope.cancel()
        AppLocale.use(AppLocaleState.BRAZILIAN_PORTUGUESE)
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun filamentSpecsFollowTheLanguageAndThePriceFollowsTheCurrency() {
        AppLocale.use(english)
        val repository = FilamentRepository()
        repository.add(Filament(id = "f", name = "PLA Y", pricePerKg = 99.9, densityGPerCm3 = 1.24))
        val viewModel = FilamentListViewModel(repository, scope, currency = { Currency.BRL })

        viewModel.startEdit("f")
        val form = viewModel.uiState.value.form!!
        assertEquals("99,9", form.pricePerKgText, "dinheiro em real: vírgula")
        assertEquals("1.24", form.densityGPerCm3Text)
        assertEquals("1.75", form.diameterMmText)

        viewModel.updateForm { it.copy(densityGPerCm3Text = "1.27") }
        viewModel.save()
        assertEquals(1.27, repository.filaments.value.single { it.id == "f" }.densityGPerCm3)
    }

    @Test
    fun inPortugueseTheSpecsKeepTheComma() {
        val repository = FilamentRepository()
        repository.add(Filament(id = "f", name = "PLA Y", pricePerKg = 99.9, densityGPerCm3 = 1.24))
        val viewModel = FilamentListViewModel(repository, scope, currency = { Currency.USD })

        viewModel.startEdit("f")
        val form = viewModel.uiState.value.form!!
        assertEquals("99.9", form.pricePerKgText, "dinheiro em dólar: ponto")
        assertEquals("1,24", form.densityGPerCm3Text, "densidade em português: vírgula, mesmo com a moeda em dólar")
    }

    @Test
    fun printerHoursAndPowerFollowTheLanguage() {
        AppLocale.use(english)
        val printer = PrinterProfile("p", "Ender", 200.5, 0.17, MachineInvestment(1500.5, 12, 25, 7.5))

        assertEquals("12", PrinterFormState.new().printingHoursPerDayText)
        assertEquals("0,17", PrinterFormState.new().maintenanceCostPerHourText, "manutenção é dinheiro: vírgula do real")

        val opened = printer.toFormState()
        assertEquals("200.5", opened.printerPowerWattsText)
        assertEquals("7.5", opened.printingHoursPerDayText)
        assertEquals("1500,5", opened.machinePriceText)
    }

    private fun quoteViewModel(history: QuoteHistoryRepository = QuoteHistoryRepository()) = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), history,
        currency = { Currency.BRL },
    )

    @Test
    fun weightIsReadWithTheLanguageSeparator() {
        // "1.500" gramas: um e meio em inglês, mil e quinhentos em português.
        fun lengthFor(locale: AppLocaleState): Double {
            AppLocale.use(locale)
            val viewModel = quoteViewModel().apply {
                setLengthMeters("1")
                setPrintTimeMinutes("60")
                setWeightGrams("1.500")
            }
            val text = viewModel.uiState.value.input.prints.single().filaments.single().lengthText
            return parseDecimal(text, NumberKind.MEASURE, locale.decimalSeparator)!!
        }

        val inEnglish = lengthFor(english)
        val inPortuguese = lengthFor(AppLocaleState.BRAZILIAN_PORTUGUESE)
        assertEquals(1000.0, inPortuguese / inEnglish, 25.0, "1.500 g em português é mil vezes 1.5 g em inglês")
    }

    @Test
    fun aSavedQuoteReopensWithTheLengthInTheLanguageAndTheShippingInTheCurrency() {
        AppLocale.use(english)
        val history = QuoteHistoryRepository()
        quoteViewModel(history).apply {
            setLengthMeters("12.5")
            setPrintTimeMinutes("190")
            setShippingCost("1,2")
            setSaveName("Peça")
            saveCurrentQuote()
        }
        val saved = history.savedQuotes.value.single()
        assertEquals(1.2, saved.shippingCost, 1e-9)

        val editing = quoteViewModel().apply { loadForEditing(saved) }
        assertEquals("12.5", editing.uiState.value.input.prints.single().filaments.single().lengthText)
        assertEquals("1,2", editing.uiState.value.input.shippingCostText)
        assertFalse(editing.hasUnsavedEdits)
    }

    @Test
    fun settingsPercentFollowTheLanguageAndTheCleanDraftFollowsASwitch() {
        val repository = SettingsRepository()
        repository.update(repository.settings.value.copy(profitMargin = 0.125, laborRatePerHour = 12.5))
        val languages = MutableStateFlow(AppLocale.current.value)
        val viewModel = SettingsViewModel(repository, scope, currency = { Currency.BRL }, languageChanges = languages)
        assertEquals("12,5", viewModel.uiState.value.profitMarginPercentText)

        AppLocale.use(english)
        languages.value = english

        assertEquals("12.5", viewModel.uiState.value.profitMarginPercentText)
        assertEquals("12,5", viewModel.uiState.value.laborRatePerHourText, "hora de trabalho é dinheiro: segue a moeda")
        assertFalse(viewModel.hasUnsavedChanges)

        viewModel.update { it.copy(profitMarginPercentText = "20.5") }
        viewModel.save()
        assertEquals(0.205, repository.settings.value.profitMargin, 1e-9)
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
        salePrice = 16.19,
    )

    @Test
    fun anAutoNamedQuoteShowsTheNameBuiltInTheLanguageInUse() {
        val history = QuoteHistoryRepository()
        val created = LocalDateTime.of(2026, 9, 24, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val saved = history.save(name = "", quote = quote, services = emptyList(), photo = null, sourceLink = null)
            .copy(savedAtEpochMillis = created, name = "Orçamento - 24/09/2026 14:30")
        assertTrue(saved.autoNamed)

        assertEquals("Orçamento - 24/09/2026 14:30", saved.displayName("Orçamento - %1\$s"))

        AppLocale.use(english)
        assertEquals("Quote - 09/24/2026 14:30", saved.displayName("Quote - %1\$s"))

        val typed = saved.copy(autoNamed = false, name = "Chaveiro")
        assertEquals("Chaveiro", typed.displayName("Quote - %1\$s"))
    }
}
