package com.threedreport.core.stl

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Alvo de triângulos da malha simplificada só pra desenhar (decisão 142). Perto de 200 mil o visualizador ainda
 * gira sem engasgar, e a peça continua reconhecível; o teto de desenhar sem simplificar é
 * [MAX_FULL_PREVIEW_STL_TRIANGLES].
 */
const val SIMPLIFIED_STL_TARGET_TRIANGLES: Int = 200_000

/**
 * Malha simplificada por grade (vertex clustering), só pra desenhar: os vértices que caem na mesma célula de uma
 * grade viram um só (a média deles), e os triângulos que ficam degenerados (dois cantos na mesma célula) ou
 * repetidos somem. O tamanho da célula é escolhido pra ficar perto de [targetTriangles], sem passar dele.
 *
 * Lê o arquivo sem montar a malha inteira: o que fica em memória é a grade e a malha de saída. Não serve pra
 * medir nada (as medidas saem de [analyzeStl], com o arquivo completo). Arquivo com até [targetTriangles]
 * triângulos volta inteiro, como no [parseStl].
 *
 * @param onProgress ver [StlProgress]; lançar daqui cancela.
 * @throws IllegalArgumentException se o arquivo binário estiver truncado.
 */
fun simplifyStl(bytes: ByteArray, targetTriangles: Int = SIMPLIFIED_STL_TARGET_TRIANGLES, onProgress: StlProgress? = null): StlMesh {
    require(targetTriangles > 0) { "targetTriangles precisa ser positivo: $targetTriangles" }
    if (peekStlTriangleCount(bytes) <= targetTriangles) {
        val mesh = parseStl(bytes)
        onProgress?.invoke(1f)
        return mesh
    }
    val progress = MonotonicProgress(onProgress)
    val measured = analyzeStlWithBounds(bytes, 45.0, progress.range(0f, 0.4f))
    return simplifyWithBounds(bytes, targetTriangles, measured, progress.range(0.4f, 1f))
}

/** Progresso que nunca volta, dividido em faixas pros passos de um trabalho só. */
internal class MonotonicProgress(private val sink: StlProgress?) {
    private var last = 0f

    fun report(fraction: Float) {
        if (sink == null) return
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped >= last) {
            last = clamped
            sink.invoke(clamped)
        }
    }

    fun range(from: Float, to: Float): StlProgress = { report(from + (to - from) * it) }
}

/**
 * Faz a simplificação com a caixa e a área já medidas. Cada tentativa é uma passada pelo arquivo; se a grade
 * escolhida der triângulos demais, a tentativa para no meio (sem gastar memória com a saída inteira) e a célula
 * cresce na proporção do excesso. Fica com a melhor tentativa que coube no alvo.
 */
internal fun simplifyWithBounds(bytes: ByteArray, targetTriangles: Int, measured: AnalysisWithBounds, onProgress: StlProgress): StlMesh {
    val progress = MonotonicProgress(onProgress)
    val extent = measured.boundsMax - measured.boundsMin
    val longest = max(extent.x, max(extent.y, extent.z)).toDouble()
    if (longest <= 0.0) {
        progress.report(1f)
        return StlMesh(emptyList())
    }
    // Uma superfície de área A corta perto de 1,5 × A / s² células de lado s, e uma malha fechada tem uns dois
    // triângulos por vértice: s = √(3A / alvo). A área é a da malha completa, já medida.
    val area = measured.analysis.surfaceAreaMm2
    var cell = if (area > 0.0) sqrt(3.0 * area / targetTriangles) else longest / 256.0
    // A grade cabe nos inteiros da chave e no empacotamento de 21 bits por canto do triângulo.
    cell = max(cell, longest / MAX_CELLS_PER_AXIS)

    var best: StlMesh? = null
    var start = 0f
    for (attempt in 0 until MAX_ATTEMPTS) {
        val end = if (attempt == MAX_ATTEMPTS - 1) 1f else start + (1f - start) * 0.6f
        val outcome = clusterOnce(bytes, measured.boundsMin, cell, targetTriangles, progress.range(start, end))
        start = end
        when (outcome) {
            is Outcome.TooMany -> cell *= sqrt(outcome.estimatedTriangles / targetTriangles) * 1.1

            is Outcome.Done -> {
                if (best == null || outcome.mesh.triangles.size > best.triangles.size) best = outcome.mesh
                // Longe demais do alvo pra baixo: tenta uma grade mais fina (que pode passar e ser descartada).
                if (outcome.mesh.triangles.size >= targetTriangles / 2 || attempt == MAX_ATTEMPTS - 1) break
                cell *= sqrt(max(outcome.mesh.triangles.size, 1).toDouble() / targetTriangles).coerceAtLeast(0.35) * 1.05
                cell = max(cell, longest / MAX_CELLS_PER_AXIS)
            }
        }
    }
    // Sem nenhuma tentativa que coube: grade bem grossa, que sempre cabe.
    val result = best ?: (clusterOnce(bytes, measured.boundsMin, longest / 8.0, Int.MAX_VALUE, {}) as Outcome.Done).mesh
    progress.report(1f)
    return result
}

