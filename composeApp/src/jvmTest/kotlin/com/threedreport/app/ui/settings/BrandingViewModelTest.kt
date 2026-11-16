package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.settings_docs_error_options
import _3dreport.composeapp.generated.resources.settings_docs_error_validity
import _3dreport.composeapp.generated.resources.settings_docs_logo_unreadable
import _3dreport.composeapp.generated.resources.settings_docs_notice_template_saved
import _3dreport.composeapp.generated.resources.settings_notice_saved
import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.LogoChange
import com.threedreport.app.data.TemplateRepository
import com.threedreport.app.platform.PickedFile
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Valida a regra: com nome preenchido, ao menos marca d'água ou rodapé precisa estar marcado. */
class BrandingViewModelTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        scope.cancel()
        System.clearProperty("threedreport.dataDir")
    }

    private fun newViewModel(
        brandingRepository: BrandingRepository = BrandingRepository(),
        templateRepository: TemplateRepository = TemplateRepository(),
    ) = BrandingViewModel(brandingRepository, templateRepository, scope)

    /** O aviso emitido depois da última ação, ou `null` se não houve nenhum. */
    private fun BrandingViewModel.pendingNotice(): UiNotice? = runBlocking { withTimeoutOrNull(200) { notices.first() } }

    @Test
    fun savingWithNameAndBothOptionsUncheckedFails() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.save()

        assertEquals(uiText(Res.string.settings_docs_error_options), viewModel.uiState.value.optionsError)
        assertNull(repository.branding.value.brandName) // nada foi persistido
        assertNull(viewModel.pendingNotice())
    }

    @Test
    fun savingWithNameAndAtLeastOneOptionChecked() {
        val repository = BrandingRepository()
        val viewModel = newViewModel(repository)

        viewModel.update("Minha Marca")
        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(true)
        viewModel.save()

        assertNull(viewModel.uiState.value.optionsError)
        assertEquals(UiNotice(uiText(Res.string.settings_notice_saved)), viewModel.pendingNotice())
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

        assertNull(viewModel.uiState.value.optionsError)
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
        assertEquals(UiNotice(uiText(Res.string.settings_docs_notice_template_saved)), viewModel.pendingNotice())
        assertNull(viewModel.uiState.value.savingTemplate)
    }

    @Test
    fun confirmSaveAsTemplateWithBlankNameFailsWithError() {
        val viewModel = newViewModel()

        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("   ")
        viewModel.confirmSaveAsTemplate()

        assertEquals(uiText(Res.string.common_error_name_required), viewModel.uiState.value.savingTemplate?.error)
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

        // O erro é das opções do formulário, não do nome do modelo: o diálogo fecha e o erro aparece no campo certo.
        assertNull(viewModel.uiState.value.savingTemplate)
        assertEquals(uiText(Res.string.settings_docs_error_options), viewModel.uiState.value.optionsError)
        assertTrue(templateRepository.templates.value.isEmpty())
    }

    @Test
    fun confirmSaveAsTemplateWithAnInvalidValidityClosesTheDialogAndShowsTheErrorOnTheValidityField() {
        val templateRepository = TemplateRepository()
        val viewModel = newViewModel(templateRepository = templateRepository)

        viewModel.setValidityDays("dez")
        viewModel.startSaveAsTemplate()
        viewModel.updateTemplateName("Formal")
        viewModel.confirmSaveAsTemplate()

        assertNull(viewModel.uiState.value.savingTemplate)
        assertEquals(uiText(Res.string.settings_docs_error_validity, BrandingSettings.MAX_VALIDITY_DAYS), viewModel.uiState.value.validityError)
        assertNull(viewModel.uiState.value.optionsError)
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
        val viewModel = BrandingViewModel(repository, TemplateRepository(), scope, pickImage = { PickedFile("logo.png", png) })

        viewModel.pickLogo()
        assertNull(repository.branding.value.logoFileName, "escolher não grava: o formulário é rascunho até Salvar")

        viewModel.save()
        assertNotNull(repository.logoBytes())
        assertEquals(LogoChange.Keep, viewModel.uiState.value.logoChange, "salvar de novo não regrava a mesma logo")
    }

    @Test
    fun unreadableLogoShowsAnErrorAndKeepsTheFormAsItWas() {
        val viewModel = BrandingViewModel(BrandingRepository(), TemplateRepository(), scope, pickImage = { PickedFile("logo.png", byteArrayOf(1, 2, 3)) })

        viewModel.pickLogo()

        assertEquals(uiText(Res.string.settings_docs_logo_unreadable), viewModel.uiState.value.logoError)
        assertNull(viewModel.uiState.value.logoBytes)
    }

    @Test
    fun discardDropsTheEditsAndThePickedLogo() {
        val repository = BrandingRepository()
        val viewModel = BrandingViewModel(repository, TemplateRepository(), scope, pickImage = { PickedFile("logo.png", tinyPng()) })
        viewModel.update("Loja Nova")
        viewModel.pickLogo()
        assertTrue(viewModel.hasUnsavedChanges)

        viewModel.discard()

        assertFalse(viewModel.hasUnsavedChanges)
        assertEquals("", viewModel.uiState.value.brandNameInput)
        assertNull(viewModel.uiState.value.logoBytes)
        assertNull(repository.branding.value.logoFileName)
    }

    @Test
    fun removingTheLogoTakesEffectOnSave() {
        val repository = BrandingRepository()
        val png = tinyPng()
        val viewModel = BrandingViewModel(repository, TemplateRepository(), scope, pickImage = { PickedFile("logo.png", png) })
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

        assertNull(viewModel.uiState.value.optionsError)
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

        viewModel.showPreview("Exemplo")

        val png = assertNotNull(viewModel.uiState.value.previewPng)
        assertNotNull(javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png)))
        assertNull(repository.branding.value.brandName, "a prévia não salva nada")

        viewModel.closePreview()
        assertNull(viewModel.uiState.value.previewPng)
    }

    private val loja = com.threedreport.core.model.QuoteTemplate(
        id = "t", name = "Loja", brandName = "Minha Loja", showWatermark = false, showFooter = true, showPrintTime = true, showBorder = true,
    )

    @Test
    fun loadingATemplateOnlyFillsTheDraftAndDoesNotSave() {
        val brandingRepository = BrandingRepository()
        val viewModel = newViewModel(brandingRepository = brandingRepository)
        assertFalse(viewModel.hasUnsavedChanges)

        viewModel.applyTemplate(loja)

        assertEquals("Minha Loja", viewModel.uiState.value.brandNameInput)
        assertTrue(viewModel.uiState.value.showBorder)
        assertTrue(viewModel.hasUnsavedChanges, "a barra de alterações não salvas aparece")
        assertNull(brandingRepository.branding.value.brandName, "carregar não grava")
        assertFalse(brandingRepository.branding.value.showBorder)
        assertNull(viewModel.pendingNotice(), "e não avisa de salvo")
    }

    @Test
    fun savingAfterLoadingATemplateAdoptsIt() {
        val brandingRepository = BrandingRepository()
        val viewModel = newViewModel(brandingRepository = brandingRepository)

        viewModel.applyTemplate(loja)
        viewModel.save()

        assertEquals("Minha Loja", brandingRepository.branding.value.brandName)
        assertTrue(brandingRepository.branding.value.showPrintTime)
        assertFalse(viewModel.hasUnsavedChanges)
    }

    @Test
    fun discardingAfterLoadingATemplateBringsBackWhatWasSaved() {
        val brandingRepository = BrandingRepository()
        val viewModel = newViewModel(brandingRepository = brandingRepository)

        viewModel.applyTemplate(loja)
        viewModel.discard()

        assertEquals("", viewModel.uiState.value.brandNameInput)
        assertFalse(viewModel.uiState.value.showBorder)
        assertFalse(viewModel.hasUnsavedChanges)
        assertNull(brandingRepository.branding.value.brandName)
    }

    @Test
    fun loadingATemplateKeepsTheSellerLogoAndContact() {
        val brandingRepository = BrandingRepository()
        brandingRepository.update(
            BrandingSettings(contactInstagram = "loja"),
            LogoChange.Replace(PickedFile("logo.png", byteArrayOf(1))),
        )
        val viewModel = newViewModel(brandingRepository = brandingRepository)

        viewModel.applyTemplate(loja)
        viewModel.save()

        val branding = brandingRepository.branding.value
        assertTrue(branding.showBorder)
        assertEquals("@loja", branding.instagramHandle)
        assertNotNull(branding.logoFileName)
    }

    @Test
    fun anInvalidValidityIsAnErrorThatNamesTheField() {
        val brandingRepository = BrandingRepository()
        val viewModel = newViewModel(brandingRepository = brandingRepository)

        viewModel.setValidityDays("dez")
        viewModel.save()

        assertEquals(
            uiText(Res.string.settings_docs_error_validity, BrandingSettings.MAX_VALIDITY_DAYS),
            viewModel.uiState.value.validityError,
        )
        assertEquals(BrandingSettings.DEFAULT_VALIDITY_DAYS, brandingRepository.branding.value.quoteValidityDays)
    }
}
