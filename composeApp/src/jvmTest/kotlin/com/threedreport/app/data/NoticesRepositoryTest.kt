package com.threedreport.app.data

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticesRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun aNoticeIsUnseenUntilMarkedAndStaysSeenAfterRestart() {
        assertFalse(NOTICE_APP_SIGNATURE in NoticesRepository().seen.value)

        NoticesRepository().markSeen(NOTICE_APP_SIGNATURE)

        assertTrue(NOTICE_APP_SIGNATURE in NoticesRepository().seen.value)
    }
}
