package com.threedreport.app.data

import com.threedreport.core.model.SalesChannel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Valida que o catálogo de canais de venda sobrevive a uma nova instância do repositório (persistência em disco). */
class SalesChannelRepositoryTest {

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
        // Venda direta (sem taxa) é o padrão da tela de Orçamento; não precisa de cadastro nenhum.
        assertTrue(SalesChannelRepository().channels.value.isEmpty())
    }

    @Test
    fun addedChannelSurvivesNewRepositoryInstance() {
        SalesChannelRepository().add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20))

        val reloaded = SalesChannelRepository().channels.value
        assertEquals("Shopee", reloaded.single().name)
    }
}
