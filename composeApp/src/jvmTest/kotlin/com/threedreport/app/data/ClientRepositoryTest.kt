package com.threedreport.app.data

import com.threedreport.core.model.Client
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** O cadastro de clientes nasce de quem aparece nos pedidos (decisão 106). */
class ClientRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun aNewNameCreatesAClientWithAnId() {
        val resolved = ClientRepository().resolve(Client(name = "  Maria Silva ", contact = "(11) 99999-0000"))

        assertNotNull(resolved.id)
        assertEquals("Maria Silva", resolved.name)
        assertEquals(listOf(resolved), ClientRepository().clients.value)
    }

    @Test
    fun theSameNameWithDifferentCaseAndSpacesIsTheSameClient() {
        val repository = ClientRepository()
        val first = repository.resolve(Client(name = "Maria Silva"))

        val second = repository.resolve(Client(name = "maria  silva", contact = "maria@example.com"))

        assertEquals(first.id, second.id)
        assertEquals(1, repository.clients.value.size)
        assertEquals("maria@example.com", repository.clients.value.single().contact, "um contato novo atualiza o cadastro")
    }

    @Test
    fun anEmptyContactDoesNotEraseTheOneAlreadyKnown() {
        val repository = ClientRepository()
        repository.resolve(Client(name = "João", contact = "(21) 98888-0000"))

        val resolved = repository.resolve(Client(name = "João"))

        assertEquals("(21) 98888-0000", resolved.contact)
    }
}
