package com.threedreport.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.components.ShowSnackbarOnce
import com.threedreport.app.ui.components.SubsectionTitle
import com.threedreport.app.ui.icons.AppIcons

/**
 * Seção "Documentos pro cliente" de Configurações, em três blocos: **sua marca** (nome, logo e
 * contato, a identidade de quem vende), **aparência do PDF** (o que um template guarda) e as ações
 * (salvar, ver como fica, templates). Separar identidade de aparência na tela é o mesmo corte do
 * modelo: aplicar um template muda o segundo bloco e nunca o primeiro (decisão 86).
 */
@Composable
internal fun ClientDocumentsSection(viewModel: BrandingViewModel, onShowTemplates: () -> Unit) {
    val branding by viewModel.uiState.collectAsState()

    SectionTitle(AppIcons.Description, "Documentos pro cliente")
    Text(
        "O que aparece no PDF, na imagem quadrada e na mensagem que você manda pro cliente.",
        style = MaterialTheme.typography.bodySmall,
    )

    SubsectionTitle(AppIcons.Badge, "Sua marca", modifier = Modifier.padding(top = 4.dp))
    LabeledField("Nome da sua marca (opcional)", branding.brandNameInput, viewModel::update)

    LogoPicker(branding, viewModel)

    Text(
        "Seu contato (aparece no PDF e na imagem, pro cliente falar com você)",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 4.dp),
    )
    LabeledField("WhatsApp (opcional)", branding.contactWhatsAppInput, viewModel::setContactWhatsApp)
    LabeledField("E-mail (opcional)", branding.contactEmailInput, viewModel::setContactEmail)
    LabeledField("Instagram (opcional, ex.: @minhaloja)", branding.contactInstagramInput, viewModel::setContactInstagram)

    SubsectionTitle(AppIcons.FormatPaint, "Aparência do PDF", modifier = Modifier.padding(top = 4.dp))
    CheckboxRow("Marca d'água diagonal com o nome", branding.showWatermark, viewModel::setShowWatermark)
    CheckboxRow("Rodapé com o nome", branding.showFooter, viewModel::setShowFooter)
    CheckboxRow("Borda em volta da página", branding.showBorder, viewModel::setShowBorder)
    CheckboxRow("Tempo de impressão no PDF e na mensagem", branding.showPrintTime, viewModel::setShowPrintTime)
    Text(
        "O tempo de impressão é o tempo de máquina do pedido (ex.: \"6 h 30 min\"). Ajuda o cliente a " +
            "entender o trabalho, mas também pode virar argumento pra pedir desconto, por isso começa desligado.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        "Os PDFs e a imagem do WhatsApp levam uma linha discreta \"Gerado com 3DReport\" no canto, longe do seu " +
            "nome e do preço, com link pro site no PDF. É assim que o app, que é gratuito, chega a outros vendedores.",
        style = MaterialTheme.typography.bodySmall,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::save) { Text("Salvar") }
        // Mostra o que está no formulário, mesmo sem salvar: dá pra testar uma logo antes de adotá-la.
        OutlinedButton(onClick = viewModel::showPreview) { Text("Ver como fica") }
    }

    branding.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    ShowSnackbarOnce(branding.savedConfirmation, "Configurações dos documentos salvas.", viewModel::consumeSavedConfirmation)

    if (branding.isSavingAsTemplate) {
        LabeledField("Nome do template (ex.: Formal, Simples)", branding.templateNameInput, viewModel::updateTemplateName)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::confirmSaveAsTemplate) { Text("Salvar template") }
            TextButton(onClick = viewModel::cancelSaveAsTemplate) { Text("Cancelar") }
        }
        branding.templateSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = viewModel::startSaveAsTemplate) { Text("Salvar como template") }
            TextButton(onClick = onShowTemplates) { Text("Ver templates salvos") }
        }
        Text(
            "Um template guarda o nome e a aparência do PDF. Sua logo e seu contato ficam sempre os mesmos.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    ShowSnackbarOnce(branding.templateSavedConfirmation, "Template salvo.", viewModel::consumeTemplateSavedConfirmation)

    branding.previewPng?.let { png -> PdfPreviewDialog(png, onDismiss = viewModel::closePreview) }
}

@Composable
private fun LogoPicker(branding: BrandingUiState, viewModel: BrandingViewModel) {
    val logo = branding.logoBytes
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (logo != null) {
            val bitmap = remember(logo) { runCatching { decodeImageBitmap(logo) }.getOrNull() }
            if (bitmap != null) {
                OutlinedCard {
                    Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp).padding(4.dp))
                }
            }
            OutlinedButton(onClick = viewModel::pickLogo) { Text("Trocar logo…") }
            TextButton(onClick = viewModel::removeLogo) { Text("Remover logo") }
        } else {
            OutlinedButton(onClick = viewModel::pickLogo) { Text("Escolher logo… (opcional)") }
        }
    }
    when {
        branding.logoError != null -> Text(branding.logoError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        branding.logoMissing -> Text(
            "A logo salva não foi encontrada na pasta do app. Escolha de novo.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        else -> Text(
            "Vai no cabeçalho do PDF. PNG com fundo transparente fica melhor.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** A primeira página do PDF de exemplo, do jeito que o cliente vai ver. */
@Composable
private fun PdfPreviewDialog(png: ByteArray, onDismiss: () -> Unit) {
    val bitmap = remember(png) { decodeImageBitmap(png) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Como fica o PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Orçamento de exemplo, com o que está no formulário agora (mesmo sem salvar).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                    OutlinedCard {
                        Image(bitmap = bitmap, contentDescription = "Prévia do PDF", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}
