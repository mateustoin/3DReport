package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_details_category
import _3dreport.composeapp.generated.resources.quote_details_category_placeholder
import _3dreport.composeapp.generated.resources.quote_details_deadline
import _3dreport.composeapp.generated.resources.quote_details_link
import _3dreport.composeapp.generated.resources.quote_details_open_link
import _3dreport.composeapp.generated.resources.quote_details_photo_automatic
import _3dreport.composeapp.generated.resources.quote_details_photo_description
import _3dreport.composeapp.generated.resources.quote_details_photo_label
import _3dreport.composeapp.generated.resources.quote_details_photo_pick
import _3dreport.composeapp.generated.resources.quote_details_photo_remove
import _3dreport.composeapp.generated.resources.quote_details_photo_unavailable
import _3dreport.composeapp.generated.resources.quote_details_queue
import _3dreport.composeapp.generated.resources.quote_details_settings_add
import _3dreport.composeapp.generated.resources.quote_details_settings_edit
import _3dreport.composeapp.generated.resources.quote_help_category
import _3dreport.composeapp.generated.resources.quote_help_model
import _3dreport.composeapp.generated.resources.quote_help_stl_analysis
import _3dreport.composeapp.generated.resources.quote_help_stl_analysis_detail
import _3dreport.composeapp.generated.resources.quote_help_stl_viewer
import _3dreport.composeapp.generated.resources.quote_model_3mf_no_thumbnail
import _3dreport.composeapp.generated.resources.quote_model_3mf_thumbnail
import _3dreport.composeapp.generated.resources.quote_model_3mf_unreadable
import _3dreport.composeapp.generated.resources.quote_model_3mf_use_as_photo
import _3dreport.composeapp.generated.resources.quote_model_attach
import _3dreport.composeapp.generated.resources.quote_model_hint_label
import _3dreport.composeapp.generated.resources.quote_model_label
import _3dreport.composeapp.generated.resources.quote_model_loading
import _3dreport.composeapp.generated.resources.quote_model_remove
import _3dreport.composeapp.generated.resources.quote_model_stl_simplified
import _3dreport.composeapp.generated.resources.quote_model_stl_unreadable
import _3dreport.composeapp.generated.resources.quote_model_too_large
import _3dreport.composeapp.generated.resources.quote_stl_analysis_label
import _3dreport.composeapp.generated.resources.quote_stl_capture
import _3dreport.composeapp.generated.resources.quote_stl_difficulty
import _3dreport.composeapp.generated.resources.quote_stl_measures
import _3dreport.composeapp.generated.resources.quote_stl_not_manifold
import _3dreport.composeapp.generated.resources.quote_stl_pieces
import _3dreport.composeapp.generated.resources.quote_stl_viewer_label
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.platform.encodeImageBitmapToPng
import com.threedreport.app.ui.components.AppButton
import com.threedreport.app.ui.components.AppProgressBar
import com.threedreport.app.ui.components.AppSuggestField
import com.threedreport.app.ui.components.AppTextField
import com.threedreport.app.ui.components.FormGrid
import com.threedreport.app.ui.components.HelpHint
import com.threedreport.app.ui.components.HelpText
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.oneDecimalText
import com.threedreport.app.ui.format.toPercentText
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import com.threedreport.app.ui.viewer.Stl3DViewer
import com.threedreport.app.ui.viewer.rememberStl3DViewerState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Prazo, categoria, link, foto, arquivo do modelo e configurações de impressão: o que acompanha o pedido ao salvar. */
@Composable
internal fun DetailsFields(
    viewModel: QuoteViewModel,
    catalog: QuoteCatalog,
    modelPreview: ModelPreview?,
    readingModelName: String?,
    input: QuoteInputState,
    form: SaveQuoteFormState,
    queueHints: List<QueueHint>,
    deliveryDaysLate: Int,
    today: Long,
) {
    val space = MaterialTheme.spacing
    val savedQuotes = catalog.savedQuotes
    // Uma linha por impressora que o pedido usa: cada uma tem a própria fila (a conta é do ViewModel).
    val queueHint = queueHints.map { queue ->
        pluralStringResource(
            Res.plurals.quote_details_queue,
            queue.queuedQuoteCount,
            queue.printerName,
            queue.queuedMinutes.minutesToDurationText(),
            queue.queuedQuoteCount,
            queue.thisMinutes.minutesToDurationText(),
        )
    }.joinToString("\n").ifEmpty { null }

    Column(verticalArrangement = Arrangement.spacedBy(space.md)) {
        if (!input.isProduct) {
            Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
                Text(stringResource(Res.string.quote_details_deadline), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DeliveryDatePicker(epochDay = form.deliveryDateEpochDay, daysLate = deliveryDaysLate, today = today, onChange = viewModel::setDeliveryDate)
                queueHint?.let { Text(it, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        FormGrid {
            if (input.isProduct) {
                cell {
                    CategoryField(form.category, viewModel.knownCategories(savedQuotes), viewModel::setCategory)
                }
            }
            cell(2) {
                AppTextField(
                    label = stringResource(Res.string.quote_details_link),
                    value = form.sourceLink,
                    onValueChange = viewModel::setSourceLink,
                )
            }
        }
        if (form.sourceLink.isNotBlank()) LinkText(text = stringResource(Res.string.quote_details_open_link), url = form.sourceLink)

        PhotoRow(viewModel, form)
        ModelAttachment(viewModel, form, modelPreview, readingModelName)

        // Com várias impressões, cada bloco tem as configurações da própria mesa (decisão 114).
        val singlePrint = input.prints.singleOrNull()
        if (singlePrint != null) {
            var showDialog by remember { mutableStateOf(false) }
            AppButton(
                stringResource(if (singlePrint.settings.isEmpty) Res.string.quote_details_settings_add else Res.string.quote_details_settings_edit),
                onClick = { showDialog = true },
            )
            if (showDialog) {
                PrintSettingsDialog(
                    initial = singlePrint.settings,
                    onDismiss = { showDialog = false },
                    onSave = { viewModel.setPrintSettings(it, singlePrint.id) },
                )
            }
        }
    }
}

@Composable
private fun PhotoRow(viewModel: QuoteViewModel, form: SaveQuoteFormState) {
    val space = MaterialTheme.spacing
    val photo = form.photo
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        Text(stringResource(Res.string.quote_details_photo_label), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (photo != null) {
            // Decodifica uma vez por foto, e não a cada tecla digitada em outro campo; uma imagem que não
            // abre vira aviso, e não derruba a tela.
            val bitmap = remember(photo) { runCatching { decodeImageBitmap(photo.bytes) }.getOrNull() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(Res.string.quote_details_photo_description),
                        modifier = Modifier.size(AppDimens.thumbnailSize).clip(MaterialTheme.shapes.small),
                    )
                } else {
                    Text(stringResource(Res.string.quote_details_photo_unavailable), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    if (form.photoAutomatic) stringResource(Res.string.quote_details_photo_automatic) else photo.fileName,
                    style = MaterialTheme.appType.body,
                )
                AppButton(stringResource(Res.string.quote_details_photo_remove), onClick = viewModel::clearPhoto)
            }
        } else {
            AppButton(stringResource(Res.string.quote_details_photo_pick), onClick = viewModel::pickPhoto, icon = AppIcons.AddPhotoAlternate)
        }
        form.photoError?.let { Text(it.asString(), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.error) }
    }
}

/**
 * O arquivo do modelo anexado (STL ou 3MF): a prévia 3D e a análise de um STL, ou a miniatura de um 3MF, calculadas
 * fora da thread da tela ([QuoteUiState.modelPreview]).
 */
@Composable
private fun ModelAttachment(viewModel: QuoteViewModel, form: SaveQuoteFormState, preview: ModelPreview?, readingName: String?) {
    val space = MaterialTheme.spacing
    val modelFile = form.modelFile
    Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
        Text(stringResource(Res.string.quote_model_label), style = MaterialTheme.appType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (modelFile == null && readingName != null) {
            Text(readingName, style = MaterialTheme.appType.body)
            val description = stringResource(Res.string.quote_model_loading)
            AppProgressBar(progress = null, modifier = Modifier.semantics { contentDescription = description })
            Text(description, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (modelFile == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
                AppButton(stringResource(Res.string.quote_model_attach), onClick = viewModel::pickModelFile, icon = AppIcons.FolderOpen)
                HelpHint(HelpText(stringResource(Res.string.quote_help_model)), stringResource(Res.string.quote_model_hint_label))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                Text(modelFile.fileName, style = MaterialTheme.appType.body)
                AppButton(stringResource(Res.string.quote_model_remove), onClick = viewModel::clearModelFile)
            }
        }
    }
    if (modelFile == null) return
    Column(verticalArrangement = Arrangement.spacedBy(space.sm)) {
        when (val current = preview) {
            null -> Unit

            is ModelPreview.Loading -> {
                val description = stringResource(Res.string.quote_model_loading)
                Column(verticalArrangement = Arrangement.spacedBy(space.xs)) {
                    AppProgressBar(
                        progress = current.progress.takeIf { it > 0f },
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                    Text(description, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            is ModelPreview.TooLarge -> Text(
                stringResource(Res.string.quote_model_too_large, current.megabytes),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ModelPreview.Unreadable -> ErrorCaption(stringResource(Res.string.quote_model_stl_unreadable))

            is ModelPreview.Stl -> StlPreviewBlock(viewModel, current)

            is ModelPreview.ThreeMf -> ThreeMfThumbnailBlock(viewModel, current, form)

            ModelPreview.ThreeMfNoThumbnail -> Text(
                stringResource(Res.string.quote_model_3mf_no_thumbnail),
                style = MaterialTheme.appType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ModelPreview.ThreeMfUnreadable -> ErrorCaption(stringResource(Res.string.quote_model_3mf_unreadable))
        }
    }
}

@Composable
private fun ErrorCaption(text: String) {
    Text(text, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.error)
}

/** A miniatura que o fatiador gravou no 3MF, no lugar da prévia 3D, e o botão que a usa como foto. */
@Composable
private fun ThreeMfThumbnailBlock(viewModel: QuoteViewModel, preview: ModelPreview.ThreeMf, form: SaveQuoteFormState) {
    val bitmap = remember(preview.thumbnail) { runCatching { decodeImageBitmap(preview.thumbnail.bytes) }.getOrNull() }
    if (bitmap == null) {
        ErrorCaption(stringResource(Res.string.quote_model_3mf_unreadable))
        return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(AppDimens.stlViewerHeight)
            .clip(MaterialTheme.shapes.medium)
            .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(Res.string.quote_model_3mf_thumbnail),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
    // A foto já é esta miniatura (a regra automática a pôs): o botão só serve pra trocar uma foto diferente.
    val alreadyThePhoto = form.photo?.bytes?.contentEquals(preview.thumbnail.bytes) == true
    if (!alreadyThePhoto) {
        AppButton(stringResource(Res.string.quote_model_3mf_use_as_photo), onClick = viewModel::useModelThumbnailAsPhoto, icon = AppIcons.Image)
    }
}

/** A prévia 3D de um STL (completa ou simplificada), a captura como foto, o nível de dificuldade e as medidas. */
@Composable
private fun StlPreviewBlock(viewModel: QuoteViewModel, current: ModelPreview.Stl) {
    val space = MaterialTheme.spacing
    val viewerState = rememberStl3DViewerState(current.mesh)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium),
    ) {
        Stl3DViewer(state = viewerState, mesh = current.mesh, modifier = Modifier.fillMaxWidth().height(AppDimens.stlViewerHeight))
    }
    if (current.isSimplified) {
        Text(stringResource(Res.string.quote_model_stl_simplified), style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val baseColor = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xs)) {
        AppButton(stringResource(Res.string.quote_stl_capture), onClick = {
            val snapshot = viewerState.captureSnapshot(baseColor, width = 1000, height = 1000)
            viewModel.setPhotoFromStlSnapshot(encodeImageBitmapToPng(snapshot))
        })
        HelpHint(HelpText(stringResource(Res.string.quote_help_stl_viewer)), stringResource(Res.string.quote_stl_viewer_label))
    }

    val analysis = current.analysis
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space.xxs)) {
        Text(stringResource(Res.string.quote_stl_difficulty, stringResource(analysis.difficulty.labelRes())), style = MaterialTheme.appType.bodyStrong)
        HelpHint(
            HelpText(stringResource(Res.string.quote_help_stl_analysis), stringResource(Res.string.quote_help_stl_analysis_detail)),
            stringResource(Res.string.quote_stl_analysis_label),
        )
    }
    Text(
        stringResource(
            Res.string.quote_stl_measures,
            (analysis.surfaceAreaMm2 / 100).oneDecimalText(),
            (analysis.volumeMm3 / 1000).oneDecimalText(),
            (analysis.overhangPercentage / 100).toPercentText(),
            pluralStringResource(Res.plurals.quote_stl_pieces, analysis.disconnectedComponents, analysis.disconnectedComponents),
        ),
        style = MaterialTheme.appType.caption,
    )
    if (!analysis.isManifold) {
        ErrorCaption(stringResource(Res.string.quote_stl_not_manifold))
    }
}

/**
 * Categoria do produto (decisão 102): texto livre com as já usadas sugeridas embaixo, filtradas pelo
 * que se digita. Sem tela de cadastro: a lista de categorias é o que já foi digitado antes.
 */
@Composable
private fun CategoryField(value: String, suggestions: List<String>, onValueChange: (String) -> Unit) {
    val matches = suggestions.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }
    AppSuggestField(
        label = stringResource(Res.string.quote_details_category),
        value = value,
        onValueChange = onValueChange,
        suggestions = matches,
        suggestionLabel = { it },
        onChoose = onValueChange,
        placeholder = stringResource(Res.string.quote_details_category_placeholder),
        help = HelpText(stringResource(Res.string.quote_help_category)),
    )
}
