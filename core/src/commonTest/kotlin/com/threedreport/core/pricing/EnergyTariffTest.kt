package com.threedreport.core.pricing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnergyTariffTest {

    @Test
    fun theBillGivesTheRealPricePerKwh() {
        assertEquals(0.92, EnergyTariff.fromBill(billTotal = 230.0, consumedKwh = 250.0)!!, 1e-9)
        assertNull(EnergyTariff.fromBill(billTotal = 230.0, consumedKwh = 0.0))
    }

    @Test
    fun theFlagAddsToTheTariff() {
        assertEquals(0.84, EnergyTariff.withFlag(tariffPerKwh = 0.80, flagSurchargePerKwh = 0.04), 1e-9)
    }

    @Test
    fun theWhiteTariffIsWeightedByThePrintingHours() {
        // 70% das horas fora de ponta a R$ 0,60, 20% no intermediário a R$ 0,90, 10% na ponta a R$ 1,40.
        val periods = listOf(
            EnergyTariff.Period(pricePerKwh = 0.60, share = 70.0),
            EnergyTariff.Period(pricePerKwh = 0.90, share = 20.0),
            EnergyTariff.Period(pricePerKwh = 1.40, share = 10.0),
        )
        assertEquals(0.42 + 0.18 + 0.14, EnergyTariff.weightedAverage(periods)!!, 1e-9)
        assertNull(EnergyTariff.weightedAverage(listOf(EnergyTariff.Period(1.0, 0.0))))
    }
}
