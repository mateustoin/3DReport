package com.threedreport.app.ui.services

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.services_error_minutes_invalid
import _3dreport.composeapp.generated.resources.services_notice_archived
import _3dreport.composeapp.generated.resources.services_notice_deleted
import _3dreport.composeapp.generated.resources.services_notice_restored
import _3dreport.composeapp.generated.resources.services_notice_saved
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Seu tempo num serviço (decisão 123), ver [Service.laborMinutes], e o estado único da tela. */
class ServiceListViewModelTest {

    // Unconfined: a lista acompanha o repositório e os avisos chegam na hora, sem esperar relógio nenhum.
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

    private fun viewModelOf(repository: ServiceRepository) = ServiceListViewModel(repository, scope)

    /** Os avisos emitidos daqui em diante (o fluxo não guarda os antigos). */
    private fun ServiceListViewModel.collectNotices(): List<UiText> =
        mutableListOf<UiText>().also { received -> scope.launch { notices.collect { received += it } } }

    @Test
    fun savingWithMinutesTextParsesItAsLaborMinutesAndClosesTheForm() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Pintura", minutesText = "1h30") }
        viewModel.save()

        val saved = repository.services.value.first { it.name == "Pintura" }
        assertEquals(90.0, saved.laborMinutes)
        assertNull(viewModel.uiState.value.form)
        assertEquals(listOf(uiText(Res.string.services_notice_saved)), notices)
    }

    @Test
    fun blankMinutesAndPriceSaveAsNull() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Lixamento") }
        viewModel.save()

        val saved = repository.services.value.first { it.name == "Lixamento" }
        assertNull(saved.laborMinutes)
        assertNull(saved.suggestedPrice)
    }

    @Test
    fun theSuggestedPriceAndChargeModeAreSaved() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Montagem", priceText = "12,50", chargedPerOrder = true) }
        viewModel.save()

        val saved = repository.services.value.single()
        assertEquals(12.5, saved.suggestedPrice)
        assertTrue(saved.chargedPerOrder)
    }

    @Test
    fun invalidMinutesFailOnTheMinutesFieldAndSaveNothing() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Pintura", minutesText = "abc") }
        viewModel.save()

        assertEquals(uiText(Res.string.services_error_minutes_invalid), viewModel.uiState.value.form!!.minutesError)
        assertEquals(0, repository.services.value.size)
    }

    @Test
    fun aBlankNameFailsOnTheNameField() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)

        viewModel.startAdd()
        viewModel.save()

        assertEquals(uiText(Res.string.common_error_name_required), viewModel.uiState.value.form!!.nameError)
        assertEquals(0, repository.services.value.size)
    }

    @Test
    fun aNegativeOrInvalidPriceFailsOnThePriceField() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)
        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Pintura", priceText = "-5") }

        viewModel.save()
        assertEquals(uiText(Res.string.common_error_negative), viewModel.uiState.value.form!!.priceError)

        viewModel.updateForm { it.copy(priceText = "abc") }
        viewModel.save()
        assertEquals(uiText(Res.string.common_error_number_invalid), viewModel.uiState.value.form!!.priceError)
        assertEquals(0, repository.services.value.size)
    }

    @Test
    fun editingAServiceWithMinutesRestoresTheFieldAndSavingKeepsItArchived() {
        val repository = ServiceRepository()
        repository.add(Service(id = "pintura", name = "Pintura", laborMinutes = 45.0, archived = true))
        val viewModel = viewModelOf(repository)

        viewModel.startEdit("pintura")
        assertEquals("45", viewModel.uiState.value.form!!.minutesText)

        viewModel.updateForm { it.copy(name = "Pintura fina") }
        viewModel.save()
        val saved = repository.services.value.single()
        assertEquals("Pintura fina", saved.name)
        assertTrue(saved.archived)
    }

    @Test
    fun theListFollowsTheRepositoryAndSplitsArchived() {
        val repository = ServiceRepository()
        val viewModel = viewModelOf(repository)
        assertTrue(viewModel.uiState.value.isEmpty)

        repository.add(Service(id = "pintura", name = "Pintura"))
        repository.add(Service(id = "embalagem", name = "Embalagem", archived = true))

        assertEquals(listOf("pintura"), viewModel.uiState.value.active.map { it.id })
        assertEquals(listOf("embalagem"), viewModel.uiState.value.archived.map { it.id })
    }

    @Test
    fun requestDeleteCarriesTheUsageAndWhetherItCanBeArchived() {
        val repository = ServiceRepository()
        repository.add(Service(id = "pintura", name = "Pintura"))
        repository.add(Service(id = "embalagem", name = "Embalagem", archived = true))
        val viewModel = viewModelOf(repository)

        viewModel.requestDelete("pintura")
        assertTrue(viewModel.uiState.value.pendingDelete!!.canArchive)

        viewModel.requestDelete("embalagem")
        assertEquals(false, viewModel.uiState.value.pendingDelete!!.canArchive)

        viewModel.dismissDelete()
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun archivingRestoringAndDeletingWarn() {
        val repository = ServiceRepository()
        repository.add(Service(id = "pintura", name = "Pintura"))
        val viewModel = viewModelOf(repository)
        val notices = viewModel.collectNotices()
        viewModel.startEdit("pintura")

        viewModel.setArchived("pintura", true)
        viewModel.setArchived("pintura", false)
        viewModel.delete("pintura")

        assertNull(viewModel.uiState.value.form)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(
            listOf(
                uiText(Res.string.services_notice_archived),
                uiText(Res.string.services_notice_restored),
                uiText(Res.string.services_notice_deleted),
            ),
            notices,
        )
    }
}
