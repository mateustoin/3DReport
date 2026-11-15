package com.threedreport.app.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors

/** Divisor de 1 px em `line`: o do rodapé de seleção, o do menu ([AppMenuDivider]) e o resto que separa sem caixa. */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.appColors.line, thickness = AppDimens.borderWidth)
}
