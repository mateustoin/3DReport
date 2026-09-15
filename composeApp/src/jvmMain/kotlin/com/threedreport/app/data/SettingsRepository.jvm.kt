package com.threedreport.app.data

import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private val DEFAULT_SETTINGS = PricingSettings(
    energyPricePerKwh = 1.23,
    failureRate = 0.10,
    finishingRate = 0.10,
    administrativeCost = 0.0,
    profitMargin = 1.0,
)

actual class SettingsRepository actual constructor() {
    private val file = File(appDataDir(), "settings.json")
    private val state = MutableStateFlow(readJsonFile(file, DEFAULT_SETTINGS))

    actual val settings: StateFlow<PricingSettings> = state.asStateFlow()

    actual fun update(settings: PricingSettings) {
        state.value = settings
        writeJsonFile(file, settings)
    }
}
