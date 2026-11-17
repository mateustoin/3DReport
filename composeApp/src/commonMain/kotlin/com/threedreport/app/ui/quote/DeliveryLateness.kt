package com.threedreport.app.ui.quote

/**
 * Quantos dias o prazo [deadlineEpochDay] já passou em [todayEpochDay], ou 0 sem prazo ou com o prazo em dia.
 * A conta de "atrasado há N dias" do painel e do formulário de prazo, feita nos ViewModels e não nas telas.
 */
fun deliveryDaysLate(deadlineEpochDay: Long?, todayEpochDay: Long): Int =
    if (deadlineEpochDay == null) 0 else (todayEpochDay - deadlineEpochDay).coerceAtLeast(0).toInt()

/** Com o prazo a esta distância (em dias, de 0 a este valor) o app sugere marcar a entrega urgente. */
const val RUSH_SUGGESTION_MAX_DAYS: Long = 2

/**
 * Se a tela sugere marcar "Entrega urgente": só num pedido (produto não tem prazo), com o acréscimo de urgência
 * configurado ([rushSurchargeRate] maior que zero), a urgência ainda desmarcada e o prazo hoje ou a até
 * [RUSH_SUGGESTION_MAX_DAYS] dias. Prazo vencido não sugere: já passou.
 */
fun shouldSuggestRush(deadlineEpochDay: Long?, todayEpochDay: Long, rush: Boolean, rushSurchargeRate: Double, isProduct: Boolean): Boolean =
    !isProduct && !rush && rushSurchargeRate > 0 && deadlineEpochDay != null && deadlineEpochDay - todayEpochDay in 0..RUSH_SUGGESTION_MAX_DAYS
