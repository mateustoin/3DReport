package com.threedreport.core.stl

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * STL de milhões de triângulos (decisão 142): a prévia sai sem estourar a memória e as medidas batem com a peça
 * inteira. Roda na JVM de teste do Gradle, que tem 512 MB de heap, menos que a memória padrão do app.
 */
class LargeStlPreviewTest {

    @Test
    fun twoMillionTrianglePreviewFitsInTheTestHeap() {
        val rings = 1_000
        val sides = 1_000
        val bytes = syntheticTorusStl(rings, sides, majorRadius = 60.0, minorRadius = 20.0)
        val reported = ArrayList<Float>()

        val result = prepareStlPreview(bytes, onProgress = { reported += it })

        assertIs<StlPreviewResult.Ready>(result)
        assertTrue(result.isSimplified)
        assertTrue(result.mesh.triangles.size <= SIMPLIFIED_STL_TARGET_TRIANGLES, "${result.mesh.triangles.size}")
        assertTrue(result.mesh.triangles.size >= SIMPLIFIED_STL_TARGET_TRIANGLES / 4, "${result.mesh.triangles.size}")
        val analysis = result.analysis
        assertEquals(rings * sides * 2, analysis.triangleCount)
        assertEquals(1, analysis.disconnectedComponents)
        assertTrue(analysis.isManifold)
        // Toro de verdade: área 4π²Rr e volume 2π²Rr². A malha facetada fica um pouco abaixo.
        assertTrue(abs(analysis.surfaceAreaMm2 / (4 * PI * PI * 60 * 20) - 1) < 0.001, "${analysis.surfaceAreaMm2}")
        assertTrue(abs(analysis.volumeMm3 / (2 * PI * PI * 60 * 20 * 20) - 1) < 0.001, "${analysis.volumeMm3}")
        assertEquals(1f, reported.last())
        assertTrue(reported.zipWithNext().all { (a, b) -> b >= a })
    }

    companion object {
        /** Toro em STL binário escrito direto nos bytes, sem objeto por triângulo. */
        fun syntheticTorusStl(rings: Int, sides: Int, majorRadius: Double, minorRadius: Double): ByteArray {
            val count = rings * sides * 2
            val bytes = ByteArray(84 + count * 50)
            putInt(bytes, 80, count)
            val px = FloatArray(rings * sides)
            val py = FloatArray(rings * sides)
            val pz = FloatArray(rings * sides)
            for (i in 0 until rings) {
                for (j in 0 until sides) {
                    val u = 2.0 * PI * i / rings
                    val v = 2.0 * PI * j / sides
                    val r = majorRadius + minorRadius * cos(v)
                    px[i * sides + j] = (r * cos(u)).toFloat()
                    py[i * sides + j] = (r * sin(u)).toFloat()
                    pz[i * sides + j] = (minorRadius * sin(v)).toFloat()
                }
            }
            var offset = 84
            fun vertex(i: Int, j: Int) {
                val k = (i % rings) * sides + (j % sides)
                putInt(bytes, offset, px[k].toRawBits())
                putInt(bytes, offset + 4, py[k].toRawBits())
                putInt(bytes, offset + 8, pz[k].toRawBits())
                offset += 12
            }
            for (i in 0 until rings) {
                for (j in 0 until sides) {
                    offset += 12
                    vertex(i, j)
                    vertex(i + 1, j)
                    vertex(i + 1, j + 1)
                    offset += 2
                    offset += 12
                    vertex(i, j)
                    vertex(i + 1, j + 1)
                    vertex(i, j + 1)
                    offset += 2
                }
            }
            return bytes
        }

        private fun putInt(bytes: ByteArray, offset: Int, value: Int) {
            bytes[offset] = value.toByte()
            bytes[offset + 1] = (value shr 8).toByte()
            bytes[offset + 2] = (value shr 16).toByte()
            bytes[offset + 3] = (value shr 24).toByte()
        }

        /**
         * Medição manual do pico de memória (não roda nos testes): `java -Xmx<heap> -cp <classes> ...
         * LargeStlPreviewTest <milhões de triângulos>`. Imprime o pico do heap durante a prévia.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val millions = args.firstOrNull()?.toDouble() ?: 4.0
            val side = kotlin.math.sqrt(millions * 1_000_000 / 2).toInt()
            val bytes = syntheticTorusStl(side, side, 60.0, 20.0)
            System.gc()
            ManagementFactory.getMemoryPoolMXBeans().forEach { it.resetPeakUsage() }
            val started = System.nanoTime()
            val result = prepareStlPreview(bytes) as StlPreviewResult.Ready
            val seconds = (System.nanoTime() - started) / 1e9
            val peak = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }.sumOf { it.peakUsage.used }
            println(
                "triângulos=${side * side * 2} arquivo=${bytes.size / 1048576} MB pico=${peak / 1048576} MB " +
                    "tempo=${"%.1f".format(seconds)} s prévia=${result.mesh.triangles.size}",
            )
        }
    }
}
