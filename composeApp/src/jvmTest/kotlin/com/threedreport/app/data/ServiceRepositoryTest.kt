package com.threedreport.app.data

import com.threedreport.core.model.Service
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que o catálogo de serviços sobrevive a uma nova instância do repositório (persistência em disco). */
class ServiceRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsEmpty() {
        // Diferente de filamentos/impressoras, não há serviço "padrão" que sirva pra qualquer criador.
        assertTrue(ServiceRepository().services.value.isEmpty())
    }

    @Test
    fun addedServiceSurvivesNewRepositoryInstance() {
        val original = ServiceRepository()
        original.add(Service(id = "paint", name = "Pintura", price = 20.0))

        val reloaded = ServiceRepository().services.value
        assertTrue(reloaded.any { it.id == "paint" && it.name == "Pintura" && it.price == 20.0 })
    }

    @Test
    fun updatePersistsChange() {
        val repository = ServiceRepository()
        repository.add(Service(id = "paint", name = "Pintura", price = 20.0))
        val service = repository.services.value.first()

        repository.update(service.copy(price = 30.0))

        val reloaded = ServiceRepository().services.value.first { it.id == service.id }
        assertEquals(30.0, reloaded.price)
    }

    @Test
    fun deleteRemovesFromDisk() {
        val repository = ServiceRepository()
        repository.add(Service(id = "paint", name = "Pintura", price = 20.0))
        val service = repository.services.value.first()

        repository.delete(service.id)

        val reloaded = ServiceRepository().services.value
        assertTrue(reloaded.none { it.id == service.id })
    }
}
