package com.threedreport.app.ui.settings

/**
 * Estado da tela de Configurações.
 *
 * Todos os campos ficam como texto (entrada livre do usuário) até o
 * [SettingsViewModel] validar e salvar no repositório compartilhado.
 */
data class SettingsUiState(
    val energyPricePerKwhText: String = "",
    val printerPowerWattsText: String = "",
    val maintenanceCostPerHourText: String = "",
    val failureRatePercentText: String = "",
    val finishingRatePercentText: String = "",
    val administrativeCostText: String = "",
    val machinePriceText: String = "",
    val paybackMonthsText: String = "",
    val printingDaysPerMonthText: String = "",
    val printingHoursPerDayText: String = "",
    val profitMarginPercentText: String = "",
    val errorMessage: String? = null,
    val savedConfirmation: Boolean = false,
)
