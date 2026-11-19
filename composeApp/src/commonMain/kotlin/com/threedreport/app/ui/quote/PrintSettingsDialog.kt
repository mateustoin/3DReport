package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.quote_print_settings
import _3dreport.composeapp.generated.resources.quote_settings_help
import _3dreport.composeapp.generated.resources.quote_settings_infill
import _3dreport.composeapp.generated.resources.quote_settings_layer
import _3dreport.composeapp.generated.resources.quote_settings_pattern
import _3dreport.composeapp.generated.resources.quote_settings_pattern_placeholder
import _3dreport.composeapp.generated.resources.quote_settings_supports
import _3dreport.composeapp.generated.resources.quote_settings_supports_no
import _3dreport.composeapp.generated.resources.quote_settings_supports_unknown
import _3dreport.composeapp.generated.resources.quote_settings_supports_yes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppChoiceField
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.PrintSettings
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round

/** Arredonda pra 2 casas decimais e evita ".0"/"0" à toa, mesmo espírito de `QuoteViewModel.formatImportedNumber`. */
private fun Double.toSettingText(): String {
    val rounded = round(this * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

/**
 * Configurações de fatiamento (altura de camada, preenchimento, suporte) de uma peça, usado tanto
 * no cadastro do orçamento (preenche [initial] com o que veio do G-code, se houver, e o que o
 * criador digitar aqui fica no formulário de salvar) quanto no Histórico (edição rápida direto no
 * card salvo, sem precisar reabrir a edição completa do orçamento). Ver KDoc de [PrintSettings]
 * pro porquê desses dados existirem: só consulta/replicação futura, não afetam o cálculo do preço.
 */
@Composable
fun PrintSettingsDialog(initial: PrintSettings, onDismiss: () -> Unit, onSave: (PrintSettings) -> Unit) {
    var layerHeightText by remember { mutableStateOf(initial.layerHeightMm?.toSettingText().orEmpty()) }
    var infillText by remember { mutableStateOf(initial.infillPercentage?.toSettingText().orEmpty()) }
    var infillPattern by remember { mutableStateOf(initial.infillPattern.orEmpty()) }
    var supportsEnabled by remember { mutableStateOf(initial.supportsEnabled) }
    // A ordem das opções: Sim, Não, Não informado.
    val supportOptions = listOf(true, false, null)

    AppDialog(
        title = stringResource(Res.string.quote_print_settings),
        onDismiss = onDismiss,
        help = HelpText(stringResource(Res.string.quote_settings_help)),
        buttons = {
            AppButton(stringResource(Res.string.common_cancel), onDismiss)
            AppButton(
                stringResource(Res.string.common_save),
                onClick = {
                    onSave(
                        PrintSettings(
                            layerHeightMm = parseDecimal(layerHeightText, NumberKind.MEASURE, AppLocale.decimalSeparator),
                            infillPercentage = parseDecimal(infillText, NumberKind.MEASURE, AppLocale.decimalSeparator),
                            infillPattern = infillPattern.trim().ifEmpty { null },
                            supportsEnabled = supportsEnabled,
                        ),
                    )
                    onDismiss()
                },
                kind = ButtonKind.Primary,
            )
        },
    ) {
        AppTextField(label = stringResource(Res.string.quote_settings_layer), value = layerHeightText, onValueChange = { layerHeightText = it })
        AppTextField(label = stringResource(Res.string.quote_settings_infill), value = infillText, onValueChange = { infillText = it })
        AppTextField(
            label = stringResource(Res.string.quote_settings_pattern),
            value = infillPattern,
            onValueChange = { infillPattern = it },
            placeholder = stringResource(Res.string.quote_settings_pattern_placeholder),
        )
        AppChoiceField(
            label = stringResource(Res.string.quote_settings_supports),
            options = listOf(
                stringResource(Res.string.quote_settings_supports_yes),
                stringResource(Res.string.quote_settings_supports_no),
                stringResource(Res.string.quote_settings_supports_unknown),
            ),
            selectedIndex = supportOptions.indexOf(supportsEnabled),
            onSelect = { supportsEnabled = supportOptions[it] },
        )
    }
}
