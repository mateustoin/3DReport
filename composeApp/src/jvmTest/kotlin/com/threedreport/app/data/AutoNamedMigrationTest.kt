package com.threedreport.app.data

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
 * Nome automático pelo campo `autoNamed` (Leva 10B, parte 7). A pasta de dados e o backup da 2.1 (formato 2) não
 * têm o campo: ao abrir, o pedido cujo nome é o automático da 2.1 ("Orçamento - 24/09/2026 14:30") é marcado uma vez,
 * e o nome continua o mesmo.
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

    @Test
    fun aDataFolderFromVersionTwoOneIsMarkedOnceAndKeepsTheNames() {
        dataDir.mkdirs()
        File(dataDir, "format.json").writeText("""{"version": 2}""")
        File(dataDir, "quotes.json").writeText(QUOTES_FROM_2_1)

        val result = assertIs<DataDirResult.Ready>(prepareDataDir())

        assertEquals(2, result.migrated?.fromVersion)
        assertEquals(QUOTES_FROM_2_1, File(result.migrated!!.originalCopy, "quotes.json").readText(), "a cópia de antes fica intacta")
        val quotes = QuoteHistoryRepository().quotesIncludingTrash().associateBy { it.id }
        assertEquals("Orçamento - 24/09/2026 14:30", quotes.getValue("auto").name)
        assertTrue(quotes.getValue("auto").autoNamed)
        assertEquals("Suporte de fone", quotes.getValue("named").name)
        assertFalse(quotes.getValue("named").autoNamed)
        // Começa igual, mas não é o padrão do app: a 2.1 também não tratava como automático.
        assertFalse(quotes.getValue("prefix-only").autoNamed)
        assertTrue(quotes.getValue("trashed").autoNamed, "o da lixeira também, pra voltar certo")
    }

    @Test
    fun aFolderAlreadyInTheNewFormatIsNotMarkedAgain() {
        // Quem renomeou à mão pra algo igual ao padrão antigo, já na 2.2, continua com o nome dele.
        prepareDataDir()
        File(dataDir, "quotes.json").writeText(QUOTES_FROM_2_1.replace("\"name\": \"Orçamento - 24", "\"autoNamed\": false, \"name\": \"Orçamento - 24"))

        assertEquals(DataDirResult.Ready(), prepareDataDir())

        assertFalse(QuoteHistoryRepository().quotesIncludingTrash().single { it.id == "auto" }.autoNamed)
    }

    @Test
    fun aDamagedQuotesFileDoesNotStopTheMigration() {
        // O arquivo que não abre é tratado na leitura (guardado como ilegível); a pasta não vai pra outro lugar por causa dele.
        dataDir.mkdirs()
        File(dataDir, "format.json").writeText("""{"version": 2}""")
        File(dataDir, "quotes.json").writeText("""[{"id": "pela metade"""")
        File(dataDir, "quotes.json.bak").writeText(QUOTES_FROM_2_1)

        val result = assertIs<DataDirResult.Ready>(prepareDataDir())

        assertEquals(null, result.moved)
        assertEquals(2, result.migrated?.fromVersion)
        val recovered = QuoteHistoryRepository().quotesIncludingTrash().single { it.id == "auto" }
        assertTrue(recovered.autoNamed, "a cópia anterior também foi convertida")
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
        assertEquals(DATA_FORMAT_VERSION, readDataFormatVersion(File(dataDir, "format.json")))
    }

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
