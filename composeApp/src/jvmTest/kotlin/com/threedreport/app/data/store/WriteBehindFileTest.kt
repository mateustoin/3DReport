package com.threedreport.app.data.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WriteBehindFileTest {

    /** Arquivo que falha enquanto [failing] estiver ligado, como um disco cheio ou travado pelo antivírus. */
    private class FlakyFile : DataFile<Int> {
        var failing = false
        val written = mutableListOf<Int>()
        override val name = "numero.json"
        override fun read(): Int? = written.lastOrNull()
        override fun write(value: Int) {
            if (failing) throw IOException("disco cheio")
            written += value
        }
    }

    @Test
    fun severalChangesInARowBecomeOneWriteOfTheLatestState() = runTest {
        val target = FlakyFile()
        val file = WriteBehindFile(target, backgroundScope, StandardTestDispatcher(testScheduler), StorageHealth())

        file.write(1)
        file.write(2)
        file.write(3)
        assertFalse(file.isWritten)
        runCurrent()

        assertEquals(listOf(3), target.written)
        assertTrue(file.isWritten)
    }

    @Test
    fun aFailedWriteIsReportedAndRetryWritesTheLatestState() = runTest {
        val target = FlakyFile().apply { failing = true }
        val health = StorageHealth()
        val file = WriteBehindFile(target, backgroundScope, StandardTestDispatcher(testScheduler), health)

        file.write(7)
        runCurrent()
        assertEquals("disco cheio", health.writeFailures.value["numero.json"])
        assertFalse(file.isWritten, "o que mudou continua só na memória")

        target.failing = false
        file.retry()
        runCurrent()

        assertEquals(listOf(7), target.written)
        assertTrue(health.writeFailures.value.isEmpty())
        assertTrue(file.isWritten)
    }
}
