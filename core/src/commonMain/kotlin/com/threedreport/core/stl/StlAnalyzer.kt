package com.threedreport.core.stl

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

/** Nível de dificuldade de impressão sugerido pra peça, calculado a partir de heurísticas da malha. */
enum class PrintDifficulty(val label: String) {
    FACIL("Fácil"),
    MEDIO("Médio"),
    DIFICIL("Difícil"),
}

/**
 * Resultado da análise geométrica de uma [StlMesh] (Fase 3 do roadmap — "Análise de
 * complexidade"). Tudo aqui é heurístico e uso **só interno** do criador (não entra no PDF nem no
 * copiar-colar) — serve pra decidir se cobra uma margem extra por complexidade, não substitui o
 * fatiador real.
 *
 * @property surfaceAreaMm2/[volumeMm3] medidos assumindo que as coordenadas do STL estão em
 *   milímetros (convenção usual de fatiadores/impressão 3D).
 * @property overhangPercentage % da área de superfície que aponta pra baixo além do ângulo limite
 *   configurado — proxy de necessidade de suporte, não uma simulação real de fatiamento.
 * @property disconnectedComponents quantas peças soltas existem no mesmo arquivo (1 = uma malha
 *   só). Mais partes tende a mais trabalho de organização na mesa de impressão.
 * @property isManifold `false` se a malha tem furos ou geometria não-manifold (aviso técnico
 *   separado do nível de dificuldade — "esse arquivo tem um problema", não "essa peça é difícil").
 */
data class StlAnalysis(
    val surfaceAreaMm2: Double,
    val volumeMm3: Double,
    val overhangPercentage: Double,
    val triangleCount: Int,
    val disconnectedComponents: Int,
    val isManifold: Boolean,
    val difficulty: PrintDifficulty,
)

/**
 * Calcula [StlAnalysis] a partir de uma [StlMesh] já carregada — reusa a mesma malha do
 * visualizador 3D, não lê o arquivo de novo.
 */
object StlAnalyzer {

    fun analyze(mesh: StlMesh, overhangThresholdDegrees: Double = 45.0): StlAnalysis {
        val triangles = mesh.triangles
        if (triangles.isEmpty()) {
            return StlAnalysis(0.0, 0.0, 0.0, 0, 0, isManifold = true, difficulty = PrintDifficulty.FACIL)
        }

        var totalAreaMm2 = 0.0
        var overhangAreaMm2 = 0.0
        var signedVolumeSum = 0.0
        // Face conta como overhang se aponta pra baixo além do limite configurado — comparação por
        // seno evita chamar acos() por triângulo (só um dot product contra um valor pré-calculado).
        val overhangDotThreshold = sin(overhangThresholdDegrees * PI / 180.0)

        val vertexIds = HashMap<Triple<Int, Int, Int>, Int>()
        val unionFind = UnionFind(triangles.size)
        val firstTriangleForVertex = HashMap<Int, Int>()
        val edgeCounts = HashMap<Pair<Int, Int>, Int>()

        for ((index, triangle) in triangles.withIndex()) {
            val area = 0.5 * ((triangle.v2 - triangle.v1) cross (triangle.v3 - triangle.v1)).length.toDouble()
            totalAreaMm2 += area

            signedVolumeSum += (triangle.v1 dot (triangle.v2 cross triangle.v3)).toDouble()

            if (-triangle.computedNormal.z > overhangDotThreshold) overhangAreaMm2 += area

            val v1 = vertexId(vertexIds, triangle.v1)
            val v2 = vertexId(vertexIds, triangle.v2)
            val v3 = vertexId(vertexIds, triangle.v3)

            for (vertex in intArrayOf(v1, v2, v3)) {
                val firstTriangle = firstTriangleForVertex.putIfAbsent(vertex, index)
                if (firstTriangle != null) unionFind.union(index, firstTriangle)
            }
            for ((a, b) in listOf(v1 to v2, v2 to v3, v3 to v1)) {
                val edge = if (a < b) a to b else b to a
                edgeCounts[edge] = (edgeCounts[edge] ?: 0) + 1
            }
        }

        val volumeMm3 = abs(signedVolumeSum) / 6.0
        val overhangPercentage = if (totalAreaMm2 > 0) overhangAreaMm2 / totalAreaMm2 * 100.0 else 0.0
        val componentCount = triangles.indices.map { unionFind.find(it) }.toSet().size
        val isManifold = edgeCounts.values.all { it == 2 }

        return StlAnalysis(
            surfaceAreaMm2 = totalAreaMm2,
            volumeMm3 = volumeMm3,
            overhangPercentage = overhangPercentage,
            triangleCount = triangles.size,
            disconnectedComponents = componentCount,
            isManifold = isManifold,
            difficulty = classifyDifficulty(totalAreaMm2, volumeMm3, overhangPercentage, triangles.size, componentCount),
        )
    }

