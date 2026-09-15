package com.threedreport.app.platform

/**
 * Abre o seletor de arquivo nativo da plataforma pra escolher uma imagem.
 *
 * @return os bytes do arquivo escolhido e seu nome original, ou `null` se o
 *   usuário cancelou.
 */
expect fun pickImageFile(): PickedFile?

/** Arquivo escolhido pelo usuário: nome original (com extensão) e conteúdo. */
data class PickedFile(val fileName: String, val bytes: ByteArray)
