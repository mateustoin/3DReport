package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Chip de texto clicável pra escolher um valor sugerido (marca, tipo de
 * material etc.) sem impedir digitar outro valor à mão no campo ao lado —
 * mesmo espírito visual dos chips de cor já usados no cadastro de filamento.
 */
@Composable
fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .let {
                if (selected) {
                    it.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                } else {
                    it.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
