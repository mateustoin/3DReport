package com.threedreport.app.ui.printers

import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.MachineInvestment
import com.threedreport.core.model.PrinterProfile

/**
 * Rascunho do formulário de perfil de impressora. `id == null` significa que
 * é um perfil novo (ainda não salvo); caso contrário, é uma edição. Cada erro fica no campo a que se refere.
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
    /** Separador decimal da moeda em uso, o mesmo do campo: a prévia lê os números com ele. */
    val decimalSeparator: Char = ',',
    val nameError: UiText? = null,
    val powerError: UiText? = null,
    val maintenanceError: UiText? = null,
    val priceError: UiText? = null,
    val paybackError: UiText? = null,
    val daysError: UiText? = null,
    val hoursError: UiText? = null,
) {
    /**
     * Quanto a máquina custa por hora de impressão, sem a energia: retorno do investimento mais
     * manutenção. `null` enquanto algum campo não é número. É a prévia do formulário (decisão 108), pra o
     * efeito de "12 meses" ou "16 h por dia" aparecer antes de salvar.
     */
    val machineCostPerHour: Double?
        get() {
            val price = parseDecimal(machinePriceText, NumberKind.AMOUNT, decimalSeparator)?.takeIf { it >= 0 } ?: return null
            val months = parseWholeNumber(paybackMonthsText)?.takeIf { it > 0 } ?: return null
            val days = parseWholeNumber(printingDaysPerMonthText)?.takeIf { it > 0 } ?: return null
            val hours = parseDecimal(printingHoursPerDayText, NumberKind.MEASURE, decimalSeparator)?.takeIf { it > 0 } ?: return null
            val maintenance = parseDecimal(maintenanceCostPerHourText, NumberKind.AMOUNT, decimalSeparator) ?: 0.0
            // A conta é do core: a mesma que a lista mostra depois de salvar.
            return MachineInvestment(price, months, days, hours).costPerHour + maintenance
        }

    companion object {
        /**
         * Formulário de uma impressora nova, com o uso típico já preenchido (os mesmos valores da impressora
         * de exemplo): campo vazio era um erro de "não é um número" na cara de quem só quer cadastrar.
         */
        fun new(name: String = "", powerWatts: Double? = null, fromPreset: Boolean = false, decimalSeparator: Char = ',') = PrinterFormState(
            name = name,
            printerPowerWattsText = powerWatts?.toInputText(decimalSeparator).orEmpty(),
            maintenanceCostPerHourText = 0.17.toInputText(decimalSeparator),
            paybackMonthsText = "12",
            printingDaysPerMonthText = "25",
            printingHoursPerDayText = "16",
            powerFromPreset = fromPreset,
            decimalSeparator = decimalSeparator,
        )
    }
}

internal fun PrinterProfile.toFormState(decimalSeparator: Char = ',') = PrinterFormState(
    id = id,
    name = name,
    printerPowerWattsText = printerPowerWatts.toInputText(decimalSeparator),
    maintenanceCostPerHourText = maintenanceCostPerHour.toInputText(decimalSeparator),
    machinePriceText = machineInvestment.machinePrice.toInputText(decimalSeparator),
    paybackMonthsText = machineInvestment.paybackMonths.toString(),
    printingDaysPerMonthText = machineInvestment.printingDaysPerMonth.toString(),
    printingHoursPerDayText = machineInvestment.printingHoursPerDay.toInputText(decimalSeparator),
    decimalSeparator = decimalSeparator,
)