private const val MAX_ATTEMPTS = 4

/** Células por eixo, no máximo: 2²⁰ cabe com folga nos 21 bits de cada canto da chave do triângulo. */
private const val MAX_CELLS_PER_AXIS = 1_000_000.0

/** Quantos agrupamentos cabem nos 21 bits de cada canto da chave do triângulo. */
private const val MAX_CLUSTERS = 1 shl 21

private sealed interface Outcome {
    class Done(val mesh: StlMesh) : Outcome

    /** Parou no meio: a grade daria uns [estimatedTriangles] triângulos, mais que o alvo. */
    class TooMany(val estimatedTriangles: Double) : Outcome
}

private fun clusterOnce(bytes: ByteArray, origin: Vec3, cell: Double, limit: Int, onProgress: StlProgress): Outcome {
    val total = peekStlTriangleCount(bytes).coerceAtLeast(1L)
    val cells = Int3IdTable(minOf(limit, 1 shl 20))
    val sums = DoubleList()
    val counts = IntList()
    val output = IntList()
    val seen = LongHashSet(minOf(limit, 1 shl 20))
    var processed = 0L

    fun clusterOf(x: Float, y: Float, z: Float): Int {
        val ix = floor((x - origin.x) / cell).toInt()
        val iy = floor((y - origin.y) / cell).toInt()
        val iz = floor((z - origin.z) / cell).toInt()
        val before = cells.size
        val id = cells.idOf(ix, iy, iz)
        if (cells.size > before) {
            sums.add(0.0)
            sums.add(0.0)
            sums.add(0.0)
            counts.add(0)
        }
        sums[id * 3] = sums[id * 3] + x
        sums[id * 3 + 1] = sums[id * 3 + 1] + y
        sums[id * 3 + 2] = sums[id * 3 + 2] + z
        counts[id] = counts[id] + 1
        return id
    }

    // O leitor não tem saída antecipada: parar no meio é lançar uma exceção própria daqui e pegar logo abaixo.
    try {
        forEachStlTriangle(bytes, onProgress) { x1, y1, z1, x2, y2, z2, x3, y3, z3 ->
            processed++
            val a = clusterOf(x1, y1, z1)
            val b = clusterOf(x2, y2, z2)
            val c = clusterOf(x3, y3, z3)
            if (a != b && b != c && a != c && seen.add(triangleKey(a, b, c))) {
                output.add(a)
                output.add(b)
                output.add(c)
            }
            if (output.size / 3 > limit || cells.size >= MAX_CLUSTERS) {
                throw StopClustering()
            }
        }
    } catch (_: StopClustering) {
        val fraction = processed.toDouble() / total
        val estimate = if (cells.size >= MAX_CLUSTERS) limit * 4.0 else (output.size / 3) / fraction.coerceAtLeast(1e-6)
        return Outcome.TooMany(max(estimate, limit * 1.2))
    }

    val positions = Array(cells.size) { id ->
        val n = counts[id].toDouble()
        Vec3((sums[id * 3] / n).toFloat(), (sums[id * 3 + 1] / n).toFloat(), (sums[id * 3 + 2] / n).toFloat())
    }
    val triangles = ArrayList<StlTriangle>(output.size / 3)
    var i = 0
    while (i < output.size) {
        val v1 = positions[output[i]]
        val v2 = positions[output[i + 1]]
        val v3 = positions[output[i + 2]]
        // Médias diferentes podem cair no mesmo Float: esse também é degenerado.
        if (v1 != v2 && v2 != v3 && v1 != v3) triangles += StlTriangle(Vec3.ZERO, v1, v2, v3)
        i += 3
    }
    return Outcome.Done(StlMesh(triangles))
}

/** Interrompe a passada pelo arquivo quando a grade já passou do alvo. Não sai deste arquivo. */
private class StopClustering : RuntimeException()

/** Chave do triângulo sem orientação (os três agrupamentos em ordem), pra jogar fora os repetidos. */
private fun triangleKey(a: Int, b: Int, c: Int): Long {
    val low = minOf(a, minOf(b, c))
    val high = maxOf(a, maxOf(b, c))
    val mid = a + b + c - low - high
    return (low.toLong() shl 42) or (mid.toLong() shl 21) or high.toLong()
}
