package com.threedreport.app.data

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.Client
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.OrderStatus
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrintSettings
import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.QuoteService
import com.threedreport.core.pricing.PricingCalculator
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val services = listOf(QuoteService(id = "s1", name = "Pintura", price = 20.0))

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
    fun saveWithPhotoReferenceReusesTheExistingFileInsteadOfDuplicating() {
        // Simula duplicar um orçamento cuja foto não mudou.
        val repository = QuoteHistoryRepository()
        val original = repository.save(
            name = "Original",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(1, 2, 3)),
            sourceLink = null,
        )

        val duplicate = repository.save(
            name = "Duplicado",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(1, 2, 3)),
            photoReferenceFileName = original.photoFileName,
            sourceLink = null,
        )

        assertEquals(original.photoFileName, duplicate.photoFileName)
    }

    @Test
    fun saveWithStlReferenceReusesTheExistingFileInsteadOfDuplicating() {
        val repository = QuoteHistoryRepository()
        val original = repository.save(
            name = "Original",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "modelo.stl", bytes = byteArrayOf(9, 9)),
            sourceLink = null,
        )

        val duplicate = repository.save(
            name = "Duplicado",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = PickedFile(fileName = "modelo.stl", bytes = byteArrayOf(9, 9)),
            stlReferenceFileName = original.stlFileName,
            sourceLink = null,
        )

        assertEquals(original.stlFileName, duplicate.stlFileName)
    }

    @Test
    fun deletingOneOfTwoQuotesSharingAPhotoKeepsTheFileForTheOther() {
        val repository = QuoteHistoryRepository()
        val photoBytes = byteArrayOf(1, 2, 3)
        val original = repository.save(
            name = "Original",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            sourceLink = null,
        )
        val duplicate = repository.save(
            name = "Duplicado",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            photoReferenceFileName = original.photoFileName,
            sourceLink = null,
        )

        repository.delete(original.id)

        assertContentEquals(photoBytes, repository.photoBytes(duplicate))
    }

    @Test
    fun deletingBothQuotesSharingAPhotoRemovesTheFile() {
        val repository = QuoteHistoryRepository()
        val original = repository.save(
            name = "Original",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(1)),
            sourceLink = null,
        )
        val duplicate = repository.save(
            name = "Duplicado",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = byteArrayOf(1)),
            photoReferenceFileName = original.photoFileName,
            sourceLink = null,
        )

        repository.delete(original.id)
        repository.delete(duplicate.id)

        assertNull(repository.photoBytes(duplicate))
    }

    @Test
    fun editingAQuoteWithoutTouchingItsSharedPhotoDoesNotBreakTheOtherReference() {
        val repository = QuoteHistoryRepository()
        val photoBytes = byteArrayOf(1, 2, 3)
        val original = repository.save(
            name = "Original",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            sourceLink = null,
        )
        val duplicate = repository.save(
            name = "Duplicado",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            photoReferenceFileName = original.photoFileName,
            sourceLink = null,
        )

        // Editar o duplicado sem trocar a foto (mesmo padrão do QuoteViewModel.loadForEditing):
        // reenvia a mesma referência, não deveria apagar o arquivo que o original ainda usa.
        repository.update(
            id = duplicate.id,
            name = "Duplicado editado",
            quote = quote,
            services = emptyList(),
            photo = PickedFile(fileName = "produto.png", bytes = photoBytes),
            photoReferenceFileName = duplicate.photoFileName,
            stlFile = null,
            sourceLink = null,
            client = null,
        )

        assertContentEquals(photoBytes, repository.photoBytes(original))
    }

    @Test
    fun printSettingsSurviveNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val settings = PrintSettings(layerHeightMm = 0.2, infillPercentage = 15.0, infillPattern = "gyroid", supportsEnabled = true)
        val saved = repository.save(
            name = "Com configurações",
            quote = quote,
            services = emptyList(),
            photo = null,
            sourceLink = null,
            printSettings = settings,
        )

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }
        assertEquals(settings, reloaded.printSettings)
    }

    @Test
    fun negotiatedTablePriceSurvivesNewRepositoryInstance() {
        val repository = QuoteHistoryRepository()
        val negotiated = quote.copy(salePrice = quote.salePrice - 5.0, tableSalePrice = quote.salePrice)
        val saved = repository.save(name = "Negociado", quote = negotiated, services = emptyList(), photo = null, sourceLink = null)

        val reloaded = QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }
        assertEquals(quote.salePrice, reloaded.quote.tableSalePrice)
        assertEquals(5.0, reloaded.quote.negotiatedDiscount, 1e-9)
    }

    @Test
    fun savedQuoteWithoutPrintSettingsHasNullPrintSettings() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(name = "Sem configurações", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertNull(saved.printSettings)
    }

    @Test
    fun updatePrintSettingsChangesOnlyTheTargetQuoteAndSurvivesReload() {
        val repository = QuoteHistoryRepository()
        val target = repository.save(name = "Alvo", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val other = repository.save(name = "Outro", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val settings = PrintSettings(layerHeightMm = 0.16, supportsEnabled = false)

        repository.updatePrintSettings(target.id, settings)

        val reloaded = QuoteHistoryRepository().savedQuotes.value
        assertEquals(settings, reloaded.first { it.id == target.id }.printSettings)
        assertNull(reloaded.first { it.id == other.id }.printSettings)
    }

    @Test
    fun deliveryDateSurvivesNewRepositoryInstanceAndEdit() {
        val repository = QuoteHistoryRepository()
        val saved = repository.save(
            name = "Com prazo",
            quote = quote,
            services = emptyList(),
            photo = null,
            sourceLink = null,
            deliveryDateEpochDay = 20_700L,
        )

        assertEquals(20_700L, QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }.deliveryDateEpochDay)

        repository.update(
            id = saved.id,
            name = "Com prazo",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = null,
            sourceLink = null,
            client = null,
            deliveryDateEpochDay = 20_710L,
        )
        assertEquals(20_710L, QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }.deliveryDateEpochDay)
    }

    @Test
    fun updateDeliveryDateChangesOnlyTheTargetQuoteAndCanRemoveIt() {
        val repository = QuoteHistoryRepository()
        val target = repository.save(name = "Alvo", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 10L)
        val other = repository.save(name = "Outro", quote = quote, services = emptyList(), photo = null, sourceLink = null, deliveryDateEpochDay = 10L)

        repository.updateDeliveryDate(target.id, 20L)
        assertEquals(20L, QuoteHistoryRepository().savedQuotes.value.first { it.id == target.id }.deliveryDateEpochDay)
        assertEquals(10L, QuoteHistoryRepository().savedQuotes.value.first { it.id == other.id }.deliveryDateEpochDay)

        repository.updateDeliveryDate(target.id, null)
        assertNull(QuoteHistoryRepository().savedQuotes.value.first { it.id == target.id }.deliveryDateEpochDay)
    }

    @Test
    fun quoteSavedWithoutDeliveryDateHasNone() {
        val saved = QuoteHistoryRepository().save(name = "Sem prazo", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        assertNull(QuoteHistoryRepository().savedQuotes.value.first { it.id == saved.id }.deliveryDateEpochDay)
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

    @Test
    fun ordersKeepTheOldFileFormat() {
        QuoteHistoryRepository().save(name = "Vaso", quote = quote, services = emptyList(), photo = null, sourceLink = null)

        val json = java.io.File(appDataDir(), "quotes.json").readText()

        assertFalse("\"kind\"" in json, "pedido não grava o campo novo: o arquivo continua igual ao de antes")
        assertTrue(QuoteHistoryRepository().savedQuotes.value.single().isOrder)
    }

    @Test
    fun productDropsClientShippingAndDeadlineAndSurvivesReload() {
        val repository = QuoteHistoryRepository()
        repository.save(
            name = "Chaveiro",
            quote = quote,
            services = emptyList(),
            photo = null,
            sourceLink = null,
            client = Client(name = "Maria"),
            shippingCost = 15.0,
            deliveryDateEpochDay = 20_000L,
            kind = QuoteKind.PRODUCT,
        )

        val reloaded = QuoteHistoryRepository().savedQuotes.value.single()
        assertEquals(QuoteKind.PRODUCT, reloaded.kind)
        assertNull(reloaded.client)
        assertEquals(0.0, reloaded.shippingCost)
        assertNull(reloaded.deliveryDateEpochDay)
    }

    @Test
    fun updateKeepsTheKind() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)

        repository.update(
            id = product.id,
            name = "Chaveiro novo",
            quote = quote,
            services = emptyList(),
            photo = null,
            stlFile = null,
            sourceLink = null,
            client = Client(name = "Maria"),
        )

        val updated = repository.savedQuotes.value.single()
        assertEquals(QuoteKind.PRODUCT, updated.kind)
        assertNull(updated.client)
    }

    @Test
    fun sourceProductIdSurvivesReload() {
        QuoteHistoryRepository().save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null, sourceProductId = "produto-1")

        assertEquals("produto-1", QuoteHistoryRepository().savedQuotes.value.single().sourceProductId)
    }

    @Test
    fun convertToOrderTurnsAProductIntoAnOrcadoOrderAndIgnoresOrders() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
        val order = repository.save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        repository.updateStatus(order.id, OrderStatus.ENTREGUE)

        repository.convertToOrder(product.id)
        repository.convertToOrder(order.id)

        val reloaded = QuoteHistoryRepository().savedQuotes.value
        val converted = reloaded.first { it.id == product.id }
        assertTrue(converted.isOrder)
        assertEquals(OrderStatus.ORCADO, converted.status)
        assertEquals(OrderStatus.ENTREGUE, reloaded.first { it.id == order.id }.status)
    }

    @Test
    fun convertToOrderDatesTheOrderAtTheConversion() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT)
        val before = System.currentTimeMillis()
        Thread.sleep(5)

        repository.convertToOrder(product.id)

        val converted = QuoteHistoryRepository().savedQuotes.value.single()
        assertTrue(converted.savedAtEpochMillis > product.savedAtEpochMillis)
        assertTrue(converted.savedAtEpochMillis >= before)
        assertNull(converted.lastEditedEpochMillis)
    }

    @Test
    fun productCategoryIsTrimmedReusesAnExistingSpellingAndSurvivesReload() {
        val repository = QuoteHistoryRepository()
        repository.save(name = "A", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = "Chaveiros")
        repository.save(name = "B", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = "  chaveiros ")
        repository.save(name = "C", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = "   ")

        val reloaded = QuoteHistoryRepository().savedQuotes.value
        assertEquals(listOf("Chaveiros", "Chaveiros", null), reloaded.map { it.category })
    }

    @Test
    fun orderNeverKeepsACategory() {
        val repository = QuoteHistoryRepository()
        val order = repository.save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null, category = "Chaveiros")

        repository.update(id = order.id, name = "Pedido", quote = quote, services = emptyList(), photo = null, stlFile = null, sourceLink = null, client = null, category = "Chaveiros")

        assertNull(repository.savedQuotes.value.single().category)
    }

    @Test
    fun updateQuoteChangesOnlyTheProductPriceAndIgnoresOrders() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "Chaveiro", quote = quote, services = emptyList(), photo = null, sourceLink = "https://x", kind = QuoteKind.PRODUCT, category = "Chaveiros")
        val order = repository.save(name = "Pedido", quote = quote, services = emptyList(), photo = null, sourceLink = null)
        val newQuote = quote.copy(salePrice = quote.salePrice + 5)

        repository.updateQuote(product.id, newQuote)
        repository.updateQuote(order.id, newQuote)

        val reloaded = QuoteHistoryRepository().savedQuotes.value
        val updated = reloaded.first { it.id == product.id }
        assertEquals(newQuote, updated.quote)
        assertEquals("https://x", updated.sourceLink)
        assertEquals("Chaveiros", updated.category)
        assertTrue(updated.lastEditedEpochMillis != null)
        assertEquals(quote, reloaded.first { it.id == order.id }.quote)
    }

    @Test
    fun editingAProductCanFixTheCapitalizationOfItsOwnCategory() {
        val repository = QuoteHistoryRepository()
        val product = repository.save(name = "A", quote = quote, services = emptyList(), photo = null, sourceLink = null, kind = QuoteKind.PRODUCT, category = "chaveiros")

        repository.update(id = product.id, name = "A", quote = quote, services = emptyList(), photo = null, stlFile = null, sourceLink = null, client = null, category = "Chaveiros")

        assertEquals("Chaveiros", repository.savedQuotes.value.single().category)
    }
}
