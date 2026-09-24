package com.threedreport.core.slicer

import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogMatcherTest {

    private fun printer(name: String) = PrinterProfile(
        id = name,
        name = name,
        printerPowerWatts = 300.0,
        maintenanceCostPerHour = 0.1,
        machineInvestment = MachineInvestment(2000.0, 12, 25, 8.0),
    )

    private fun filament(
        id: String,
        type: String?,
        brand: String? = null,
        colors: List<FilamentColor> = listOf(FilamentColor(id = "$id-default")),
    ) = Filament(id = id, name = id, pricePerKg = 100.0, densityGPerCm3 = 1.24, brand = brand, materialType = type, colors = colors)

    private val x1c = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_X1C)

    @Test
    fun printerWithTheSameNameAsThePresetIsChosen() {
        val match = CatalogMatcher.matchPrinter(x1c, listOf(printer("Bambu Lab X1-Carbon"), printer("Creality K1")))

        assertEquals(PrinterMatch.Found(printer("Bambu Lab X1-Carbon")), match)
    }

    @Test
    fun aSimilarNameIsOnlySuggestedNeverChosen() {
        // G-code de A1 mini com só uma A1 cadastrada: máquinas diferentes, consumo diferente.
        val a1mini = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_A1_MINI)

        val match = CatalogMatcher.matchPrinter(a1mini, listOf(printer("Bambu Lab A1")))

        assertEquals(PrinterMatch.Similar("Bambu Lab A1 mini", printer("Bambu Lab A1")), match)
    }

    @Test
    fun unregisteredAndUnknownPrinters() {
        assertEquals(PrinterMatch.NotRegistered("Bambu Lab X1 Carbon"), CatalogMatcher.matchPrinter(x1c, listOf(printer("Creality K1"))))
        assertEquals(PrinterMatch.Unknown, CatalogMatcher.matchPrinter(GCodeMetadataParser.parse(";TIME:60"), listOf(printer("Creality K1"))))
    }

    @Test
    fun curaPrinterMatchesByMachineName() {
        val cura = GCodeMetadataParser.parse(RealGCodeFixtures.CURA_ENDER3_V2)

        assertEquals(PrinterMatch.Found(printer("Creality Ender-3 V2")), CatalogMatcher.matchPrinter(cura, listOf(printer("Creality Ender-3 V2"))))
    }

    @Test
    fun theOnlyFilamentOfThatTypeIsChosenWithTheClosestColor() {
        val green = FilamentColor(id = "verde", name = "Verde", hex = "#00AA40")
        val black = FilamentColor(id = "preto", name = "Preto", hex = "#000000")
        val pla = filament("pla", "PLA", colors = listOf(black, green))

        val match = CatalogMatcher.matchFilament(x1c, listOf(pla, filament("petg", "PETG")), currentFilamentId = null)

        assertEquals(FilamentMatch.Found(pla, green), match)
    }

    @Test
    fun farAwayColorIsNotGuessed() {
        val pla = filament("pla", "PLA", colors = listOf(FilamentColor(id = "vermelho", hex = "#FF0000"), FilamentColor(id = "azul", hex = "#0000FF")))

        assertEquals(FilamentMatch.Found(pla, null), CatalogMatcher.matchFilament(x1c, listOf(pla), currentFilamentId = null))
    }

    @Test
    fun severalOfTheSameTypeKeepTheCurrentChoiceOrAreAmbiguous() {
        val a = filament("a", "PLA", brand = "eSUN")
        val b = filament("b", "PLA", brand = "Voolt")

        assertEquals(FilamentMatch.Ambiguous("PLA", 2), CatalogMatcher.matchFilament(x1c, listOf(a, b), currentFilamentId = null))
        assertEquals(FilamentMatch.Found(b, b.colors.single()), CatalogMatcher.matchFilament(x1c, listOf(a, b), currentFilamentId = "b"))
    }

    @Test
    fun aKnownVendorMustMatchTheBrand() {
        val gcode = GCodeMetadataParser.parse("; filament_type = PLA\n; filament_vendor = eSUN\n")
        val voolt = filament("voolt", "PLA", brand = "Voolt")
        val esun = filament("esun", "PLA", brand = "eSUN")

        assertEquals(FilamentMatch.NotRegistered("PLA", "eSUN"), CatalogMatcher.matchFilament(gcode, listOf(voolt), currentFilamentId = null))
        assertEquals(FilamentMatch.Found(esun, esun.colors.single()), CatalogMatcher.matchFilament(gcode, listOf(voolt, esun), currentFilamentId = null))
    }

    @Test
    fun outOfStockFilamentsAreNotCandidates() {
        val soldOut = filament("pla", "PLA", colors = listOf(FilamentColor(id = "x", inStock = false)))

        assertEquals(FilamentMatch.NotRegistered("PLA", null), CatalogMatcher.matchFilament(x1c, listOf(soldOut), currentFilamentId = null))
    }

    @Test
    fun differentMaterialsInDifferentExtrudersAreNotReducedToOne() {
        val gcode = GCodeMetadataParser.parse("; filament_type = PLA;PETG\n")

        assertEquals(FilamentMatch.Multimaterial(listOf("PLA", "PETG")), CatalogMatcher.matchFilament(gcode, listOf(filament("pla", "PLA")), currentFilamentId = null))
    }

    @Test
    fun curaSaysNothingAboutTheFilament() {
        val cura = GCodeMetadataParser.parse(RealGCodeFixtures.CURA_ENDER3_V2)

        assertEquals(FilamentMatch.Unknown, CatalogMatcher.matchFilament(cura, listOf(filament("pla", "PLA")), currentFilamentId = null))
    }
}
