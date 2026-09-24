package com.threedreport.app.ui.templates

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.core.model.QuoteTemplate
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

    private fun sampleTemplate(name: String = "Formal") = QuoteTemplate(
        id = name,
        name = name,
        watermarkText = "Minha Loja",
        showWatermark = true,
        showFooter = false,
    )

    @Test
    fun deletingRemovesFromCatalog() {
        val repository = TemplateRepository()
        repository.add(sampleTemplate())
        val viewModel = TemplateListViewModel(repository, BrandingRepository())
        val template = repository.templates.value.first()

        viewModel.delete(template.id)

        assertTrue(repository.templates.value.isEmpty())
    }

    @Test
    fun loadCopiesTemplateIntoBrandingSettings() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate())
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository)
        val template = templateRepository.templates.value.first()

        viewModel.load(template.id)

        val branding = brandingRepository.branding.value
        assertEquals("Minha Loja", branding.watermarkText)
        assertTrue(branding.showWatermark)
        assertEquals(false, branding.showFooter)
    }

    @Test
    fun loadDoesNothingForUnknownId() {
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(TemplateRepository(), brandingRepository)

        viewModel.load("does-not-exist")

        assertNull(brandingRepository.branding.value.watermarkText)
    }

    @Test
    fun activeTemplateIdMatchesTemplateWithSameFieldsAsBranding() {
        val templateRepository = TemplateRepository()
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository)
        val formal = sampleTemplate("Formal")
        templateRepository.add(formal)

        assertNull(viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value))

        viewModel.load(formal.id)

        assertEquals(formal.id, viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value))
    }

    @Test
    fun printTimeChoiceTravelsWithTheTemplateAndCountsForActiveDetection() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate().copy(showPrintTime = true))
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository)
        val template = templateRepository.templates.value.first()

        viewModel.load(template.id)

        assertTrue(brandingRepository.branding.value.showPrintTime)
        assertEquals(template.id, viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value))
        assertNull(
            viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value.copy(showPrintTime = false)),
            "com o tempo desligado, a configuração ativa já não é mais esse template",
        )
    }
}
