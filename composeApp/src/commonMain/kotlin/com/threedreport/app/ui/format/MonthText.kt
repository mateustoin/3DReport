package com.threedreport.app.ui.format

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.month_short_01
import _3dreport.composeapp.generated.resources.month_short_02
import _3dreport.composeapp.generated.resources.month_short_03
import _3dreport.composeapp.generated.resources.month_short_04
import _3dreport.composeapp.generated.resources.month_short_05
import _3dreport.composeapp.generated.resources.month_short_06
import _3dreport.composeapp.generated.resources.month_short_07
import _3dreport.composeapp.generated.resources.month_short_08
import _3dreport.composeapp.generated.resources.month_short_09
import _3dreport.composeapp.generated.resources.month_short_10
import _3dreport.composeapp.generated.resources.month_short_11
import _3dreport.composeapp.generated.resources.month_short_12
import _3dreport.composeapp.generated.resources.month_year_short
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val SHORT_MONTHS: List<StringResource> = listOf(
    Res.string.month_short_01, Res.string.month_short_02, Res.string.month_short_03, Res.string.month_short_04,
    Res.string.month_short_05, Res.string.month_short_06, Res.string.month_short_07, Res.string.month_short_08,
    Res.string.month_short_09, Res.string.month_short_10, Res.string.month_short_11, Res.string.month_short_12,
)

/** Mês e ano abreviados no idioma do app: "nov/2026" em português, "Nov 2026" em inglês. [month] vai de 1 a 12. */
@Composable
fun shortMonthYearText(year: Int, month: Int): String =
    stringResource(Res.string.month_year_short, stringResource(SHORT_MONTHS[month.coerceIn(1, 12) - 1]), year)
