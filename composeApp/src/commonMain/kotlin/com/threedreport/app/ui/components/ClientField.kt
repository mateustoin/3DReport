package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.focus.tabToNavigate
import com.threedreport.core.model.Client

/**
 * Nome do cliente com sugestões do cadastro de clientes (decisão 106): o cliente que já comprou aparece
 * ao digitar, com o contato junto. Escolher liga o pedido ao cliente do cadastro; digitar um nome novo
 * cria o cliente ao salvar. Usado no Orçamento e no "Editar detalhes" do Histórico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientField(name: String, suggestions: List<Client>, onNameChange: (String) -> Unit, onChoose: (Client) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && suggestions.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).tabToNavigate(),
            value = name,
            onValueChange = { onNameChange(it); expanded = true },
            label = { Text("Cliente (opcional)") },
            singleLine = true,
        )
        DropdownMenu(expanded = expanded && suggestions.isNotEmpty(), onDismissRequest = { expanded = false }) {
            suggestions.forEach { client ->
                DropdownMenuItem(
                    text = { Text(client.name + (client.contact?.let { " · $it" } ?: "")) },
                    onClick = { onChoose(client); expanded = false },
                )
            }
        }
    }
}
