package com.threedreport.app.ui.quote

import com.threedreport.app.data.ConsumableRepository
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.PrinterRepository
import com.threedreport.app.data.QuoteHistoryRepository
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.data.ServiceRepository
import com.threedreport.app.data.SettingsRepository
import com.threedreport.core.model.Consumable
import com.threedreport.core.model.Service
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A tela de Orçamento com as regras da Leva 10 (decisões 122 a 125): o que a pessoa marca chega ao motor,
 * é gravado no pedido e volta igual ao reabrir, sem reprecificar em silêncio (decisão 108).
 */
class QuoteLeva10Test {

    private lateinit var settings: SettingsRepository
    private lateinit var services: ServiceRepository
    private lateinit var consumables: ConsumableRepository
    private lateinit var history: QuoteHistoryRepository

    @BeforeTest
    fun setUp() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
        settings = SettingsRepository()
        services = ServiceRepository()
        consumables = ConsumableRepository()
        history = QuoteHistoryRepository()
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    private fun viewModel() = QuoteViewModel(
        FilamentRepository(), PrinterRepository(), settings, services, SalesChannelRepository(), history,
        consumableRepository = consumables,
    ).apply {
        setLengthMeters("12")
        setPrintTimeMinutes("190")
    }

    private fun QuoteViewModel.quote() = assertNotNull(currentResult().quote, currentResult().fieldErrors.toString())

    @Test
    fun aConsumableEntersTheCostWithTodaysCatalogPrice() {
        consumables.add(Consumable(id = "argola", name = "Argola", unitCost = 0.5))
        val vm = viewModel()
        vm.setQuantity("3")
        vm.toggleConsumable("argola")
        vm.setConsumableQuantity("argola", "2")

        assertEquals(0.5 * 2 * 3, vm.quote().costs.consumables, 1e-9)

        consumables.update(Consumable(id = "argola", name = "Argola", unitCost = 1.0))
        assertEquals(1.0 * 2 * 3, vm.quote().costs.consumables, 1e-9)
    }

    @Test
    fun anInvalidConsumableQuantityIsAFieldErrorNotZero() {
        consumables.add(Consumable(id = "argola", name = "Argola", unitCost = 0.5))
        val vm = viewModel()
        vm.toggleConsumable("argola")
        vm.setConsumableQuantity("argola", "0")

        val result = vm.currentResult()
        assertNull(result.quote)
        assertTrue(QuoteFields.consumable("argola") in result.fieldErrors)
    }

    @Test
    fun rushOnlyCountsWithTheSurchargeConfigured() {
        val vm = viewModel()
        val normal = vm.quote().salePrice
        vm.setRush(true)
        assertFalse(vm.quote().rush)
        assertEquals(normal, vm.quote().salePrice, 1e-9)

        settings.update(settings.settings.value.copy(rushSurchargeRate = 0.3))
        assertTrue(vm.quote().rush)
        assertEquals(normal * 1.3, vm.quote().salePrice, 1e-9)
    }

    @Test
    fun freeShippingStaysOutOfTheCustomerTotalAndIsSaved() {
        val vm = viewModel()
        vm.setShippingCost("18")
        val charged = vm.currentResult()
        vm.setShippingAbsorbed(true)
        val free = vm.currentResult()
        val quote = assertNotNull(free.quote)

        assertEquals(assertNotNull(charged.grandTotal) - 18.0, assertNotNull(free.grandTotal), 1e-9)
        assertEquals(18.0, quote.absorbedShippingCost, 1e-9)
        assertEquals(assertNotNull(charged.quote).profit - 18.0, quote.profit, 1e-9)

        vm.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()
        assertTrue(saved.shippingAbsorbed)
        assertEquals(18.0, saved.shippingCost)
        assertEquals(0.0, saved.chargedShipping)
    }

    @Test
    fun theShippingCoveringPriceKeepsTheTableProfit() {
        val vm = viewModel()
        val tableProfit = vm.quote().profit
        vm.setShippingCost("18")
        vm.setShippingAbsorbed(true)

        vm.useShippingCoveringPrice()

        assertTrue(vm.input.value.targetTotalText.isNotBlank())
        assertEquals(tableProfit, vm.quote().profit, 0.01)
    }

    @Test
    fun aTimedServiceSuggestsItsPriceFromYourHour() {
        settings.update(settings.settings.value.copy(laborRatePerHour = 30.0))
        services.add(Service(id = "paint", name = "Pintura", laborMinutes = 30.0))
        val vm = viewModel()
        vm.toggleService("paint")

        val input = assertNotNull(vm.input.value.selectedServices["paint"])
        assertEquals("15", input.priceText)
        assertEquals("30", input.laborMinutesText)
        val quote = vm.quote()
        assertEquals(30.0, quote.serviceLaborMinutes, 1e-9)
        assertEquals(0.0, quote.serviceProfit, 1e-9)

        vm.setServicePrice("paint", "25")
        assertEquals(10.0, vm.quote().serviceProfit, 1e-9)
    }

    @Test
    fun reopeningKeepsConsumablesRushAndFreeShippingWithoutRepricing() {
        settings.update(settings.settings.value.copy(rushSurchargeRate = 0.3))
        consumables.add(Consumable(id = "caixa", name = "Caixa", unitCost = 2.0, chargedPerOrder = true))
        val vm = viewModel()
        vm.toggleConsumable("caixa")
        vm.setRush(true)
        vm.setShippingCost("18")
        vm.setShippingAbsorbed(true)
        vm.saveCurrentQuote()
        val saved = history.savedQuotes.value.single()

        consumables.update(Consumable(id = "caixa", name = "Caixa", unitCost = 5.0, chargedPerOrder = true))
        val editor = viewModel()
        editor.loadForEditing(saved)

        val input = editor.input.value
        assertTrue(input.rush)
        assertTrue(input.shippingAbsorbed)
        assertEquals("1", input.selectedConsumables["caixa"]?.quantityText)
        val reopened = editor.currentResult()
        assertTrue(reopened.keepsOriginalPrice)
        assertEquals(saved.quote, reopened.quote)
        // O custo de hoje aparece só como "sairia R$ X hoje".
        assertEquals(5.0, assertNotNull(reopened.todaysQuote).costs.consumables, 1e-9)

        editor.saveCurrentQuote()
        val resaved = history.savedQuotes.value.single()
        assertEquals(saved.totalWithServices, resaved.totalWithServices, 1e-9)
        assertEquals(saved.quote.profit, resaved.quote.profit, 1e-9)
        assertTrue(resaved.shippingAbsorbed)
    }
}
