package com.threedreport.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.format.LocalCurrency
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.PricingSettings
import com.threedreport.core.model.UsageProfile

/**
 * Quatro perguntas na primeira execução, pra o app não abrir com a conta calibrada pra outra pessoa.
 *
 * A primeira é como a pessoa usa o app (decisão 103): quem ainda não vende começa salvando no
 * catálogo e vendo Produtos primeiro no Histórico. Só muda o que vem escolhido; nada some.
 *
 * São justamente os números que mais mudam o preço e que ninguém adivinha por padrão: energia, o
 * valor da própria hora (que nasce zerado e, sem preencher, faz o trabalho sumir do preço) e a
 * margem. Impressora e filamento ficam de fora de propósito: já vêm com um cadastro padrão
 * utilizável, e as telas deles têm catálogo pronto, então perguntar aqui só atrasaria o primeiro
 * orçamento.
 *
 * Dá pra pular: quem só quer ver o app funcionando não deveria ser barrado por um formulário.
 */
@Composable
fun OnboardingDialog(
    currentSettings: PricingSettings,
    onFinish: (PricingSettings, UsageProfile) -> Unit,
    onSkip: () -> Unit,
) {
    val currency = LocalCurrency.current
    var energyText by remember { mutableStateOf(currentSettings.energyPricePerKwh.toString()) }
    var laborText by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(UsageProfile.SELLER) }
    var marginText by remember { mutableStateOf((currentSettings.profitMargin * 100).toString()) }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Bem-vindo ao 3DReport") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Quatro perguntas rápidas pra o app sair do jeito do seu negócio. Dá pra mudar " +
                        "tudo depois em Configurações.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text("Como você vai usar o 3DReport?", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    UsageProfile.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = option == profile, onClick = { profile = option }, role = Role.RadioButton)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == profile, onClick = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = energyText,
                    onValueChange = { energyText = it },
                    label = { Text("Preço do kWh (${currency.symbol})") },
                )
                Text(
                    "Está na sua conta de luz, na linha de consumo. Varia por estado e por " +
                        "distribuidora, então vale conferir em vez de chutar.",
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = laborText,
                    onValueChange = { laborText = it },
                    label = { Text("Valor da sua hora de trabalho (${currency.symbol}/h)") },
                )
                Text(
                    "Preparar o arquivo, tirar da mesa, remover suporte, lixar, pintar, embalar. É o " +
                        "custo que mais some da conta de quem vende impressão 3D. Deixe zero se " +
                        "preferir não cobrar por enquanto.",
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = marginText,
                    onValueChange = { marginText = it },
                    label = { Text("Margem de lucro (%)") },
                )
                Text(
                    "Quanto você quer ganhar em cima do custo. 100% significa cobrar o dobro do que " +
                        "a peça custou pra produzir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onFinish(
                    currentSettings.copy(
                        energyPricePerKwh = parseDecimal(energyText) ?: currentSettings.energyPricePerKwh,
                        laborRatePerHour = parseDecimal(laborText) ?: 0.0,
                        profitMargin = (parseDecimal(marginText) ?: (currentSettings.profitMargin * 100)) / 100.0,
                    ),
                    profile,
                )
            }) { Text("Começar") }
        },
        dismissButton = { TextButton(onClick = onSkip) { Text("Pular") } },
    )
}
