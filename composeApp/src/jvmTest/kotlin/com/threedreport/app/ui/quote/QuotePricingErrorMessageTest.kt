package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_error_deductions
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.SalesChannel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** O erro com tipo do `core` vira a mesma frase de antes na tela de Orçamento (decisão 130). */
class QuotePricingErrorMessageTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun deductionsReachingTheWholeSaleShowTheSameSentenceAsBefore() {
        val viewModel = QuoteViewModel(
            FilamentRepository(),
            PrinterRepository(),
            SettingsRepository(),
            ServiceRepository(),
            SalesChannelRepository(),
            QuoteHistoryRepository(),
        ).apply {
            setLengthMeters("12")
            setPrintTimeMinutes("190")
            selectSalesChannel("caro")
        }
        val channel = SalesChannel(id = "caro", name = "Canal caro", feeRate = 0.6)

        val result = viewModel.calculate(
            viewModel.uiState.value.catalog.filaments,
            viewModel.uiState.value.catalog.printers,
            viewModel.uiState.value.catalog.settings.copy(taxRate = 0.4),
            viewModel.uiState.value.catalog.services,
            viewModel.uiState.value.input,
            channels = listOf(channel),
        )

        assertNull(result.quote)
        assertEquals(uiText(Res.string.quote_error_deductions), result.errorMessage)
    }
}
