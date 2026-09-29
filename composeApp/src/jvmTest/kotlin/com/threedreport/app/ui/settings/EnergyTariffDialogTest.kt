package com.threedreport.app.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A lógica pura da calculadora de kWh (decisão 126), sem Compose: ver [EnergyTariffDialog]. */
class EnergyTariffDialogTest {

    @Test
    fun billModeDividesTotalByConsumption() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.BILL, billTotalText = "92", billConsumedKwhText = "100")

        assertEquals(0.92, state.result()!!, 1e-9)
    }

    @Test
    fun billModeWithoutConsumptionHasNoResult() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.BILL, billTotalText = "92", billConsumedKwhText = "")

        assertNull(state.result())
    }

    @Test
    fun flagModeAddsTheSurchargeToTheTariff() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.FLAG, flagTariffText = "0,85", flagSurchargeText = "0,05")

        assertEquals(0.90, state.result()!!, 1e-9)
    }

    @Test
    fun flagModeWithoutSurchargeUsesJustTheTariff() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.FLAG, flagTariffText = "0,85", flagSurchargeText = "")

        assertEquals(0.85, state.result()!!, 1e-9)
    }

    @Test
    fun flagModeWithoutTariffHasNoResult() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.FLAG, flagTariffText = "", flagSurchargeText = "0,05")

        assertNull(state.result())
    }

    @Test
    fun whiteTariffWeighsEachPeriodByItsShare() {
        val state = EnergyTariffFormState(
            mode = EnergyTariffMode.WHITE,
            offPeakPriceText = "0,60",
            offPeakShareText = "80",
            midPeakPriceText = "0,80",
            midPeakShareText = "10",
            peakPriceText = "1,20",
            peakShareText = "10",
        )

        val result = state.result()!!
        assertEquals((0.60 * 80 + 0.80 * 10 + 1.20 * 10) / 100, result, 1e-9)
    }

    @Test
    fun whiteTariffWithAllSharesBlankHasNoResult() {
        val state = EnergyTariffFormState(mode = EnergyTariffMode.WHITE)

        assertNull(state.result())
    }

    @Test
    fun whiteTariffWithInvalidTextInOnePeriodHasNoResult() {
        val state = EnergyTariffFormState(
            mode = EnergyTariffMode.WHITE,
            offPeakPriceText = "abc",
            offPeakShareText = "80",
            midPeakShareText = "10",
            peakShareText = "10",
        )

        assertNull(state.result())
    }
}
