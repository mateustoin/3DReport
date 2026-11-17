package com.threedreport.app.ui.quote

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.ui.history.ClientExport
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "Enviar pro cliente" com prazo vencido pede a mesma confirmação de Pedidos (decisão 131). */
class QuoteOverdueSendTest {
    private lateinit var platform: FakePlatform

    @BeforeTest
    fun setUp() {
        val folder = createTempDirectory("3dreport-test").toString()
        System.setProperty("threedreport.dataDir", folder)
        platform = FakePlatform()
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel(deadline: Long?) = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), SettingsRepository(), ServiceRepository(), SalesChannelRepository(), QuoteHistoryRepository(),
        platform = platform,
        brandingRepository = BrandingRepository(),
        today = { TODAY },
    ).apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
        setDeliveryDate(deadline)
    }

    @Test
    fun anOverdueDeadlineHoldsTheSendUntilConfirmed() {
        val viewModel = viewModel(TODAY - 3)
        assertTrue(viewModel.sendToClient())

        viewModel.sendCopy()

        assertEquals(ClientExport.COPY, viewModel.uiState.value.pendingSend)
        assertTrue(platform.copied.isEmpty(), "segurado: nada foi enviado ainda")

        viewModel.confirmPendingSend()

        assertNull(viewModel.uiState.value.pendingSend)
        assertEquals(1, platform.copied.size)
    }

    @Test
    fun cancellingTheWarningSendsNothing() {
        val viewModel = viewModel(TODAY - 3)
        viewModel.sendToClient()
        viewModel.sendCopy()

        viewModel.dismissPendingSend()
        viewModel.confirmPendingSend()

        assertNull(viewModel.uiState.value.pendingSend)
        assertTrue(platform.copied.isEmpty())
    }

    @Test
    fun anOnTimeDeadlineSendsStraightAway() {
        val viewModel = viewModel(TODAY)
        viewModel.sendToClient()

        viewModel.sendCopy()

        assertNull(viewModel.uiState.value.pendingSend)
        assertEquals(1, platform.copied.size)
    }

    @Test
    fun noDeadlineSendsStraightAway() {
        val viewModel = viewModel(null)
        viewModel.sendToClient()

        viewModel.sendCopy()

        assertNull(viewModel.uiState.value.pendingSend)
        assertEquals(1, platform.copied.size)
    }

    @Test
    fun theStateCarriesHowManyDaysTheDeadlineIsLate() {
        assertEquals(45, viewModel(TODAY - 45).uiState.value.deliveryDaysLate)
        assertEquals(0, viewModel(TODAY).uiState.value.deliveryDaysLate)
        assertEquals(0, viewModel(TODAY + 5).uiState.value.deliveryDaysLate)
        assertEquals(0, viewModel(null).uiState.value.deliveryDaysLate)
        assertEquals(TODAY, viewModel(null).uiState.value.today)
    }

    private companion object {
        const val TODAY = 20_000L
    }
}
