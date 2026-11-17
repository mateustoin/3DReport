package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.ModelFileKind
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.pricing.PricingCalculator
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Arquivo do modelo (decisão 141): o campo virou `modelFileName` no código, mas no disco continua `stlFileName`.
 * Orçamento gravado pela 2.2 com STL abre igual, e o 3MF é guardado com a extensão dele, sem campo novo nem mudança
 * de formato.
 */
class Leva10CFormatCompatibilityTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = PricingCalculator.calculate(
        job = PrintJob(
            filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = 190.0,
        ),
        printer = PrinterProfile(
            id = "printer",
            name = "Impressora",
            printerPowerWatts = 380.0,
            maintenanceCostPerHour = 0.17,
            machineInvestment = MachineInvestment(2700.0, 12, 25, 16.0),
        ),
        settings = PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, profitMargin = 1.0),
    )

    private val stlKey = "a".repeat(64) + ".stl"

    @Test
    fun anOrderOfVersion22WithStlOpensWithTheModelFile() {
        val json = """
            {"id":"1","name":"Peça","savedAtEpochMillis":0,"stlFileName":"$stlKey","autoNamed":false,
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":23.0}}
        """.trimIndent()
        val saved = dataJson.decodeFromString(SavedQuote.serializer(), json)

        assertEquals(stlKey, saved.modelFileName)
        assertEquals(ModelFileKind.STL, saved.modelFileKind)
        assertEquals(23.0, saved.quote.salePrice)
    }

    @Test
    fun theKeyWrittenToDiskIsStillStlFileName() {
        val saved = SavedQuote(id = "1", name = "Peça", quote = quote, savedAtEpochMillis = 0, modelFileName = stlKey)
        val text = dataJson.encodeToString(SavedQuote.serializer(), saved)

        assertTrue(text.contains("\"stlFileName\": \"$stlKey\""), text)
        assertFalse(text.contains("modelFileName"), text)
        assertEquals(saved, dataJson.decodeFromString(SavedQuote.serializer(), text))
    }

    @Test
    fun aThreeMfIsStoredWithItsExtensionAndComesBackAfterReopening() {
        val bytes = byteArrayOf(0x50, 0x4B, 3, 4, 9, 9, 9)
        val saved = QuoteHistoryRepository().save(
            name = "Com 3MF",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "Projeto Bambu.3MF", bytes = bytes),
            sourceLink = null,
        )

        assertTrue(saved.modelFileName!!.endsWith(".3mf"), saved.modelFileName)
        assertEquals(ModelFileKind.THREE_MF, saved.modelFileKind)

        val reopened = QuoteHistoryRepository()
        val reloaded = reopened.savedQuotes.value.first { it.id == saved.id }
        assertEquals(saved.modelFileName, reloaded.modelFileName)
        assertContentEquals(bytes, reopened.modelBytes(reloaded))
        assertTrue(reloaded.modelFileName!! in reopened.referencedAttachments())
        assertTrue(File(dataDir, "attachments/${saved.modelFileName}").isFile)
        val onDisk = File(dataDir, "quotes.json").readText()
        assertTrue(onDisk.contains("\"stlFileName\": \"${saved.modelFileName}\""), onDisk)
        assertFalse(onDisk.contains("modelFileName"))
    }

    @Test
    fun anStlStillComesBackAsBefore() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val saved = QuoteHistoryRepository().save(
            name = "Com STL",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "peça.stl", bytes = bytes),
            sourceLink = null,
        )
        val reopened = QuoteHistoryRepository()
        val reloaded = reopened.savedQuotes.value.first { it.id == saved.id }

        assertTrue(reloaded.modelFileName!!.endsWith(".stl"))
        assertEquals(ModelFileKind.STL, reloaded.modelFileKind)
        assertContentEquals(bytes, reopened.modelBytes(reloaded))
    }

    @Test
    fun replacingTheModelFileWithA3mfDropsTheStl() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Troca",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "peça.stl", bytes = byteArrayOf(1)),
            sourceLink = null,
        )
        val updated = repository.update(
            id = saved.id,
            name = "Troca",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "peça.3mf", bytes = byteArrayOf(2)),
            sourceLink = null,
            client = null,
        )!!

        assertEquals(ModelFileKind.THREE_MF, updated.modelFileKind)
        assertFalse(saved.modelFileName in repository.referencedAttachments())
    }
}
