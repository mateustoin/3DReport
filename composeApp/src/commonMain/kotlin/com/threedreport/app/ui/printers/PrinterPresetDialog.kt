package com.threedreport.app.ui.printers

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.printers_presets_field_search
import _3dreport.composeapp.generated.resources.printers_presets_intro
import _3dreport.composeapp.generated.resources.printers_presets_none
import _3dreport.composeapp.generated.resources.printers_presets_power_average
import _3dreport.composeapp.generated.resources.printers_presets_power_average_max
import _3dreport.composeapp.generated.resources.printers_presets_power_estimated
import _3dreport.composeapp.generated.resources.printers_presets_power_estimated_max
import _3dreport.composeapp.generated.resources.printers_presets_search_hint
import _3dreport.composeapp.generated.resources.printers_presets_title
import _3dreport.composeapp.generated.resources.printers_presets_use
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.DialogText
import com.threedreport.app.ui.components.ListRow
import com.threedreport.app.ui.components.RowAction
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppDimens
import org.jetbrains.compose.resources.stringResource

/**
 * Diálogo com o catálogo pré-cadastrado de impressoras (decisões 54 e 144), aberto pelo botão "Escolher da lista" em
 * Impressoras. Escolher um preset abre o formulário de nova impressora com os valores de referência já preenchidos (continuam
 * editáveis); não salva nada sozinho. A busca (marca ou modelo) filtra a lista.
 */
@Composable
fun PrinterPresetDialog(onPick: (PrinterPreset) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = printerPresetsMatching(query)

    AppDialog(
        title = stringResource(Res.string.printers_presets_title),
        onDismiss = onDismiss,
        buttons = { AppButton(stringResource(Res.string.common_close), onDismiss) },
    ) {
        DialogText(stringResource(Res.string.printers_presets_intro), muted = true)
        AppTextField(
            label = stringResource(Res.string.printers_presets_field_search),
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(Res.string.printers_presets_search_hint),
        )
        Column(modifier = Modifier.heightIn(max = AppDimens.dialogListMaxHeight).verticalScroll(rememberScrollState())) {
            if (filtered.isEmpty()) DialogText(stringResource(Res.string.printers_presets_none, query.trim()))
            filtered.forEach { preset ->
                ListRow(
                    title = preset.displayName,
                    details = listOfNotNull(presetPowerText(preset), preset.referencePrice?.toCurrencyText()),
                    onClick = { onPick(preset) },
                    actions = listOf(RowAction(AppIcons.Check, stringResource(Res.string.printers_presets_use), { onPick(preset) })),
                )
            }
        }
    }
}

/** Consumo da linha: a média (estimada quando veio da irmã mais próxima) e, quando o fabricante publica, a máxima. */
@Composable
private fun presetPowerText(preset: PrinterPreset): String {
    val average = preset.averagePowerWatts.toInt()
    val max = preset.maxPowerWatts?.toInt()
    val estimated = preset.powerSource == PowerSource.ESTIMATED
    return when {
        max == null && estimated -> stringResource(Res.string.printers_presets_power_estimated, average)
        max == null -> stringResource(Res.string.printers_presets_power_average, average)
        estimated -> stringResource(Res.string.printers_presets_power_estimated_max, average, max)
        else -> stringResource(Res.string.printers_presets_power_average_max, average, max)
    }
}
