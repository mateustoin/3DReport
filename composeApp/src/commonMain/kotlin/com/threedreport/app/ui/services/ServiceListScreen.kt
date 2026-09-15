package com.threedreport.app.ui.services

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.Service

/** Tela de Serviços: catálogo salvo (pintura, lixamento, acabamento etc.), escolhido depois na tela de Orçamento. */
@Composable
fun ServiceListScreen(viewModel: ServiceListViewModel, modifier: Modifier = Modifier) {
    val services by viewModel.services.collectAsState()
    val form by viewModel.form.collectAsState()

    Column(
        modifier = modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Serviços", style = MaterialTheme.typography.titleLarge)
        Text(
            "Serviços opcionais que você oferece junto com a impressão (ex.: pintura, lixamento, " +
                "embalagem especial). O preço aqui já é o valor cobrado do cliente. Taxa de " +
                "marketplace (ex.: Shopee) não entra aqui — ela é configurada em Configurações, " +
                "porque desconta da sua venda em vez de somar no total do cliente.",
            style = MaterialTheme.typography.bodySmall,
        )

        services.forEach { service ->
            ServiceRow(
                service = service,
                onEdit = { viewModel.startEdit(service) },
                onDelete = { viewModel.delete(service.id) },
            )
        }

        val currentForm = form
        if (currentForm == null) {
            Button(onClick = viewModel::startAdd) { Text("+ Novo serviço") }
        } else {
            HorizontalDivider()
            ServiceForm(
                form = currentForm,
                onChange = viewModel::updateForm,
                onSave = viewModel::save,
                onCancel = viewModel::cancelEdit,
            )
        }
    }
}

@Composable
private fun ServiceRow(service: Service, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(service.name, style = MaterialTheme.typography.titleMedium)
                Text(service.price.toBrl(), style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ServiceForm(
    form: ServiceFormState,
    onChange: ((ServiceFormState) -> ServiceFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (form.id == null) "Novo serviço" else "Editar serviço", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            label = { Text("Nome (ex.: Pintura)") },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().tabToNavigate(),
            value = form.priceText,
            onValueChange = { text -> onChange { it.copy(priceText = text) } },
            label = { Text("Preço cobrado do cliente (R$)") },
        )

        form.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Salvar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
