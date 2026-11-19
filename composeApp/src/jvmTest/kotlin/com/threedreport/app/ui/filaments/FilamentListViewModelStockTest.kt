package com.threedreport.app.ui.filaments

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.filaments_export_done
import _3dreport.composeapp.generated.resources.filaments_import_already
import _3dreport.composeapp.generated.resources.filaments_import_done
import _3dreport.composeapp.generated.resources.filaments_import_newer_version
import _3dreport.composeapp.generated.resources.filaments_import_none
import _3dreport.composeapp.generated.resources.filaments_import_not_this_format
import _3dreport.composeapp.generated.resources.filaments_import_result
import com.threedreport.app.data.StockApp
import com.threedreport.app.data.StockSamples
import com.threedreport.app.data.TestClock
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.StockItemRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Filamentos com o estoque da Leva 12: o saldo no selo da cor e "Exportar" e "Importar filamentos" (decisão 164). */
class FilamentListViewModelStockTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var dataDir: File
    private val red = StockItemRef.ColorRef("pla", "red")
    private val black = StockItemRef.ColorRef("pla", "black")

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
        // Cadastro vazio: sem os filamentos de exemplo da primeira abertura, só os do teste.
        File(dataDir, "filaments.json").writeText("[]")
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    private class Rig(val app: StockApp, val platform: FakePlatform, val viewModel: FilamentListViewModel, val notices: List<UiNotice>)

    private fun rig(): Rig {
        val app = StockApp(dataDir, TestClock())
        app.filaments.add(StockSamples.pla)
        val platform = FakePlatform()
        val viewModel = FilamentListViewModel(app.filaments, scope, stock = app.stock, platform = platform, today = { 20_000L })
        val notices = mutableListOf<UiNotice>()
        scope.launch { viewModel.fileNotices.collect { notices += it } }
        return Rig(app, platform, viewModel, notices)
    }

    private fun picked(text: String) = PickResult.Picked(PickedFile("filamentos.json", text.encodeToByteArray()))

    @Test
    fun aTrackedColorShowsItsBalanceAndFlagsLowAndEmpty() {
        val rig = rig()
        rig.app.stock.startTracking(red, 640.0)
        rig.app.stock.startTracking(black, 120.0)
        rig.app.stock.updateItemSettings(black, lowThreshold = 200.0, emptySpoolGrams = null)

        val stock = rig.viewModel.uiState.value.colorStock

        assertEquals(640.0, stock.getValue(red).amount, 1e-9)
        assertEquals(false, stock.getValue(red).needsAttention)
        assertEquals(120.0, stock.getValue(black).amount, 1e-9)
        assertEquals(true, stock.getValue(black).needsAttention, "abaixo do limite fica vermelho")

        rig.app.stock.recordManualUse(red, 700.0, com.threedreport.core.model.ManualUseReason.FAILURE, null)
        assertEquals(true, rig.viewModel.uiState.value.colorStock.getValue(red).needsAttention, "saldo negativo também")
    }

    @Test
    fun anUntrackedColorHasNoBalanceAndStopsTrackingWhenDeleted() {
        val rig = rig()
        assertTrue(rig.viewModel.uiState.value.colorStock.isEmpty())

        rig.app.stock.startTracking(red, 100.0)
        assertEquals(setOf(red), rig.viewModel.uiState.value.colorStock.keys)
        rig.app.stock.stopTracking(red)
        assertTrue(rig.viewModel.uiState.value.colorStock.isEmpty())
    }

    @Test
    fun exportingWritesTheFileWithTodaysDateAndOffersToOpenTheFolder() {
        val rig = rig()
        rig.platform.nextSaveLocation = "/pasta/"

        rig.viewModel.exportFilaments()

        val (path, bytes) = rig.platform.written.entries.single()
        assertEquals("/pasta/filamentos-3dreport-2024-10-04.json", path)
        val text = bytes.decodeToString()
        assertTrue("\"format\": \"3dreport-filaments\"" in text, text)
        assertTrue("PLA Voolt" in text)
        assertEquals(uiText(Res.string.filaments_export_done), rig.notices.single().message)
        assertNotNull(rig.notices.single().action)
    }

    @Test
    fun cancellingTheSaveDialogSaysNothing() {
        val rig = rig()
        rig.platform.nextSaveLocation = null

        rig.viewModel.exportFilaments()

        assertTrue(rig.platform.written.isEmpty())
        assertTrue(rig.notices.isEmpty())
    }

    @Test
    fun importingAddsTheNewOnesSkipsTheExistingAndSaysSo() {
        val rig = rig()
        rig.platform.nextSaveLocation = "/pasta/"
        rig.viewModel.exportFilaments()
        val file = rig.platform.written.values.single().decodeToString()
        // O arquivo traz o PLA que já existe e um PETG novo.
        val withPetg = file.replace("\"filaments\": [", "\"filaments\": [{\"name\":\"PETG Novo\",\"materialType\":\"PETG\",\"densityGPerCm3\":1.27,\"diameterMm\":1.75,\"pricePerKg\":110.0,\"colors\":[{\"name\":\"Branco\",\"hex\":\"#FFFFFF\"}]},")
        rig.platform.nextPick = picked(withPetg)

        rig.viewModel.importFilaments()

        val names = rig.app.filaments.filaments.value.map { it.name }
        assertTrue("PETG Novo" in names, names.toString())
        assertEquals(2, names.size)
        val expected = uiText(
            Res.string.filaments_import_result,
            UiText.Joined(
                listOf(
                    UiText.Plural(Res.plurals.filaments_import_done, 1, listOf(1)),
                    UiText.Plural(Res.plurals.filaments_import_already, 1, listOf(1)),
                ),
                separator = ", ",
            ),
        )
        assertEquals(expected, rig.notices.last().message)
        assertEquals(false, rig.notices.last().isError)
        assertTrue(rig.app.filaments.filaments.value.first { it.name == "PETG Novo" }.colors.all { it.inStock })
    }

    @Test
    fun importingTheSameFileTwiceAddsNothingTheSecondTime() {
        val rig = rig()
        rig.platform.nextSaveLocation = "/pasta/"
        rig.viewModel.exportFilaments()
        rig.platform.nextPick = picked(rig.platform.written.values.single().decodeToString())

        rig.viewModel.importFilaments()

        assertEquals(1, rig.app.filaments.filaments.value.size)
        val message = rig.notices.last().message as UiText.Res
        assertEquals(Res.string.filaments_import_result, message.resource)
        val parts = (message.args.single() as UiText.Joined).parts
        assertEquals(uiText(Res.string.filaments_import_none), parts.first())
    }

    @Test
    fun aFileThatIsNotOfThisFormatOrIsNewerImportsNothingAndGivesAnError() {
        val rig = rig()

        rig.platform.nextPick = picked("{\"qualquer\": 1}")
        rig.viewModel.importFilaments()
        rig.platform.nextPick = picked("{\"format\":\"3dreport-filaments\",\"version\":99,\"filaments\":[]}")
        rig.viewModel.importFilaments()

        assertEquals(listOf(uiText(Res.string.filaments_import_not_this_format), uiText(Res.string.filaments_import_newer_version)), rig.notices.map { it.message })
        assertTrue(rig.notices.all { it.isError })
        assertEquals(1, rig.app.filaments.filaments.value.size)
    }

    @Test
    fun cancellingTheOpenDialogSaysNothing() {
        val rig = rig()
        rig.platform.nextPick = PickResult.Cancelled

        rig.viewModel.importFilaments()

        assertTrue(rig.notices.isEmpty())
        assertNull(rig.platform.opened.firstOrNull())
    }
}
