package com.threedreport.app.platform

/**
 * Abre o seletor nativo "salvar como" e grava [bytes] no destino escolhido.
 *
 * @param suggestedFileName nome sugerido, pré-preenchido no diálogo.
 * @return `true` se salvou, `false` se o usuário cancelou.
 */
expect fun saveBytesToFile(bytes: ByteArray, suggestedFileName: String): Boolean
