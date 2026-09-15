package com.threedreport.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Ponto de entrada do app desktop. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "3DReport") {
        App()
    }
}
