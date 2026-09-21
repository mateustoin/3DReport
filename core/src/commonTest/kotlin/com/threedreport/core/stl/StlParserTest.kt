package com.threedreport.core.stl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StlParserTest {

    private fun floatBytesLE(value: Float): ByteArray {
        val bits = value.toRawBits()
        return byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits shr 8) and 0xFF).toByte(),
            ((bits shr 16) and 0xFF).toByte(),
            ((bits shr 24) and 0xFF).toByte(),
        )
    }

    private fun uint32BytesLE(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun vec3BytesLE(v: Vec3): ByteArray = floatBytesLE(v.x) + floatBytesLE(v.y) + floatBytesLE(v.z)

    /** Monta um STL binário válido com um triângulo por entrada de [triangles]. */
    private fun binaryStlBytes(triangles: List<StlTriangle>, header: ByteArray = ByteArray(80)): ByteArray {
        var bytes = header + uint32BytesLE(triangles.size)
        for (triangle in triangles) {
            bytes += vec3BytesLE(triangle.normal) + vec3BytesLE(triangle.v1) + vec3BytesLE(triangle.v2) +
                vec3BytesLE(triangle.v3) + byteArrayOf(0, 0)
        }
        return bytes
    }

    private val sampleTriangle = StlTriangle(
        normal = Vec3(0f, 0f, 1f),
        v1 = Vec3(0f, 0f, 0f),
        v2 = Vec3(1f, 0f, 0f),
        v3 = Vec3(0f, 1f, 0f),
    )

    @Test
    fun parsesBinaryStlWithOneTriangle() {
        val mesh = parseStl(binaryStlBytes(listOf(sampleTriangle)))

        assertEquals(1, mesh.triangles.size)
        assertEquals(sampleTriangle.v1, mesh.triangles[0].v1)
        assertEquals(sampleTriangle.v2, mesh.triangles[0].v2)
        assertEquals(sampleTriangle.v3, mesh.triangles[0].v3)
        assertEquals(sampleTriangle.normal, mesh.triangles[0].normal)
    }

    @Test
    fun parsesAsciiStlWithOneTriangle() {
        val ascii = """
            solid test
            facet normal 0 0 1
              outer loop
                vertex 0 0 0
                vertex 1 0 0
                vertex 0 1 0
              endloop
            endfacet
            endsolid test
        """.trimIndent()

        val mesh = parseStl(ascii.encodeToByteArray())

        assertEquals(1, mesh.triangles.size)
        assertEquals(Vec3(0f, 0f, 0f), mesh.triangles[0].v1)
        assertEquals(Vec3(1f, 0f, 0f), mesh.triangles[0].v2)
        assertEquals(Vec3(0f, 1f, 0f), mesh.triangles[0].v3)
    }

    @Test
    fun detectsBinaryStlEvenWhenHeaderStartsWithSolid() {
        // Alguns exportadores gravam "solid ..." no cabeçalho de um STL binário de verdade.
        val header = "solid exported by weird tool".encodeToByteArray().copyOf(80)
        val bytes = binaryStlBytes(listOf(sampleTriangle, sampleTriangle), header)

        val mesh = parseStl(bytes)

        assertEquals(2, mesh.triangles.size)
    }

    @Test
    fun throwsOnTruncatedBinaryFile() {
        val fullBytes = binaryStlBytes(listOf(sampleTriangle, sampleTriangle))
        val truncated = fullBytes.copyOf(fullBytes.size - 10)

        assertFailsWith<IllegalArgumentException> { parseStl(truncated) }
    }

    @Test
    fun computesMeshBounds() {
        val triangle = StlTriangle(
            normal = Vec3.ZERO,
            v1 = Vec3(-1f, -2f, -3f),
            v2 = Vec3(4f, 5f, 6f),
            v3 = Vec3(0f, 0f, 0f),
        )
        val mesh = StlMesh(listOf(triangle))

        assertEquals(Vec3(-1f, -2f, -3f), mesh.boundsMin)
        assertEquals(Vec3(4f, 5f, 6f), mesh.boundsMax)
        assertEquals(Vec3(1.5f, 1.5f, 1.5f), mesh.center)
    }

    @Test
    fun computedNormalIsRobustWhenFileNormalIsZero() {
        val triangle = StlTriangle(normal = Vec3.ZERO, v1 = Vec3(0f, 0f, 0f), v2 = Vec3(1f, 0f, 0f), v3 = Vec3(0f, 1f, 0f))

        val computed = triangle.computedNormal

        assertTrue(computed.length > 0.99f && computed.length < 1.01f, "normal computada deveria ser unitária")
        assertEquals(Vec3(0f, 0f, 1f), computed)
    }
}
