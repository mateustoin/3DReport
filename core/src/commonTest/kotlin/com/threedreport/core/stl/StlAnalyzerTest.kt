package com.threedreport.core.stl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StlAnalyzerTest {

    // Tetraedro com vértices na origem e nos 3 eixos — A=(0,0,0), B=(1,0,0), C=(0,1,0), D=(0,0,1).
    // Volume real = 1/6; área real = 0,5 + 0,5 + 0,5 + (√3)/2 ≈ 2,3660 (verificado à mão). Winding de
    // cada face escolhido pra normal apontar pra fora (testado via produto vetorial antes de escrever
    // o teste) — importante pro cálculo de volume, que depende de orientação consistente.
    private val a = Vec3(0f, 0f, 0f)
    private val b = Vec3(1f, 0f, 0f)
    private val c = Vec3(0f, 1f, 0f)
    private val d = Vec3(0f, 0f, 1f)

    private fun tetrahedronTriangles() = listOf(
        StlTriangle(Vec3.ZERO, a, c, b), // base (z=0), normal aponta pra baixo
        StlTriangle(Vec3.ZERO, a, b, d),
        StlTriangle(Vec3.ZERO, a, d, c),
        StlTriangle(Vec3.ZERO, b, c, d),
    )

    @Test
    fun computesAreaAndVolumeOfATetrahedron() {
        val analysis = StlAnalyzer.analyze(StlMesh(tetrahedronTriangles()))

        assertEquals(1.0 / 6.0, analysis.volumeMm3, 0.0001)
        assertEquals(0.5 + 0.5 + 0.5 + kotlin.math.sqrt(3.0) / 2.0, analysis.surfaceAreaMm2, 0.0001)
    }

    @Test
    fun tetrahedronIsOneManifoldComponent() {
        val analysis = StlAnalyzer.analyze(StlMesh(tetrahedronTriangles()))

        assertEquals(1, analysis.disconnectedComponents)
        assertTrue(analysis.isManifold)
    }

    @Test
    fun openMeshMissingAFaceIsNotManifold() {
        val openMesh = StlMesh(tetrahedronTriangles().dropLast(1)) // remove a face BCD -> deixa um furo

        val analysis = StlAnalyzer.analyze(openMesh)

        assertFalse(analysis.isManifold)
    }

    @Test
    fun twoSeparateTetrahedraCountAsTwoComponents() {
        val offset = Vec3(100f, 100f, 100f)
        val secondTetrahedron = tetrahedronTriangles().map {
            StlTriangle(Vec3.ZERO, it.v1 + offset, it.v2 + offset, it.v3 + offset)
        }

        val analysis = StlAnalyzer.analyze(StlMesh(tetrahedronTriangles() + secondTetrahedron))

        assertEquals(2, analysis.disconnectedComponents)
        assertEquals(8, analysis.triangleCount)
    }

    @Test
    fun downwardFacingTriangleIsFullOverhang() {
        // (v2-v1)x(v3-v1) = (0,1,0)x(1,0,0) = (0,0,-1) -> normal aponta reto pra baixo.
        val downwardTriangle = StlTriangle(
            normal = Vec3.ZERO,
            v1 = Vec3(0f, 0f, 0f),
            v2 = Vec3(0f, 1f, 0f),
            v3 = Vec3(1f, 0f, 0f),
        )

        val analysis = StlAnalyzer.analyze(StlMesh(listOf(downwardTriangle)))

        assertEquals(100.0, analysis.overhangPercentage, 0.0001)
    }

    @Test
    fun upwardFacingTriangleHasNoOverhang() {
        // (v2-v1)x(v3-v1) = (1,0,0)x(0,1,0) = (0,0,1) -> normal aponta reto pra cima.
        val upwardTriangle = StlTriangle(
            normal = Vec3.ZERO,
            v1 = Vec3(0f, 0f, 0f),
            v2 = Vec3(1f, 0f, 0f),
            v3 = Vec3(0f, 1f, 0f),
        )

        val analysis = StlAnalyzer.analyze(StlMesh(listOf(upwardTriangle)))

        assertEquals(0.0, analysis.overhangPercentage, 0.0001)
    }

    @Test
    fun simpleCompactShapeIsClassifiedAsFacil() {
        val analysis = StlAnalyzer.analyze(StlMesh(tetrahedronTriangles()))

        assertEquals(PrintDifficulty.FACIL, analysis.difficulty)
    }

    @Test
    fun emptyMeshIsFacilWithNoAreaOrVolume() {
        val analysis = StlAnalyzer.analyze(StlMesh(emptyList()))

        assertEquals(0.0, analysis.surfaceAreaMm2)
        assertEquals(0.0, analysis.volumeMm3)
        assertEquals(0, analysis.triangleCount)
        assertEquals(PrintDifficulty.FACIL, analysis.difficulty)
    }
}
