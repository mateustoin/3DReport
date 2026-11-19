package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FilamentExchangeTest {

    private val pla = Filament(
        id = "pla",
        name = "PLA",
        brand = "Voolt",
        materialType = "PLA",
        pricePerKg = 89.90,
        densityGPerCm3 = 1.24,
        diameterMm = 1.75,
        colors = listOf(FilamentColor("red", "Vermelho", "#E53935", inStock = false), FilamentColor("black", "Preto", "#000000")),
    )
    private val archived = Filament(id = "old", name = "ABS velho", pricePerKg = 50.0, densityGPerCm3 = 1.04, archived = true)

    private var nextId = 0
    private val newId: () -> String = { "id-${nextId++}" }

    @Test
    fun exportKeepsOnlyLiveFilamentsWithoutIdsOrStock() {
        val file = FilamentExchange.fileOf(listOf(pla, archived))

        assertEquals(FilamentExchange.FORMAT, file.format)
        assertEquals(FilamentExchange.VERSION, file.version)
        val exported = file.filaments.single()
        assertEquals(
            ExchangedFilament(
                name = "PLA",
                brand = "Voolt",
                materialType = "PLA",
                densityGPerCm3 = 1.24,
                diameterMm = 1.75,
                pricePerKg = 89.90,
                colors = listOf(ExchangedColor("Vermelho", "#E53935"), ExchangedColor("Preto", "#000000")),
            ),
            exported,
        )
    }

    @Test
    fun importAddsNewFilamentsWithNewIdsAndColorsInStock() {
        val file = FilamentExchange.fileOf(listOf(pla))

        val result = assertIs<FilamentImportPlan.Ready>(FilamentExchange.plan(file, existing = emptyList(), newId = newId))

        val imported = result.toAdd.single()
        assertTrue(imported.id != "pla")
        assertEquals("PLA", imported.name)
        assertEquals(89.90, imported.pricePerKg)
        assertEquals(listOf(true, true), imported.colors.map { it.inStock })
        assertEquals(listOf("Vermelho", "Preto"), imported.colors.map { it.name })
        assertEquals(2, imported.colors.map { it.id }.toSet().size)
        assertEquals(0, result.alreadyPresent)
        assertEquals(0, result.invalid)
    }

    @Test
    fun aFilamentWithTheSameNameBrandAndTypeIsSkipped() {
        val file = FilamentExchangeFile(
            format = FilamentExchange.FORMAT,
            version = 1,
            filaments = listOf(
                ExchangedFilament(name = "  pla ", brand = "VOOLT", materialType = "pla", densityGPerCm3 = 1.24, pricePerKg = 80.0),
                ExchangedFilament(name = "PLA", brand = "Outra", materialType = "PLA", densityGPerCm3 = 1.24, pricePerKg = 80.0),
                // Repetido dentro do próprio arquivo.
                ExchangedFilament(name = "PLA", brand = "outra", materialType = "PLA", densityGPerCm3 = 1.24, pricePerKg = 80.0),
            ),
        )

        val result = assertIs<FilamentImportPlan.Ready>(FilamentExchange.plan(file, existing = listOf(pla, archived), newId = newId))

        assertEquals(listOf("Outra"), result.toAdd.map { it.brand })
        assertEquals(2, result.alreadyPresent)
    }

    @Test
    fun anArchivedFilamentAlsoCountsAsAlreadyThere() {
        val file = FilamentExchange.fileOf(listOf(archived.copy(archived = false)))

        val result = assertIs<FilamentImportPlan.Ready>(FilamentExchange.plan(file, existing = listOf(archived), newId = newId))

        assertTrue(result.toAdd.isEmpty())
        assertEquals(1, result.alreadyPresent)
    }

    @Test
    fun invalidFilamentsAreSkippedAndCounted() {
        val file = FilamentExchangeFile(
            format = FilamentExchange.FORMAT,
            version = 1,
            filaments = listOf(
                ExchangedFilament(name = "", densityGPerCm3 = 1.24, pricePerKg = 80.0),
                ExchangedFilament(name = "Sem densidade", densityGPerCm3 = 0.0, pricePerKg = 80.0),
                ExchangedFilament(name = "Sem preço", densityGPerCm3 = 1.24),
                ExchangedFilament(name = "Preço negativo", densityGPerCm3 = 1.24, pricePerKg = -1.0),
                ExchangedFilament(name = "Diâmetro zero", densityGPerCm3 = 1.24, pricePerKg = 1.0, diameterMm = 0.0),
                ExchangedFilament(name = "Bom", densityGPerCm3 = 1.24, pricePerKg = 80.0),
            ),
        )

        val result = assertIs<FilamentImportPlan.Ready>(FilamentExchange.plan(file, existing = emptyList(), newId = newId))

        assertEquals(listOf("Bom"), result.toAdd.map { it.name })
        assertEquals(5, result.invalid)
        // Sem diâmetro no arquivo: o padrão do cadastro. Sem cores: uma cor sem nome, como o cadastro cria.
        assertEquals(Filament.DEFAULT_DIAMETER_MM, result.toAdd.single().diameterMm)
        assertEquals(1, result.toAdd.single().colors.size)
    }

    @Test
    fun aFileOfAnotherFormatIsRejected() {
        assertEquals(
            FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT),
            FilamentExchange.plan(FilamentExchangeFile(format = "spoolman", version = 1), emptyList(), newId),
        )
        assertEquals(
            FilamentImportPlan.Rejected(FilamentImportError.NOT_THIS_FORMAT),
            FilamentExchange.plan(FilamentExchangeFile(format = null, version = null), emptyList(), newId),
        )
        assertEquals(FilamentImportError.NOT_THIS_FORMAT, FilamentExchange.headerError(FilamentExchange.FORMAT, version = 0))
    }

    @Test
    fun aNewerVersionIsRejected() {
        assertEquals(FilamentImportError.NEWER_VERSION, FilamentExchange.headerError(FilamentExchange.FORMAT, version = 2))
        assertEquals(null, FilamentExchange.headerError(FilamentExchange.FORMAT, version = 1))
    }

    @Test
    fun suggestedFileNameHasTheDate() {
        // 18/11/2026 = dia 20775 desde 01/01/1970.
        assertEquals("filamentos-3dreport-2026-11-18.json", FilamentExchange.suggestedFileName(20_775))
        assertEquals("filamentos-3dreport-1970-01-01.json", FilamentExchange.suggestedFileName(0))
        assertEquals("filamentos-3dreport-2024-02-29.json", FilamentExchange.suggestedFileName(19_782))
    }
}
