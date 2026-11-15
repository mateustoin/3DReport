package com.threedreport.app.ui.quote

/**
 * Quantos dias o prazo [deadlineEpochDay] já passou em [todayEpochDay], ou 0 sem prazo ou com o prazo em dia.
 * A conta de "atrasado há N dias" do painel e do formulário de prazo, feita nos ViewModels e não nas telas.
 */
fun deliveryDaysLate(deadlineEpochDay: Long?, todayEpochDay: Long): Int =
    if (deadlineEpochDay == null) 0 else (todayEpochDay - deadlineEpochDay).coerceAtLeast(0).toInt()
