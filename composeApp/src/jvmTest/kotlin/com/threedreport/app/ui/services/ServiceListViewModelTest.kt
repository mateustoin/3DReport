package com.threedreport.app.ui.services

import com.threedreport.app.data.ServiceRepository
import com.threedreport.core.model.Service
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Seu tempo num serviço (decisão 123), ver [Service.laborMinutes]. */
class ServiceListViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingWithMinutesTextParsesItAsLaborMinutes() {
        val repository = ServiceRepository()
        val viewModel = ServiceListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Pintura", minutesText = "1h30") }
        viewModel.save()

        val saved = repository.services.value.first { it.name == "Pintura" }
        assertEquals(90.0, saved.laborMinutes)
    }

    @Test
    fun blankMinutesTextSavesAsNull() {
        val repository = ServiceRepository()
        val viewModel = ServiceListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Lixamento") }
        viewModel.save()

        val saved = repository.services.value.first { it.name == "Lixamento" }
        assertNull(saved.laborMinutes)
    }

    @Test
    fun invalidMinutesTextFailsAndSavesNothing() {
        val repository = ServiceRepository()
        val viewModel = ServiceListViewModel(repository)

        viewModel.startAdd()
        viewModel.updateForm { it.copy(name = "Pintura", minutesText = "abc") }
        viewModel.save()

        assertNotNull(viewModel.form.value!!.errorMessage)
        assertEquals(0, repository.services.value.size)
    }

    @Test
    fun editingAServiceWithMinutesRestoresTheField() {
        val repository = ServiceRepository()
        repository.add(Service(id = "pintura", name = "Pintura", laborMinutes = 45.0))
        val viewModel = ServiceListViewModel(repository)

        viewModel.startEdit(repository.services.value.single())

        assertEquals("45", viewModel.form.value!!.minutesText)
    }
}
