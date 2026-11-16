package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.theme.AppDimens

/**
 * Escolha de uma opção entre poucas: o [SegmentedChoice] quando cabe e, com [compact] (janela estreita), a lista
 * suspensa do kit com o mesmo [label]. Quem usa decide o [compact] pela largura da tela.
 */
@Composable
fun AdaptiveChoice(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        AppDropdownField(
            label = label,
            items = options.indices.toList(),
            selected = selectedIndex,
            itemLabel = { options[it] },
            displayText = { options[it] },
            onSelect = onSelect,
            modifier = modifier.width(AppDimens.filterFieldWidth),
        )
    } else {
        SegmentedChoice(options, selectedIndex, onSelect, modifier)
    }
}
