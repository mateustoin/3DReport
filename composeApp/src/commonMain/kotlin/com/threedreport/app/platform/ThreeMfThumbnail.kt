package com.threedreport.app.platform

/** O que deu pra tirar de miniatura de um 3MF (decisão 141). */
sealed interface ThreeMfThumbnail {

    /** A imagem que o fatiador gravou, como está no pacote (PNG ou JPEG), e o caminho dela lá dentro. */
    class Found(val imageBytes: ByteArray, val path: String) : ThreeMfThumbnail

    /** O pacote abre, mas não tem miniatura (ou a que tem não é uma imagem que dê pra ler). */
    data object Missing : ThreeMfThumbnail

    /** Não é um pacote ZIP que dê pra ler (arquivo corrompido ou que não é 3MF). */
    data object Unreadable : ThreeMfThumbnail
}

/**
 * Lê a miniatura de um 3MF (Bambu Studio, OrcaSlicer, PrusaSlicer). O caminho da imagem sai do próprio pacote, com
 * os caminhos conhecidos como reserva (ver [com.threedreport.core.slicer.findThreeMfThumbnailPath]). Nunca lança:
 * o arquivo é guardado do mesmo jeito, e a tela só escolhe o aviso.
 */
expect fun readThreeMfThumbnail(bytes: ByteArray): ThreeMfThumbnail
