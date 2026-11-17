package com.threedreport.app.ui.dashboard

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.dashboard_period_all
import _3dreport.composeapp.generated.resources.dashboard_period_last_30_days
import _3dreport.composeapp.generated.resources.dashboard_period_last_7_days
import _3dreport.composeapp.generated.resources.dashboard_period_last_month
import _3dreport.composeapp.generated.resources.dashboard_period_this_month
import _3dreport.composeapp.generated.resources.dashboard_period_this_year
import com.threedreport.app.platform.PeriodPreset
import org.jetbrains.compose.resources.StringResource

/** Os períodos do seletor, na ordem em que aparecem (o "Tudo" por último). */
val DashboardPeriods: List<PeriodPreset> = listOf(
    PeriodPreset.LAST_7_DAYS,
    PeriodPreset.LAST_30_DAYS,
    PeriodPreset.THIS_MONTH,
    PeriodPreset.LAST_MONTH,
    PeriodPreset.THIS_YEAR,
    PeriodPreset.ALL,
)

/** O nome curto do período, pro seletor do Dashboard. */
fun PeriodPreset.dashboardLabelRes(): StringResource = when (this) {
    PeriodPreset.ALL -> Res.string.dashboard_period_all
    PeriodPreset.LAST_7_DAYS -> Res.string.dashboard_period_last_7_days
    PeriodPreset.LAST_30_DAYS -> Res.string.dashboard_period_last_30_days
    PeriodPreset.THIS_MONTH -> Res.string.dashboard_period_this_month
    PeriodPreset.LAST_MONTH -> Res.string.dashboard_period_last_month
    PeriodPreset.THIS_YEAR -> Res.string.dashboard_period_this_year
}
