package com.threedreport.core.stl

/**
 * Até quantos triângulos a prévia 3D desenha a malha do arquivo como ela é. Acima disso, desenha a malha
 * simplificada ([simplifyStl], decisão 142). É o mesmo limite que a 2.2 usava pra recusar a prévia.
 */
const val MAX_FULL_PREVIEW_STL_TRIANGLES: Int = 500_000

/**
 * Tamanho de arquivo acima do qual o STL fica sem prévia (decisão 142, item 10). Medido com um toro sintético em STL
 * binário (`LargeStlPreviewTest.main`): o pico do heap na prévia, contando o próprio arquivo, fica perto de 100 bytes
 * por triângulo (2 milhões: 246 MB; 4 milhões: 434 MB; 6 milhões: 644 MB; 8 milhões: 789 MB), em 1 a 3 segundos.
 * 300 MB são uns 6,3 milhões de triângulos, pico perto de 700 MB: cabe na memória padrão da JVM (um quarto da RAM)
 * num computador de 4 GB, com folga pro resto do app. STL ASCII do mesmo tamanho tem uns 5 vezes menos triângulos.
 */
const val MAX_PREVIEW_STL_FILE_BYTES: Long = 300L * 1024 * 1024

/** O que a prévia 3D tem pra mostrar de um STL (ver [prepareStlPreview]). */
sealed interface StlPreviewResult {

    /**
     * Malha pronta pra desenhar e a análise da peça. [analysis] é sempre do arquivo completo; [mesh] é a
     * simplificada quando [isSimplified] (aí a tela mostra a nota de prévia simplificada).
     */
    data class Ready(val mesh: StlMesh, val analysis: StlAnalysis, val isSimplified: Boolean) : StlPreviewResult

    /** Arquivo maior que o teto de tamanho: fica sem prévia nem análise. */
    data class TooLarge(val fileSizeBytes: Long) : StlPreviewResult
}

/**
 * Prepara a prévia 3D de um STL: analisa o arquivo completo e, se ele tiver mais de [fullPreviewLimit]
 * triângulos, monta a malha simplificada de perto de [simplifiedTarget] triângulos. É trabalho pesado: chame fora
 * da thread da tela. Pra STL de até [fullPreviewLimit] triângulos, a malha e a análise são as mesmas de
 * `parseStl` + `StlAnalyzer.analyze`.
 *
 * @param onProgress ver [StlProgress]; vai de 0 a 1 e é o ponto de cancelar (lance daqui, por exemplo com o
 *   `ensureActive()` da corrotina).
 * @throws IllegalArgumentException se o arquivo não é um STL que dê pra ler (binário truncado).
 */
fun prepareStlPreview(
    bytes: ByteArray,
    onProgress: StlProgress? = null,
    overhangThresholdDegrees: Double = 45.0,
    fullPreviewLimit: Int = MAX_FULL_PREVIEW_STL_TRIANGLES,
    simplifiedTarget: Int = SIMPLIFIED_STL_TARGET_TRIANGLES,
    maxFileBytes: Long = MAX_PREVIEW_STL_FILE_BYTES,
): StlPreviewResult {
    if (bytes.size.toLong() > maxFileBytes) return StlPreviewResult.TooLarge(bytes.size.toLong())
    val progress = MonotonicProgress(onProgress)
    // Contada uma vez: no ASCII é uma varredura do arquivo inteiro.
    val triangleCount = plausibleStlTriangleCount(bytes)
    if (triangleCount <= fullPreviewLimit) {
        val mesh = parseStl(bytes, triangleCount, progress.range(0f, 0.5f))
        val analysis = StlAnalyzer.analyze(mesh, overhangThresholdDegrees, progress.range(0.5f, 1f))
        return StlPreviewResult.Ready(mesh, analysis, isSimplified = false)
    }
    val measured = analyzeStlWithBounds(bytes, triangleCount, overhangThresholdDegrees, progress.range(0f, 0.4f))
    val mesh = simplifyWithBounds(bytes, triangleCount, simplifiedTarget, measured, progress.range(0.4f, 1f))
    return StlPreviewResult.Ready(mesh, measured.analysis, isSimplified = true)
}
