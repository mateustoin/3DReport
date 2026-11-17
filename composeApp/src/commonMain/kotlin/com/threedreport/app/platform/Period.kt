package com.threedreport.app.platform

/**
 * Atalho de período, usado no filtro do Histórico e no recorte do Dashboard. O nome na tela vem de recurso
 * (`PeriodPreset.labelRes()` e `dashboardLabelRes()`), não daqui.
 */
enum class PeriodPreset {
    ALL,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,

    /** O mês fechado anterior: é o que se compara no fechamento do mês (decisão 106). */
    LAST_MONTH,
    THIS_YEAR,
}

/** Início (epoch millis) de [preset]; `null` para [PeriodPreset.ALL] (sem limite inferior). */
expect fun periodStartEpochMillis(preset: PeriodPreset): Long?

/** Fim (epoch millis, exclusivo) de [preset]; `null` quando o período vai até agora. */
expect fun periodEndEpochMillis(preset: PeriodPreset): Long?

/** Se [epochMillis] cai dentro de [preset]. */
fun PeriodPreset.contains(epochMillis: Long): Boolean {
    val start = periodStartEpochMillis(this)
    val end = periodEndEpochMillis(this)
    return (start == null || epochMillis >= start) && (end == null || epochMillis < end)
}
