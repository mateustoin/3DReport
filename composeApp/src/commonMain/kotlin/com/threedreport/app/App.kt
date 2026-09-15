package com.threedreport.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrintJob
import com.threedreport.core.pricing.PricingCalculator

/**
 * Tela PROVISÓRIA: só comprova que o app compila, abre e usa o módulo `core`.
 * Será substituída pela UI aprovada (ver docs/decisions.md).
 */
@Composable
fun App() {
    val quote = remember { PricingCalculator.calculate(sampleJob, sampleSettings) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("3DReport", style = MaterialTheme.typography.headlineMedium)
                Text("Interface em definição. Exemplo calculado pelo módulo core:")
                Text("Produção: ${quote.productionCost.toBrl()}  ·  Venda: ${quote.salePrice.toBrl()}")
            }
        }
    }
}

private fun Double.toBrl(): String {
    val cents = kotlin.math.round(this * 100).toLong()
    return "R$ ${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
}

private val sampleJob = PrintJob(
    filament = Filament(name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
    filamentLengthMeters = 12.0,
    printTimeMinutes = 190.0,
)

private val sampleSettings = PricingSettings(
    energyPricePerKwh = 1.23,
    printerPowerWatts = 380.0,
    maintenanceCostPerHour = 0.17,
    failureRate = 0.10,
    finishingRate = 0.10,
    machineInvestment = MachineInvestment(
        machinePrice = 2700.0,
        paybackMonths = 12,
        printingDaysPerMonth = 25,
        printingHoursPerDay = 16.0,
    ),
    profitMargin = 1.0,
)
