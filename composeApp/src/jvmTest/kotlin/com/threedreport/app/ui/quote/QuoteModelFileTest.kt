package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.FakePlatform
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickFailure
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.platform.ThreeMfThumbnail
import com.threedreport.app.platform.isModelFile
import com.threedreport.app.platform.isModelFileName
import com.threedreport.core.stl.StlAnalyzer
import com.threedreport.core.stl.StlMesh
import com.threedreport.core.stl.StlPreviewResult
import com.threedreport.core.stl.StlProgress
import com.threedreport.core.stl.StlTriangle
import com.threedreport.core.stl.Vec3
import kotlinx.coroutines.CancellationException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O arquivo do modelo (STL ou 3MF) no Orçamento (decisões 141 e 142): a miniatura do 3MF vira foto pela regra da
 * miniatura do G-code, a prévia do STL informa o progresso e para quando o arquivo é trocado, e orçamento salvo
 * reabre sem mexer na foto.
 */
class QuoteModelFileTest {

    private lateinit var history: QuoteHistoryRepository
    private lateinit var settings: SettingsRepository
    private val platform = FakePlatform()

    @BeforeTest
    fun setUp() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        history = QuoteHistoryRepository()
        settings = SettingsRepository()
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel(
        today: Long = 100L,
        threeMf: (ByteArray) -> ThreeMfThumbnail = { ThreeMfThumbnail.Missing },
        stl: (ByteArray, StlProgress) -> StlPreviewResult = { _, _ -> StlPreviewResult.TooLarge(1) },
    ) = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), settings, ServiceRepository(), SalesChannelRepository(), history,
        platform = platform,
        today = { today },
        stlPreviewer = stl,
        threeMfReader = threeMf,
    ).apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
    }

    private fun file(name: String, vararg bytes: Byte = byteArrayOf(1)) = PickedFile(name, bytes)

    private fun thumbnail(vararg bytes: Byte) = ThreeMfThumbnail.Found(bytes, "Metadata/plate_1.png")

    private fun tetrahedron(): StlMesh {
        val a = Vec3(0f, 0f, 0f)
        val b = Vec3(10f, 0f, 0f)
        val c = Vec3(0f, 10f, 0f)
        val d = Vec3(0f, 0f, 10f)
        return StlMesh(listOf(StlTriangle(Vec3.ZERO, a, c, b), StlTriangle(Vec3.ZERO, a, b, d), StlTriangle(Vec3.ZERO, b, c, d), StlTriangle(Vec3.ZERO, c, a, d)))
    }

    @Test
    fun theFilePickerAndTheDropAcceptStlAnd3mfButNotTheSlicedPackage() {
        assertEquals(setOf("stl", "3mf"), FileKind.MODEL.extensions)
        assertTrue(isModelFileName("peça.STL"))
        assertTrue(isModelFileName("projeto.3mf"))
        assertFalse(isModelFileName("fatiado.gcode.3mf"))
        assertFalse(isModelFileName("peca.gcode"))
        assertTrue(PickResult.Picked(file("a.3mf")).isModelFile())
        assertTrue(PickResult.Failed(PickFailure.TOO_LARGE, "enorme.stl").isModelFile())
        assertFalse(PickResult.Cancelled.isModelFile())
    }

    @Test
    fun the3mfThumbnailBecomesThePhotoWhenThereIsNone() {
        val vm = viewModel(threeMf = { thumbnail(7, 7, 7) })

        vm.attachModelFile(file("projeto.3mf"))

        val state = vm.uiState.value
        assertEquals("projeto.3mf", state.form.modelFile?.fileName)
        val preview = assertIs<ModelPreview.ThreeMf>(state.modelPreview)
        assertContentEquals(byteArrayOf(7, 7, 7), preview.thumbnail.bytes)
        assertEquals(preview.thumbnail, state.form.photo)
        assertTrue(state.form.photoAutomatic)
    }

    @Test
    fun aPhotoChosenByHandIsKeptAndTheButtonSwapsItOnPurpose() {
        val vm = viewModel(threeMf = { thumbnail(7) })
        vm.setPhotoFromStlSnapshot(byteArrayOf(9, 9))

        vm.attachModelFile(file("projeto.3mf"))

        assertContentEquals(byteArrayOf(9, 9), vm.uiState.value.form.photo?.bytes)
        assertIs<ModelPreview.ThreeMf>(vm.uiState.value.modelPreview)

        vm.useModelThumbnailAsPhoto()

        assertContentEquals(byteArrayOf(7), vm.uiState.value.form.photo?.bytes)
        assertFalse(vm.uiState.value.form.photoAutomatic, "escolhida à mão: o próximo 3MF não troca")
    }

    @Test
    fun anAutomaticPhotoIsReplacedByTheNext3mfThumbnail() {
        var next = byteArrayOf(1)
        val vm = viewModel(threeMf = { ThreeMfThumbnail.Found(next, "Metadata/plate_1.png") })
        vm.attachModelFile(file("a.3mf"))
        next = byteArrayOf(2)

        vm.attachModelFile(file("b.3mf"))

        assertEquals("b.3mf", vm.uiState.value.form.modelFile?.fileName)
        assertContentEquals(byteArrayOf(2), vm.uiState.value.form.photo?.bytes)
    }

    @Test
    fun a3mfWithoutThumbnailOrUnreadableIsStoredAnyway() {
        val vm = viewModel(threeMf = { ThreeMfThumbnail.Missing })
        vm.attachModelFile(file("a.3mf"))
        assertEquals(ModelPreview.ThreeMfNoThumbnail, vm.uiState.value.modelPreview)
        assertNull(vm.uiState.value.form.photo)
        assertNotNull(vm.uiState.value.form.modelFile)

        val broken = viewModel(threeMf = { ThreeMfThumbnail.Unreadable })
        broken.attachModelFile(file("b.3mf"))
        assertEquals(ModelPreview.ThreeMfUnreadable, broken.uiState.value.modelPreview)
        assertNotNull(broken.uiState.value.form.modelFile)
        assertTrue(broken.saveCurrentQuote(), "nunca bloqueia salvar")
    }

    @Test
    fun pickingAFileThroughThePickerAttachesIt() {
        val vm = viewModel(threeMf = { thumbnail(5) })
        platform.nextPick = PickResult.Picked(file("projeto.3mf"))

        vm.pickModelFile()

        assertEquals("projeto.3mf", vm.uiState.value.form.modelFile?.fileName)
    }

    @Test
    fun aFileThatCouldNotBeReadSaysSoAndAttachesNothing() {
        val vm = viewModel()
        platform.nextPick = PickResult.Failed(PickFailure.UNREADABLE, "x.stl")

        vm.pickModelFile()

        assertNull(vm.uiState.value.form.modelFile)
    }

    @Test
    fun theStlPreviewReportsProgressAndEndsReady() {
        lateinit var vm: QuoteViewModel
        val seen = mutableListOf<ModelPreview?>()
        vm = viewModel(
            stl = { _, onProgress ->
                seen += vm.uiState.value.modelPreview
                onProgress(0.5f)
                seen += vm.uiState.value.modelPreview
                val mesh = tetrahedron()
                StlPreviewResult.Ready(mesh, StlAnalyzer.analyze(mesh), isSimplified = true)
            },
        )

        vm.attachModelFile(file("peca.stl"))

        assertEquals(listOf<ModelPreview?>(ModelPreview.Loading(0f), ModelPreview.Loading(0.5f)), seen)
        val ready = assertIs<ModelPreview.Stl>(vm.uiState.value.modelPreview)
        assertTrue(ready.isSimplified)
    }

    @Test
    fun removingTheFileInTheMiddleCancelsThePreview() {
        lateinit var vm: QuoteViewModel
        var cancelled = false
        vm = viewModel(
            stl = { _, onProgress ->
                onProgress(0.3f)
                vm.clearModelFile()
                try {
                    onProgress(0.6f)
                } catch (e: CancellationException) {
                    cancelled = true
                    throw e
                }
                error("não devia chegar aqui")
            },
        )

        vm.attachModelFile(file("peca.stl"))

        assertTrue(cancelled)
        assertNull(vm.uiState.value.modelPreview)
        assertNull(vm.uiState.value.form.modelFile)
    }

    @Test
    fun replacingTheFileInTheMiddleCancelsTheFirstAndShowsTheSecond() {
        lateinit var vm: QuoteViewModel
        var calls = 0
        var firstCancelled = false
        vm = viewModel(
            stl = { bytes, onProgress ->
                calls++
                if (bytes[0] == 1.toByte()) {
                    vm.attachModelFile(PickedFile("segunda.stl", byteArrayOf(2)))
                    try {
                        onProgress(0.5f)
                    } catch (e: CancellationException) {
                        firstCancelled = true
                        throw e
                    }
                }
                StlPreviewResult.TooLarge(bytes.size.toLong())
            },
        )

        vm.attachModelFile(PickedFile("primeira.stl", byteArrayOf(1)))

        assertTrue(firstCancelled)
        assertEquals(2, calls)
        assertEquals("segunda.stl", vm.uiState.value.form.modelFile?.fileName)
        assertIs<ModelPreview.TooLarge>(vm.uiState.value.modelPreview)
    }

    @Test
    fun aTooLargeStlIsStoredWithoutPreview() {
        val vm = viewModel(stl = { _, _ -> StlPreviewResult.TooLarge(400L * 1024 * 1024) })

        vm.attachModelFile(file("enorme.stl"))

        assertEquals(400L, assertIs<ModelPreview.TooLarge>(vm.uiState.value.modelPreview).megabytes)
        assertNotNull(vm.uiState.value.form.modelFile)
    }

    @Test
    fun anStlThatFailsToParseIsUnreadableButStored() {
        val vm = viewModel(stl = { _, _ -> throw IllegalArgumentException("truncado") })

        vm.attachModelFile(file("quebrado.stl"))

        assertEquals(ModelPreview.Unreadable, vm.uiState.value.modelPreview)
        assertNotNull(vm.uiState.value.form.modelFile)
    }

    @Test
    fun theRealPreviewerReadsASmallStlUnsimplified() {
        val ascii = """
            solid t
            facet normal 0 0 0
            outer loop
            vertex 0 0 0
            vertex 0 10 0
            vertex 10 0 0
            endloop
            endfacet
            endsolid t
        """.trimIndent().encodeToByteArray()
        val vm = QuoteViewModel(
            FilamentRepository(), PrinterRepository(), settings, ServiceRepository(), SalesChannelRepository(), history,
            platform = platform,
        )

        vm.attachModelFile(PickedFile("peca.stl", ascii))

        val ready = assertIs<ModelPreview.Stl>(vm.uiState.value.modelPreview)
        assertFalse(ready.isSimplified)
        assertEquals(1, ready.mesh.triangles.size)
    }

    @Test
    fun theModelFileIsSavedWithItsKindAndReopensWithoutTouchingThePhoto() {
        var thumbnailBytes = byteArrayOf(7)
        val vm = viewModel(threeMf = { ThreeMfThumbnail.Found(thumbnailBytes, "Metadata/plate_1.png") })
        vm.attachModelFile(file("projeto.3mf", 1, 2, 3))
        vm.setPhotoFromStlSnapshot(byteArrayOf(9))
        assertTrue(vm.saveCurrentQuote())
        val saved = history.savedQuotes.value.single()
        assertEquals(com.threedreport.core.model.ModelFileKind.THREE_MF, saved.modelFileKind)

        thumbnailBytes = byteArrayOf(8)
        val editing = viewModel(threeMf = { ThreeMfThumbnail.Found(thumbnailBytes, "Metadata/plate_1.png") })
        editing.loadForEditing(saved)

        // Abrir um orçamento salvo mostra a miniatura, mas a foto dele continua a que foi gravada.
        assertIs<ModelPreview.ThreeMf>(editing.uiState.value.modelPreview)
        assertContentEquals(byteArrayOf(9), editing.uiState.value.form.photo?.bytes)
        assertContentEquals(byteArrayOf(1, 2, 3), editing.uiState.value.form.modelFile?.bytes)
    }

    @Test
    fun theRushSuggestionComesFromTheViewModel() {
        settings.update(settings.settings.value.copy(rushSurchargeRate = 0.3))
        val vm = viewModel(today = 100L)
        assertFalse(vm.uiState.value.suggestRush, "sem prazo")

        vm.setDeliveryDate(101L)
        assertTrue(vm.uiState.value.suggestRush)
        vm.setDeliveryDate(100L)
        assertTrue(vm.uiState.value.suggestRush, "hoje conta")
        vm.setDeliveryDate(102L)
        assertTrue(vm.uiState.value.suggestRush)
        vm.setDeliveryDate(103L)
        assertFalse(vm.uiState.value.suggestRush, "mais de 2 dias")
        vm.setDeliveryDate(99L)
        assertFalse(vm.uiState.value.suggestRush, "prazo vencido")

        vm.setDeliveryDate(101L)
        vm.setRush(true)
        assertFalse(vm.uiState.value.suggestRush, "já marcada")
    }

    @Test
    fun noRushSuggestionWithoutTheSurchargeOrInAProduct() {
        val noRate = viewModel(today = 100L)
        noRate.setDeliveryDate(101L)
        assertFalse(noRate.uiState.value.suggestRush)

        assertFalse(shouldSuggestRush(101L, 100L, rush = false, rushSurchargeRate = 0.3, isProduct = true))
        assertTrue(shouldSuggestRush(101L, 100L, rush = false, rushSurchargeRate = 0.3, isProduct = false))
    }
}
