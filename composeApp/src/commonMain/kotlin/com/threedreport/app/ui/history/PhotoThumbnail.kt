package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_photo_alt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.appColors
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * A foto de um orçamento, lida e decodificada fora do thread da tela e só quando a foto muda (decisão 108): decodificar
 * a foto inteira de cada cartão a cada tecla na busca travava a lista. O tamanho vem de [modifier]. Foto que não abre
 * vira o fundo `column` com o ícone de imagem; sem foto, some, a não ser com [placeholder] (o cartão do Catálogo e a
 * linha dele mostram sempre o fundo, sem ilustração).
 */
@Composable
fun PhotoThumbnail(
    savedQuote: SavedQuote,
    load: (SavedQuote) -> ByteArray?,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    if (savedQuote.photoFileName == null) {
        if (placeholder) PhotoPlaceholder(modifier, shape)
        return
    }
    val bitmap by produceState<ImageBitmap?>(null, savedQuote.photoFileName) {
        value = withContext(Dispatchers.Default) {
            runCatching { load(savedQuote)?.let(::decodeImageBitmap) }.getOrNull()
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = stringResource(Res.string.history_photo_alt, savedQuote.name),
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        PhotoPlaceholder(modifier, shape)
    }
}

@Composable
private fun PhotoPlaceholder(modifier: Modifier, shape: Shape) {
    Box(modifier = modifier.clip(shape).background(MaterialTheme.appColors.column), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
