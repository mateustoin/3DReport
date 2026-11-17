package com.threedreport.app.data

import com.threedreport.core.model.Filament
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.PrinterProfile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * O que quem abre o app pela primeira vez já encontra. Os ids são aleatórios, como os de qualquer
 * cadastro feito na tela (decisão 106): ids fixos ("default-pla") seriam iguais em todo computador e
 * colidiriam numa sincronização. O armazenamento grava o seed na primeira abertura, pra o id continuar o
 * mesmo depois.
 */
@OptIn(ExperimentalUuidApi::class)
object Seeds {

    fun filaments(): List<Filament> = listOf(
        Filament(id = Uuid.random().toString(), name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24, materialType = "PLA"),
        Filament(id = Uuid.random().toString(), name = "ABS", pricePerKg = 90.0, densityGPerCm3 = 1.04, materialType = "ABS"),
        Filament(id = Uuid.random().toString(), name = "PETG", pricePerKg = 110.0, densityGPerCm3 = 1.27, materialType = "PETG"),
    )

    /**
     * A impressora de exemplo. O nome é texto que a pessoa lê e edita, então vem de recurso (`seed_printer_name`) e é
     * gravado em disco na primeira abertura, no idioma de então; quem já tem dados não é afetado. Os nomes dos
     * filamentos de [filaments] são nomes de material (PLA, ABS, PETG), iguais em qualquer idioma.
     */
    fun printers(name: String): List<PrinterProfile> = listOf(
        PrinterProfile(
            id = Uuid.random().toString(),
            name = name,
            printerPowerWatts = 380.0,
            maintenanceCostPerHour = 0.17,
            machineInvestment = MachineInvestment(
                machinePrice = 2700.0,
                paybackMonths = 12,
                printingDaysPerMonth = 25,
                printingHoursPerDay = 16.0,
            ),
        ),
    )

    val settings = PricingSettings(
        energyPricePerKwh = 1.23,
        failureRate = 0.10,
        finishingRate = 0.10,
        administrativeCost = 0.0,
        profitMargin = 1.0,
    )
}
