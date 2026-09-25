package com.threedreport.app.ui.printers

import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toInputText
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
    /** O consumo veio de um preset, que traz a potência máxima da fonte (ver aviso no campo). */
    val powerFromPreset: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Quanto a máquina custa por hora de impressão, sem a energia: retorno do investimento mais
     * manutenção. `null` enquanto algum campo não é número. É a prévia do formulário (decisão 108), pra o
     * efeito de "12 meses" ou "16 h por dia" aparecer antes de salvar.
     */
    val machineCostPerHour: Double?
        get() {
            val price = parseDecimal(machinePriceText) ?: return null
            val months = parseWholeNumber(paybackMonthsText)?.takeIf { it > 0 } ?: return null
            val days = parseWholeNumber(printingDaysPerMonthText)?.takeIf { it > 0 } ?: return null
            val hours = parseDecimal(printingHoursPerDayText, NumberKind.MEASURE)?.takeIf { it > 0 } ?: return null
            val maintenance = parseDecimal(maintenanceCostPerHourText) ?: 0.0
            return price / (months * days * hours) + maintenance
        }

    companion object {
        /**
         * Formulário de uma impressora nova, com o uso típico já preenchido (os mesmos valores da impressora
         * de exemplo): campo vazio era um erro de "não é um número" na cara de quem só quer cadastrar.
         */
        fun new(name: String = "", powerWatts: Double? = null, fromPreset: Boolean = false) = PrinterFormState(
            name = name,
            printerPowerWattsText = powerWatts?.toInputText().orEmpty(),
            maintenanceCostPerHourText = 0.17.toInputText(),
            paybackMonthsText = "12",
            printingDaysPerMonthText = "25",
            printingHoursPerDayText = "16",
            powerFromPreset = fromPreset,
        )
    }
}

internal fun PrinterProfile.toFormState() = PrinterFormState(
    id = id,
    name = name,
    printerPowerWattsText = printerPowerWatts.toInputText(),
    maintenanceCostPerHourText = maintenanceCostPerHour.toInputText(),
    machinePriceText = machineInvestment.machinePrice.toInputText(),
    paybackMonthsText = machineInvestment.paybackMonths.toString(),
    printingDaysPerMonthText = machineInvestment.printingDaysPerMonth.toString(),
    printingHoursPerDayText = machineInvestment.printingHoursPerDay.toInputText(),
)
