package com.threedreport.core.stl

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sin

/**
 * Cópia da análise da 2.2 (antes da leitura em fluxo da Leva 10C), guardada só pra teste: a análise nova tem que
 * dar exatamente os mesmos números. Devolve as medidas sem o nível de dificuldade, que é a mesma função nos dois
 * caminhos.
 */
internal object ReferenceStlAnalyzer {

    data class Result(
        val surfaceAreaMm2: Double,
        val volumeMm3: Double,
        val overhangPercentage: Double,
        val triangleCount: Int,
        val disconnectedComponents: Int,
        val isManifold: Boolean,
    )

    fun analyze(mesh: StlMesh, overhangThresholdDegrees: Double = 45.0): Result {
        val triangles = mesh.triangles
        if (triangles.isEmpty()) return Result(0.0, 0.0, 0.0, 0, 0, true)

        var totalAreaMm2 = 0.0
        var overhangAreaMm2 = 0.0
        var signedVolumeSum = 0.0
        val overhangDotThreshold = sin(overhangThresholdDegrees * PI / 180.0)

        val vertexIds = HashMap<Triple<Int, Int, Int>, Int>()
        val parent = IntArray(triangles.size) { it }
        fun find(x: Int): Int {
            var root = x
            while (parent[root] != root) root = parent[root]
            return root
        }
        val firstTriangleForVertex = HashMap<Int, Int>()
        val edgeCounts = HashMap<Pair<Int, Int>, Int>()

        fun vertexId(v: Vec3): Int {
            val key = Triple(round(v.x * 1000f).toInt(), round(v.y * 1000f).toInt(), round(v.z * 1000f).toInt())
            return vertexIds.getOrPut(key) { vertexIds.size }
        }

        for ((index, triangle) in triangles.withIndex()) {
            val area = 0.5 * ((triangle.v2 - triangle.v1) cross (triangle.v3 - triangle.v1)).length.toDouble()
            totalAreaMm2 += area
            signedVolumeSum += (triangle.v1 dot (triangle.v2 cross triangle.v3)).toDouble()
            if (-triangle.computedNormal.z > overhangDotThreshold) overhangAreaMm2 += area

            val v1 = vertexId(triangle.v1)
            val v2 = vertexId(triangle.v2)
            val v3 = vertexId(triangle.v3)
            for (vertex in intArrayOf(v1, v2, v3)) {
                val firstTriangle = firstTriangleForVertex.getOrPut(vertex) { index }
                if (firstTriangle != index) {
                    val ra = find(index)
                    val rb = find(firstTriangle)
                    if (ra != rb) parent[ra] = rb
                }
            }
            for ((a, b) in listOf(v1 to v2, v2 to v3, v3 to v1)) {
                val edge = if (a < b) a to b else b to a
                edgeCounts[edge] = (edgeCounts[edge] ?: 0) + 1
            }
        }

        return Result(
            surfaceAreaMm2 = totalAreaMm2,
            volumeMm3 = abs(signedVolumeSum) / 6.0,
            overhangPercentage = if (totalAreaMm2 > 0) overhangAreaMm2 / totalAreaMm2 * 100.0 else 0.0,
            triangleCount = triangles.size,
            disconnectedComponents = triangles.indices.map { find(it) }.toSet().size,
            isManifold = edgeCounts.values.all { it == 2 },
        )
    }
}
