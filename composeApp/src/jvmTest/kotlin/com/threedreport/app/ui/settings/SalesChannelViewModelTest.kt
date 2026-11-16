package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.settings_channels_error_tier_limit_order
import _3dreport.composeapp.generated.resources.settings_channels_notice_saved
import _3dreport.composeapp.generated.resources.settings_channels_tier_promoted
import com.threedreport.app.data.SalesChannelRepository
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    /** O formulário aberto (os testes abrem um antes de mexer nele). */
    private fun SalesChannelViewModel.form(): SalesChannelFormState = assertNotNull(uiState.value.form)

    /** Um ViewModel com o formulário de um canal novo já aberto. */
    private fun newForm(repository: SalesChannelRepository = SalesChannelRepository()) =
        SalesChannelViewModel(repository).also { it.startAdd() }

    private fun SalesChannelViewModel.pendingNotice(): UiNotice? = runBlocking { withTimeoutOrNull(200) { notices.first() } }

    @Test
    fun savingAFixedFeeRoundTripsThroughTheChannel() {
        val repository = SalesChannelRepository()
        val viewModel = newForm(repository)

        viewModel.setName("Shopee")
        viewModel.setFeeRate("20")
        viewModel.setFixedFeePerItem("4")
        viewModel.save()

        val saved = repository.channels.value.single()
        assertEquals(0.20, saved.feeRate, 1e-9)
        assertEquals(4.0, saved.fixedFeePerItem, 1e-9)
        assertNull(viewModel.uiState.value.form, "salvar fecha o formulário")
        assertEquals(UiNotice(uiText(Res.string.settings_channels_notice_saved)), viewModel.pendingNotice())
    }

    @Test
    fun blankFixedFeeSavesAsZero() {
        val repository = SalesChannelRepository()
        val viewModel = newForm(repository)

        viewModel.setName("Pix")
        viewModel.save()

        assertEquals(0.0, repository.channels.value.single().fixedFeePerItem)
    }

    @Test
    fun aBlankNameIsAnErrorOfTheNameFieldAndSavesNothing() {
        val repository = SalesChannelRepository()
        val viewModel = newForm(repository)

        viewModel.save()

        assertEquals(uiText(Res.string.common_error_name_required), viewModel.form().nameError)
        assertTrue(repository.channels.value.isEmpty())
    }

    @Test
    fun addingTheFirstTierCreatesTwoRowsWithTheLastOneUnbounded() {
        val viewModel = newForm()
        viewModel.setName("Shopee")

        viewModel.addTierRow()

        assertEquals(2, viewModel.form().tiers.size)
        assertTrue(viewModel.form().tiersExpanded)
    }

    @Test
    fun validTiersSaveAsChannelFeeTiers() {
        val repository = SalesChannelRepository()
        val viewModel = newForm(repository)
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        val (first, last) = viewModel.form().tiers
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
        val viewModel = newForm(repository)
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        viewModel.addTierRow()
        val (first, second, last) = viewModel.form().tiers
        viewModel.updateTierRow(first.id) { it.copy(upToUnitPriceText = "50", feeRatePercentText = "10") }
        // Segunda faixa com limite menor que a primeira: não pode subir.
        viewModel.updateTierRow(second.id) { it.copy(upToUnitPriceText = "30", feeRatePercentText = "12") }
        viewModel.updateTierRow(last.id) { it.copy(feeRatePercentText = "20") }

        viewModel.save()

        assertEquals(uiText(Res.string.settings_channels_error_tier_limit_order), viewModel.form().tiersError)
        assertTrue(repository.channels.value.isEmpty())
    }

    @Test
    fun removingDownToOneTierRowClearsTheTiersEntirely() {
        val viewModel = newForm()
        viewModel.setName("Shopee")
        viewModel.addTierRow()
        val first = viewModel.form().tiers.first()

        viewModel.removeTierRow(first.id)

        assertTrue(viewModel.form().tiers.isEmpty())
    }

    @Test
    fun editingAChannelWithTiersRestoresThemExpanded() {
        val repository = SalesChannelRepository()
        val tiers = listOf(
            ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.14, fixedFeePerItem = 2.0),
            ChannelFeeTier(upToUnitPrice = null, feeRate = 0.20, fixedFeePerItem = 4.0),
        )
        repository.add(SalesChannel(id = "shopee", name = "Shopee", feeRate = 0.0, tiers = tiers))
        val viewModel = SalesChannelViewModel(repository)

        viewModel.startEdit(repository.channels.value.single())

        val form = viewModel.form()
        assertTrue(form.tiersExpanded)
        assertEquals(2, form.tiers.size)
        assertEquals("50", form.tiers[0].upToUnitPriceText)
        assertEquals("14", form.tiers[0].feeRatePercentText)
    }

    @Test
    fun removingTheLastTierPromotesThePreviousOneAndWarnsAboutItsLimit() {
        val viewModel = newForm()
        viewModel.addTierRow()
        viewModel.addTierRow()
        val (first, second, last) = viewModel.form().tiers
        viewModel.updateTierRow(first.id) { it.copy(upToUnitPriceText = "50", feeRatePercentText = "14") }
        viewModel.updateTierRow(second.id) { it.copy(upToUnitPriceText = "100", feeRatePercentText = "16") }

        viewModel.removeTierRow(last.id)

        val form = viewModel.form()
        assertEquals(2, form.tiers.size)
        assertEquals("", form.tiers.last().upToUnitPriceText)
        assertEquals("16", form.tiers.last().feeRatePercentText)
        assertEquals(uiText(Res.string.settings_channels_tier_promoted, "100"), form.tierNotice)
    }

    @Test
    fun removingAMiddleTierKeepsTheOthersWithoutNotice() {
        val viewModel = newForm()
        viewModel.addTierRow()
        viewModel.addTierRow()
        val middle = viewModel.form().tiers[1]

        viewModel.removeTierRow(middle.id)

        assertEquals(2, viewModel.form().tiers.size)
        assertEquals(null, viewModel.form().tierNotice)
    }

    @Test
    fun deletingAsksFirstAndOnlyAnActiveChannelCanBeArchivedInstead() {
        val repository = SalesChannelRepository()
        repository.add(SalesChannel(id = "a", name = "Shopee", feeRate = 0.2))
        repository.add(SalesChannel(id = "b", name = "Pix", feeRate = 0.0))
        val viewModel = SalesChannelViewModel(repository)

        viewModel.requestDelete("a")

        val pending = assertNotNull(viewModel.uiState.value.pendingDelete)
        assertEquals("a", pending.id)
        assertTrue(pending.canArchive)
        viewModel.delete("a")
        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(listOf("b"), repository.channels.value.map { it.id })
    }

    @Test
    fun archivedChannelsGoToTheirOwnListAndFollowTheRepository() {
        val repository = SalesChannelRepository()
        repository.add(SalesChannel(id = "a", name = "Shopee", feeRate = 0.2))
        repository.add(SalesChannel(id = "b", name = "Pix", feeRate = 0.0))
        val viewModel = SalesChannelViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        assertEquals(listOf("a", "b"), viewModel.uiState.value.active.map { it.id })

        viewModel.setArchived("b", true)

        assertEquals(listOf("a"), viewModel.uiState.value.active.map { it.id })
        assertEquals(listOf("b"), viewModel.uiState.value.archived.map { it.id })
        assertFalse(viewModel.uiState.value.isEmpty)
    }
}
