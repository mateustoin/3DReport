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
        assertEquals(listOf(GCodeFilament(type = "PLA", vendor = null, colorHex = "#00AE42")), metadata.filaments)
        assertEquals(0.32549, metadata.filamentLengthMeters!!, 1e-9)
        // Bambu Studio 2.x grava "model printing time: ...; total estimated time: ..." numa linha só:
        // antes desta correção, o tempo ficava null.
        assertEquals(11.0 + 36.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun genericVendorIsNotABrand() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.BAMBU_STUDIO_A1_MINI)

        assertEquals("Bambu Lab A1 mini", metadata.printerModel)
        assertEquals(GCodeFilament(type = "TPU", vendor = null, colorHex = "#00AE42"), metadata.filaments.single())
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
        assertEquals(GCodeFilament(type = "ABS", vendor = null, colorHex = "#A768DF"), metadata.filaments.single())
        assertEquals(8.0 + 56.0 / 60, metadata.printTimeMinutes!!, 1e-9)
    }

    @Test
    fun curaHasThePrinterInsideSetting3ButNoFilamentType() {
        val metadata = GCodeMetadataParser.parse(RealGCodeFixtures.CURA_ENDER3_V2)

        assertEquals("Creality Ender-3 V2", metadata.printerModel)
        assertNull(metadata.printerSettingsName)
        assertTrue(metadata.filaments.isEmpty())
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
}
