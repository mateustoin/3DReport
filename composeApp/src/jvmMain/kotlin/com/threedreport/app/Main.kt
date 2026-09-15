package com.threedreport.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

/** Ponto de entrada do app desktop. */
fun main() = application {
    // Tamanho padrão maior que o mínimo do sistema, para caber os formulários
    // (ex.: cadastro de impressora) sem cortar texto; a janela é redimensionável
    // e as telas rolam se ainda assim não couberem.
    val windowState = rememberWindowState(size = DpSize(960.dp, 720.dp))
    Window(onCloseRequest = ::exitApplication, title = "3DReport", state = windowState) {
        FixMultiMonitorDpiRedrawBug()
        App()
    }
}

/**
 * Contorna um bug conhecido do Compose Desktop/Skiko: ao arrastar a janela pra
 * um monitor com DPI/escala diferente, o conteúdo (ex.: a barra de abas) fica
 * desenhado com o layout antigo até algo forçar um relayout — normalmente só
 * volta ao redimensionar a janela na mão. Não há correção oficial (ver
 * https://github.com/JetBrains/compose-multiplatform/issues/3685 e relacionadas);
 * a solução da comunidade é detectar a troca de monitor e simular esse
 * redimensionamento (1px pra frente e de volta) programaticamente.
 */
@Composable
private fun FrameWindowScope.FixMultiMonitorDpiRedrawBug() {
    DisposableEffect(window) {
        var lastGraphicsConfiguration = window.graphicsConfiguration
        val listener = object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) {
                val currentGraphicsConfiguration = window.graphicsConfiguration
                if (currentGraphicsConfiguration != lastGraphicsConfiguration) {
                    lastGraphicsConfiguration = currentGraphicsConfiguration
                    val size = window.size
                    window.size = Dimension(size.width, size.height + 1)
                    window.size = size
                }
            }
        }
        window.addComponentListener(listener)
        onDispose { window.removeComponentListener(listener) }
    }
}
