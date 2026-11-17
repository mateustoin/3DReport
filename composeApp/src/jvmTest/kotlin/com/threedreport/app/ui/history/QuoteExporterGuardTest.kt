package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_busy_pdf
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A guarda contra duplo clique vale desde o clique, não só depois do diálogo de "Salvar como". */
class QuoteExporterGuardTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private val notices = mutableListOf<UiNotice>()
    private var dialogs = 0
    private var whileDialogIsOpen: () -> Unit = {}

    /** O "Salvar como" abre e, com ele aberto, a pessoa clica de novo (o diálogo do sistema deixa os cliques entrarem). */
    private val platform: PlatformServices = FakePlatform().apply { nextSaveLocation = "/tmp/" }.let { fake ->
        object : PlatformServices by fake {
            override fun chooseSaveLocation(suggestedFileName: String, initialDirectory: String?): String? {
                dialogs++
                whileDialogIsOpen()
                return fake.chooseSaveLocation(suggestedFileName, initialDirectory)
            }
        }
    }

    private fun exporter() = QuoteExporter(
        platform,
        BrandingRepository(),
        QuoteHistoryRepository(),
        CoroutineScope(Dispatchers.Unconfined),
        Dispatchers.Unconfined,
        Dispatchers.Unconfined,
    ) { notices += it }

    @Test
    fun twoClicksInARowOpenOnlyOneDialog() {
        val exporter = exporter()
        whileDialogIsOpen = {
            whileDialogIsOpen = {}
            exporter.saveGenerated({ "a.pdf" }, uiText(busyForTest), uiText(busyForTest)) { ByteArray(1) }
        }

        exporter.saveGenerated({ "a.pdf" }, uiText(busyForTest), uiText(busyForTest)) { ByteArray(1) }

        assertEquals(1, dialogs)
    }

    @Test
    fun aFailureBuildingTheSuggestedNameBecomesANoticeAndReleasesTheGuard() {
        val exporter = exporter()

        exporter.saveGenerated({ error("sem nome") }, uiText(busyForTest), uiText(busyForTest)) { ByteArray(1) }

        assertTrue(notices.single().isError)
        exporter.saveGenerated({ "b.pdf" }, uiText(busyForTest), uiText(busyForTest)) { ByteArray(1) }
        assertEquals(1, dialogs, "o clique seguinte à falha volta a funcionar")
    }
}

private val busyForTest = Res.string.export_busy_pdf
