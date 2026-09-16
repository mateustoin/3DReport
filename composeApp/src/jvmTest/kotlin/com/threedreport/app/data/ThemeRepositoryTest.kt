package com.threedreport.app.data

import com.threedreport.core.model.ThemeMode
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Valida que a preferência de tema sobrevive a uma nova instância do repositório (persistência em disco). */
class ThemeRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsWithSystemMode() {
        assertEquals(ThemeMode.SYSTEM, ThemeRepository().mode.value)
    }

    @Test
    fun updatedModeSurvivesNewRepositoryInstance() {
        val original = ThemeRepository()
        original.update(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, ThemeRepository().mode.value)
    }
}
