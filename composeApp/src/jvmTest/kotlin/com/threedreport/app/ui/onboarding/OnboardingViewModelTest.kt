package com.threedreport.app.ui.onboarding

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import com.threedreport.app.ui.printers.PRINTER_PRESETS
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.PricingSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingViewModelTest {

    private val settings = PricingSettings(
        energyPricePerKwh = 0.8,
        failureRate = 0.0,
        finishingRate = 0.0,
        laborRatePerHour = 0.0,
        monthlyFixedCost = 0.0,
        productiveHoursPerMonth = 0.0,
        administrativeCost = 0.0,
        profitMargin = 1.0,
        taxRate = 0.0,
        minimumOrderPrice = 0.0,
        rushSurchargeRate = 0.0,
    )

    @Test
    fun startsOnThePrinterStepWithTheCurrentEnergyAndMargin() {
        val state = OnboardingViewModel(settings).uiState.value

        assertEquals(OnboardingStep.PRINTER, state.step)
        assertEquals("0,8", state.energyText)
        assertEquals("100", state.marginText)
        assertTrue(state.canContinue)
    }

    @Test
    fun nextAndBackMoveBetweenTheTwoSteps() {
        val viewModel = OnboardingViewModel(settings)

        viewModel.next()
        assertEquals(OnboardingStep.COSTS, viewModel.uiState.value.step)
        assertEquals(2, viewModel.uiState.value.stepNumber)

        viewModel.back()
        assertEquals(OnboardingStep.PRINTER, viewModel.uiState.value.step)
    }

    @Test
    fun aMachinePriceThatIsNotANumberBlocksTheFirstStep() {
        val viewModel = OnboardingViewModel(settings)
        viewModel.setMachinePrice("muito")

        assertEquals(uiText(Res.string.common_error_number_invalid), viewModel.uiState.value.machinePriceError)
        assertFalse(viewModel.uiState.value.canContinue)
        viewModel.next()
        assertEquals(OnboardingStep.PRINTER, viewModel.uiState.value.step)
        assertNull(viewModel.finish())
    }

    @Test
    fun blankLaborAndMachinePriceAreFineButBlankEnergyIsNot() {
        val viewModel = OnboardingViewModel(settings)
        viewModel.next()

        viewModel.setLabor("")
        assertNull(viewModel.uiState.value.laborError)
        viewModel.setEnergy("")
        assertEquals(uiText(Res.string.common_error_number_invalid), viewModel.uiState.value.energyError)
        viewModel.setMargin("-5")
        assertEquals(uiText(Res.string.common_error_negative), viewModel.uiState.value.marginError)
        assertFalse(viewModel.uiState.value.canContinue)
    }

    @Test
    fun finishingBuildsTheSettingsAndThePrinterAnswer() {
        val viewModel = OnboardingViewModel(settings)
        val preset = PRINTER_PRESETS.first()
        viewModel.setPreset(preset)
        viewModel.setMachinePrice("2.700")
        viewModel.next()
        viewModel.setEnergy("0,95")
        viewModel.setLabor("30")
        viewModel.setMargin("150")

        val result = assertNotNull(viewModel.finish())

        assertEquals(0.95, result.settings.energyPricePerKwh, 1e-9)
        assertEquals(30.0, result.settings.laborRatePerHour, 1e-9)
        assertEquals(1.5, result.settings.profitMargin, 1e-9)
        assertEquals(OnboardingPrinter(preset, 2700.0), result.printer)
    }

    @Test
    fun withoutAPrinterAnswerNothingIsAppliedToThePrinter() {
        val viewModel = OnboardingViewModel(settings)
        viewModel.next()

        val result = assertNotNull(viewModel.finish())

        assertNull(result.printer)
        assertEquals(0.0, result.settings.laborRatePerHour)
        assertEquals(0.8, result.settings.energyPricePerKwh, 1e-9)
    }

    @Test
    fun energyAndMarginAreMeasuresSoADotIsAlwaysDecimalButTheMachinePriceKeepsThousands() {
        val viewModel = OnboardingViewModel(settings)
        viewModel.setMachinePrice("2.700")
        viewModel.next()
        viewModel.setEnergy("1.250")
        viewModel.setMargin("1.250")

        val result = assertNotNull(viewModel.finish())

        assertEquals(1.25, result.settings.energyPricePerKwh, 1e-9, "kWh com ponto é decimal, como em Configurações")
        assertEquals(0.0125, result.settings.profitMargin, 1e-9, "1.250 na margem é 1,25%, e não 1250%")
        assertEquals(2700.0, result.printer?.machinePrice)
    }

    @Test
    fun withADotCurrencyTheTextsAreShownAndReadWithTheDot() {
        val viewModel = OnboardingViewModel(settings, Currency.USD)
        assertEquals("0.8", viewModel.uiState.value.energyText)

        viewModel.setLabor("1,250")
        viewModel.setMachinePrice("2,700.50")
        viewModel.setEnergy("0.15")
        viewModel.next()
        val result = assertNotNull(viewModel.finish())

        assertEquals(1250.0, result.settings.laborRatePerHour, 1e-9)
        assertEquals(0.15, result.settings.energyPricePerKwh, 1e-9)
        assertEquals(2700.5, result.printer?.machinePrice)
    }

    @Test
    fun withACommaCurrencyAThousandsDotIsStillThousands() {
        val viewModel = OnboardingViewModel(settings, Currency.BRL)
        viewModel.setLabor("1.250")
        viewModel.setMachinePrice("2.700,50")

        val result = assertNotNull(viewModel.finish())

        assertEquals(1250.0, result.settings.laborRatePerHour, 1e-9)
        assertEquals(2700.5, result.printer?.machinePrice)
    }
}
