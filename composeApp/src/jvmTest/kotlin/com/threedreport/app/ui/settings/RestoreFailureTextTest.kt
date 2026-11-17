package com.threedreport.app.ui.settings

import com.threedreport.app.data.RestoreFailure
import com.threedreport.app.ui.text.resolve
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestoreFailureTextTest {

    @Test
    fun theTextsReadAsBefore() = runBlocking {
        assertEquals("Esse arquivo não parece ser um backup do 3DReport. Nada foi alterado.", RestoreFailure.NotABackup.text().resolve())
        assertEquals("Não consegui aplicar o backup. Seus dados atuais foram mantidos.", RestoreFailure.ApplyFailedKept.text().resolve())
        val moved = RestoreFailure.CurrentDataMoved("/home/x/.3dreport-anterior", ".3dreport").text().resolve()
        assertTrue(moved.contains("guardados em /home/x/.3dreport-anterior. Renomeie essa pasta de volta pra .3dreport antes"))
    }

    @Test
    fun everyReasonHasAText() = runBlocking {
        val all = listOf(
            RestoreFailure.NoDataFolder, RestoreFailure.UnreadableFile, RestoreFailure.NotABackup, RestoreFailure.NewerVersion,
            RestoreFailure.OldFormat, RestoreFailure.CouldNotKeepCurrentData, RestoreFailure.ApplyFailedKept,
            RestoreFailure.CurrentDataMoved("/a", "b"),
        )
        assertEquals(all.size, all.map { it.text().resolve() }.toSet().size)
    }
}
