package com.threedreport.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.icons.AppIcons

/** "Atualizações" no Sobre (decisão 116): a opção, o "Verificar agora" e o resultado. */
@Composable
fun UpdateSection(viewModel: UpdateViewModel) {
    val enabled by viewModel.enabled.collectAsState()
    val state by viewModel.state.collectAsState()

    SectionTitle(AppIcons.Sync, "Atualizações")
    Row(
        modifier = Modifier.toggleable(value = enabled, role = Role.Checkbox, onValueChange = viewModel::setEnabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = enabled, onCheckedChange = null)
        Text("Avisar quando sair uma versão nova")
    }
    Text(
        "Ao abrir o app, ele pergunta ao GitHub qual é a versão mais recente. Só sai do computador esse pedido; " +
            "seus orçamentos e cadastros continuam só aqui.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::checkNow, enabled = state != UpdateState.Checking) { Text("Verificar agora") }
        when (val current = state) {
            UpdateState.Checking -> Text("Verificando…", style = MaterialTheme.typography.bodySmall)
            UpdateState.UpToDate -> Text("Você está na versão mais recente.", style = MaterialTheme.typography.bodySmall)
            UpdateState.Failed -> Text("Não consegui verificar. Confira a internet e tente de novo.", style = MaterialTheme.typography.bodySmall)
            is UpdateState.Available -> LinkText("Versão ${current.release.version} disponível: ver novidades", current.release.url)
            UpdateState.Idle -> Unit
        }
    }
}
