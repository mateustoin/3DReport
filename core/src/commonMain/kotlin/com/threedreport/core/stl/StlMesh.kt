package com.threedreport.core.stl

/**
 * Um triângulo da malha. [normal] é a normal gravada no arquivo — nem todo
 * exportador grava um valor confiável nela (alguns zeram), então o
 * visualizador usa [computedNormal] (calculada a partir dos vértices) pra
 * sombreamento, não [normal] diretamente.
 */
data class StlTriangle(val normal: Vec3, val v1: Vec3, val v2: Vec3, val v3: Vec3) {
    /** Normal geométrica, calculada a partir dos vértices — mais confiável que [normal]. */
    val computedNormal: Vec3
        get() = ((v2 - v1) cross (v3 - v1)).normalized()

    val centroid: Vec3
        get() = (v1 + v2 + v3) / 3f
}

/**
 * Malha de triângulos lida de um arquivo STL (ver [com.threedreport.core.stl.parseStl]).
 *
 * @property boundsMin/[boundsMax] caixa delimitadora da malha, calculada uma
 *   vez na criação — usada pelo visualizador pra centralizar e enquadrar a
 *   câmera automaticamente, sem o usuário precisar ajustar zoom na mão.
 */
class StlMesh(val triangles: List<StlTriangle>) {
    val boundsMin: Vec3
    val boundsMax: Vec3

    init {
        if (triangles.isEmpty()) {
            boundsMin = Vec3.ZERO
            boundsMax = Vec3.ZERO
        } else {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            for (triangle in triangles) {
                for (v in listOf(triangle.v1, triangle.v2, triangle.v3)) {
                    if (v.x < minX) minX = v.x
                    if (v.y < minY) minY = v.y
                    if (v.z < minZ) minZ = v.z
                    if (v.x > maxX) maxX = v.x
                    if (v.y > maxY) maxY = v.y
                    if (v.z > maxZ) maxZ = v.z
                }
            }
            boundsMin = Vec3(minX, minY, minZ)
            boundsMax = Vec3(maxX, maxY, maxZ)
        }
    }

    val center: Vec3 get() = (boundsMin + boundsMax) / 2f

    /** Raio da esfera que envolve a malha a partir de [center] — base pra distância inicial da câmera. */
    val boundingRadius: Float get() = (boundsMax - boundsMin).length / 2f
}
