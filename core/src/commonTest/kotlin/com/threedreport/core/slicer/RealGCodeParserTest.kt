package com.threedreport.core.slicer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O parser contra trechos de G-codes reais de cada fatiador (ver [RealGCodeFixtures]). */
class RealGCodeParserTest {

    @Test
    fun bambuStudioPrinterFilamentAndTime() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_X1C)

        assertEquals("Bambu Lab X1 Carbon", metadata.printerModel)
        assertEquals("Bambu Lab X1 Carbon", metadata.printerSettingsName)
        assertEquals(listOf(GCodeFilament(type = "PLA", vendor = null, colorHex = "#00AE42", lengthMeters = 0.32549)), metadata.filaments)
        assertEquals(0.32549, metadata.filamentLengthMeters!!, 1e-9)
        // Bambu Studio 2.x grava "model printing time: ...; total estimated time: ..." numa linha só:
        // antes desta correção, o tempo ficava null.
        assertEquals(11.0 + 36.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun genericVendorIsNotABrand() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_A1_MINI)

        assertEquals("Bambu Lab A1 mini", metadata.printerModel)
        assertEquals(GCodeFilament(type = "TPU", vendor = null, colorHex = "#00AE42", lengthMeters = 0.07574), metadata.filaments.single())
        assertEquals(9.0 + 3.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun orcaSlicer() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.ORCA_SLICER_VORON)

        assertEquals("Voron 2.4 350", metadata.printerModel)
        assertEquals("Voron 2.4 350", metadata.printerSettingsName)
        assertEquals("ASA", metadata.filaments.single().type)
        assertEquals(8.0, metadata.printTimeMinutes!!, 1e-9)
        assertEquals(2.93298, metadata.filamentLengthMeters!!, 1e-9)
    }

    @Test
    fun prusaSlicerProfileWrappedInAProjectNameIsCleanedUp() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.PRUSA_SLICER_VORON)

        assertEquals("Voron_v2_300_aferburner", metadata.printerModel)
        assertEquals("Voron_v2_300_afterburner", metadata.printerSettingsName)
        assertEquals(GCodeFilament(type = "ABS", vendor = null, colorHex = "#A768DF", lengthMeters = 0.21544), metadata.filaments.single())
        assertEquals(8.0 + 56.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun curaHasThePrinterInsideSetting3ButNoFilamentType() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.CURA_ENDER3_V2)

        assertEquals("Creality Ender-3 V2", metadata.printerModel)
        assertNull(metadata.printerSettingsName)
        // Só o consumo: o Cura não grava tipo nem marca.
        assertNull(metadata.filaments.single().type)
        assertEquals(metadata.filamentLengthMeters, metadata.filaments.single().lengthMeters)
        assertEquals(7.0, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun curaWithoutMachineNameFallsBackToTheDefinition() {
        val metadata = GCodeMetadataParser.parse(
            ";SETTING_3 {\"global_quality\": \"[general]\\nversion = 4\\nname = Draft\\\n" +
                ";SETTING_3 ndefinition = creality_ender3\\n\"}",
        )

        assertEquals("creality ender3", metadata.printerModel)
    }

    @Test
    fun severalExtrudersAreKeptSeparately() {
        val metadata = GCodeMetadataParser.parse(
            "; filament_type = PLA;PETG\n; filament_vendor = eSUN;Generic\n; filament_colour = #FFFFFF;#000000\n",
        )

        assertEquals(
            listOf(
                GCodeFilament(type = "PLA", vendor = "eSUN", colorHex = "#FFFFFF"),
                GCodeFilament(type = "PETG", vendor = null, colorHex = "#000000"),
            ),
            metadata.filaments,
        )
    }

    @Test
    fun bambuStudioTwoColorsGiveTheLengthOfEachExtruder() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_A1_TWO_COLORS)

        assertEquals(
            listOf(
                GCodeFilament(type = "PLA", vendor = "Bambu Lab", colorHex = "#00AE42", lengthMeters = 9.03547),
                GCodeFilament(type = "PLA", vendor = "Bambu Lab", colorHex = "#FFFF00", lengthMeters = 11.46779),
            ),
            metadata.filaments,
        )
        assertEquals(9.03547 + 11.46779, metadata.filamentLengthMeters!!, 1e-9)
        assertEquals(4 * 60 + 12 + 53.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun orcaSlicerIdexKeepsTheExtrudersInOrderAndTheSumMatchesTheTotal() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.ORCA_SLICER_IDEX_PURGE_TOWER)

        assertEquals(1.38473, metadata.filaments[0].lengthMeters!!, 1e-9)
        assertEquals(1.80562, metadata.filaments[1].lengthMeters!!, 1e-9)
        assertEquals(listOf("#26A69A", "#FFFF00"), metadata.filaments.map { it.colorHex })
        assertEquals(metadata.filamentLengthMeters!!, metadata.filaments.sumOf { it.lengthMeters!! }, 1e-9)
    }

    @Test
    fun aDeclaredSlotThatWasNotUsedIsLeftOut() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.ORCA_SLICER_UNUSED_SLOT)

        assertEquals(listOf(GCodeFilament(type = "PLA", vendor = null, colorHex = "#ffffff", lengthMeters = 1.48569)), metadata.filaments)
    }

    @Test
    fun lengthsThatDoNotLineUpWithTheTypesAreNotGuessed() {
        val metadata = GCodeMetadataParser.parse("; filament used [mm] = 1000, 500, 250\n; filament_type = PLA;PETG\n")

        assertEquals(listOf(null, null), metadata.filaments.map { it.lengthMeters })
        assertEquals(1.75, metadata.filamentLengthMeters!!, 1e-9)
    }
}