    // Quantiza pra 0,001mm antes de usar como chave de vértice — tolera ruído de ponto flutuante
    // entre triângulos que deveriam compartilhar exatamente o mesmo vértice.
    private fun vertexId(vertexIds: HashMap<Triple<Int, Int, Int>, Int>, v: Vec3): Int {
        val key = Triple(round(v.x * 1000f).toInt(), round(v.y * 1000f).toInt(), round(v.z * 1000f).toInt())
        return vertexIds.getOrPut(key) { vertexIds.size }
    }

    /**
     * Combina 4 heurísticas independentes num nível de dificuldade — cada uma pontua 0 (fácil), 1
     * (médio) ou 2 (difícil); a soma (0-8) decide o nível final. Limiares são um chute educado, sem
     * benchmark real — ajustar se um caso real mostrar que estão errados.
     */
    private fun classifyDifficulty(
        areaMm2: Double,
        volumeMm3: Double,
        overhangPercentage: Double,
        triangleCount: Int,
        componentCount: Int,
    ): PrintDifficulty {
        // Razão área/volume normalizada contra uma esfera do mesmo volume (quociente isoperimétrico)
        // em vez da razão bruta — a razão bruta cai com o tamanho do objeto pra qualquer forma (um
        // cubo pequeno tem razão maior que um cubo grande, sem ser mais "complexo"), o que confundiria
        // tamanho com complexidade geométrica de verdade.
        val sphereArea = sphereSurfaceAreaForVolume(volumeMm3)
        val complexityRatio = if (sphereArea > 0.0) areaMm2 / sphereArea else 1.0

        var score = 0
        score += when { complexityRatio > 3.0 -> 2; complexityRatio > 1.5 -> 1; else -> 0 }
        score += when { overhangPercentage > 30.0 -> 2; overhangPercentage > 10.0 -> 1; else -> 0 }
        score += when { triangleCount > 100_000 -> 2; triangleCount > 20_000 -> 1; else -> 0 }
        score += when { componentCount > 3 -> 2; componentCount > 1 -> 1; else -> 0 }

        return when {
            score >= 6 -> PrintDifficulty.DIFICIL
            score >= 3 -> PrintDifficulty.MEDIO
            else -> PrintDifficulty.FACIL
        }
    }

    /** Área da esfera com o mesmo volume — base do quociente isoperimétrico usado em [classifyDifficulty]. */
    private fun sphereSurfaceAreaForVolume(volume: Double): Double {
        if (volume <= 0.0) return 0.0
        val radius = (3.0 * volume / (4.0 * PI)).pow(1.0 / 3.0)
        return 4.0 * PI * radius * radius
    }
}

/** União-busca simples sobre índices de triângulo, pra achar componentes desconexos da malha. */
private class UnionFind(size: Int) {
    private val parent = IntArray(size) { it }

    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) root = parent[root]
        var current = x
        while (parent[current] != root) {
            val next = parent[current]
            parent[current] = root
            current = next
        }
        return root
    }

    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parent[rootA] = rootB
    }
}
