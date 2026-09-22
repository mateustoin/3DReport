package com.threedreport.app.ui.settings

import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.RestoreResult
import com.threedreport.app.platform.defaultDocumentsDirectory
import com.threedreport.app.platform.exitApp
import com.threedreport.app.platform.pickBackupFile
import com.threedreport.app.platform.saveBytesToFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado da seção "Backup" das Configurações.
 *
 * @property message resultado da última ação, exibido abaixo dos botões.
 * @property isError se [message] é um erro (muda a cor do texto).
 * @property fileNameToRestore nome do arquivo escolhido pra restaurar, enquanto a confirmação
 *   está aberta; `null` quando não há restauração pendente.
 * @property restoredFromPreviousDataAt caminho onde os dados anteriores foram guardados, depois de
 *   uma restauração bem-sucedida. Não-nulo também significa "o app precisa ser fechado agora".
 */
data class BackupUiState(
    val message: String? = null,
    val isError: Boolean = false,
    val fileNameToRestore: String? = null,
    val restoredFromPreviousDataAt: String? = null,
)

/** Ver [BackupRepository] pro porquê de existir backup/restauração. */
class BackupViewModel(private val repository: BackupRepository) {

    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    private var bytesToRestore: ByteArray? = null

    fun createBackup() {
        val saved = runCatching {
            saveBytesToFile(repository.createBackupZip(), repository.suggestedBackupFileName(), defaultDocumentsDirectory())
        }
        state.value = when {
            saved.isFailure -> BackupUiState(message = "Não consegui gerar o backup.", isError = true)
            saved.getOrDefault(false) -> BackupUiState(message = "Backup salvo.")
            else -> BackupUiState()
        }
    }

    /** Escolhe o arquivo e **só** pede confirmação — restaurar de fato é [confirmRestore]. */
    fun pickBackupToRestore() {
        val picked = pickBackupFile() ?: return
        bytesToRestore = picked.bytes
        state.value = BackupUiState(fileNameToRestore = picked.fileName)
    }

    fun cancelRestore() {
        bytesToRestore = null
        state.value = BackupUiState()
    }

    fun confirmRestore() {
        val bytes = bytesToRestore ?: return
        bytesToRestore = null
        state.value = when (val result = repository.restoreFromZip(bytes)) {
            is RestoreResult.Success -> BackupUiState(restoredFromPreviousDataAt = result.previousDataPath)
            is RestoreResult.Failure -> BackupUiState(message = result.message, isError = true)
        }
    }

    /** Ver [exitApp]: continuar aberto depois de restaurar sobrescreveria os dados restaurados. */
    fun closeApp() = exitApp()
}
