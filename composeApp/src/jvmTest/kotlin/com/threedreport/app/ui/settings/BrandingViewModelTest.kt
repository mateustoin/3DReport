package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
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
        assertNull(repository.branding.value.watermarkText) // nada foi persistido
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
        assertEquals("Minha Marca", repository.branding.value.watermarkText)
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
        assertNull(repository.branding.value.watermarkText)
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
        assertEquals("Minha Marca", saved.watermarkText)
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
}
