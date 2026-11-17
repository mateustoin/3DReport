package com.threedreport.app.ui.quote

import com.threedreport.core.stl.PrintDifficulty
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

/** O nível de dificuldade do STL tem o texto em strings.xml, e não no core (Leva 10B, parte 7). O texto não muda. */
class PrintDifficultyTextTest {

    @Test
    fun eachLevelReadsLikeBefore() = runBlocking {
        assertEquals(
            listOf("fácil", "médio", "difícil"),
            PrintDifficulty.entries.map { getString(it.labelRes()) },
        )
    }
}
