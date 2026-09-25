package com.threedreport.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Faz a área aceitar um arquivo arrastado do explorador de arquivos do sistema (decisão 89: arrastar
 * o G-code pra janela monta o orçamento).
 *
 * @param onDragActive `true` enquanto um arquivo está sendo arrastado por cima, `false` quando sai
 *   ou é solto — pra tela mostrar onde soltar.
 * @param onDragMoved onde o arquivo está, em pixels a partir do canto da janela (a mesma régua do
 *   `boundsInRoot`), pra tela destacar a área embaixo dele; `null` quando o sistema não diz.
 * @param onDrop os arquivos soltos, na ordem, e onde foram soltos (decisão 114: vários G-codes viram
 *   várias impressões). Um G-code vem lido (grande, só pelas pontas, onde ficam os metadados); outro tipo
 *   de arquivo vem só com o nome, sem ser lido, pra quem chama explicar o que fazer com ele: ler um vídeo
 *   de vários GB arrastado por engano travaria o app.
 */
@Composable
expect fun Modifier.fileDropTarget(
    onDragActive: (Boolean) -> Unit,
    onDragMoved: (Offset?) -> Unit = {},
    onDrop: (files: List<PickResult>, position: Offset?) -> Unit,
): Modifier
