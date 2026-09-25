package com.threedreport.app.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.net.URI

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.fileDropTarget(
    onDragActive: (Boolean) -> Unit,
    onDragMoved: (Offset?) -> Unit,
    onDrop: (files: List<PickResult>, position: Offset?) -> Unit,
): Modifier {
    val currentOnDragActive by rememberUpdatedState(onDragActive)
    val currentOnDragMoved by rememberUpdatedState(onDragMoved)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val density by rememberUpdatedState(LocalDensity.current.density)

    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = currentOnDragActive(true)
            override fun onExited(event: DragAndDropEvent) = currentOnDragActive(false)
            override fun onEnded(event: DragAndDropEvent) = currentOnDragActive(false)
            override fun onMoved(event: DragAndDropEvent) = currentOnDragMoved(event.windowPosition(density))

            override fun onDrop(event: DragAndDropEvent): Boolean {
                currentOnDragActive(false)
                val files = droppedFiles(event).ifEmpty { return false }
                val picked = files.map { file ->
                    val isGCode = file.extension.lowercase() in FileKind.GCODE.extensions
                    if (isGCode) DesktopPlatform.readPicked(file, FileKind.GCODE) else PickResult.Picked(PickedFile(file.name, ByteArray(0)))
                }
                currentOnDrop(picked, event.windowPosition(density))
                return true
            }
        }
    }

    // Só entra em modo "soltar aqui" pra arquivos: texto ou imagem arrastados de outro app não
    // são G-code e não devem cobrir a tela com o aviso.
    return dragAndDropTarget(shouldStartDragAndDrop = { event -> event.dragData() is DragData.FilesList }, target = target)
}

/**
 * Onde o arquivo está, pelo evento do AWT: em pontos a partir do canto da área da janela, que viram os
 * pixels do Compose multiplicando pela densidade. A posição do próprio Compose ainda não é pública.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.windowPosition(density: Float): Offset? {
    val point = when (val native = nativeEvent) {
        is DropTargetDragEvent -> native.location
        is DropTargetDropEvent -> native.location
        else -> null
    } ?: return null
    return Offset(point.x * density, point.y * density)
}

/** Os arquivos soltos, que o sistema entrega como URIs ("file:/C:/..."). Pastas ficam de fora. */
@OptIn(ExperimentalComposeUiApi::class)
private fun droppedFiles(event: DragAndDropEvent): List<File> {
    val uris = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
    return uris.mapNotNull { uri -> runCatching { File(URI(uri)) }.getOrNull()?.takeIf { it.isFile } }
}
