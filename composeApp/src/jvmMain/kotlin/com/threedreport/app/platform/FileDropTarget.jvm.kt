package com.threedreport.app.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.io.File
import java.net.URI

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.fileDropTarget(onDragActive: (Boolean) -> Unit, onDrop: (PickedFile) -> Unit): Modifier {
    val currentOnDragActive by rememberUpdatedState(onDragActive)
    val currentOnDrop by rememberUpdatedState(onDrop)

    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = currentOnDragActive(true)
            override fun onExited(event: DragAndDropEvent) = currentOnDragActive(false)
            override fun onEnded(event: DragAndDropEvent) = currentOnDragActive(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragActive(false)
                val file = droppedFiles(event).firstOrNull() ?: return false
                currentOnDrop(PickedFile(file.name, file.readBytes()))
                return true
            }
        }
    }

    // Só entra em modo "soltar aqui" pra arquivos: texto ou imagem arrastados de outro app não
    // são G-code e não devem cobrir a tela com o aviso.
    return dragAndDropTarget(shouldStartDragAndDrop = { event -> event.dragData() is DragData.FilesList }, target = target)
}

/** Os arquivos soltos, que o sistema entrega como URIs ("file:/C:/..."). Pastas ficam de fora. */
@OptIn(ExperimentalComposeUiApi::class)
private fun droppedFiles(event: DragAndDropEvent): List<File> {
    val uris = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
    return uris.mapNotNull { uri -> runCatching { File(URI(uri)) }.getOrNull()?.takeIf { it.isFile } }
}
