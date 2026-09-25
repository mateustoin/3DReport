package com.threedreport.app.data

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun newInstallStartsWithTheFinishingPercentage() {
        val settings = SettingsRepository().settings.value

        assertEquals(0.10, settings.finishingRate)
    }
}
