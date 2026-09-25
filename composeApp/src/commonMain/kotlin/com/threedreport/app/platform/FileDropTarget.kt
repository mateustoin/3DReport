package com.threedreport.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Faz a área aceitar um arquivo arrastado do explorador de arquivos do sistema (decisão 89: arrastar
 * o G-code pra janela monta o orçamento).
 *
 * @param onDragActive `true` enquanto um arquivo está sendo arrastado por cima, `false` quando sai
 *   ou é solto — pra tela mostrar onde soltar.
 * @param onDrop o primeiro arquivo solto. Um G-code vem lido (grande, só pelas pontas, onde ficam os
 *   metadados); outro tipo de arquivo vem só com o nome, sem ser lido, pra quem chama explicar o que
 *   fazer com ele — ler um vídeo de vários GB arrastado por engano travaria o app.
 */
@Composable
expect fun Modifier.fileDropTarget(onDragActive: (Boolean) -> Unit, onDrop: (PickResult) -> Unit): Modifier
