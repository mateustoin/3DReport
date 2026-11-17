package com.threedreport.core.stl

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nível de dificuldade de impressão sugerido pra peça, calculado a partir de heurísticas da malha. Sem texto: a
 * interface escolhe o rótulo (`labelRes()` no `composeApp`).
 */
enum class PrintDifficulty {
    FACIL,
    MEDIO,
    DIFICIL,
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
 * Calcula [StlAnalysis] a partir de uma [StlMesh] já carregada ([analyze]) ou direto dos bytes do arquivo
 * ([analyzeStl], sem montar a malha). Os dois caminhos passam pela mesma conta, triângulo a triângulo, e dão
 * exatamente o mesmo resultado (teste comparando com a análise da 2.2).
 */
object StlAnalyzer {

    fun analyze(mesh: StlMesh, overhangThresholdDegrees: Double = 45.0): StlAnalysis {
        val accumulator = StlAnalysisAccumulator(overhangThresholdDegrees, mesh.triangles.size)
        for (t in mesh.triangles) {
            accumulator.triangle(t.v1.x, t.v1.y, t.v1.z, t.v2.x, t.v2.y, t.v2.z, t.v3.x, t.v3.y, t.v3.z)
        }
        return accumulator.result()
    }

    /**
     * Combina 4 heurísticas independentes num nível de dificuldade — cada uma pontua 0 (fácil), 1
     * (médio) ou 2 (difícil); a soma (0-8) decide o nível final. Limiares são um chute educado, sem
     * benchmark real — ajustar se um caso real mostrar que estão errados.
     */
    internal fun classifyDifficulty(
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

/**
 * Análise de um STL lido do arquivo, sem montar a malha (ver [StlAnalyzer]). Guarda só o que a conta precisa, em
 * tipos primitivos: o número de cada vértice, a união-busca por vértice e as arestas, perto de 50 bytes por
 * triângulo (a malha com objetos passa de 300).
 *
 * @throws IllegalArgumentException se o arquivo binário estiver truncado.
 */
fun analyzeStl(bytes: ByteArray, overhangThresholdDegrees: Double = 45.0, onProgress: StlProgress? = null): StlAnalysis =
    analyzeStlWithBounds(bytes, overhangThresholdDegrees, onProgress).analysis

/** A análise mais a caixa da peça, que a simplificação usa pra montar a grade sem ler o arquivo de novo. */
internal class AnalysisWithBounds(val analysis: StlAnalysis, val boundsMin: Vec3, val boundsMax: Vec3)

internal fun analyzeStlWithBounds(bytes: ByteArray, overhangThresholdDegrees: Double, onProgress: StlProgress?): AnalysisWithBounds {
    val expected = peekStlTriangleCount(bytes).coerceIn(0L, Int.MAX_VALUE.toLong() / 4).toInt()
    val accumulator = StlAnalysisAccumulator(overhangThresholdDegrees, expected)
    forEachStlTriangle(bytes, onProgress, accumulator)
    return AnalysisWithBounds(accumulator.result(), accumulator.boundsMin(), accumulator.boundsMax())
}

/**
 * A conta da análise, um triângulo por vez. As operações em `Float` seguem a mesma ordem das de [Vec3] na
 * análise da 2.2 (diferença, produto vetorial, comprimento), pra dar o mesmo número até o último bit.
 *
 * Peças soltas: a união-busca é por vértice, não por triângulo. Dá a mesma contagem, porque todo vértice está
 * em algum triângulo e os três vértices de um triângulo ficam juntos. Malha fechada: cada aresta aparece
 * exatamente duas vezes; as arestas vão numa lista de `Long` e são contadas depois de ordenar.
 */
internal class StlAnalysisAccumulator(overhangThresholdDegrees: Double, expectedTriangles: Int) : StlTriangleVisitor {
    // Face conta como overhang se aponta pra baixo além do limite configurado — comparação por
    // seno evita chamar acos() por triângulo (só um dot product contra um valor pré-calculado).
    private val overhangDotThreshold = sin(overhangThresholdDegrees * PI / 180.0)
    private var totalAreaMm2 = 0.0
    private var overhangAreaMm2 = 0.0
    private var signedVolumeSum = 0.0
    private var count = 0

    private val vertexIds = Int3IdTable(expectedTriangles / 2 + 16)
    private val parent = IntList(expectedTriangles / 2 + 16)
    private val edges = LongList(expectedTriangles.toLong().times(3).coerceAtMost(Int.MAX_VALUE - 8L).toInt())

    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var minZ = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var maxY = -Float.MAX_VALUE
    private var maxZ = -Float.MAX_VALUE

    override fun triangle(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float) {
        count++
        // (v2 - v1) cross (v3 - v1)
        val ax = x2 - x1
        val ay = y2 - y1
        val az = z2 - z1
        val bx = x3 - x1
        val by = y3 - y1
        val bz = z3 - z1
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        val length = sqrt(cx * cx + cy * cy + cz * cz)
        val area = 0.5 * length.toDouble()
        totalAreaMm2 += area

        // v1 dot (v2 cross v3)
        val vx = y2 * z3 - z2 * y3
        val vy = z2 * x3 - x2 * z3
        val vz = x2 * y3 - y2 * x3
        signedVolumeSum += (x1 * vx + y1 * vy + z1 * vz).toDouble()

        val normalZ = if (length == 0f) cz else cz / length
        if (-normalZ > overhangDotThreshold) overhangAreaMm2 += area

        val v1 = vertexId(x1, y1, z1)
        val v2 = vertexId(x2, y2, z2)
        val v3 = vertexId(x3, y3, z3)
        union(v1, v2)
        union(v1, v3)
        edges.add(edgeKey(v1, v2))
        edges.add(edgeKey(v2, v3))
        edges.add(edgeKey(v3, v1))

        includeInBounds(x1, y1, z1)
        includeInBounds(x2, y2, z2)
        includeInBounds(x3, y3, z3)
    }

    // Quantiza pra 0,001mm antes de usar como chave de vértice — tolera ruído de ponto flutuante
    // entre triângulos que deveriam compartilhar exatamente o mesmo vértice.
    private fun vertexId(x: Float, y: Float, z: Float): Int {
        val before = vertexIds.size
        val id = vertexIds.idOf(round(x * 1000f).toInt(), round(y * 1000f).toInt(), round(z * 1000f).toInt())
        if (vertexIds.size > before) parent.add(id)
        return id
    }

    private fun edgeKey(a: Int, b: Int): Long {
        val low = minOf(a, b).toLong()
        val high = maxOf(a, b).toLong()
        return (low shl 32) or high
    }

    private fun find(x: Int): Int {
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

    private fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parent[rootA] = rootB
    }

    private fun includeInBounds(x: Float, y: Float, z: Float) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (z < minZ) minZ = z
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        if (z > maxZ) maxZ = z
    }

    fun boundsMin(): Vec3 = if (count == 0) Vec3.ZERO else Vec3(minX, minY, minZ)

    fun boundsMax(): Vec3 = if (count == 0) Vec3.ZERO else Vec3(maxX, maxY, maxZ)

    fun result(): StlAnalysis {
        if (count == 0) return StlAnalysis(0.0, 0.0, 0.0, 0, 0, isManifold = true, difficulty = PrintDifficulty.FACIL)

        val volumeMm3 = abs(signedVolumeSum) / 6.0
        val overhangPercentage = if (totalAreaMm2 > 0) overhangAreaMm2 / totalAreaMm2 * 100.0 else 0.0
        var componentCount = 0
        for (vertex in 0 until parent.size) if (find(vertex) == vertex) componentCount++

        val sortedEdges = edges.values
        sortedEdges.sort(0, edges.size)
        var isManifold = true
        var i = 0
        while (i < edges.size && isManifold) {
            var j = i + 1
            while (j < edges.size && sortedEdges[j] == sortedEdges[i]) j++
            if (j - i != 2) isManifold = false
            i = j
        }

        return StlAnalysis(
            surfaceAreaMm2 = totalAreaMm2,
            volumeMm3 = volumeMm3,
            overhangPercentage = overhangPercentage,
            triangleCount = count,
            disconnectedComponents = componentCount,
            isManifold = isManifold,
            difficulty = StlAnalyzer.classifyDifficulty(totalAreaMm2, volumeMm3, overhangPercentage, count, componentCount),
        )
    }
}
