package com.threedreport.app.platform

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Pacotes 3MF mínimos montados aqui, no formato de cada fatiador, sem arquivo de terceiros no repositório. */
class ThreeMfThumbnailTest {

    private val png: ByteArray = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB), "png", it) }.toByteArray()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun rels(target: String) = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
 <Relationship Target="$target" Id="rel-2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"/>
</Relationships>""".encodeToByteArray()

    private val model = "3D/3dmodel.model" to "<model/>".encodeToByteArray()
    private val contentTypes = "[Content_Types].xml" to "<Types/>".encodeToByteArray()

    @Test
    fun bambuStudioPackage() {
        val bytes = zip(
            contentTypes,
            "_rels/.rels" to rels("/Metadata/plate_1.png"),
            model,
            "Metadata/pick_1.png" to byteArrayOf(1, 2, 3),
            "Metadata/plate_1.png" to png,
            "Metadata/plate_1_small.png" to png,
        )

        val result = readThreeMfThumbnail(bytes)

        assertIs<ThreeMfThumbnail.Found>(result)
        assertEquals("Metadata/plate_1.png", result.path)
        assertContentEquals(png, result.imageBytes)
    }

    @Test
    fun prusaSlicerPackage() {
        val bytes = zip(contentTypes, "_rels/.rels" to rels("/Metadata/thumbnail.png"), model, "Metadata/thumbnail.png" to png)

        val result = readThreeMfThumbnail(bytes)

        assertIs<ThreeMfThumbnail.Found>(result)
        assertEquals("Metadata/thumbnail.png", result.path)
    }

    @Test
    fun orcaSlicerPackageWithoutThumbnailRelationship() {
        val plainRels = """<Relationships><Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/></Relationships>"""
        val bytes = zip(
            contentTypes,
            "_rels/.rels" to plainRels.encodeToByteArray(),
            model,
            "Auxiliaries/.thumbnails/thumbnail_small.png" to png,
            "Auxiliaries/.thumbnails/thumbnail_3mf.png" to png,
        )

        val result = readThreeMfThumbnail(bytes)

        assertIs<ThreeMfThumbnail.Found>(result)
        assertEquals("Auxiliaries/.thumbnails/thumbnail_3mf.png", result.path)
    }

    @Test
    fun packageWithoutThumbnail() {
        assertEquals(ThreeMfThumbnail.Missing, readThreeMfThumbnail(zip(contentTypes, model)))
    }

    @Test
    fun thumbnailThatIsNotAnImageCountsAsMissing() {
        val bytes = zip(contentTypes, "_rels/.rels" to rels("/Metadata/thumbnail.png"), model, "Metadata/thumbnail.png" to "não é png".encodeToByteArray())
        assertEquals(ThreeMfThumbnail.Missing, readThreeMfThumbnail(bytes))
    }

    @Test
    fun fileThatIsNotAPackage() {
        assertEquals(ThreeMfThumbnail.Unreadable, readThreeMfThumbnail("solid peça\nendsolid".encodeToByteArray()))
        assertEquals(ThreeMfThumbnail.Unreadable, readThreeMfThumbnail(ByteArray(0)))
    }

    @Test
    fun truncatedPackage() {
        val bytes = zip(contentTypes, "_rels/.rels" to rels("/Metadata/thumbnail.png"), model, "Metadata/thumbnail.png" to png)
        val result = readThreeMfThumbnail(bytes.copyOf(bytes.size / 2))
        // Cortado no meio: o que vem antes do corte ainda pode ser lido, mas nunca vira exceção.
        assertIs<ThreeMfThumbnail>(result)
    }
}
