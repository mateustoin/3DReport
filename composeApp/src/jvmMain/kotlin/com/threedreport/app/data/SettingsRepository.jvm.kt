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
    schemaVersion = PricingSettings.CURRENT_SCHEMA_VERSION,
    administrativeCost = 0.0,
    profitMargin = 1.0,
)

actual class SettingsRepository actual constructor() {
    private val file = File(appDataDir(), "settings.json")
    private val state = MutableStateFlow(loadMigrated())

    actual val settings: StateFlow<PricingSettings> = state.asStateFlow()

    /** Lê o arquivo e, se ele era de uma versão anterior, grava já convertido (ver [PricingSettings.migrated]). */
    private fun loadMigrated(): PricingSettings {
        val stored = readJsonFile(file, DEFAULT_SETTINGS)
        val migrated = stored.migrated()
        if (migrated != stored) writeJsonFile(file, migrated)
        return migrated
    }

    actual fun update(settings: PricingSettings) {
        state.value = settings
        writeJsonFile(file, settings)
    }
}
