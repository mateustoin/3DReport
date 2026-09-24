package com.threedreport.app.data

import com.threedreport.core.model.PricingSettings
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que as configurações gerais sobrevivem a uma nova instância do repositório (persistência em disco). */
class SettingsRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun updatedSettingsSurviveNewRepositoryInstance() {
        val original = SettingsRepository()
        val changed = original.settings.value.copy(profitMargin = 2.0)

        original.update(changed)

        val reloaded = SettingsRepository().settings.value
        assertEquals(2.0, reloaded.profitMargin)
    }

    @Test
    fun newInstallStartsInTheCurrentSchemaWithTheFinishingPercentage() {
        val settings = SettingsRepository().settings.value

        assertEquals(PricingSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertEquals(0.10, settings.finishingRate)
    }

    @Test
    fun settingsFileFromBeforeDecision93IsMigratedOnceAndSaved() {
        val file = File(System.getProperty("threedreport.dataDir"), "settings.json")
        file.writeText(
            """{"energyPricePerKwh":1.23,"failureRate":0.1,"finishingRate":0.1,"laborRatePerHour":30.0,"profitMargin":1.0}""",
        )

        val settings = SettingsRepository().settings.value

        assertEquals(0.0, settings.finishingRate)
        assertEquals(PricingSettings.CURRENT_SCHEMA_VERSION, settings.schemaVersion)
        assertTrue(file.readText().contains("schemaVersion"))
    }
}
