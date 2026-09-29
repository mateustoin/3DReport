package com.threedreport.app.ui.consumables

import com.threedreport.app.ui.components.ArchivedSection
import com.threedreport.app.ui.components.DeleteOrArchiveDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.ConfirmDialog
import com.threedreport.app.ui.components.EmptyState
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.toMoney
import com.threedreport.app.ui.services.ServiceChargeSelector
import com.threedreport.app.ui.services.chargeLabel
import com.threedreport.core.model.Consumable

/**
 * Tela de Insumos (decisão 122): argola, ímã, parafuso, caixa, saquinho, o que entra em cada peça
 * e custa dinheiro além do filamento. Espelha [com.threedreport.app.ui.services.ServiceListScreen].
 */
@Composable
fun ConsumableListScreen(viewModel: ConsumableListViewModel, modifier: Modifier = Modifier) {
    val consumables by viewModel.consumables.collectAsState()
    val form by viewModel.form.collectAsState()
    var pendingDelete by remember { mutableStateOf<Consumable?>(null) }

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Insumos", style = MaterialTheme.typography.titleLarge)
        Text(
            "O que entra em cada peça e custa dinheiro, além do filamento (ex.: argola de chaveiro, " +
                "ímã, caixa). O custo entra no preço, como o material; escolha depois no Orçamento.",
            style = MaterialTheme.typography.bodySmall,
        )

        val (archived, active) = consumables.partition { it.archived }
        if (active.isEmpty()) {
            EmptyState(
                "Nenhum insumo cadastrado. Argola, ímã, parafuso, caixa, saquinho: o que entra em cada " +
                    "peça e custa dinheiro. Cadastre aqui e escolha no orçamento; o custo entra no preço.",
            )
        }

        val row: @Composable (Consumable) -> Unit = { consumable ->
            ConsumableRow(
                consumable = consumable,
                onEdit = { viewModel.startEdit(consumable) },
                onArchiveToggle = { viewModel.setArchived(consumable.id, !consumable.archived) },
                onDelete = { pendingDelete = consumable },
            )
        }
        active.forEach { row(it) }
        ArchivedSection(archived.size) { archived.forEach { row(it) } }

        val currentForm = form
        if (currentForm == null) {
            Button(onClick = viewModel::startAdd) { Text("+ Novo insumo") }
        } else {
            HorizontalDivider()
            ConsumableForm(
                form = currentForm,
                onChange = viewModel::updateForm,
                onSave = viewModel::save,
                onCancel = viewModel::cancelEdit,
            )
        }
    }

    pendingDelete?.let { consumable ->
        DeleteOrArchiveDialog(
            title = "Excluir insumo?",
            what = "o insumo \"${consumable.name}\"",
            usageCount = viewModel.usageCount(consumable.id),
            deleteMessage = "\"${consumable.name}\" será removido do catálogo. Pedidos já salvos continuam com o custo que tinham.",
            onArchive = if (consumable.archived) null else ({
                viewModel.setArchived(consumable.id, true)
                pendingDelete = null
            }),
            onDelete = {
                viewModel.delete(consumable.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ConsumableRow(consumable: Consumable, onEdit: () -> Unit, onArchiveToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(consumable.name + if (consumable.archived) " · arquivado" else "", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${consumable.unitCost.toMoney()} ${chargeLabel(consumable.chargedPerOrder)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onArchiveToggle) { Text(if (consumable.archived) "Restaurar" else "Arquivar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ConsumableForm(
    form: ConsumableFormState,
    onChange: ((ConsumableFormState) -> ConsumableFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.id == null) "Novo insumo" else "Editar insumo", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome") },
            placeholder = { Text("Argola de chaveiro") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.unitCostText,
            onValueChange = { text -> onChange { it.copy(unitCostText = text) } },
            label = { Text("Custo por unidade (${LocalCurrency.current.symbol})") },
        )
        Text("Como contar quando o pedido tem várias peças", style = MaterialTheme.typography.labelLarge)
        ServiceChargeSelector(
            chargedPerOrder = form.chargedPerOrder,
            onChange = { value -> onChange { it.copy(chargedPerOrder = value) } },
        )
        Text(
            "Uma argola por chaveiro é por peça; uma caixa pra entrega inteira é uma vez no pedido. " +
                "Dá pra trocar em cada orçamento.",
            style = MaterialTheme.typography.bodySmall,
        )

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
