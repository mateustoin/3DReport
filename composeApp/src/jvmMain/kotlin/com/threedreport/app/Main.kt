package com.threedreport.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Ponto de entrada do app desktop. */
fun main() = application {
    // Tamanho padrão maior que o mínimo do sistema, para caber os formulários
    // (ex.: cadastro de impressora) sem cortar texto; a janela é redimensionável
    // e as telas rolam se ainda assim não couberem.
    val windowState = rememberWindowState(size = DpSize(960.dp, 720.dp))
    Window(onCloseRequest = ::exitApplication, title = "3DReport", state = windowState) {
        App()
    }
}
