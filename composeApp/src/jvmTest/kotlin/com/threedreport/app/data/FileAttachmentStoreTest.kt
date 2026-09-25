package com.threedreport.app.data

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Anexos endereçados pelo conteúdo (decisão 108): a chave é o arquivo, e um arquivo gravado nunca muda. */
class FileAttachmentStoreTest {

    private lateinit var dir: File
    private lateinit var store: FileAttachmentStore

    @BeforeTest
    fun createStore() {
        dir = createTempDirectory("3dreport-anexos").toFile()
        store = FileAttachmentStore(dir)
    }

    @Test
    fun theSameBytesAreStoredOnceUnderTheSameKey() {
        val first = store.put(byteArrayOf(1, 2, 3), "foto.PNG")
        val second = store.put(byteArrayOf(1, 2, 3), "outra-foto.png")

        assertEquals(first, second)
        assertTrue(first.endsWith(".png"))
        assertEquals(1, dir.listFiles()!!.count { it.isFile })
    }

    @Test
    fun differentBytesGetDifferentKeysSoNothingIsOverwritten() {
        val old = store.put(byteArrayOf(1), "foto.png")
        val new = store.put(byteArrayOf(2), "foto.png")

        assertNotEquals(old, new)
        assertContentEquals(byteArrayOf(1), store.read(old))
    }

    @Test
    fun aKeyThatCouldLeaveTheFolderIsNeverRead() {
        File(dir.parentFile, "segredo.txt").writeText("fora da pasta")

        assertNull(store.read("../segredo.txt"))
    }

    @Test
    fun theThumbnailIsSmallerAndCached() {
        val key = store.put(pngOf(1200, 800), "foto.png")

        val thumbnail = store.thumbnail(key, 256)

        val image = ImageIO.read(thumbnail!!.inputStream())
        assertEquals(256, image.width)
        assertTrue(File(dir, "thumbs/256/$key.png").isFile)
    }

    @Test
    fun garbageCollectionKeepsWhatIsReferencedAndRemovesTheRestWithItsThumbnails() {
        val kept = store.put(pngOf(10, 10), "a.png")
        val orphan = store.put(pngOf(20, 20), "b.png")
        assertNotNull(store.thumbnail(orphan, 64))

        assertEquals(1, store.collectGarbage(setOf(kept)))

        assertTrue(store.exists(kept))
        assertNull(store.read(orphan))
        assertTrue(!File(dir, "thumbs/64/$orphan.png").exists())
    }

    private fun pngOf(width: Int, height: Int): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
}
