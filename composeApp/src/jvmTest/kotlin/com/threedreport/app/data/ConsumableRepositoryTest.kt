package com.threedreport.app.data

import com.threedreport.core.model.Consumable
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** O cadastro de insumos (decisão 122) nasce vazio e sobrevive a uma nova instância do repositório. */
class ConsumableRepositoryTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.path)
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsEmpty() {
        assertTrue(ConsumableRepository().consumables.value.isEmpty())
    }

    @Test
    fun addedConsumableSurvivesNewRepositoryInstance() {
        ConsumableRepository().add(Consumable(id = "argola", name = "Argola", unitCost = 0.35, chargedPerOrder = false))

        val reloaded = ConsumableRepository().consumables.value.single()
        assertEquals("Argola", reloaded.name)
        assertEquals(0.35, reloaded.unitCost)
    }
}
