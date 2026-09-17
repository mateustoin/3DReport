package com.threedreport.app.ui.templates

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingANewTemplatePersistsAllFields() {
        val repository = TemplateRepository()
        val viewModel = TemplateListViewModel(repository, BrandingRepository())

        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(name = "Formal", watermarkText = "Minha Loja", showWatermark = true, showFooter = false)
        }
        viewModel.save()

        val saved = repository.templates.value.first { it.name == "Formal" }
        assertEquals("Minha Loja", saved.watermarkText)
        assertTrue(saved.showWatermark)
        assertEquals(false, saved.showFooter)
    }

    @Test
    fun savingWithBlankNameFailsWithError() {
        val viewModel = TemplateListViewModel(TemplateRepository(), BrandingRepository())

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "   ") }
        viewModel.save()

        assertTrue(viewModel.form.value?.errorMessage != null)
    }

    @Test
    fun deletingRemovesFromCatalog() {
        val repository = TemplateRepository()
        val viewModel = TemplateListViewModel(repository, BrandingRepository())
        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Simples") }
        viewModel.save()
        val template = repository.templates.value.first()

        viewModel.delete(template.id)

        assertTrue(repository.templates.value.isEmpty())
    }

    @Test
    fun applyToActiveCopiesTemplateIntoBrandingSettings() {
        val templateRepository = TemplateRepository()
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository)
        viewModel.startAdd()
        viewModel.updateForm {
            it.copy(name = "Formal", watermarkText = "Minha Loja", showWatermark = true, showFooter = false)
        }
        viewModel.save()
        val template = templateRepository.templates.value.first()

        viewModel.applyToActive(template.id)

        val branding = brandingRepository.branding.value
        assertEquals("Minha Loja", branding.watermarkText)
        assertTrue(branding.showWatermark)
        assertEquals(false, branding.showFooter)
    }

    @Test
    fun applyToActiveDoesNothingForUnknownId() {
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(TemplateRepository(), brandingRepository)

        viewModel.applyToActive("does-not-exist")

        assertNull(brandingRepository.branding.value.watermarkText)
    }
}
