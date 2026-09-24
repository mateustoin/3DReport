package com.threedreport.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Faz a área aceitar um arquivo arrastado do explorador de arquivos do sistema (decisão 89: arrastar
 * o G-code pra janela monta o orçamento).
 *
 * @param onDragActive `true` enquanto um arquivo está sendo arrastado por cima, `false` quando sai
 *   ou é solto — pra tela mostrar onde soltar.
 * @param onDrop o primeiro arquivo solto, já lido. Quem chama decide o que fazer com ele.
 */
@Composable
expect fun Modifier.fileDropTarget(onDragActive: (Boolean) -> Unit, onDrop: (PickedFile) -> Unit): Modifier
