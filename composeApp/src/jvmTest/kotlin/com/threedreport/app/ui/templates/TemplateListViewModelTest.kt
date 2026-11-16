package com.threedreport.app.ui.templates

import com.threedreport.app.data.BrandingRepository
import com.threedreport.app.data.TemplateRepository
import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun sampleTemplate(name: String = "Formal") = QuoteTemplate(
        id = name,
        name = name,
        brandName = "Minha Loja",
        showWatermark = true,
        showFooter = false,
    )

    @Test
    fun deletingRemovesFromCatalog() {
        val repository = TemplateRepository()
        repository.add(sampleTemplate())
        val viewModel = TemplateListViewModel(repository, BrandingRepository(), scope)
        val template = repository.templates.value.first()

        viewModel.delete(template.id)

        assertTrue(repository.templates.value.isEmpty())
    }

    @Test
    fun loadCopiesTemplateIntoBrandingSettings() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate())
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository, scope)
        val template = templateRepository.templates.value.first()

        viewModel.load(template.id)

        val branding = brandingRepository.branding.value
        assertEquals("Minha Loja", branding.brandName)
        assertTrue(branding.showWatermark)
        assertEquals(false, branding.showFooter)
    }

    @Test
    fun loadDoesNothingForUnknownId() {
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(TemplateRepository(), brandingRepository, scope)

        viewModel.load("does-not-exist")

        assertNull(brandingRepository.branding.value.brandName)
    }

    @Test
    fun activeTemplateIdMatchesTemplateWithSameFieldsAsBranding() {
        val templateRepository = TemplateRepository()
        val brandingRepository = BrandingRepository()
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository, scope)
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
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository, scope)
        val template = templateRepository.templates.value.first()

        viewModel.load(template.id)

        assertTrue(brandingRepository.branding.value.showPrintTime)
        assertEquals(template.id, viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value))
        assertNull(
            viewModel.activeTemplateId(templateRepository.templates.value, brandingRepository.branding.value.copy(showPrintTime = false)),
            "com o tempo desligado, a configuração ativa já não é mais esse template",
        )
    }

    @Test
    fun loadingATemplateKeepsTheSellerLogoAndContact() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate().copy(showBorder = true))
        val brandingRepository = BrandingRepository()
        brandingRepository.update(
            com.threedreport.core.model.BrandingSettings(contactInstagram = "loja"),
            com.threedreport.app.data.LogoChange.Replace(com.threedreport.app.platform.PickedFile("logo.png", byteArrayOf(1))),
        )
        val viewModel = TemplateListViewModel(templateRepository, brandingRepository, scope)

        viewModel.load(templateRepository.templates.value.first().id)

        val branding = brandingRepository.branding.value
        assertTrue(branding.showBorder)
        assertEquals("@loja", branding.instagramHandle)
        assertTrue(branding.logoFileName != null)
        assertEquals(templateRepository.templates.value.first().id, viewModel.activeTemplateId(templateRepository.templates.value, branding))
    }

    @Test
    fun theListMarksOnlyTheTemplateThatIsActive() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate("Formal"))
        templateRepository.add(sampleTemplate("Simples").copy(brandName = "Outra", showFooter = true))
        val viewModel = TemplateListViewModel(templateRepository, BrandingRepository(), scope)

        assertEquals(listOf(false, false), viewModel.uiState.value.rows.map { it.isActive })

        viewModel.load("Formal")

        assertEquals(listOf("Formal" to true, "Simples" to false), viewModel.uiState.value.rows.map { it.template.name to it.isActive })
    }

    @Test
    fun deletingAsksFirstAndCancellingKeepsTheTemplate() {
        val templateRepository = TemplateRepository()
        templateRepository.add(sampleTemplate())
        val viewModel = TemplateListViewModel(templateRepository, BrandingRepository(), scope)

        viewModel.requestDelete("Formal")
        assertEquals("Formal", assertNotNull(viewModel.uiState.value.pendingDelete).id)
        viewModel.dismissDelete()
        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(1, viewModel.uiState.value.rows.size)

        viewModel.requestDelete("Formal")
        viewModel.delete("Formal")

        assertNull(viewModel.uiState.value.pendingDelete)
        assertTrue(viewModel.uiState.value.rows.isEmpty())
    }
}
