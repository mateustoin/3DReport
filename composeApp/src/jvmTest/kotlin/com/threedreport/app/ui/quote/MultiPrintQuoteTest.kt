package com.threedreport.app.ui.quote

import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.FilamentUsage
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.pricing.PricingCalculator
import java.util.Base64
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pedido com várias impressões na tela de Orçamento (decisão 114, Fase 3 da Leva 9). */
class MultiPrintQuoteTest {

    // Criados só depois de apontar a pasta de dados pra uma temporária: como campos com valor, eles nasciam
    // antes do @BeforeTest e liam e gravavam na pasta de dados de verdade.
    private lateinit var filaments: FilamentRepository
    private lateinit var printers: PrinterRepository
    private lateinit var settings: SettingsRepository
    private lateinit var history: QuoteHistoryRepository

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        filaments = FilamentRepository()
        printers = PrinterRepository()
        settings = SettingsRepository()
        history = QuoteHistoryRepository()
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel() = QuoteViewModel(filaments, printers, settings, ServiceRepository(), SalesChannelRepository(), history)

    /** Duas impressões: 12 m em 190 min e, na segunda, 30 m em 5h, rodando 2 vezes. */
    private fun twoPrints(): QuoteViewModel = viewModel().apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
        val second = addPrint()
        setLengthMeters("30", second)
        setPrintTimeMinutes("5h", second)
        setRuns("2", second)
        setPrintName("Base", second)
    }

    private val QuoteViewModel.prints get() = input.value.prints

    @Test
    fun aNewPrintComesWithThePrinterAndFilamentOfTheLastOneAndEmptyNumbers() {
        val viewModel = viewModel()
        val printer = printers.printers.value.first()
        viewModel.selectPrinter(printer.id)
        viewModel.setLengthMeters("12")
        viewModel.setPrintTimeMinutes("190")

        val id = viewModel.addPrint()

        val added = viewModel.prints.single { it.id == id }
        assertEquals(printer.id, added.printerId)
        assertEquals(filaments.filaments.value.first { it.hasStockAvailable }.id, added.filaments.single().filamentId)
        assertEquals("", added.filaments.single().lengthText)
        assertEquals("", added.printTimeText)
    }

    @Test
    fun theOrderIsCalculatedPrintByPrintLikeTheCore() {
        val viewModel = twoPrints()
        val filament = filaments.filaments.value.first { it.hasStockAvailable }
        val printer = printers.printers.value.first()

        val expected = PricingCalculator.calculate(
            prints = listOf(
                PrintJob(filament = filament, filamentLengthMeters = 12.0, printTimeMinutes = 190.0) to printer,
                PrintJob(filaments = listOf(FilamentUsage(filament, 30.0, null)), printTimeMinutes = 300.0, runs = 2, name = "Base") to printer,
            ),
            settings = settings.settings.value,
        )

        val quote = assertNotNull(viewModel.currentResult().quote)
        assertEquals(expected.salePrice, quote.salePrice, 1e-9)
        assertEquals(expected.productionCost, quote.productionCost, 1e-9)
        assertEquals(2, quote.prints.size)
        assertEquals(2, quote.prints[1].job.runs)
    }

    @Test
    fun invalidRunsIsAFieldErrorAndNotOne() {
        val viewModel = twoPrints()
        val second = viewModel.prints[1].id

        viewModel.setRuns("dois", second)

        val result = viewModel.currentResult()
        assertNull(result.quote)
        assertNotNull(result.fieldErrors[QuoteFields.runs(second)])
    }

    @Test
    fun savingAndReopeningKeepsNameRunsAndSettingsOfEachPrint() {
        val viewModel = twoPrints()
        val second = viewModel.prints[1].id
        val settingsOfBase = PrintSettings(layerHeightMm = 0.28, infillPercentage = 10.0)
        viewModel.setPrintSettings(settingsOfBase, second)
        assertTrue(viewModel.saveCurrentQuote())
        val saved = history.savedQuotes.value.single()
        assertEquals(listOf(null, "Base"), saved.quote.prints.map { it.job.name })
        assertEquals(listOf(null, settingsOfBase), saved.quote.prints.map { it.job.settings })

        viewModel.loadForEditing(saved)

        val reopened = viewModel.prints
        assertEquals(2, reopened.size)
        assertEquals("Base", reopened[1].name)
        assertEquals("2", reopened[1].runsText)
        assertEquals(settingsOfBase, reopened[1].settings)
        assertEquals(PrintSettings(), reopened[0].settings)
    }

    @Test
    fun renamingAPrintOfAReopenedOrderKeepsThePriceButSavesTheName() {
        val viewModel = twoPrints()
        viewModel.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()
        viewModel.loadForEditing(saved)

        viewModel.setPrintName("Cabeça", viewModel.prints[0].id)
        viewModel.setPrintSettings(PrintSettings(layerHeightMm = 0.12), viewModel.prints[0].id)

        assertTrue(viewModel.currentResult().keepsOriginalPrice, "nome e configurações não mudam o preço")
        viewModel.saveCurrentQuote()
        val updated = history.savedQuotes.value.single()
        assertEquals("Cabeça", updated.quote.prints[0].job.name)
        assertEquals(0.12, updated.quote.prints[0].job.settings?.layerHeightMm)
        assertEquals(saved.quote.salePrice, updated.quote.salePrice)
    }

    @Test
    fun duplicateSellAndCopyToCatalogBringEveryPrint() {
        val viewModel = twoPrints()
        viewModel.saveCurrentQuote()
        val order = history.savedQuotes.value.single()

        viewModel.duplicateForNewQuote(order)
        assertEquals(2, viewModel.prints.size)
        assertEquals("Base", viewModel.prints[1].name)

        viewModel.copyToCatalog(order)
        assertEquals(2, viewModel.prints.size)
        viewModel.saveCurrentQuote()
        val product = history.savedQuotes.value.single { it.kind == QuoteKind.PRODUCT }
        assertEquals(2, product.quote.prints.size)

        viewModel.sellFromProduct(product)
        assertEquals(2, viewModel.prints.size)
        assertEquals("2", viewModel.prints[1].runsText)
    }

    @Test
    fun removingAPrintCanBeUndoneInThePlaceItWas() {
        val viewModel = twoPrints()
        viewModel.addPrint()
        val middle = viewModel.prints[1]

        viewModel.removePrint(middle.id)

        assertEquals(2, viewModel.prints.size)
        val notice = assertNotNull(viewModel.notice.value)
        assertEquals("Desfazer", notice.actionLabel)
        notice.action!!.invoke()
        assertEquals(middle, viewModel.prints[1])
        assertEquals(3, viewModel.prints.size)
    }

    @Test
    fun theLastPrintCannotBeRemoved() {
        val viewModel = viewModel()

        viewModel.removePrint(viewModel.prints.single().id)

        assertEquals(1, viewModel.prints.size)
        assertNull(viewModel.notice.value)
    }

    @Test
    fun duplicatingAPrintPutsTheCopyRightAfterIt() {
        val viewModel = twoPrints()
        val first = viewModel.prints[0]

        viewModel.duplicatePrint(first.id)

        assertEquals(3, viewModel.prints.size)
        assertEquals(first.filaments.single().lengthText, viewModel.prints[1].filaments.single().lengthText)
        assertTrue(viewModel.prints[1].id != first.id)
        assertEquals("Base", viewModel.prints[2].name)
    }

    private fun gcode(name: String, meters: Double, minutes: Int, thumbnail: ByteArray? = null): PickResult {
        val thumb = thumbnail?.let {
            val base64 = Base64.getEncoder().encodeToString(it)
            "; thumbnail begin 2x2 ${base64.length}\n; $base64\n; thumbnail end\n"
        }.orEmpty()
        val text = "; filament used [mm] = ${(meters * 1000).toInt()}\n; estimated printing time (normal mode) = ${minutes}m\n$thumb"
        return PickResult.Picked(PickedFile(name, text.encodeToByteArray()))
    }

    private fun tinyPng(): ByteArray {
        val image = java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test
    fun droppingSeveralGCodesMakesOnePrintPerFileStartingOnTheBlankOne() {
        val viewModel = viewModel()

        viewModel.importDroppedFiles(listOf(gcode("cabeca.gcode", 5.0, 60), gcode("corpo.gcode", 20.0, 300), gcode("base.gcode", 8.0, 90)))

        assertEquals(listOf("5", "20", "8"), viewModel.prints.map { it.filaments.single().lengthText })
        assertEquals(listOf("1h", "5h", "1h30"), viewModel.prints.map { it.printTimeText })
    }

    @Test
    fun oneGCodeOnAFilledSinglePrintStillReplacesIt() {
        val viewModel = viewModel()
        viewModel.setLengthMeters("12")

        viewModel.importDroppedFiles(listOf(gcode("peca.gcode", 5.0, 60)))

        assertEquals(1, viewModel.prints.size)
        assertEquals("5", viewModel.prints.single().filaments.single().lengthText)
    }

    @Test
    fun droppingOnAPrintFillsThatOneAndAddingAsNewCanBeUndone() {
        val viewModel = twoPrints()
        val second = viewModel.prints[1].id

        viewModel.importDroppedInto(second, listOf(gcode("base.gcode", 8.0, 90)))
        assertEquals("8", viewModel.prints[1].filaments.single().lengthText)
        assertEquals("12", viewModel.prints[0].filaments.single().lengthText)

        viewModel.importDroppedAsNewPrint(gcode("extra.gcode", 3.0, 30))
        assertEquals(3, viewModel.prints.size)
        viewModel.undoGCodeImport(viewModel.prints[2].id)
        assertEquals(2, viewModel.prints.size, "desfazer a importação que abriu a impressão tira a impressão")
    }

    @Test
    fun aFileThatIsNotAGCodeDoesNotOpenAPrint() {
        val viewModel = twoPrints()

        viewModel.importDroppedAsNewPrint(PickResult.Picked(PickedFile("video.mp4", ByteArray(0))))

        assertEquals(2, viewModel.prints.size)
        assertNotNull(viewModel.prints.last().gcodeImportMessage)
    }

    @Test
    fun eachPrintKeepsItsOwnThumbnailAndTheOrderPhotoComesFromTheFirst() {
        val viewModel = viewModel()
        val first = tinyPng()
        val second = tinyPng().also { it[it.size - 1] = 1 }

        viewModel.importDroppedFiles(listOf(gcode("a.gcode", 5.0, 60, first), gcode("b.gcode", 6.0, 60, second)))

        assertContentEquals(first, viewModel.prints[0].thumbnail?.bytes)
        assertContentEquals(second, viewModel.prints[1].thumbnail?.bytes)
        assertContentEquals(first, viewModel.saveForm.value.photo?.bytes, "a miniatura da mesa 2 não troca a foto do pedido")

        viewModel.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()
        assertTrue(saved.quote.prints.all { it.job.thumbnailFileName != null })
        viewModel.loadForEditing(saved)
        assertContentEquals(second, viewModel.prints[1].thumbnail?.bytes)
    }
}
