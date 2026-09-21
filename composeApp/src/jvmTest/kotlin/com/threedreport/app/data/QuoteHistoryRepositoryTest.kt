package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
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
    fun stlBytesSurviveNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val stlBytes = byteArrayOf(5, 6, 7, 8)
        val saved = repository.save(
            name = "Com STL",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "modelo.stl", bytes = stlBytes),
            sourceLink = null,
        )

        val reloadedRepository = QuoteHistoryRepository()
        val reloaded = reloadedRepository.savedQuotes.value.first { it.id == saved.id }
        assertContentEquals(stlBytes, reloadedRepository.stlBytes(reloaded))
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
    fun clientSurvivesNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Com cliente",
            quote = quote,
            services = emptyList(),
            photo = null,
            sourceLink = null,
            client = Client(name = "Maria", contact = "(11) 99999-0000"),
        )

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }
        assertEquals(Client(name = "Maria", contact = "(11) 99999-0000"), reloaded.client)
    }

    @Test
    fun savedQuoteWithoutClientHasNullClient() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "Sem cliente", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertNull(saved.client)
    }

    @Test
    fun savedQuoteStartsWithOrcadoStatus() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "Novo", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertEquals(OrderStatus.ORCADO, saved.status)
    }

    @Test
    fun updateStatusChangesOnlyTheTargetQuoteAndSurvivesReload() {
        val repository = QuoteHistoryRepository()
        val target = repository.save(name = "Alvo", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val other = repository.save(name = "Outro", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        repository.updateStatus(target.id, OrderStatus.APROVADO)

        val reloaded = QuoteHistoryRepository().savedQuotes.value
        assertEquals(OrderStatus.APROVADO, reloaded.first { it.id == target.id }.status)
        assertEquals(OrderStatus.ORCADO, reloaded.first { it.id == other.id }.status)
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

    @Test
    fun updateKeepsIdAndSavedAtButChangesFieldsAndMarksLastEdited() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "Original", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        val updated = repository.update(
            id = saved.id,
            name = "Corrigido",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = null,
            sourceLink = "https://example.com/novo-link",
            client = null,
        )

        assertEquals(saved.id, updated?.id)
        assertEquals(saved.savedAtEpochMillis, updated?.savedAtEpochMillis)
        assertEquals("Corrigido", updated?.name)
        assertEquals("https://example.com/novo-link", updated?.sourceLink)
        assertTrue((updated?.lastEditedEpochMillis ?: 0) > 0)

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }
        assertEquals("Corrigido", reloaded.name)
    }

    @Test
    fun updateReturnsNullWhenIdDoesNotExist() {
        val repository = QuoteHistoryRepository()

        val result = repository.update(
            id = "nao-existe",
            name = "X",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = null,
            sourceLink = null,
            client = null,
        )

        assertNull(result)
    }

    @Test
    fun updateReplacesPhotoAndRemovesTheOldFile() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Com foto",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "antiga.png", bytes = byteArrayOf(1)),
            sourceLink = null,
        )

        val newPhotoBytes = byteArrayOf(2, 2, 2)
        val updated = repository.update(
            id = saved.id,
            name = saved.name,
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "nova.jpg", bytes = newPhotoBytes),
            stlFile = null,
            sourceLink = null,
            client = null,
        )

        assertContentEquals(newPhotoBytes, updated?.let { repository.photoBytes(it) })
    }

    @Test
    fun updateWithNullPhotoRemovesExistingPhoto() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Com foto",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(1)),
            sourceLink = null,
        )

        val updated = repository.update(
            id = saved.id,
            name = saved.name,
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = null,
            sourceLink = null,
            client = null,
        )

        assertNull(updated?.photoFileName)
        assertNull(updated?.let { repository.photoBytes(it) })
    }

    @Test
    fun deleteRemovesStlFile() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Pra excluir",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "modelo.stl", bytes = byteArrayOf(9)),
            sourceLink = null,
        )

        repository.delete(saved.id)

        assertNull(repository.stlBytes(saved))
    }
}
