package com.threedreport.core.stl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StlStreamingTest {

    private fun collect(bytes: ByteArray): List<List<Float>> {
        val out = ArrayList<List<Float>>()
        forEachStlTriangle(bytes) { x1, y1, z1, x2, y2, z2, x3, y3, z3 -> out += listOf(x1, y1, z1, x2, y2, z2, x3, y3, z3) }
        return out
    }

    private fun flatten(mesh: StlMesh): List<List<Float>> =
        mesh.triangles.map { listOf(it.v1.x, it.v1.y, it.v1.z, it.v2.x, it.v2.y, it.v2.z, it.v3.x, it.v3.y, it.v3.z) }

    /** Malhas variadas: fechada, aberta, várias peças, com vértice repetido e com degenerado. */
    private fun sampleMeshes(): List<List<StlTriangle>> {
        val tetra = StlTestMeshes.tetrahedron()
        val degenerate = StlTriangle(Vec3.ZERO, Vec3(5f, 5f, 5f), Vec3(5f, 5f, 5f), Vec3(6f, 5f, 5f))
        return listOf(
            emptyList(),
            tetra,
            tetra.dropLast(1),
            tetra + StlTestMeshes.tetrahedron(Vec3(100f, 100f, 100f)) + StlTestMeshes.tetrahedron(Vec3(-50f, 3f, 0f)),
            tetra + degenerate,
            StlTestMeshes.torus(24, 12),
            StlTestMeshes.torus(17, 9, majorRadius = 12.345f, minorRadius = 3.21f).drop(5),
        )
    }

    @Test
    fun walksBinaryTrianglesInFileOrder() {
        for (triangles in sampleMeshes()) {
            val bytes = StlTestMeshes.binaryStl(triangles)
            // No JS, Float não é arredondado pra 32 bits na memória: compara com o que o arquivo guardou.
            assertEquals(flatten(parseStl(bytes)), collect(bytes))
            assertEquals(triangles.size, collect(bytes).size)
        }
    }

    @Test
    fun walksAsciiTrianglesLikeTheParser() {
        for (triangles in sampleMeshes()) {
            val bytes = StlTestMeshes.asciiStl(triangles)
            assertEquals(flatten(parseStl(bytes)), collect(bytes))
            assertEquals(triangles.size, collect(bytes).size)
        }
    }

    @Test
    fun asciiWithWindowsLineBreaksAndUppercaseKeywords() {
        val ascii = "solid x\r\nFACET NORMAL 0 0 1\r\n OUTER LOOP\r\n  VERTEX 0 0 0\r\n  VERTEX 1 0 0\r\n  VERTEX 0 1 0\r\n ENDLOOP\r\nENDFACET\r\nendsolid x\r\n"
        val triangles = collect(ascii.encodeToByteArray())
        assertEquals(listOf(listOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f)), triangles)
        assertEquals(1L, peekStlTriangleCount(ascii.encodeToByteArray()))
    }

    @Test
    fun truncatedBinaryIsRefusedByTheWalkerToo() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.tetrahedron())
        assertFailsWith<IllegalArgumentException> { forEachStlTriangle(bytes.copyOf(bytes.size - 10)) { _, _, _, _, _, _, _, _, _ -> } }
    }

    @Test
    fun meshAnalysisMatchesTheAnalysisOfVersion22() {
        for (triangles in sampleMeshes()) {
            val mesh = StlMesh(triangles)
            for (threshold in listOf(45.0, 30.0, 60.0)) {
                assertSameAsReference(ReferenceStlAnalyzer.analyze(mesh, threshold), StlAnalyzer.analyze(mesh, threshold))
            }
        }
    }

    @Test
    fun streamingAnalysisOfTheFileMatchesTheAnalysisOfTheParsedMesh() {
        for (triangles in sampleMeshes()) {
            for (bytes in listOf(StlTestMeshes.binaryStl(triangles), StlTestMeshes.asciiStl(triangles))) {
                val expected = StlAnalyzer.analyze(parseStl(bytes))
                assertEquals(expected, analyzeStl(bytes))
                assertSameAsReference(ReferenceStlAnalyzer.analyze(parseStl(bytes)), analyzeStl(bytes))
            }
        }
    }

    private fun assertSameAsReference(reference: ReferenceStlAnalyzer.Result, actual: StlAnalysis) {
        assertEquals(reference.surfaceAreaMm2, actual.surfaceAreaMm2)
        assertEquals(reference.volumeMm3, actual.volumeMm3)
        assertEquals(reference.overhangPercentage, actual.overhangPercentage)
        assertEquals(reference.triangleCount, actual.triangleCount)
        assertEquals(reference.disconnectedComponents, actual.disconnectedComponents)
        assertEquals(reference.isManifold, actual.isManifold)
    }

    @Test
    fun progressGoesFromZeroToOneWithoutGoingBack() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(120, 60))
        val reported = ArrayList<Float>()
        analyzeStl(bytes, onProgress = { reported += it })
        assertTrue(reported.isNotEmpty())
        assertEquals(1f, reported.last())
        assertTrue(reported.zipWithNext().all { (a, b) -> b >= a }, "progresso não pode voltar: $reported")
        assertTrue(reported.all { it in 0f..1f })
    }

    @Test
    fun throwingFromTheProgressCallbackStopsTheWork() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(120, 60))
        class Stop : RuntimeException()
        assertFailsWith<Stop> { simplifyStl(bytes, targetTriangles = 500, onProgress = { if (it > 0.1f) throw Stop() }) }
    }

    @Test
    fun simplifiedMeshStaysUnderTheTargetAndCloseToIt() {
        val original = StlTestMeshes.torus(200, 100) // 40 mil triângulos
        val target = 2_000
        val mesh = simplifyStl(StlTestMeshes.binaryStl(original), targetTriangles = target)

        assertTrue(mesh.triangles.size <= target, "passou do alvo: ${mesh.triangles.size}")
        assertTrue(mesh.triangles.size >= target / 4, "longe demais do alvo: ${mesh.triangles.size}")
        val full = StlMesh(original)
        // A caixa da prévia simplificada fica perto da caixa da peça (enquadramento da câmera igual).
        assertTrue((mesh.boundsMax - full.boundsMax).length < 4f && (mesh.boundsMin - full.boundsMin).length < 4f)
    }

    @Test
    fun simplifiedMeshHasNoDegenerateNorRepeatedTriangles() {
        val mesh = simplifyStl(StlTestMeshes.binaryStl(StlTestMeshes.torus(200, 100)), targetTriangles = 2_000)
        val keys = HashSet<Set<Vec3>>()
        for (t in mesh.triangles) {
            assertTrue(t.v1 != t.v2 && t.v2 != t.v3 && t.v1 != t.v3, "triângulo degenerado: $t")
            assertTrue(keys.add(setOf(t.v1, t.v2, t.v3)), "triângulo repetido: $t")
        }
    }

    @Test
    fun meshAlreadyUnderTheTargetIsNotTouched() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(10, 10))
        assertEquals(flatten(parseStl(bytes)), flatten(simplifyStl(bytes, targetTriangles = 1_000)))
    }

    @Test
    fun previewConstantsAreTheOnesOfTheSpecification() {
        assertEquals(500_000, MAX_FULL_PREVIEW_STL_TRIANGLES)
        assertEquals(200_000, SIMPLIFIED_STL_TARGET_TRIANGLES)
        assertEquals(300L * 1024 * 1024, MAX_PREVIEW_STL_FILE_BYTES)
    }

    @Test
    fun smallFilePreviewIsTheFullMeshWithTheSameAnalysis() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(30, 20))
        val result = prepareStlPreview(bytes)

        assertIs<StlPreviewResult.Ready>(result)
        assertFalse(result.isSimplified)
        assertEquals(flatten(parseStl(bytes)), flatten(result.mesh))
        assertEquals(StlAnalyzer.analyze(parseStl(bytes)), result.analysis)
    }

    @Test
    fun heavyFilePreviewIsSimplifiedButTheAnalysisUsesTheFullFile() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(200, 100))
        val result = prepareStlPreview(bytes, fullPreviewLimit = 10_000, simplifiedTarget = 2_000)

        assertIs<StlPreviewResult.Ready>(result)
        assertTrue(result.isSimplified)
        assertTrue(result.mesh.triangles.size <= 2_000)
        assertEquals(40_000, result.analysis.triangleCount)
        assertEquals(StlAnalyzer.analyze(parseStl(bytes)), result.analysis)
    }

    @Test
    fun fileOverTheSizeLimitHasNoPreview() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(10, 10))
        assertEquals(StlPreviewResult.TooLarge(bytes.size.toLong()), prepareStlPreview(bytes, maxFileBytes = bytes.size - 1L))
    }

    /** Cabeçalho que declara [declared] triângulos num arquivo que só tem [actual]. */
    private fun lyingHeader(declared: Long, actual: Int): ByteArray {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(actual, 1).take(actual))
        for (k in 0 until 4) bytes[80 + k] = (declared shr (8 * k)).toByte()
        return bytes
    }

    @Test
    fun lyingHeaderDoesNotReserveMemoryForTrianglesThatAreNotInTheFile() {
        // Sem conferir com o tamanho, 50 milhões declarados reservavam mais de 1 GB antes de notar o corte.
        for (declared in listOf(50_000_000L, 0xFFFFFFFFL)) {
            val bytes = lyingHeader(declared, actual = 2)
            assertFailsWith<IllegalArgumentException> { analyzeStl(bytes) }
            assertFailsWith<IllegalArgumentException> { simplifyStl(bytes, targetTriangles = 1) }
            assertFailsWith<IllegalArgumentException> { prepareStlPreview(bytes) }
            assertFailsWith<IllegalArgumentException> { parseStl(bytes) }
        }
    }

    @Test
    fun fullMeshPreviewReportsProgressWhileReadingAndAnalysingSoItCanBeCancelled() {
        val bytes = StlTestMeshes.binaryStl(StlTestMeshes.torus(200, 100)) // 40 mil, abaixo do limite
        val reported = ArrayList<Float>()
        prepareStlPreview(bytes, onProgress = { reported += it })

        assertTrue(reported.any { it > 0f && it < 0.5f }, "sem progresso durante a leitura: $reported")
        assertTrue(reported.any { it > 0.5f && it < 1f }, "sem progresso durante a análise: $reported")
        assertTrue(reported.zipWithNext().all { (a, b) -> b >= a })

        class Stop : RuntimeException()
        var calls = 0
        assertFailsWith<Stop> { prepareStlPreview(bytes, onProgress = { if (++calls == 2) throw Stop() }) }
    }
}
