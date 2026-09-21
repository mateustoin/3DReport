package com.threedreport.app.platform

/**
 * Abre o seletor de arquivo nativo da plataforma pra escolher um arquivo STL
 * do modelo.
 *
 * @return os bytes do arquivo escolhido e seu nome original, ou `null` se o
 *   usuário cancelou.
 */
expect fun pickStlFile(): PickedFile?
