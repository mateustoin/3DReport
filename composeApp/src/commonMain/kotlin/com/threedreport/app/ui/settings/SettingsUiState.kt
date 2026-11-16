package com.threedreport.app.ui.settings

import com.threedreport.app.ui.text.UiText

/** Os campos de custo que a validação do [SettingsViewModel] pode apontar. */
enum class SettingsField {
    ENERGY,
    LABOR,
    MONTHLY_FIXED_COST,
    PRODUCTIVE_HOURS,
    FAILURE_RATE,
    FINISHING_RATE,
    ADMINISTRATIVE_COST,
    MARGIN,
    TAX,
    MINIMUM_PRICE,
    RUSH,
}

/**
 * Estado da seção Negócio de Configurações (parâmetros do negócio, não específicos de uma impressora, ver
 * [com.threedreport.core.model.PrinterProfile]).
 *
 * Todos os campos ficam como texto (entrada livre do usuário) até o [SettingsViewModel] validar e salvar no
 * repositório compartilhado. [errors] diz o que corrigir em cada campo; [fixedCostWithoutHours] é o aviso de
 * depois de salvar, quando algo gravado não vai ter o efeito que a pessoa espera; [hasUnsavedChanges] é se o
 * rascunho difere do que está gravado.
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
    val minimumOrderPriceText: String = "",
    val rushSurchargeRatePercentText: String = "",
    val errors: Map<SettingsField, UiText> = emptyMap(),
    val fixedCostWithoutHours: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)
