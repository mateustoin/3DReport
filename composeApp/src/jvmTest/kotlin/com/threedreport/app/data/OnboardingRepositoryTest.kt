package com.threedreport.app.data

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsIncompleteOnAFreshInstall() {
        assertFalse(OnboardingRepository().completed.value)
    }

    @Test
    fun onceCompletedItStaysCompletedAfterRestart() {
        OnboardingRepository().markCompleted()

        assertTrue(OnboardingRepository().completed.value, "as perguntas iniciais não podem voltar a cada abertura")
    }
}
