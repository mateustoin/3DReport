package com.threedreport.app.data

import com.threedreport.core.model.BrandingSettings
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Valida que a marca d'água configurada sobrevive a uma nova instância do repositório (persistência em disco). */
class BrandingRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsWithoutWatermark() {
        assertNull(BrandingRepository().branding.value.watermarkText)
    }

    @Test
    fun updatedWatermarkSurvivesNewRepositoryInstance() {
        val original = BrandingRepository()
        original.update(BrandingSettings(watermarkText = "Minha Marca"))

        val reloaded = BrandingRepository().branding.value
        assertEquals("Minha Marca", reloaded.watermarkText)
    }
}
