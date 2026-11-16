package com.threedreport.app.data

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.pricing.PricingCalculator
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Nome automático pelo campo `autoNamed` (Leva 10B, parte 7), sem mudar o formato da pasta de dados. A 2.1 não grava
 * o campo: ao ler o `quotes.json`, o registro que vem sem a chave e cujo nome é o automático da 2.1
 * ("Orçamento - 24/09/2026 14:30") é marcado; o que já tem a chave fica como está. O nome continua o mesmo, nada vai
 * pra uma pasta à parte e a 2.1 continua abrindo os dados.
 */
class AutoNamedMigrationTest {

    private lateinit var parent: File
    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        parent = createTempDirectory("3dreport-autoname-test").toFile()
        dataDir = File(parent, ".3dreport")
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
        parent.deleteRecursively()
    }

    private fun dataFrom21(quotes: String = QUOTES_FROM_2_1) {
        dataDir.mkdirs()
        File(dataDir, "format.json").writeText("""{"version": 2}""")
        File(dataDir, "quotes.json").writeText(quotes)
    }

    @Test
    fun aDataFolderFromVersionTwoOneIsMarkedOnReadAndKeepsTheNames() {
        dataFrom21()

        assertEquals(DataDirResult.Ready(), prepareDataDir(), "sem aviso de migração e sem pasta à parte")

        val quotes = QuoteHistoryRepository().quotesIncludingTrash().associateBy { it.id }
        assertEquals("Orçamento - 24/09/2026 14:30", quotes.getValue("auto").name)
        assertTrue(quotes.getValue("auto").autoNamed)
        assertEquals("Suporte de fone", quotes.getValue("named").name)
        assertFalse(quotes.getValue("named").autoNamed)
        // Começa igual, mas não é o padrão do app: a 2.1 também não tratava como automático.
        assertFalse(quotes.getValue("prefix-only").autoNamed)
        assertTrue(quotes.getValue("trashed").autoNamed, "o da lixeira também, pra voltar certo")
        assertEquals(listOf(".3dreport"), parent.list()!!.toList(), "nenhuma cópia ~/.3dreport-v2")
    }

    @Test
    fun theFormatStaysTwoAfterOpeningAndSaving() {
        dataFrom21()
        prepareDataDir()

        val repository = QuoteHistoryRepository()
        repository.save(name = "", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertEquals(2, DATA_FORMAT_VERSION)
        assertEquals(2, readDataFormatVersion(File(dataDir, "format.json")))
        assertEquals(DataDirResult.Ready(), prepareDataDir())
        // Gravado pela 2.2, cada registro leva a chave (encodeDefaults): na próxima leitura, ninguém é remarcado.
        val written = File(dataDir, "quotes.json").readText()
        assertEquals(5, Regex("\"autoNamed\"").findAll(written).count(), "os quatro com dado e o novo")
    }

    @Test
    fun aRecordThatAlreadyHasTheKeyIsNotMarkedAgain() {
        // Quem renomeou à mão, já na 2.2, pra algo igual ao padrão antigo, continua com o nome dele.
        dataFrom21(QUOTES_FROM_2_1.replace("\"name\": \"Orçamento - 24", "\"autoNamed\": false, \"name\": \"Orçamento - 24"))

        assertFalse(QuoteHistoryRepository().quotesIncludingTrash().single { it.id == "auto" }.autoNamed)
    }

    @Test
    fun aTypedNameThatLooksAutomaticStaysTypedAfterReopening() {
        prepareDataDir()
        val typed = QuoteHistoryRepository().save(name = "Orçamento - 24/09/2026 14:30", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        assertFalse(typed.autoNamed)

        assertFalse(QuoteHistoryRepository().quotesIncludingTrash().single { it.id == typed.id }.autoNamed)
    }

    @Test
    fun theBackupCopyIsMarkedWhenTheMainFileDoesNotOpen() {
        // O arquivo que não abre é guardado como ilegível, como sempre; a cópia anterior entra no lugar, já marcada.
        dataFrom21("""[{"id": "pela metade""")
        File(dataDir, "quotes.json.bak").writeText(QUOTES_FROM_2_1)

        assertEquals(DataDirResult.Ready(), prepareDataDir())

        val recovered = QuoteHistoryRepository().quotesIncludingTrash().associateBy { it.id }
        assertTrue(recovered.getValue("auto").autoNamed)
        assertFalse(recovered.getValue("named").autoNamed)
    }

    @Test
    fun aBackupFromVersionTwoOneIsMarkedOnRestore() {
        prepareDataDir()
        val backup = zipOf(
            "3dreport-backup.json" to """{"app":"3DReport","appVersion":"2.1.0","createdAtEpochMillis":0,"dataFormatVersion":2}""",
            "quotes.json" to QUOTES_FROM_2_1,
        )

        assertIs<RestoreResult.Success>(BackupRepository().restoreFromZip(backup))

        val quotes = QuoteHistoryRepository().quotesIncludingTrash().associateBy { it.id }
        assertTrue(quotes.getValue("auto").autoNamed)
        assertFalse(quotes.getValue("named").autoNamed)
        assertEquals(2, readDataFormatVersion(File(dataDir, "format.json")))
    }

    private val quote = PricingCalculator.calculate(
        job = PrintJob(filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24), filamentLengthMeters = 12.0, printTimeMinutes = 190.0),
        printer = PrinterProfile(id = "p", name = "Impressora", printerPowerWatts = 380.0, maintenanceCostPerHour = 0.17, machineInvestment = MachineInvestment(2700.0, 12, 25, 16.0)),
        settings = PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, profitMargin = 1.0),
    )

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.encodeToByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private companion object {
        val QUOTE = """
            {"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0}],"printTimeMinutes":190.0},
              "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
             "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
             "salePrice":23.0,"channelFeeRate":0.2}
        """.trimIndent()

        /** `quotes.json` como a 2.1 grava: registros carimbados, sem `autoNamed`, um deles na lixeira. */
        val QUOTES_FROM_2_1 = """
            [
              {"id": "auto", "data": {"id": "auto", "name": "Orçamento - 24/09/2026 14:30", "quote": $QUOTE, "savedAtEpochMillis": 0, "number": 1},
               "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0},
              {"id": "named", "data": {"id": "named", "name": "Suporte de fone", "quote": $QUOTE, "savedAtEpochMillis": 0, "number": 2},
               "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0},
              {"id": "prefix-only", "data": {"id": "prefix-only", "name": "Orçamento - suporte de fone", "quote": $QUOTE, "savedAtEpochMillis": 0, "number": 3},
               "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0},
              {"id": "trashed", "data": {"id": "trashed", "name": "Orçamento - 01/10/2026 09:05", "quote": $QUOTE, "savedAtEpochMillis": 0, "number": 4},
               "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0, "deletedAtEpochMillis": ${Long.MAX_VALUE / 2}},
              {"id": "gone", "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0, "deletedAtEpochMillis": 0}
            ]
        """.trimIndent()
    }
}
