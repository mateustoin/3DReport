package com.threedreport.app.data

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentImportError
import com.threedreport.core.model.FilamentImportPlan
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Exportar e importar filamentos em JSON (decisão 164), do texto ao cadastro. */
class FilamentExchangeJsonTest {

    private lateinit var dataDir: File

    private val pla = Filament(
        id = "pla",
        name = "PLA",
        brand = "Voolt",
        materialType = "PLA",
        pricePerKg = 89.90,
        densityGPerCm3 = 1.24,
        colors = listOf(FilamentColor("red", "Vermelho", "#E53935", inStock = false)),
    )

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
        // Cadastro vazio, como outro app que nunca teve esses filamentos (sem os filamentos de exemplo da primeira abertura).
        File(dataDir, "filaments.json").writeText("[]")
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun theExportHasTheFormatAndNoIdsNorStock() {
        val text = FilamentExchangeJson.encode(listOf(pla, pla.copy(id = "old", name = "Arquivado", archived = true)))

        assertTrue(text.contains("\"format\": \"3dreport-filaments\""), text)
        assertTrue(text.contains("\"version\": 1"), text)
        assertFalse(text.contains("\"id\""), text)
        assertFalse(text.contains("inStock"), text)
        assertFalse(text.contains("Arquivado"), text)
    }

    @Test
    fun exportingAndImportingInAnotherAppAddsTheFilaments() {
        val text = FilamentExchangeJson.encode(listOf(pla))
        val repository = FilamentRepository()
        val before = repository.filaments.value.size

        val result = assertIs<FilamentImportPlan.Ready>(repository.importFilaments(text))

        assertEquals(1, result.toAdd.size)
        val reopened = FilamentRepository().filaments.value
        assertEquals(before + 1, reopened.size)
        val imported = reopened.single { it.name == "PLA" }
        assertTrue(imported.id != "pla")
        assertEquals(listOf(true), imported.colors.map { it.inStock })

        // Importar de novo pula o que já está no cadastro.
        val again = assertIs<FilamentImportPlan.Ready>(FilamentRepository().importFilaments(text))
        assertEquals(0, again.toAdd.size)
        assertEquals(1, again.alreadyPresent)
        assertEquals(before + 1, FilamentRepository().filaments.value.size)
    }

    @Test
    fun aFileThatIsNotOfThisFormatImportsNothing() {
        val repository = FilamentRepository()
        val before = repository.filaments.value

        listOf("não é json", "[]", """{"filaments":[]}""", """{"format":"spoolman","version":1,"filaments":[]}""", """{"format":"3dreport-filaments","version":1,"filaments":"x"}""")
            .forEach { assertEquals(FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT), repository.importFilaments(it), it) }

        assertEquals(before, FilamentRepository().filaments.value)
    }

    @Test
    fun aNewerVersionIsRejectedEvenWithFieldsThisVersionDoesNotKnow() {
        val newer = """{"format":"3dreport-filaments","version":2,"filaments":{"porMarca":{}}}"""

        assertEquals(FilamentImportPlan.Rejected(FilamentImportError.NEWER_VERSION), FilamentRepository().importFilaments(newer))
    }

    @Test
    fun invalidEntriesAreCountedApart() {
        val text = """
            {"format":"3dreport-filaments","version":1,"filaments":[
              {"name":"","densityGPerCm3":1.24,"pricePerKg":80.0},
              {"name":"PETG","densityGPerCm3":1.27,"pricePerKg":95.0,"campoNovo":true,"colors":[{"name":"Azul","hex":"#0000FF"}]}
            ]}
        """.trimIndent()

        val result = assertIs<FilamentImportPlan.Ready>(FilamentRepository().importFilaments(text))

        assertEquals(1, result.invalid)
        assertEquals(listOf("PETG"), result.toAdd.map { it.name })
        assertEquals("Azul", FilamentRepository().filaments.value.single { it.name == "PETG" }.colors.single().name)
    }

    @Test
    fun anEntryWithAWrongTypeCountsAsInvalidAndTheRestIsImported() {
        val text = """
            {"format":"3dreport-filaments","version":1,"filaments":[
              {"name":"PLA","densityGPerCm3":1.24,"pricePerKg":"89,90"},
              {"name":"ABS","densityGPerCm3":1.04,"pricePerKg":80.0,"colors":null},
              "não é um filamento",
              {"name":"PETG","densityGPerCm3":1.27,"pricePerKg":95.0}
            ]}
        """.trimIndent()

        val result = assertIs<FilamentImportPlan.Ready>(FilamentRepository().importFilaments(text))

        assertEquals(3, result.invalid)
        assertEquals(listOf("PETG"), result.toAdd.map { it.name })
        assertEquals(listOf("PETG"), FilamentRepository().filaments.value.map { it.name })
    }

    @Test
    fun aFileStartingWithAByteOrderMarkIsRead() {
        val text = Char(0xFEFF).toString() + FilamentExchangeJson.encode(listOf(pla))

        val result = assertIs<FilamentImportPlan.Ready>(FilamentRepository().importFilaments(text))

        assertEquals(listOf("PLA"), result.toAdd.map { it.name })
    }

    @Test
    fun theFileWrittenToDiskWithAByteOrderMarkIsRead() {
        // Como um editor de texto do Windows grava "UTF-8 com BOM": o BOM chega como primeiro caractere do texto.
        val file = File(dataDir, "com-bom.json")
        file.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + FilamentExchangeJson.encode(listOf(pla)).toByteArray(Charsets.UTF_8))

        val result = assertIs<FilamentImportPlan.Ready>(FilamentRepository().importFilaments(file.readText(Charsets.UTF_8)))

        assertEquals(1, result.toAdd.size)
    }
}
