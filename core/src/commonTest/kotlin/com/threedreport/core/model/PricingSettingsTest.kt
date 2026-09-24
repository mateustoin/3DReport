package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Conversão das configurações salvas por versões anteriores do app (ver [PricingSettings.migrated]). */
class PricingSettingsTest {

    private val v1 = PricingSettings(energyPricePerKwh = 1.0, failureRate = 0.1, finishingRate = 0.1, profitMargin = 1.0)

    @Test
    fun oldSettingsWithAnHourlyRateLoseTheFinishingPercentageOnce() {
        // Na versão 1, a hora configurada já zerava o acabamento no cálculo: o preço fica igual.
        val migrated = v1.copy(laborRatePerHour = 30.0).migrated()

        assertEquals(0.0, migrated.finishingRate)
        assertEquals(PricingSettings.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
    }

    @Test
    fun oldSettingsWithoutAnHourlyRateKeepTheFinishingPercentage() {
        val migrated = v1.migrated()

        assertEquals(0.1, migrated.finishingRate)
        assertEquals(PricingSettings.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
    }

    @Test
    fun currentSettingsAreNeverTouched() {
        val current = v1.copy(laborRatePerHour = 30.0, schemaVersion = PricingSettings.CURRENT_SCHEMA_VERSION)

        assertEquals(current, current.migrated())
    }
}
