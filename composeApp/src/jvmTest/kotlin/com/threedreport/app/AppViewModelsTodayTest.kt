package com.threedreport.app

import com.threedreport.app.data.Clock
import com.threedreport.app.data.LocalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

/** O dia injetado em [AppViewModels] chega a todas as telas, inclusive ao atraso do Orçamento. */
class AppViewModelsTodayTest {
    @Test
    fun theInjectedDayReachesTheQuoteScreen() {
        val storage = LocalStorage(createTempDirectory("3dreport-test").toFile(), Clock.System)
        val container = AppContainer(
            filaments = storage.filaments(),
            printers = storage.printers(),
            services = storage.services(),
            salesChannels = storage.salesChannels(),
            consumables = storage.consumables(),
            templates = storage.templates(),
            clients = storage.clients(),
            maintenance = storage.maintenance(),
            quoteHistory = storage.quoteHistory(),
            settings = storage.settings(),
            branding = storage.branding(),
            theme = storage.theme(),
            currency = storage.currency(),
            onboarding = storage.onboarding(),
            preferences = storage.preferences(),
            backup = storage.backup(),
            storageHealth = storage.health,
            releases = { null },
        )
        val models = AppViewModels(container, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined), showOrders = {}, today = { DAY })

        models.quoteViewModel.setDeliveryDate(DAY - 47)

        assertEquals(DAY, models.quoteViewModel.uiState.value.today)
        assertEquals(47, models.quoteViewModel.uiState.value.deliveryDaysLate)
    }

    private companion object {
        const val DAY = 20_722L
    }
}
