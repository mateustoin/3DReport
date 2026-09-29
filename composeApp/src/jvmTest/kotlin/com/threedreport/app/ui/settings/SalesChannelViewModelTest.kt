package com.threedreport.app.ui.settings

import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.core.model.SalesChannel
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SalesChannelViewModelTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun savingAFixedFeeRoundTripsThroughTheChannel() {
        val repository = SalesChannelRepository()
        val viewModel = SalesChannelViewModel(repository)

        viewModel.setName("Shopee")
        viewModel.setFeeRate("20")
        viewModel.setFixedFeePerItem("4")
        viewModel.save()

        val saved = repository.channels.value.single()
        assertEquals(0.20, saved.feeRate, 1e-9)
        assertEquals(4.0, saved.fixedFeePerItem, 1e-9)
    }

    @Test
    fun blankFixedFeeSavesAsZero() {
        val repository = SalesChannelRepository()
        val viewModel = SalesChannelViewModel(repository)

        viewModel.setName("Pix")
        viewModel.save()

        assertEquals(0.0, repository.channels.value.single().fixedFeePerItem)
    }

    @Test
    fun addingTheFirstTierCreatesTwoRowsWithTheLastOneUnbounded() {
        val viewModel = SalesChannelViewModel(SalesChannelRepository())
        viewModel.setName("Shopee")

        viewModel.addTierRow()

        assertEquals(2, viewModel.form.value.tiers.size)
        assertTrue(viewModel.form.value.tiersExpanded)
    }

    @Test
    fun validTiersSaveAsChannelFeeTiers() {
        val repository = SalesChannelRepository()
        val viewModel = SalesChannelViewModel(repository)
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        val (first, last) = viewModel.form.value.tiers
        viewModel.updateTierRow(first.id) { it.copy(upToUnitPriceText = "50", feeRatePercentText = "14", fixedFeePerItemText = "2") }
        viewModel.updateTierRow(last.id) { it.copy(feeRatePercentText = "20", fixedFeePerItemText = "4") }

        viewModel.save()

        val saved = repository.channels.value.single()
        assertEquals(2, saved.tiers.size)
        assertEquals(50.0, saved.tiers[0].upToUnitPrice)
        assertEquals(0.14, saved.tiers[0].feeRate, 1e-9)
        assertEquals(2.0, saved.tiers[0].fixedFeePerItem, 1e-9)
        assertNull(saved.tiers[1].upToUnitPrice)
        assertEquals(0.20, saved.tiers[1].feeRate, 1e-9)
    }

    @Test
    fun nonIncreasingTierLimitsShowAnErrorAndSaveNothing() {
        val repository = SalesChannelRepository()
        val viewModel = SalesChannelViewModel(repository)
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        viewModel.addTierRow()
        val (first, second, last) = viewModel.form.value.tiers
        viewModel.updateTierRow(first.id) { it.copy(upToUnitPriceText = "50", feeRatePercentText = "10") }
        // Segunda faixa com limite menor que a primeira: não pode subir.
        viewModel.updateTierRow(second.id) { it.copy(upToUnitPriceText = "30", feeRatePercentText = "12") }
        viewModel.updateTierRow(last.id) { it.copy(feeRatePercentText = "20") }

        viewModel.save()

        val error = assertNotNull(viewModel.form.value.errorMessage)
        assertTrue(error.contains("subir"))
        assertTrue(repository.channels.value.isEmpty())
    }

    @Test
    fun removingDownToOneTierRowClearsTheTiersEntirely() {
        val viewModel = SalesChannelViewModel(SalesChannelRepository())
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        val first = viewModel.form.value.tiers.first()

        viewModel.removeTierRow(first.id)

        assertTrue(viewModel.form.value.tiers.isEmpty())
    }

    @Test
    fun editingAChannelWithTiersRestoresThemExpanded() {
        val repository = SalesChannelRepository()
        val tiers = listOf(
            com.threedreport.core.model.ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.14, fixedFeePerItem = 2.0),
            com.threedreport.core.model.ChannelFeeTier(upToUnitPrice = null, feeRate = 0.20, fixedFeePerItem = 4.0),
        )
        repository.add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.0, tiers = tiers))
        val viewModel = SalesChannelViewModel(repository)

        viewModel.startEditing(repository.channels.value.single())

        val form = viewModel.form.value
        assertTrue(form.tiersExpanded)
        assertEquals(2, form.tiers.size)
        assertEquals("50", form.tiers[0].upToUnitPriceText)
        assertEquals("14", form.tiers[0].feeRatePercentText)
    }

    @Test
    fun removingTheLastTierPromotesThePreviousOneAndWarnsAboutItsLimit() {
        val viewModel = SalesChannelViewModel(SalesChannelRepository())
        viewModel.addTierRow()
        viewModel.addTierRow()
        val (first, second, last) = viewModel.form.value.tiers
        viewModel.updateTierRow(first.id) { it.copy(upToUnitPriceText = "50", feeRatePercentText = "14") }
        viewModel.updateTierRow(second.id) { it.copy(upToUnitPriceText = "100", feeRatePercentText = "16") }

        viewModel.removeTierRow(last.id)

        val form = viewModel.form.value
        assertEquals(2, form.tiers.size)
        assertEquals("", form.tiers.last().upToUnitPriceText)
        assertEquals("16", form.tiers.last().feeRatePercentText)
        assertEquals("A faixa até 100 virou \"Acima disso\", e o limite dela foi descartado.", form.tierNotice)
    }

    @Test
    fun removingAMiddleTierKeepsTheOthersWithoutNotice() {
        val viewModel = SalesChannelViewModel(SalesChannelRepository())
        viewModel.addTierRow()
        viewModel.addTierRow()
        val middle = viewModel.form.value.tiers[1]

        viewModel.removeTierRow(middle.id)

        assertEquals(2, viewModel.form.value.tiers.size)
        assertEquals(null, viewModel.form.value.tierNotice)
    }
}
