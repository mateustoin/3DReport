package com.threedreport.core.pricing

import com.threedreport.core.model.ChannelFeeSchedule
import com.threedreport.core.model.ChannelFeeTier
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.SalesChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** O erro que a tela explica chega com tipo e com os percentuais, não com frase (decisão 130). */
class PricingErrorTest {

    private val pla = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24)
    private val printer = PrinterProfile(
        id = "printer",
        name = "Impressora",
        printerPowerWatts = 380.0,
        maintenanceCostPerHour = 0.17,
        machineInvestment = MachineInvestment(machinePrice = 2700.0, paybackMonths = 12, printingDaysPerMonth = 25, printingHoursPerDay = 16.0),
    )
    private val job = PrintJob(filament = pla, filamentLengthMeters = 12.0, printTimeMinutes = 190.0)

    @Test
    fun deductionsReachingTheWholeSaleCarryTheRates() {
        val error = assertFailsWith<PricingError.DeductionsReachFullPrice> {
            ChannelFeeSchedule(feeRate = 0.5).priceLeaving(receipt = 10.0, extras = 0.0, quantity = 1, taxRate = 0.5)
        }
        assertEquals(0.5, error.channelFeeRate)
        assertEquals(0.5, error.taxRate)
    }

    @Test
    fun withTiersTheErrorCarriesTheLowestChannelRate() {
        val schedule = ChannelFeeSchedule(
            tiers = listOf(
                ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.9),
                ChannelFeeTier(upToUnitPrice = null, feeRate = 0.8),
            ),
        )
        val error = assertFailsWith<PricingError.DeductionsReachFullPrice> {
            schedule.priceLeaving(receipt = 10.0, extras = 0.0, quantity = 1, taxRate = 0.2)
        }
        assertEquals(0.8, error.channelFeeRate)
        assertEquals(0.2, error.taxRate)
    }

    @Test
    fun theCalculatorThrowsTheTypedError() {
        val error = assertFailsWith<PricingError> {
            PricingCalculator.calculate(
                job,
                printer,
                PricingSettings(energyPricePerKwh = 1.23, failureRate = 0.10, finishingRate = 0.10, taxRate = 0.06, profitMargin = 1.0),
                channel = SalesChannel(id = "x", name = "Canal impossível", feeRate = 0.95),
            )
        }
        val deductions = assertIs<PricingError.DeductionsReachFullPrice>(error)
        assertEquals(0.95, deductions.channelFeeRate)
        assertEquals(0.06, deductions.taxRate)
    }

    @Test
    fun theTypedErrorIsStillAnIllegalArgument() {
        // Quem já tratava IllegalArgumentException (os testes antigos, o histórico) continua tratando.
        assertIs<IllegalArgumentException>(PricingError.DeductionsReachFullPrice(channelFeeRate = 0.6, taxRate = 0.4))
    }
}
