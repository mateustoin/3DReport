package com.threedreport.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.threedreport.app.platform.decodeImageBitmap
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.core.model.SavedQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A miniatura da foto de um orçamento, lida e decodificada fora do thread da tela e só quando a foto
 * muda (decisão 108). Antes o Histórico relia e decodificava a foto inteira de cada card a cada tecla na
 * busca, e uma foto de celular por card travava a lista. Foto que não abre vira um ícone, e não uma queda.
 */
@Composable
fun PhotoThumbnail(savedQuote: SavedQuote, size: Dp, load: (SavedQuote) -> ByteArray?, modifier: Modifier = Modifier) {
    if (savedQuote.photoFileName == null) return
    val bitmap by produceState<ImageBitmap?>(null, savedQuote.photoFileName) {
        value = withContext(Dispatchers.Default) {
            runCatching { load(savedQuote)?.let(::decodeImageBitmap) }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(6.dp)
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = "Foto de ${savedQuote.name}",
            modifier = modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
