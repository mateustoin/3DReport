package com.threedreport.core.stl

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Malhas e bytes de STL montados nos testes, sem arquivo de terceiros no repositório. */
internal object StlTestMeshes {

    fun binaryStl(triangles: List<StlTriangle>, header: ByteArray = ByteArray(80)): ByteArray {
        val bytes = ByteArray(84 + triangles.size * 50)
        header.copyInto(bytes, 0, 0, minOf(80, header.size))
        writeInt(bytes, 80, triangles.size)
        var offset = 84
        for (t in triangles) {
            for (v in listOf(t.normal, t.v1, t.v2, t.v3)) {
                writeInt(bytes, offset, v.x.toRawBits())
                writeInt(bytes, offset + 4, v.y.toRawBits())
                writeInt(bytes, offset + 8, v.z.toRawBits())
                offset += 12
            }
            offset += 2
        }
        return bytes
    }

    fun asciiStl(triangles: List<StlTriangle>): ByteArray = buildString {
        appendLine("solid teste")
        for (t in triangles) {
            appendLine("  facet normal ${t.normal.x} ${t.normal.y} ${t.normal.z}")
            appendLine("    outer loop")
            for (v in listOf(t.v1, t.v2, t.v3)) appendLine("      vertex ${v.x} ${v.y} ${v.z}")
            appendLine("    endloop")
            appendLine("  endfacet")
        }
        appendLine("endsolid teste")
    }.encodeToByteArray()

    private fun writeInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
        bytes[offset + 2] = (value shr 16).toByte()
        bytes[offset + 3] = (value shr 24).toByte()
    }

    /** Toro fechado com [rings] × [sides] × 2 triângulos: uma peça só, manifold, com faces pra todo lado. */
    fun torus(rings: Int, sides: Int, majorRadius: Float = 30f, minorRadius: Float = 10f): List<StlTriangle> {
        fun point(i: Int, j: Int): Vec3 {
            val u = 2.0 * PI * (i % rings) / rings
            val v = 2.0 * PI * (j % sides) / sides
            val r = majorRadius + minorRadius * cos(v)
            return Vec3((r * cos(u)).toFloat(), (r * sin(u)).toFloat(), (minorRadius * sin(v)).toFloat())
        }
        val triangles = ArrayList<StlTriangle>(rings * sides * 2)
        for (i in 0 until rings) {
            for (j in 0 until sides) {
                val a = point(i, j)
                val b = point(i + 1, j)
                val c = point(i + 1, j + 1)
                val d = point(i, j + 1)
                triangles += StlTriangle(Vec3.ZERO, a, b, c)
                triangles += StlTriangle(Vec3.ZERO, a, c, d)
            }
        }
        return triangles
    }

    private val a = Vec3(0f, 0f, 0f)
    private val b = Vec3(1f, 0f, 0f)
    private val c = Vec3(0f, 1f, 0f)
    private val d = Vec3(0f, 0f, 1f)

    fun tetrahedron(offset: Vec3 = Vec3.ZERO): List<StlTriangle> = listOf(
        StlTriangle(Vec3.ZERO, a + offset, c + offset, b + offset),
        StlTriangle(Vec3.ZERO, a + offset, b + offset, d + offset),
        StlTriangle(Vec3.ZERO, a + offset, d + offset, c + offset),
        StlTriangle(Vec3.ZERO, b + offset, c + offset, d + offset),
    )
}
