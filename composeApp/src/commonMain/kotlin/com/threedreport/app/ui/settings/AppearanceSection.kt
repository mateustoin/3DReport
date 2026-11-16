package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_appearance_currency
import _3dreport.composeapp.generated.resources.settings_appearance_currency_help
import _3dreport.composeapp.generated.resources.settings_appearance_currency_help_detail
import _3dreport.composeapp.generated.resources.settings_appearance_theme
import _3dreport.composeapp.generated.resources.settings_appearance_theme_help
import _3dreport.composeapp.generated.resources.settings_theme_dark
import _3dreport.composeapp.generated.resources.settings_theme_light
import _3dreport.composeapp.generated.resources.settings_theme_system
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDropdownField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.theme.ThemeViewModel
import com.threedreport.core.model.Currency
import com.threedreport.core.model.ThemeMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A seção Aparência: tema e moeda. Os dois valem na hora, sem o "Salvar" da barra. */
@Composable
internal fun AppearanceSection(themeViewModel: ThemeViewModel, currencyViewModel: CurrencyViewModel) {
    val themeMode by themeViewModel.mode.collectAsState()
    val currency by currencyViewModel.currency.collectAsState()
    val modes = ThemeMode.entries

    FormGrid {
        cell(1) {
            AppChoiceField(
                label = stringResource(Res.string.settings_appearance_theme),
                options = modes.map { stringResource(it.labelRes()) },
                selectedIndex = modes.indexOf(themeMode),
                onSelect = { themeViewModel.setMode(modes[it]) },
                help = HelpText(stringResource(Res.string.settings_appearance_theme_help)),
            )
        }
        cell(1) {
            AppDropdownField(
                label = stringResource(Res.string.settings_appearance_currency),
                items = Currency.entries,
                selected = currency,
                itemLabel = { "${it.code} (${it.symbol})" },
                displayText = { "${it.code} (${it.symbol})" },
                onSelect = currencyViewModel::setCurrency,
                help = HelpText(
                    stringResource(Res.string.settings_appearance_currency_help),
                    stringResource(Res.string.settings_appearance_currency_help_detail),
                ),
            )
        }
    }
}

private fun ThemeMode.labelRes(): StringResource = when (this) {
    ThemeMode.LIGHT -> Res.string.settings_theme_light
    ThemeMode.DARK -> Res.string.settings_theme_dark
    ThemeMode.SYSTEM -> Res.string.settings_theme_system
}
