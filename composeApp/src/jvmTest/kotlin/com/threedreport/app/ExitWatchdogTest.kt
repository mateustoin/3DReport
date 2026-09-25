package com.threedreport.app

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExitWatchdogTest {

    @AfterTest
    fun disarm() = ExitWatchdog.disarmForTest()

    @Test
    fun aStuckExitIsForcedAndTheThreadsAreWrittenDown() {
        val dump = createTempDirectory("3dreport-test").resolve("logs/encerramento-travado.txt").toFile()
        val halted = CountDownLatch(1)
        var status = -1

        ExitWatchdog.arm(dump, graceMillis = 50) { status = it; halted.countDown() }

        assertTrue(halted.await(5, TimeUnit.SECONDS), "a saída não foi forçada")
        assertEquals(0, status)
        assertTrue(dump.readText().contains("3dreport-exit-watchdog"), "o arquivo precisa ter as pilhas das threads")
    }

    @Test
    fun armingTwiceStartsOneWatchdog() {
        val halts = CountDownLatch(2)
        ExitWatchdog.arm(null, graceMillis = 50) { halts.countDown() }
        ExitWatchdog.arm(null, graceMillis = 50) { halts.countDown() }

        Thread.sleep(500)
        assertEquals(1, halts.count)
    }
}
