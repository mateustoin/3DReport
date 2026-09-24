package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.BrandingSettings
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun showWatermarkAndShowFooterFlagsSurviveNewRepositoryInstance() {
        val original = BrandingRepository()
        original.update(BrandingSettings(watermarkText = "Minha Marca", showWatermark = false, showFooter = true))

        val reloaded = BrandingRepository().branding.value
        assertEquals(false, reloaded.showWatermark)
        assertEquals(true, reloaded.showFooter)
    }

    private fun dataDir() = java.io.File(System.getProperty("threedreport.dataDir"))

    @Test
    fun replacingTheLogoStoresTheNewFileAndDeletesTheOldOne() {
        val repository = BrandingRepository()
        repository.update(BrandingSettings(), LogoChange.Replace(PickedFile("logo.png", byteArrayOf(1, 2))))
        val firstName = repository.branding.value.logoFileName!!
        Thread.sleep(2) // nomes com timestamp: garante que o segundo seja diferente

        repository.update(repository.branding.value, LogoChange.Replace(PickedFile("nova.jpg", byteArrayOf(3))))

        val reloaded = BrandingRepository()
        assertContentEquals(byteArrayOf(3), reloaded.logoBytes())
        assertTrue(reloaded.branding.value.logoFileName!!.endsWith(".jpg"))
        assertFalse(java.io.File(dataDir(), "branding/$firstName").exists())
    }

    @Test
    fun keepingTheLogoIgnoresWhatTheSettingsSayAboutIt() {
        val repository = BrandingRepository()
        repository.update(BrandingSettings(), LogoChange.Replace(PickedFile("logo.png", byteArrayOf(1))))
        val logoName = repository.branding.value.logoFileName

        // O formulário salva outros campos sem mexer na logo, mesmo mandando um logoFileName qualquer.
        repository.update(BrandingSettings(watermarkText = "Loja", logoFileName = "outra.png"))

        assertEquals(logoName, repository.branding.value.logoFileName)
        assertEquals("Loja", repository.branding.value.watermarkText)
    }

    @Test
    fun removingTheLogoDeletesTheFile() {
        val repository = BrandingRepository()
        repository.update(BrandingSettings(), LogoChange.Replace(PickedFile("logo.png", byteArrayOf(1))))
        val logoName = repository.branding.value.logoFileName!!

        repository.update(repository.branding.value, LogoChange.Remove)

        assertNull(BrandingRepository().branding.value.logoFileName)
        assertFalse(java.io.File(dataDir(), "branding/$logoName").exists())
    }

    @Test
    fun missingLogoFileMeansNoLogoInsteadOfAnError() {
        val repository = BrandingRepository()
        repository.update(BrandingSettings(), LogoChange.Replace(PickedFile("logo.png", byteArrayOf(1))))
        java.io.File(dataDir(), "branding/${repository.branding.value.logoFileName}").delete()

        assertNull(repository.logoBytes())
    }

    @Test
    fun contactAndBorderSurviveNewRepositoryInstance() {
        BrandingRepository().update(BrandingSettings(contactWhatsApp = "(11) 99999-0000", contactInstagram = "loja", showBorder = true))

        val reloaded = BrandingRepository().branding.value
        assertEquals("(11) 99999-0000", reloaded.contactWhatsApp)
        assertEquals("@loja", reloaded.instagramHandle)
        assertTrue(reloaded.showBorder)
    }

    @Test
    fun brandingSavedBeforeTheIdentityFieldsLoadsWithNeutralDefaults() {
        java.io.File(dataDir(), "branding.json").writeText("""{"watermarkText":"Loja antiga","showWatermark":false,"showFooter":true}""")

        val loaded = BrandingRepository().branding.value

        assertEquals("Loja antiga", loaded.watermarkText)
        assertNull(loaded.logoFileName)
        assertTrue(loaded.contactLines.isEmpty())
        assertFalse(loaded.showBorder)
    }
}
