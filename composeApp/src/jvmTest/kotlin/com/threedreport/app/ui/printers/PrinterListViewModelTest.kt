package com.threedreport.app.ui.printers

import com.threedreport.app.data.PrinterRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startAddFromPresetPrefillsNameAndPower() {
        val viewModel = PrinterListViewModel(PrinterRepository())
        val preset = PRINTER_PRESETS.first()

        viewModel.startAddFromPreset(preset)

        val form = viewModel.form.value!!
        assertEquals("${preset.brand} ${preset.model}", form.name)
        assertEquals(preset.ratedPowerWatts.toString(), form.printerPowerWattsText)
    }
}
