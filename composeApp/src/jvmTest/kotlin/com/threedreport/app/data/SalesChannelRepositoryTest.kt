package com.threedreport.app.data

import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.SalesChannel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O que importa aqui é a migração: quem já usava a taxa única de marketplace não pode perder essa
 * configuração (que afetava preço) só porque o app passou a ter catálogo de canais.
 */
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
    fun legacyMarketplaceFeeBecomesAChannelAndIsWrittenToDisk() {
        SettingsRepository().update(
            PricingSettings(
                energyPricePerKwh = 1.23,
                failureRate = 0.1,
                finishingRate = 0.1,
                profitMargin = 1.0,
                marketplaceFeeRate = 0.15,
            )
        )

        val migrated = SalesChannelRepository().channels.value

        assertEquals(1, migrated.size)
        assertEquals(0.15, migrated.first().feeRate, 1e-9)
        assertTrue(File(dataDir, "channels.json").exists(), "a migração precisa virar arquivo, não só memória")
    }

    @Test
    fun zeroingTheLegacyFeeLaterDoesNotEraseTheMigratedChannel() {
        val settings = SettingsRepository()
        settings.update(
            PricingSettings(
                energyPricePerKwh = 1.23,
                failureRate = 0.1,
                finishingRate = 0.1,
                profitMargin = 1.0,
                marketplaceFeeRate = 0.15,
            )
        )
        SalesChannelRepository() // primeira abertura: migra e grava

        settings.update(settings.settings.value.copy(marketplaceFeeRate = 0.0))

        assertEquals(1, SalesChannelRepository().channels.value.size)
    }

    @Test
    fun withoutLegacyFeeTheCatalogStartsEmpty() {
        assertTrue(SalesChannelRepository().channels.value.isEmpty())
    }

    @Test
    fun addedChannelSurvivesNewRepositoryInstance() {
        SalesChannelRepository().add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.20))

        val reloaded = SalesChannelRepository().channels.value
        assertEquals("Shopee", reloaded.single().name)
    }
}
