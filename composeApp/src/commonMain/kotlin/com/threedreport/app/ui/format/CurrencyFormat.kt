package com.threedreport.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.threedreport.core.model.Currency

/** Moeda em uso na árvore de composição atual — ver [com.threedreport.app.ui.settings.CurrencyViewModel]. */
val LocalCurrency = compositionLocalOf { Currency.BRL }

/** Como [toCurrencyText], usando a moeda configurada em [LocalCurrency]. */
@Composable
fun Double.toMoney(): String = toCurrencyText(LocalCurrency.current)
