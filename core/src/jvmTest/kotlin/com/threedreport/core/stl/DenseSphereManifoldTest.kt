package com.threedreport.core.stl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A esfera das capturas (gerador "Esfera enorme" do `ScreenshotRenderer`) com 1200 segmentos aparece como não
 * manifold. Não é a análise em fluxo divergindo: perto dos polos, os vértices do primeiro anel ficam a uns
 * 592 / n² mm um do outro (0,0004 mm com n = 1200), menos que a quantização de 0,001 mm que a análise usa pra juntar
 * vértices desde a 2.2. Eles se fundem, e as arestas de lá deixam de aparecer exatamente duas vezes.
 */
class DenseSphereManifoldTest {

    /** Cópia do gerador das capturas, com o raio como parâmetro (lá é 30 mm). */
    private fun binaryStlSphere(segments: Int, radius: Double = 30.0): ByteArray {
        val triangles = 2 * segments * (segments - 1)
        val buffer = ByteBuffer.allocate(84 + 50 * triangles).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(80)
        buffer.putInt(triangles)
        fun point(lat: Int, lon: Int): FloatArray {
            val theta = Math.PI * lat / segments
            val phi = 2 * Math.PI * (lon % segments) / segments
            return floatArrayOf(
                (radius * Math.sin(theta) * Math.cos(phi)).toFloat(),
                (radius * Math.sin(theta) * Math.sin(phi)).toFloat(),
                (radius * Math.cos(theta)).toFloat(),
            )
        }
        fun put(a: FloatArray, b: FloatArray, c: FloatArray) {
            repeat(3) { buffer.putFloat(0f) }
            for (v in listOf(a, b, c)) v.forEach { buffer.putFloat(it) }
            buffer.putShort(0)
        }
        for (lat in 0 until segments) {
            for (lon in 0 until segments) {
                val p00 = point(lat, lon)
                val p01 = point(lat, lon + 1)
                val p10 = point(lat + 1, lon)
                val p11 = point(lat + 1, lon + 1)
                if (lat > 0) put(p00, p10, p01)
                if (lat < segments - 1) put(p01, p10, p11)
            }
        }
        return buffer.array()
    }

    private fun assertSameAsReference(bytes: ByteArray): StlAnalysis {
        val reference = ReferenceStlAnalyzer.analyze(parseStl(bytes))
        val streamed = analyzeStl(bytes)
        assertEquals(reference.surfaceAreaMm2, streamed.surfaceAreaMm2)
        assertEquals(reference.volumeMm3, streamed.volumeMm3)
        assertEquals(reference.overhangPercentage, streamed.overhangPercentage)
        assertEquals(reference.triangleCount, streamed.triangleCount)
        assertEquals(reference.disconnectedComponents, streamed.disconnectedComponents)
        assertEquals(reference.isManifold, streamed.isManifold)
        return streamed
    }

    @Test
    fun sphereWithWellSpacedVerticesIsManifoldInBothAnalyses() {
        assertTrue(assertSameAsReference(binaryStlSphere(segments = 300)).isManifold)
    }

    @Test
    fun sameSphereScaledSoPoleVerticesFallUnderTheQuantizationIsNotManifoldInBothAnalyses() {
        // Mesma malha, 30 vezes menor: no primeiro anel, 592 / 300² / 30 ≈ 0,0002 mm, como a de 1200 segmentos.
        assertFalse(assertSameAsReference(binaryStlSphere(segments = 300, radius = 1.0)).isManifold)
    }

    @Test
    fun screenshotSpheresGiveTheSameVerdictAsTheirSpacing() {
        // 600 segmentos (~720 mil triângulos, simplificada): 592 / 600² ≈ 0,0016 mm, acima de 0,001: manifold.
        assertTrue(analyzeStl(binaryStlSphere(segments = 600)).isManifold)
        // 1200 segmentos (~2,9 milhões): 0,0004 mm, vértices do polo se fundem: não manifold.
        assertFalse(analyzeStl(binaryStlSphere(segments = 1200)).isManifold)
    }
}
