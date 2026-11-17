package com.threedreport.app.ui.printers

import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.parseWholeNumber
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.format.toMoneyInputText
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
    /** O formulário nasceu de uma impressora da lista: mostra o aviso de que os valores são de referência. */
    val fromPreset: Boolean = false,
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
     * efeito de "12 meses" ou "12 h por dia" aparecer antes de salvar.
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
        /** Manutenção de uma impressora em branco, em reais por hora. */
        const val BLANK_MAINTENANCE_PER_HOUR = 0.17

        /**
         * Formulário de uma impressora nova em branco, com o uso típico já preenchido ([PrinterUsageDefaults]):
         * campo vazio era um erro de "não é um número" na cara de quem só quer cadastrar.
         */
        fun new(decimalSeparator: Char = ',') = PrinterFormState(
            maintenanceCostPerHourText = BLANK_MAINTENANCE_PER_HOUR.toInputText(decimalSeparator),
            paybackMonthsText = PrinterUsageDefaults.PAYBACK_MONTHS.toString(),
            printingDaysPerMonthText = PrinterUsageDefaults.DAYS_PER_MONTH.toString(),
            printingHoursPerDayText = PrinterUsageDefaults.HOURS_PER_DAY.toInputText(decimalSeparator),
            decimalSeparator = decimalSeparator,
        )

        /**
         * Formulário de uma impressora da lista: nome, consumo médio, manutenção da faixa e, quando há fonte, o
         * preço de referência (sem fonte, o campo fica vazio); o uso é o típico.
         */
        fun fromPreset(preset: PrinterPreset, decimalSeparator: Char = ',') = new(decimalSeparator).copy(
            name = preset.displayName,
            printerPowerWattsText = preset.averagePowerWatts.toInputText(decimalSeparator),
            maintenanceCostPerHourText = preset.maintenance.costPerHour.toInputText(decimalSeparator),
            machinePriceText = preset.referencePrice?.toMoneyInputText(decimalSeparator).orEmpty(),
            fromPreset = true,
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
