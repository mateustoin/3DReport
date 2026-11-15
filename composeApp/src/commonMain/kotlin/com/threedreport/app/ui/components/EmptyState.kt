package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Lista vazia: uma frase e, se houver [onAction], o botão que resolve (em âmbar quando é a ação principal da tela, como "Novo pedido"; [actionKind] secundário quando só desfaz um filtro).
 * Sem ilustração.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionKind: ButtonKind = ButtonKind.Primary,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(message, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            AppButton(actionLabel, onAction, kind = actionKind)
        }
    }
}
