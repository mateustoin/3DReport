package com.threedreport.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartupTextsTest {

    @Test
    fun theDialogTextsComeFromResourcesAndFillTheirPlaceholders() {
        StartupTexts.load()

        assertTrue(StartupTexts.alreadyRunning.startsWith("O 3DReport já está aberto."))
        assertEquals(
            "Não consegui abrir a pasta de dados\n/dados\n\nsem permissão\n\nNada foi apagado.",
            StartupTexts.dataDirFailed("/dados", "sem permissão"),
        )
        assertTrue(StartupTexts.readFailed("/dados/quotes.json").contains("\n/dados/quotes.json\n\nEle pode estar aberto"))
        assertTrue(StartupTexts.moveFailed("/dados").endsWith("Nada foi apagado."))
        assertEquals(listOf("Tentar de novo", "Fechar mesmo assim", "Voltar pro app"), StartupTexts.writeFailedButtons.toList())
        assertEquals("Fechar", StartupTexts.close)
        assertEquals("Copiar detalhes", StartupTexts.errorCopy)
    }

    @Test
    fun theErrorMessageNamesTheLogOnlyWhenThereIsOne() {
        StartupTexts.load()

        val fatal = StartupTexts.errorMessage(fatal = true, logPath = "/logs/app.log")
        assertTrue(fatal.startsWith("O 3DReport encontrou um erro e precisa fechar. Seus dados já gravados estão a salvo."))
        assertTrue(fatal.endsWith("Registro completo em: /logs/app.log"))
        val nonFatal = StartupTexts.errorMessage(fatal = false, logPath = null)
        assertTrue(nonFatal.startsWith("O 3DReport encontrou um erro. Seus dados"))
        assertTrue(!nonFatal.contains("Registro completo"))
    }
}
