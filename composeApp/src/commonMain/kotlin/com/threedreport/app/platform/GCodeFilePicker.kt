package com.threedreport.app.platform

/**
 * Abre o seletor de arquivo nativo da plataforma pra escolher um G-code
 * exportado pelo fatiador.
 *
 * @return os bytes do arquivo escolhido e seu nome original, ou `null` se o
 *   usuário cancelou.
 */
expect fun pickGCodeFile(): PickedFile?
