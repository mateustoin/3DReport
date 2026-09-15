package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.Service
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Valida que o histórico de orçamentos (metadados + foto) sobrevive a uma nova instância do repositório. */
class QuoteHistoryRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private val quote = PricingCalculator.calculate(
        job = PrintJob(
            filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
            filamentLengthMeters = 12.0,
            printTimeMinutes = 190.0,
        ),
        printer = PrinterProfile(
            id = "printer",
            name = "Impressora",
            printerPowerWatts = 380.0,
            maintenanceCostPerHour = 0.17,
            machineInvestment = MachineInvestment(2700.0, 12, 25, 16.0),
        ),
        settings = PricingSettings(
            energyPricePerKwh = 1.23,
            failureRate = 0.10,
            finishingRate = 0.10,
            profitMargin = 1.0,
        ),
    )

    @Test
    fun blankNameGetsAGenericDefault() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "  ", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertTrue(saved.name.isNotBlank())
    }

    @Test
    fun savedQuoteSurvivesNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        repository.save(name = "Suporte de celular", quote = quote, services = emptyList(), photo = null, sourceLink = "https://example.com/model")

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first()
        assertEquals("Suporte de celular", reloaded.name)
        assertEquals("https://example.com/model", reloaded.sourceLink)
        assertEquals(quote.productionCost, reloaded.quote.productionCost)
    }

    @Test
    fun photoBytesSurviveNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val photoBytes = byteArrayOf(1, 2, 3, 4)
        val saved = repository.save(
            name = "Com foto",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            sourceLink = null,
        )

        val reloadedRepository = QuoteHistoryRepository()
        val reloaded = reloadedRepository.savedQuotes.value.first { it.id == saved.id }
        assertContentEquals(photoBytes, reloadedRepository.photoBytes(reloaded))
    }

    @Test
    fun servicesSurviveNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val services = listOf(Service(id = "s1", name = "Pintura", price = 20.0))

        val saved = repository.save(name = "Com serviço", quote = quote, services = services, photo = null, sourceLink = null)

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }
        assertEquals(services, reloaded.services)
        assertEquals(quote.salePrice + 20.0, reloaded.totalWithServices)
    }

    @Test
    fun deleteRemovesMetadataAndPhotoFile() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Pra excluir",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(9)),
            sourceLink = null,
        )

        repository.delete(saved.id)

        assertTrue(QuoteHistoryRepository().savedQuotes.value.none { it.id == saved.id })
        assertNull(repository.photoBytes(saved))
    }
}
