package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_close
import _3dreport.composeapp.generated.resources.common_optional
import _3dreport.composeapp.generated.resources.common_save
import _3dreport.composeapp.generated.resources.settings_docs_action_preview
import _3dreport.composeapp.generated.resources.settings_docs_border
import _3dreport.composeapp.generated.resources.settings_docs_brand_title
import _3dreport.composeapp.generated.resources.settings_docs_client_name
import _3dreport.composeapp.generated.resources.settings_docs_contact_help
import _3dreport.composeapp.generated.resources.settings_docs_contact_title
import _3dreport.composeapp.generated.resources.settings_docs_field_brand
import _3dreport.composeapp.generated.resources.settings_docs_field_email
import _3dreport.composeapp.generated.resources.settings_docs_field_instagram
import _3dreport.composeapp.generated.resources.settings_docs_field_instagram_hint
import _3dreport.composeapp.generated.resources.settings_docs_field_validity
import _3dreport.composeapp.generated.resources.settings_docs_field_whatsapp
import _3dreport.composeapp.generated.resources.settings_docs_footer
import _3dreport.composeapp.generated.resources.settings_docs_logo_choose
import _3dreport.composeapp.generated.resources.settings_docs_logo_help
import _3dreport.composeapp.generated.resources.settings_docs_logo_label
import _3dreport.composeapp.generated.resources.settings_docs_logo_missing
import _3dreport.composeapp.generated.resources.settings_docs_logo_remove
import _3dreport.composeapp.generated.resources.settings_docs_logo_replace
import _3dreport.composeapp.generated.resources.settings_docs_pdf_help
import _3dreport.composeapp.generated.resources.settings_docs_pdf_help_detail
import _3dreport.composeapp.generated.resources.settings_docs_pdf_title
import _3dreport.composeapp.generated.resources.settings_docs_preview_description
import _3dreport.composeapp.generated.resources.settings_docs_preview_help
import _3dreport.composeapp.generated.resources.settings_docs_preview_sample
import _3dreport.composeapp.generated.resources.settings_docs_preview_title
import _3dreport.composeapp.generated.resources.settings_docs_print_time
import _3dreport.composeapp.generated.resources.settings_docs_print_time_help
import _3dreport.composeapp.generated.resources.settings_docs_print_time_help_detail
import _3dreport.composeapp.generated.resources.settings_docs_template_list
import _3dreport.composeapp.generated.resources.settings_docs_template_name
import _3dreport.composeapp.generated.resources.settings_docs_template_name_hint
import _3dreport.composeapp.generated.resources.settings_docs_template_save
import _3dreport.composeapp.generated.resources.settings_docs_templates_help
import _3dreport.composeapp.generated.resources.settings_docs_templates_title
import _3dreport.composeapp.generated.resources.settings_docs_validity_help
import _3dreport.composeapp.generated.resources.settings_docs_validity_help_detail
import _3dreport.composeapp.generated.resources.settings_docs_watermark
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppCheckbox
import com.threedreport.app.ui.components.AppDialog
import com.threedreport.app.ui.components.AppNumberField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.ButtonKind
import com.threedreport.app.ui.components.FieldError
import com.threedreport.app.ui.components.FieldLabel
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.FormSection
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.templates.TemplateListDialog
import com.threedreport.app.ui.templates.TemplateListViewModel
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * A seção "Documentos pro cliente" de Configurações, em três blocos: **sua marca** (nome, logo e contato, a identidade
 * de quem vende), **aparência do PDF** (o que um template guarda) e os templates. Separar identidade de aparência na
 * tela é o mesmo corte do modelo: aplicar um template muda o segundo bloco e nunca o primeiro (decisão 86).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClientDocumentsSection(viewModel: BrandingViewModel, templateListViewModel: TemplateListViewModel) {
    val branding by viewModel.uiState.collectAsState()
    var showTemplates by remember { mutableStateOf(false) }
    val space = MaterialTheme.spacing

    FormSection(1, stringResource(Res.string.settings_docs_brand_title)) {
        FormGrid {
            cell(3) {
                AppTextField(
                    label = stringResource(Res.string.settings_docs_field_brand),
                    value = branding.brandNameInput,
                    onValueChange = viewModel::update,
                    placeholder = stringResource(Res.string.common_optional),
                )
            }
        }
        LogoPicker(branding, viewModel)
    }

    FormSection(2, stringResource(Res.string.settings_docs_contact_title), help = HelpText(stringResource(Res.string.settings_docs_contact_help))) {
        FormGrid {
            cell(1) {
                AppTextField(
                    label = stringResource(Res.string.settings_docs_field_whatsapp),
                    value = branding.contactWhatsAppInput,
                    onValueChange = viewModel::setContactWhatsApp,
                    placeholder = stringResource(Res.string.common_optional),
                )
            }
            cell(1) {
                AppTextField(
                    label = stringResource(Res.string.settings_docs_field_email),
                    value = branding.contactEmailInput,
                    onValueChange = viewModel::setContactEmail,
                    placeholder = stringResource(Res.string.common_optional),
                )
            }
            cell(1) {
                AppTextField(
                    label = stringResource(Res.string.settings_docs_field_instagram),
                    value = branding.contactInstagramInput,
                    onValueChange = viewModel::setContactInstagram,
                    placeholder = stringResource(Res.string.settings_docs_field_instagram_hint),
                )
            }
        }
    }

    FormSection(
        3,
        stringResource(Res.string.settings_docs_pdf_title),
        help = HelpText(stringResource(Res.string.settings_docs_pdf_help), stringResource(Res.string.settings_docs_pdf_help_detail)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
            AppCheckbox(stringResource(Res.string.settings_docs_watermark), branding.showWatermark, viewModel::setShowWatermark)
            AppCheckbox(stringResource(Res.string.settings_docs_footer), branding.showFooter, viewModel::setShowFooter)
            AppCheckbox(stringResource(Res.string.settings_docs_border), branding.showBorder, viewModel::setShowBorder)
            AppCheckbox(
                stringResource(Res.string.settings_docs_print_time),
                branding.showPrintTime,
                viewModel::setShowPrintTime,
                help = HelpText(stringResource(Res.string.settings_docs_print_time_help), stringResource(Res.string.settings_docs_print_time_help_detail)),
            )
            AppCheckbox(stringResource(Res.string.settings_docs_client_name), branding.showClientName, viewModel::setShowClientName)
            FieldError(branding.optionsError?.asString())
        }
        FormGrid {
            cell(1) {
                AppNumberField(
                    label = stringResource(Res.string.settings_docs_field_validity),
                    value = branding.validityDaysText,
                    onValueChange = viewModel::setValidityDays,
                    kind = NumberKind.MEASURE,
                    error = branding.validityError?.asString(),
                    help = HelpText(stringResource(Res.string.settings_docs_validity_help), stringResource(Res.string.settings_docs_validity_help_detail)),
                )
            }
        }
        // Mostra o que está no formulário, mesmo sem salvar: dá pra testar uma logo antes de adotá-la.
        val sampleName = stringResource(Res.string.settings_docs_preview_sample)
        AppButton(stringResource(Res.string.settings_docs_action_preview), onClick = { viewModel.showPreview(sampleName) }, icon = AppIcons.Visibility)
    }

    FormSection(4, stringResource(Res.string.settings_docs_templates_title), help = HelpText(stringResource(Res.string.settings_docs_templates_help))) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space.xs), verticalArrangement = Arrangement.spacedBy(space.xs)) {
            AppButton(stringResource(Res.string.settings_docs_template_save), viewModel::startSaveAsTemplate, icon = AppIcons.Save)
            AppButton(stringResource(Res.string.settings_docs_template_list), onClick = { showTemplates = true }, icon = AppIcons.ViewList)
        }
    }

    branding.savingTemplate?.let { form ->
        AppDialog(
            title = stringResource(Res.string.settings_docs_template_save),
            onDismiss = viewModel::cancelSaveAsTemplate,
            buttons = {
                AppButton(stringResource(Res.string.common_cancel), viewModel::cancelSaveAsTemplate)
                AppButton(stringResource(Res.string.common_save), viewModel::confirmSaveAsTemplate, kind = ButtonKind.Primary)
            },
        ) {
            AppTextField(
                label = stringResource(Res.string.settings_docs_template_name),
                value = form.name,
                onValueChange = viewModel::updateTemplateName,
                placeholder = stringResource(Res.string.settings_docs_template_name_hint),
                error = form.error?.asString(),
                autoFocus = true,
            )
        }
    }
    if (showTemplates) {
        TemplateListDialog(templateListViewModel, onLoad = viewModel::applyTemplate, onDismiss = { showTemplates = false })
    }
    branding.previewPng?.let { png -> PdfPreviewDialog(png, onDismiss = viewModel::closePreview) }
}

