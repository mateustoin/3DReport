package com.threedreport.app.ui.text

import com.threedreport.app.platform.PickFailure
import com.threedreport.app.platform.PickResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PickFailureTextTest {

    @Test
    fun eachReasonReadsAsBeforeAndNamesTheFile() = runBlocking {
        assertEquals(
            "Não consegui ler peca.gcode. Ele pode estar aberto em outro programa ou ter sido movido.",
            PickResult.Failed(PickFailure.UNREADABLE, "peca.gcode").text().resolve(),
        )
        assertEquals(
            "peca.gcode é grande demais pra abrir no app.",
            PickResult.Failed(PickFailure.TOO_LARGE, "peca.gcode").text().resolve(),
        )
    }
}
