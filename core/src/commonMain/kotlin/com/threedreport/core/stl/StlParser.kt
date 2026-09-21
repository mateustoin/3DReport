package com.threedreport.core.stl

/**
 * Lê um arquivo STL (binário ou ASCII) e devolve a malha de triângulos.
 *
 * Detecção de formato: um STL ASCII começa com "solid", mas alguns
 * exportadores gravam esse texto também no cabeçalho de um STL **binário**
 * (bug conhecido de alguns fatiadores/CAD) — por isso, mesmo quando o
 * arquivo começa com "solid", confere se o tamanho bate com a fórmula do
 * formato binário (`84 + contagem × 50` bytes) antes de decidir.
 *
 * @throws IllegalArgumentException se o arquivo binário estiver truncado
 *   (menor do que a contagem de triângulos declarada permite).
 */
fun parseStl(bytes: ByteArray): StlMesh {
    val looksAscii = bytes.size >= 5 && bytes.decodeToString(0, minOf(bytes.size, 5)).trim().startsWith("solid", ignoreCase = true)
    val isBinary = if (!looksAscii) {
        true
    } else if (bytes.size < 84) {
        false
    } else {
        val triangleCount = readUInt32LE(bytes, 80)
        84L + triangleCount * 50L == bytes.size.toLong()
    }
    return if (isBinary) parseBinaryStl(bytes) else parseAsciiStl(bytes)
}

private fun parseBinaryStl(bytes: ByteArray): StlMesh {
    require(bytes.size >= 84) { "Arquivo STL binário inválido: menor que o cabeçalho mínimo (84 bytes)." }
    val triangleCount = readUInt32LE(bytes, 80)
    val expectedSize = 84L + triangleCount * 50L
    require(bytes.size.toLong() >= expectedSize) {
        "Arquivo STL binário truncado: esperava $expectedSize bytes pra $triangleCount triângulos, mas o arquivo tem ${bytes.size}."
    }

    val triangles = ArrayList<StlTriangle>(triangleCount.toInt())
    var offset = 84
    repeat(triangleCount.toInt()) {
        val normal = readVec3LE(bytes, offset)
        val v1 = readVec3LE(bytes, offset + 12)
        val v2 = readVec3LE(bytes, offset + 24)
        val v3 = readVec3LE(bytes, offset + 36)
        triangles += StlTriangle(normal, v1, v2, v3)
        offset += 50
    }
    return StlMesh(triangles)
}

private val asciiFloatRegex = Regex("""-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?""")

private fun parseAsciiStl(bytes: ByteArray): StlMesh {
    val triangles = ArrayList<StlTriangle>()
    var currentNormal = Vec3.ZERO
    val vertices = ArrayList<Vec3>(3)

    for (rawLine in bytes.decodeToString().lineSequence()) {
        val line = rawLine.trim()
        when {
            line.startsWith("facet", ignoreCase = true) -> {
                val numbers = asciiFloatRegex.findAll(line).map { it.value.toFloat() }.toList()
                currentNormal = if (numbers.size >= 3) Vec3(numbers[0], numbers[1], numbers[2]) else Vec3.ZERO
                vertices.clear()
            }
            line.startsWith("vertex", ignoreCase = true) -> {
                val numbers = asciiFloatRegex.findAll(line).map { it.value.toFloat() }.toList()
                if (numbers.size >= 3) vertices += Vec3(numbers[0], numbers[1], numbers[2])
            }
            line.startsWith("endfacet", ignoreCase = true) -> {
                if (vertices.size == 3) {
                    triangles += StlTriangle(currentNormal, vertices[0], vertices[1], vertices[2])
                }
                vertices.clear()
            }
        }
    }
    return StlMesh(triangles)
}

private fun readUInt32LE(bytes: ByteArray, offset: Int): Long =
    (bytes[offset].toLong() and 0xFF) or
        ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
        ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
        ((bytes[offset + 3].toLong() and 0xFF) shl 24)

private fun readFloatLE(bytes: ByteArray, offset: Int): Float = Float.fromBits(readUInt32LE(bytes, offset).toInt())

private fun readVec3LE(bytes: ByteArray, offset: Int): Vec3 =
    Vec3(readFloatLE(bytes, offset), readFloatLE(bytes, offset + 4), readFloatLE(bytes, offset + 8))