@Composable
private fun LogoPicker(branding: BrandingUiState, viewModel: BrandingViewModel) {
    val space = MaterialTheme.spacing
    val logo = branding.logoBytes
    Column(verticalArrangement = Arrangement.spacedBy(space.xxs)) {
        FieldLabel(stringResource(Res.string.settings_docs_logo_label), HelpText(stringResource(Res.string.settings_docs_logo_help)))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
            if (logo != null) {
                val bitmap = remember(logo) { runCatching { decodeImageBitmap(logo) }.getOrNull() }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(AppDimens.thumbnailSize)
                            .background(MaterialTheme.appColors.cardBlock, MaterialTheme.shapes.medium)
                            .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium)
                            .padding(space.xxs),
                    )
                }
                AppButton(stringResource(Res.string.settings_docs_logo_replace), viewModel::pickLogo)
                AppButton(stringResource(Res.string.settings_docs_logo_remove), viewModel::removeLogo)
            } else {
                AppButton(stringResource(Res.string.settings_docs_logo_choose), viewModel::pickLogo, icon = AppIcons.AddPhotoAlternate)
            }
        }
        FieldError(
            branding.logoError?.asString() ?: stringResource(Res.string.settings_docs_logo_missing).takeIf { branding.logoMissing },
        )
    }
}

/** A primeira página do PDF de exemplo, do jeito que o cliente vai ver. */
@Composable
private fun PdfPreviewDialog(png: ByteArray, onDismiss: () -> Unit) {
    val bitmap = remember(png) { decodeImageBitmap(png) }
    AppDialog(
        title = stringResource(Res.string.settings_docs_preview_title),
        onDismiss = onDismiss,
        scrollable = true,
        help = HelpText(stringResource(Res.string.settings_docs_preview_help)),
        buttons = { AppButton(stringResource(Res.string.common_close), onDismiss) },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(Res.string.settings_docs_preview_description),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium),
        )
    }
}
