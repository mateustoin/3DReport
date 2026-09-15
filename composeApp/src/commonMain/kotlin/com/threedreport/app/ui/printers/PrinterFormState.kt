package com.threedreport.app.ui.printers

import com.threedreport.core.model.PrinterProfile

/**
 * Rascunho do formulário de perfil de impressora. `id == null` significa que
 * é um perfil novo (ainda não salvo); caso contrário, é uma edição.
 */
data class PrinterFormState(
    val id: String? = null,
    val name: String = "",
    val printerPowerWattsText: String = "",
    val maintenanceCostPerHourText: String = "",
    val machinePriceText: String = "",
    val paybackMonthsText: String = "",
    val printingDaysPerMonthText: String = "",
    val printingHoursPerDayText: String = "",
    val errorMessage: String? = null,
)

internal fun PrinterProfile.toFormState() = PrinterFormState(
    id = id,
    name = name,
    printerPowerWattsText = printerPowerWatts.toString(),
    maintenanceCostPerHourText = maintenanceCostPerHour.toString(),
    machinePriceText = machineInvestment.machinePrice.toString(),
    paybackMonthsText = machineInvestment.paybackMonths.toString(),
    printingDaysPerMonthText = machineInvestment.printingDaysPerMonth.toString(),
    printingHoursPerDayText = machineInvestment.printingHoursPerDay.toString(),
)
