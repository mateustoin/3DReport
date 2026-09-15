package com.threedreport.app.ui.settings

import com.threedreport.app.data.BrandingRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun savingWithNameAndBothOptionsUncheckedFails() {
        val repository = BrandingRepository()
        val viewModel = BrandingViewModel(repository)

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
        val viewModel = BrandingViewModel(repository)

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
        val viewModel = BrandingViewModel(repository)

        viewModel.setShowWatermark(false)
        viewModel.setShowFooter(false)
        viewModel.save()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(repository.branding.value.watermarkText)
    }
}
