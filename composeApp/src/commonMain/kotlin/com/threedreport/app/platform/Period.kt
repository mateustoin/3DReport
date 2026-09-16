package com.threedreport.app.platform

/** Atalho de período, usado no filtro do Histórico e no recorte do Dashboard. */
enum class PeriodPreset(val label: String) {
    ALL("Tudo"),
    LAST_7_DAYS("7 dias"),
    LAST_30_DAYS("30 dias"),
    THIS_MONTH("Este mês"),
}

/** Início (epoch millis) de [preset]; `null` para [PeriodPreset.ALL] (sem limite inferior). */
expect fun periodStartEpochMillis(preset: PeriodPreset): Long?
