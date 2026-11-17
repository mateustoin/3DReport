package com.threedreport.app.ui.printers

import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** As regras puras da lista de impressoras: integridade dos dados, busca e aplicação de um preset numa impressora. */
class PrinterPresetsTest {
    private val example = PrinterProfile(
        id = "p1",
        name = "Exemplo",
        printerPowerWatts = 200.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(2000.0, 12, 25, 16.0),
    )

    @Test
    fun everyEntryHasAnAverageAndAMaxAboveIt() {
        PRINTER_PRESETS.forEach { preset ->
            assertTrue(preset.averagePowerWatts > 0, "${preset.displayName}: média > 0")
            preset.maxPowerWatts?.let { assertTrue(it > preset.averagePowerWatts, "${preset.displayName}: máxima maior que a média") }
            preset.referencePrice?.let { assertTrue(it > 0, "${preset.displayName}: preço > 0") }
        }
    }

    @Test
    fun noNameIsRepeated() {
        assertEquals(PRINTER_PRESETS.size, PRINTER_PRESETS.map { it.displayName }.toSet().size)
    }

    @Test
    fun maintenanceBandsHaveTheApprovedValues() {
        assertEquals(0.20, MaintenanceBand.OPEN_ENTRY.costPerHour)
        assertEquals(0.30, MaintenanceBand.ENCLOSED.costPerHour)
        assertEquals(0.45, MaintenanceBand.MULTI.costPerHour)
    }

    @Test
    fun searchFindsByBrandAndByModelIgnoringCase() {
        assertTrue(printerPresetsMatching("bambu").all { it.brand == "Bambu Lab" })
        assertEquals(listOf("Ender 3 V2"), printerPresetsMatching("ender 3 v2").map { it.model })
        assertEquals(PRINTER_PRESETS.size, printerPresetsMatching("  ").size)
        assertTrue(printerPresetsMatching("xyz").isEmpty())
    }

    @Test
    fun formFromAPresetHasAverageBandPriceAndDefaultUsage() {
        val preset = PRINTER_PRESETS.first { it.model == "K1C" }

        val form = PrinterFormState.fromPreset(preset)

        assertEquals("144", form.printerPowerWattsText)
        assertEquals("0,3", form.maintenanceCostPerHourText)
        assertEquals("3999", form.machinePriceText)
        assertEquals("12", form.paybackMonthsText)
        assertEquals("25", form.printingDaysPerMonthText)
        assertEquals("12", form.printingHoursPerDayText)
    }

    @Test
    fun withPresetAppliesAverageMaintenanceAndReferencePriceButKeepsUsage() {
        val result = example.withPreset(PRINTER_PRESETS.first { it.model == "A1" }, machinePrice = null)

        assertEquals("Bambu Lab A1", result.name)
        assertEquals(95.0, result.printerPowerWatts)
        assertEquals(0.20, result.maintenanceCostPerHour)
        assertEquals(2998.0, result.machineInvestment.machinePrice)
        assertEquals(16.0, result.machineInvestment.printingHoursPerDay)
    }

    @Test
    fun withPresetPrefersTheTypedPriceAndKeepsTheOldOneWhenThereIsNoSource() {
        val a1 = PRINTER_PRESETS.first { it.model == "A1" }
        val p1s = PRINTER_PRESETS.first { it.model == "P1S" }

        assertEquals(2500.0, example.withPreset(a1, machinePrice = 2500.0).machineInvestment.machinePrice)
        assertEquals(2000.0, example.withPreset(p1s, machinePrice = null).machineInvestment.machinePrice)
        assertEquals(1800.0, example.withPreset(null, machinePrice = 1800.0).machineInvestment.machinePrice)
        assertEquals(example.printerPowerWatts, example.withPreset(null, machinePrice = 1800.0).printerPowerWatts)
    }
}
