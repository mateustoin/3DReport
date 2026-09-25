package com.threedreport.app.ui.settings

import com.threedreport.app.data.AppPreferences
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.RestoreResult
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.defaultPlatform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado da seção "Backup" das Configurações.
 *
 * @property message resultado da última ação, exibido abaixo dos botões.
 * @property isError se [message] é um erro (muda a cor do texto).
 * @property savedPath onde o último backup manual ficou, pra oferecer "Abrir pasta".
 * @property pathToRestore arquivo escolhido pra restaurar, enquanto a confirmação está aberta.
 * @property busy um backup ou uma restauração está em andamento (os botões ficam desabilitados).
 * @property restoredFromPreviousDataAt caminho onde os dados anteriores foram guardados, depois de
 *   uma restauração bem-sucedida. Não-nulo também significa "o app precisa ser fechado agora".
 */
data class BackupUiState(
    val message: String? = null,
    val isError: Boolean = false,
    val savedPath: String? = null,
    val pathToRestore: String? = null,
    val busy: Boolean = false,
    val restoredFromPreviousDataAt: String? = null,
)

/**
 * Ver [BackupRepository] pro porquê de existir backup/restauração. Backup e restauração rodam fora do
 * thread da tela ([io]), com a seção mostrando que está trabalhando: com fotos e STLs, podem levar
 * alguns segundos.
 */
class BackupViewModel(
    private val repository: BackupRepository,
    private val preferencesRepository: PreferencesRepository,
    private val platform: PlatformServices = defaultPlatform,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val main: CoroutineDispatcher = Dispatchers.Main,
) {

    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences

    /** Pasta dos backups automáticos que vale agora (a escolhida ou a padrão). */
    val automaticBackupDirectory: String
        get() = preferences.value.backupDirectory ?: repository.defaultAutomaticBackupDirectory()

    fun createBackup() {
        if (state.value.busy) return
        val target = platform.chooseSaveLocation(repository.suggestedBackupFileName()) ?: return
        state.value = BackupUiState(busy = true)
        scope.launch(io) {
            val result = runCatching { repository.createBackup(target) }
            withContext(main) {
                state.value = result.fold(
                    onSuccess = {
                        markBackupDone()
                        BackupUiState(message = "Backup salvo.", savedPath = target)
                    },
                    onFailure = {
                        BackupUiState(
                            message = "Não consegui gravar o backup. Confira se a pasta existe e tem espaço, e tente de novo.",
                            isError = true,
                        )
                    },
                )
            }
        }
    }

    fun setAutomaticBackup(enabled: Boolean) = preferencesRepository.update(preferences.value.copy(autoBackupEnabled = enabled))

    fun chooseAutomaticBackupDirectory() {
        val chosen = platform.pickFolder("Pasta dos backups automáticos", automaticBackupDirectory) ?: return
        preferencesRepository.update(preferences.value.copy(backupDirectory = chosen))
    }

    fun openAutomaticBackupDirectory() {
        if (!platform.openFolder(automaticBackupDirectory)) {
            state.value = BackupUiState(message = "A pasta ainda não existe: ela é criada no primeiro backup automático.", isError = true)
        }
    }

    fun openSavedBackupFolder() {
        state.value.savedPath?.let(platform::openFolder)
    }

    /** Ver `SettingsViewModel.consumeSavedConfirmation`. */
    fun consumeMessage() = state.update { it.copy(message = null) }

    /** Escolhe o arquivo e **só** pede confirmação — restaurar de fato é [confirmRestore]. */
    fun pickBackupToRestore() {
        if (state.value.busy) return
        val path = platform.pickFilePath(FileKind.BACKUP) ?: return
        state.value = BackupUiState(pathToRestore = path)
    }

    fun cancelRestore() {
        state.value = BackupUiState()
    }

    fun confirmRestore() {
        val path = state.value.pathToRestore ?: return
        state.value = BackupUiState(busy = true)
        scope.launch(io) {
            val result = runCatching { repository.restore(path) }
                .getOrElse { RestoreResult.Failure("Não consegui restaurar esse backup. Nada foi alterado.") }
            withContext(main) {
                state.value = when (result) {
                    is RestoreResult.Success -> BackupUiState(restoredFromPreviousDataAt = result.previousDataPath)
                    is RestoreResult.Failure -> BackupUiState(message = result.message, isError = true)
                }
            }
        }
    }

    /**
     * Fecha o app depois de restaurar: os repositórios carregam os dados em memória ao abrir, e
     * continuar aberto faria a próxima gravação sobrescrever o que acabou de ser restaurado.
     */
    fun closeApp() = platform.exitApp()

    private fun markBackupDone() =
        preferencesRepository.update(preferences.value.copy(lastBackupEpochMillis = clock.nowMillis()))
}
