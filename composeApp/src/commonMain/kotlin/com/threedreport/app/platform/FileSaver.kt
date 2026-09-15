package com.threedreport.app.platform

/**
 * Abre o seletor nativo "salvar como" e grava [bytes] no destino escolhido.
 *
 * @param suggestedFileName nome sugerido, pré-preenchido no diálogo.
 * @param initialDirectory pasta aberta por padrão no diálogo, ou `null` pra
 *   deixar o padrão do sistema (ex.: última pasta usada).
 * @return `true` se salvou, `false` se o usuário cancelou.
 */
expect fun saveBytesToFile(bytes: ByteArray, suggestedFileName: String, initialDirectory: String? = null): Boolean

/**
 * Pasta "Documentos" do usuário, tentada de forma independente de idioma do
 * sistema ("Documents" ou "Documentos"); cai para a pasta pessoal do usuário
 * se nenhuma das duas existir.
 */
expect fun defaultDocumentsDirectory(): String?
