package com.threedreport.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.threedreport.app.data.DataDirResult
import com.threedreport.app.data.MovedData
import com.threedreport.app.data.prepareDataDir
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** Ponto de entrada do app desktop. */
fun main() {
    // Antes de qualquer repositório abrir um arquivo: dados de uma versão com outro formato vão
    // pra uma pasta à parte (decisão 104).
    when (val result = prepareDataDir()) {
        is DataDirResult.Ready -> runApp(result.moved)
        is DataDirResult.Failed -> {
            // Abrir assim leria os arquivos antigos pela metade e a próxima gravação apagaria o resto.
            JOptionPane.showMessageDialog(
                null,
                "Esta versão usa um formato de dados novo e precisa guardar os dados da versão anterior à " +
                    "parte, mas não conseguiu mover a pasta\n${result.dataDir.path}\n\n" +
                    "Feche os programas que estejam usando essa pasta (explorador de arquivos, antivírus, " +
                    "backup) e abra o 3DReport de novo. Nada foi apagado.",
                "3DReport",
                JOptionPane.ERROR_MESSAGE,
            )
            exitProcess(1)
        }
    }
}

private fun runApp(moved: MovedData?) = application {
    // Largo o bastante pra tela de Orçamento abrir em duas colunas (entradas à esquerda, resultado
    // à direita) sem a pessoa precisar redimensionar a janela na mão. Continua redimensionável: em
    // janela estreita a tela volta sozinha pra uma coluna só.
    val windowState = rememberWindowState(size = DpSize(1280.dp, 820.dp))
    Window(onCloseRequest = ::exitApplication, title = "3DReport", state = windowState) {
        FixMultiMonitorDpiRedrawBug()
        App(oldDataPath = moved?.path?.path, oldDataFromNewerVersion = moved?.fromNewerVersion == true)
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
