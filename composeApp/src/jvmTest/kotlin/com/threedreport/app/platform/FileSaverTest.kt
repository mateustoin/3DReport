package com.threedreport.app.platform

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileSaverTest {

    private lateinit var originalUserHome: String
    private lateinit var fakeHome: File

    @BeforeTest
    fun fakeUserHome() {
        originalUserHome = System.getProperty("user.home")
        fakeHome = createTempDirectory("3dreport-fakehome").toFile()
        System.setProperty("user.home", fakeHome.path)
    }

    @AfterTest
    fun restoreUserHome() {
        System.setProperty("user.home", originalUserHome)
    }

    @Test
    fun prefersDocumentsFolderWhenItExists() {
        File(fakeHome, "Documents").mkdirs()

        assertEquals(File(fakeHome, "Documents").path, defaultDocumentsDirectory())
    }

    @Test
    fun fallsBackToDocumentosFolderWhenThatIsWhatExists() {
        File(fakeHome, "Documentos").mkdirs()

        assertEquals(File(fakeHome, "Documentos").path, defaultDocumentsDirectory())
    }

    @Test
    fun fallsBackToHomeWhenNeitherExists() {
        assertEquals(fakeHome.path, defaultDocumentsDirectory())
    }
}
