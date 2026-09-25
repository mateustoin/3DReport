package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.LogoChange
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.platform.PickedFile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Valida a regra: com nome preenchido, ao menos marca d'água ou rodapé precisa estar marcado. */
class BrandingViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun newViewModel(
        brandingRepository: BrandingRepository = BrandingRepository(),
        templateRepository: TemplateRepository = TemplateRepository(),
    ) = BrandingViewModel(brandingRepository, templateRepository)

    @Test
    fun savingWithNameAndBothOptionsUncheckedFails() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.save()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(repository.branding.value.brandName) // nada foi persistido
    }

    @Test
    fun savingWithNameAndAtLeastOneOptionChecked() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(true)
        viewModel.save()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("Minha Marca", repository.branding.value.brandName)
        assertEquals(false, repository.branding.value.showWatermark)
        assertEquals(true, repository.branding.value.showFooter)
    }

    @Test
    fun savingWithBlankNameAndBothOptionsUncheckedIsFine() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.save()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(repository.branding.value.brandName)
    }

    @Test
    fun confirmSaveAsTemplateSnapshotsCurrentFormEvenIfUnsaved() {
        val templateRepository = TemplateRepository()
        val viewModel = newViewModel(templateRepository = templateRepository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(true)
        viewModel.setShowFooter(false)
        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("Formal")
        viewModel.confirmSaveAsTemplate()

        val saved = templateRepository.templates.value.first { it.name == "Formal" }
        assertEquals("Minha Marca", saved.brandName)
        assertTrue(saved.showWatermark)
        assertEquals(false, saved.showFooter)
        assertTrue(viewModel.uiState.value.templateSavedConfirmation)
        assertEquals(false, viewModel.uiState.value.isSavingAsTemplate)
    }

    @Test
    fun confirmSaveAsTemplateWithBlankNameFailsWithError() {
        val viewModel = newViewModel()

        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("   ")
        viewModel.confirmSaveAsTemplate()

        assertNotNull(viewModel.uiState.value.templateSaveError)
    }

    @Test
    fun confirmSaveAsTemplateWithNameAndBothOptionsUncheckedFails() {
        val templateRepository = TemplateRepository()
        val viewModel = newViewModel(templateRepository = templateRepository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("Formal")
        viewModel.confirmSaveAsTemplate()

        assertNotNull(viewModel.uiState.value.templateSaveError)
        assertTrue(templateRepository.templates.value.isEmpty())
    }

    /** PNG de 2x2 de verdade, pra passar na checagem de "a imagem abre". */
    private fun tinyPng(): ByteArray {
        val image = java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test
    fun pickedLogoIsOnlyStoredOnSave() {
        val repository = BrandingRepository()
        val png = tinyPng()
        val viewModel = BrandingViewModel(repository, TemplateRepository(), pickImage = { PickedFile("logo.png", png) })

        viewModel.pickLogo()
        assertNull(repository.branding.value.logoFileName, "escolher não grava: o formulário é rascunho até Salvar")

        viewModel.save()
        assertNotNull(repository.logoBytes())
        assertEquals(LogoChange.Keep, viewModel.uiState.value.logoChange, "salvar de novo não regrava a mesma logo")
    }

    @Test
    fun unreadableLogoShowsAnErrorAndKeepsTheFormAsItWas() {
        val viewModel = BrandingViewModel(BrandingRepository(), TemplateRepository(), pickImage = { PickedFile("logo.png", byteArrayOf(1, 2, 3)) })

        viewModel.pickLogo()

        assertNotNull(viewModel.uiState.value.logoError)
        assertNull(viewModel.uiState.value.logoBytes)
    }

    @Test
    fun removingTheLogoTakesEffectOnSave() {
        val repository = BrandingRepository()
        val png = tinyPng()
        val viewModel = BrandingViewModel(repository, TemplateRepository(), pickImage = { PickedFile("logo.png", png) })
        viewModel.pickLogo()
        viewModel.save()

        viewModel.removeLogo()
        viewModel.save()

        assertNull(repository.branding.value.logoFileName)
    }

    @Test
    fun nameWithoutWatermarkOrFooterIsFineWhenTheHeaderShowsIt() {
        // Com contato, o PDF ganha cabeçalho, e é lá que o nome aparece.
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.setContactInstagram("minhamarca")
        viewModel.save()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("@minhamarca", repository.branding.value.instagramHandle)
    }

    @Test
    fun templateKeepsPresentationButNotIdentity() {
        val templateRepository = TemplateRepository()
        val viewModel = newViewModel(templateRepository = templateRepository)

        viewModel.update("Minha Marca")
        viewModel.setShowBorder(true)
        viewModel.setContactEmail("loja@example.com")
        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("Com borda")
        viewModel.confirmSaveAsTemplate()

        val saved = templateRepository.templates.value.first { it.name == "Com borda" }
        assertTrue(saved.showBorder)
        assertEquals("Minha Marca", saved.brandName)
    }

    @Test
    fun previewRendersThePdfWithWhatIsInTheFormEvenUnsaved() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)
        viewModel.update("Minha Marca")
        viewModel.setShowBorder(true)

        viewModel.showPreview()

        val png = assertNotNull(viewModel.uiState.value.previewPng)
        assertNotNull(javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png)))
        assertNull(repository.branding.value.brandName, "a prévia não salva nada")

        viewModel.closePreview()
        assertNull(viewModel.uiState.value.previewPng)
    }
}
