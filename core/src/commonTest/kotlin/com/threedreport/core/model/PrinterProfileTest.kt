package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterProfileTest {
    private fun printer(maintenance: Double, price: Double) = PrinterProfile(
        id = "p1",
        name = "Impressora",
        printerPowerWatts = 350.0,
        maintenanceCostPerHour = maintenance,
        machineInvestment = MachineInvestment(machinePrice = price, paybackMonths = 12, printingDaysPerMonth = 24, printingHoursPerDay = 10.0),
    )

    @Test
    fun `custo por hora da maquina soma o retorno do investimento e a manutencao`() {
        // 2.880 ÷ (12 · 24 · 10) = 1,00 de retorno, mais 0,15 de manutenção.
        assertEquals(1.15, printer(maintenance = 0.15, price = 2880.0).machineCostPerHour, 1e-9)
    }

    @Test
    fun `maquina ja paga e sem manutencao custa zero por hora`() {
        assertEquals(0.0, printer(maintenance = 0.0, price = 0.0).machineCostPerHour, 1e-9)
    }
}
