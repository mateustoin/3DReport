package com.threedreport.app.ui.history

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.history_photo_alt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.threedreport.app.ui.format.shownName
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.appColors
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * A foto de um orçamento, lida e decodificada fora do thread da tela e só quando a foto muda (decisão 108): decodificar
 * a foto inteira de cada cartão a cada tecla na busca travava a lista. O tamanho vem de [modifier]. Sempre há um fundo
 * `cardBlock` atrás, com ou sem imagem e com o mesmo raio, pra a foto aparecer sobre o `card` no tema escuro. Foto
 * opaca preenche o espaço cortando as bordas; imagem com transparência (um PNG recortado) aparece inteira, sem cortar.
 * Foto que não abre vira o fundo com o ícone de imagem; sem foto, some, a não ser com [placeholder] (o cartão do
 * Catálogo e a linha dele mostram sempre o fundo, sem ilustração).
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
        if (placeholder) PhotoFrame(modifier, shape) { PhotoPlaceholderIcon() }
        return
    }
    val bitmap by produceState<ImageBitmap?>(null, savedQuote.photoFileName) {
        value = withContext(Dispatchers.Default) {
            runCatching { load(savedQuote)?.let(::decodeImageBitmap) }.getOrNull()
        }
    }
    val current = bitmap
    PhotoFrame(modifier, shape) {
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = stringResource(Res.string.history_photo_alt, savedQuote.shownName()),
                modifier = Modifier.fillMaxSize(),
                contentScale = if (current.hasAlpha) ContentScale.Fit else ContentScale.Crop,
            )
        } else {
            PhotoPlaceholderIcon()
        }
    }
}

@Composable
private fun PhotoFrame(modifier: Modifier, shape: Shape, content: @Composable () -> Unit) {
    Box(modifier = modifier.clip(shape).background(MaterialTheme.appColors.cardBlock), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PhotoPlaceholderIcon() {
    Icon(AppIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
