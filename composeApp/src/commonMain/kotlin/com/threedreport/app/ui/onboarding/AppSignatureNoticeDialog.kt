package com.threedreport.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Aviso único pra quem já usava o app quando a assinatura "Gerado com 3DReport" chegou ligada por
 * padrão (decisão 88). O documento vai pro cliente do vendedor, então nada muda nele sem o
 * vendedor ficar sabendo: o aviso mostra o que mudou e deixa ver ou desligar na hora.
 */
@Composable
fun AppSignatureNoticeDialog(onPreview: () -> Unit, onDisable: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text("Novidade nos seus PDFs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A partir desta versão, os PDFs saem com uma linha pequena no canto de baixo: " +
                        "\"Gerado com 3DReport\", com link pro site do app.",
                )
                Text(
                    "O 3DReport é gratuito e sem propaganda, e é assim que outros vendedores conhecem o app. " +
                        "Ela fica longe do seu nome e da sua logo, e dá pra desligar a qualquer momento em " +
                        "Configurações → Documentos pro cliente.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onKeep) { Text("Manter") } },
        dismissButton = {
            Row {
                TextButton(onClick = onPreview) { Text("Ver como fica") }
                TextButton(onClick = onDisable) { Text("Desligar") }
            }
        },
    )
}
