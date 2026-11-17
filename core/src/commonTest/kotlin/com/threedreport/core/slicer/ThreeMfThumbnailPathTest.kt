package com.threedreport.core.slicer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Caminho da miniatura dentro de um 3MF, com o que Bambu Studio, OrcaSlicer e PrusaSlicer gravam. */
class ThreeMfThumbnailPathTest {

    private val modelEntries = listOf("[Content_Types].xml", "_rels/.rels", "3D/3dmodel.model")

    @Test
    fun bambuStudioPointsToThePlateImageInTheRelationships() {
        val rels = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
             <Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
             <Relationship Target="/Metadata/plate_1.png" Id="rel-2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"/>
             <Relationship Target="/Metadata/plate_1.png" Id="rel-4" Type="http://schemas.bambulab.com/package/2021/cover-thumbnail-middle"/>
             <Relationship Target="/Metadata/plate_1_small.png" Id="rel-5" Type="http://schemas.bambulab.com/package/2021/cover-thumbnail-small"/>
            </Relationships>
        """.trimIndent()
        val entries = modelEntries + listOf("Metadata/pick_1.png", "Metadata/plate_1.png", "Metadata/plate_1_small.png", "Metadata/top_1.png")

        assertEquals("Metadata/plate_1.png", findThreeMfThumbnailPath(rels, entries))
    }

    @Test
    fun prusaSlicerPointsToMetadataThumbnail() {
        val rels = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/><Relationship Target="/Metadata/thumbnail.png" Id="rel-2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"/></Relationships>"""

        assertEquals("Metadata/thumbnail.png", findThreeMfThumbnailPath(rels, modelEntries + "Metadata/thumbnail.png"))
    }

    @Test
    fun attributesInAnyOrderWithSingleQuotes() {
        val rels = "<Relationships><Relationship Id='r2' Type='http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail' Target='Metadata/capa.jpg'/></Relationships>"

        assertEquals("Metadata/capa.jpg", findThreeMfThumbnailPath(rels, modelEntries + "Metadata/capa.jpg"))
    }

    @Test
    fun orcaSlicerWithoutRelationshipFallsBackToTheKnownThumbnailFolder() {
        val entries = modelEntries + listOf(
            "Auxiliaries/.thumbnails/thumbnail_small.png",
            "Auxiliaries/.thumbnails/thumbnail_middle.png",
            "Auxiliaries/.thumbnails/thumbnail_3mf.png",
        )

        assertEquals("Auxiliaries/.thumbnails/thumbnail_3mf.png", findThreeMfThumbnailPath(null, entries))
        assertEquals(
            "Auxiliaries/.thumbnails/thumbnail_middle.png",
            findThreeMfThumbnailPath(null, entries - "Auxiliaries/.thumbnails/thumbnail_3mf.png"),
        )
    }

    @Test
    fun relationshipToAMissingEntryFallsBackToKnownPaths() {
        val rels = "<Relationship Target=\"/Metadata/sumiu.png\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail\"/>"

        assertEquals("Metadata/plate_1.png", findThreeMfThumbnailPath(rels, modelEntries + "Metadata/plate_1.png"))
    }

    @Test
    fun withoutRelationshipPlatesArePickedByTheLowestNumberAndNeverThePickingImage() {
        val entries = modelEntries + listOf("Metadata/pick_1.png", "Metadata/plate_3.png", "Metadata/plate_2.png", "Metadata/plate_2_small.png")

        assertEquals("Metadata/plate_2.png", findThreeMfThumbnailPath(null, entries))
        assertNull(findThreeMfThumbnailPath(null, modelEntries + "Metadata/pick_1.png"))
    }

    @Test
    fun entryNamesAreMatchedIgnoringCaseAndLeadingSlash() {
        val rels = "<Relationship Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail\" Target=\"/metadata/Thumbnail.PNG\"/>"

        assertEquals("Metadata/thumbnail.png", findThreeMfThumbnailPath(rels, modelEntries + "Metadata/thumbnail.png"))
    }

    @Test
    fun packageWithoutAnyThumbnail() {
        assertNull(findThreeMfThumbnailPath(null, modelEntries))
        assertNull(findThreeMfThumbnailPath("não é xml", emptyList()))
    }
}
