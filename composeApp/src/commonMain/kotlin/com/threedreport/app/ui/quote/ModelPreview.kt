package com.threedreport.app.ui.quote

import com.threedreport.app.platform.PickedFile
import com.threedreport.core.stl.StlAnalysis
import com.threedreport.core.stl.StlMesh

/**
 * O que a tela mostra do arquivo do modelo anexado (decisão 141), calculado fora da thread da tela: a prévia 3D e a
 * análise de um STL (decisão 142) ou a miniatura de um 3MF.
 */
sealed interface ModelPreview {

    /** Lendo o arquivo; [progress] vai de 0 a 1 (só o STL informa). */
    data class Loading(val progress: Float = 0f) : ModelPreview

    /** O STL passa do tamanho que o app lê pra prévia: o arquivo é guardado do mesmo jeito, sem prévia nem análise. */
    data class TooLarge(val fileSizeBytes: Long) : ModelPreview {
        /** O tamanho em megabytes, pro aviso. */
        val megabytes: Long get() = fileSizeBytes / BYTES_PER_MEGABYTE
    }

    /** Não deu pra ler o STL (truncado ou corrompido). */
    data object Unreadable : ModelPreview

    /**
     * Malha e análise de um STL. [analysis] é sempre do arquivo completo; [mesh] é a malha simplificada quando
     * [isSimplified] (a tela mostra a nota).
     */
    data class Stl(val mesh: StlMesh, val analysis: StlAnalysis, val isSimplified: Boolean) : ModelPreview

    /** A miniatura que o fatiador gravou dentro do 3MF, pronta pra virar foto. */
    data class ThreeMf(val thumbnail: PickedFile) : ModelPreview

    /** O 3MF abre, mas não tem miniatura. */
    data object ThreeMfNoThumbnail : ModelPreview

    /** O 3MF não abre como pacote. */
    data object ThreeMfUnreadable : ModelPreview
}

private const val BYTES_PER_MEGABYTE = 1024L * 1024L
