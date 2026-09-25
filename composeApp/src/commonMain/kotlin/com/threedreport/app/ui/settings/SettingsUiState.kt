package com.threedreport.app.ui.settings

/**
 * Estado da tela de Configurações gerais (parâmetros do negócio, não
 * específicos de uma impressora — ver [com.threedreport.core.model.PrinterProfile]).
 *
 * Todos os campos ficam como texto (entrada livre do usuário) até o
 * [SettingsViewModel] validar e salvar no repositório compartilhado.
 */
data class SettingsUiState(
    val energyPricePerKwhText: String = "",
    val failureRatePercentText: String = "",
    val finishingRatePercentText: String = "",
    val laborRatePerHourText: String = "",
    val monthlyFixedCostText: String = "",
    val productiveHoursPerMonthText: String = "",
    val administrativeCostText: String = "",
    val profitMarginPercentText: String = "",
    val taxRatePercentText: String = "",
    val errorMessage: String? = null,
    val savedConfirmation: Boolean = false,
    /** Aviso depois de salvar, quando algo gravado não vai ter o efeito que a pessoa espera. */
    val warningMessage: String? = null,
)
