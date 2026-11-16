package com.threedreport.app.ui.settings

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_open_folder
import _3dreport.composeapp.generated.resources.settings_backup_notice_failed
import _3dreport.composeapp.generated.resources.settings_backup_notice_folder_missing
import _3dreport.composeapp.generated.resources.settings_backup_notice_restore_failed
import _3dreport.composeapp.generated.resources.settings_backup_notice_restore_reason
import _3dreport.composeapp.generated.resources.settings_backup_notice_saved
import com.threedreport.app.PendingWrites
import com.threedreport.app.data.AppPreferences
import com.threedreport.app.data.BackupRepository
import com.threedreport.app.data.Clock
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.data.RestoreResult
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.text.uiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Estado da seção "Backup" das Configurações.
 *
 * @property pathToRestore arquivo escolhido pra restaurar, enquanto a confirmação está aberta.
 * @property busy um backup ou uma restauração está em andamento (os botões ficam desabilitados).
 * @property restoredFromPreviousDataAt caminho onde os dados anteriores foram guardados, depois de
 *   uma restauração bem-sucedida. Não-nulo também significa "o app precisa ser fechado agora".
 * @property lastBackupEpochMillis quando foi o último backup, `null` se nunca houve.
 * @property autoBackupEnabled o backup automático diário está ligado.
 * @property automaticBackupDirectory a pasta dos backups automáticos que vale agora (a escolhida ou a padrão).
 */
data class BackupUiState(
    val pathToRestore: String? = null,
    val busy: Boolean = false,
    val restoredFromPreviousDataAt: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val autoBackupEnabled: Boolean = false,
    val automaticBackupDirectory: String = "",
) {
    /** O nome do arquivo escolhido pra restaurar, pra confirmação dizer o que vai substituir os dados. */
    val fileNameToRestore: String? get() = pathToRestore?.substringAfterLast('/')?.substringAfterLast('\\')
}

/**
 * Ver [BackupRepository] pro porquê de existir backup/restauração. Um [uiState] só e os [notices]. Backup e restauração
 * rodam fora do thread da tela ([io]), com a seção mostrando que está trabalhando: com fotos e STLs, podem levar
 * alguns segundos.
 */
class BackupViewModel(
    private val repository: BackupRepository,
    private val preferencesRepository: PreferencesRepository,
    /** Gravações em segundo plano, que precisam parar antes de a pasta de dados ser trocada. */
    private val pendingWrites: PendingWrites = PendingWrites.None,
    private val platform: PlatformServices = defaultPlatform,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val main: CoroutineDispatcher = Dispatchers.Main,
) {

    private val state = MutableStateFlow(BackupUiState().withPreferences(preferencesRepository.preferences.value))
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    private val noticeChannel = Channel<UiNotice>(Channel.CONFLATED)
    val notices: Flow<UiNotice> = noticeChannel.receiveAsFlow()

    init {
        preferencesRepository.preferences.onEach { preferences -> state.update { it.withPreferences(preferences) } }.launchIn(scope)
    }

    private val preferences: AppPreferences get() = preferencesRepository.preferences.value

    private fun BackupUiState.withPreferences(preferences: AppPreferences) = copy(
        lastBackupEpochMillis = preferences.lastBackupEpochMillis,
        autoBackupEnabled = preferences.autoBackupEnabled,
        automaticBackupDirectory = preferences.backupDirectory ?: repository.defaultAutomaticBackupDirectory(),
    )

    fun createBackup() {
        if (state.value.busy) return
        val target = platform.chooseSaveLocation(repository.suggestedBackupFileName()) ?: return
        state.update { it.copy(busy = true) }
        scope.launch(io) {
            val result = runCatching { repository.createBackup(target) }
            withContext(main) {
                state.update { it.copy(busy = false) }
                result.fold(
                    onSuccess = {
                        markBackupDone()
                        noticeChannel.trySend(
                            UiNotice(
                                uiText(Res.string.settings_backup_notice_saved),
                                actionLabel = uiText(Res.string.export_open_folder),
                                action = { platform.openFolder(target) },
                            ),
                        )
                    },
                    onFailure = { noticeChannel.trySend(UiNotice(uiText(Res.string.settings_backup_notice_failed), isError = true)) },
                )
            }
        }
    }

    fun setAutomaticBackup(enabled: Boolean) = preferencesRepository.update(preferences.copy(autoBackupEnabled = enabled))

    /** Abre o seletor de pasta, com [dialogTitle] no alto, e guarda a escolhida. */
    fun chooseAutomaticBackupDirectory(dialogTitle: String) {
        val chosen = platform.pickFolder(dialogTitle, state.value.automaticBackupDirectory) ?: return
        preferencesRepository.update(preferences.copy(backupDirectory = chosen))
    }

    fun openAutomaticBackupDirectory() {
        if (!platform.openFolder(state.value.automaticBackupDirectory)) {
            noticeChannel.trySend(UiNotice(uiText(Res.string.settings_backup_notice_folder_missing), isError = true))
        }
    }

    /** Escolhe o arquivo e **só** pede confirmação, restaurar de fato é [confirmRestore]. */
    fun pickBackupToRestore() {
        if (state.value.busy) return
        val path = platform.pickFilePath(FileKind.BACKUP) ?: return
        state.update { it.copy(pathToRestore = path) }
    }

    fun cancelRestore() {
        state.update { it.copy(pathToRestore = null) }
    }

    fun confirmRestore() {
        val path = state.value.pathToRestore ?: return
        state.update { it.copy(pathToRestore = null, busy = true) }
        scope.launch(io) {
            // Primeiro o que está pendente vai pro disco (a cópia dos dados atuais sai completa); depois as
            // gravações param. Uma gravação atrasada levaria o estado antigo pra pasta restaurada. Com sucesso,
            // continuam paradas até o app fechar, que é o próximo passo obrigatório.
            withTimeoutOrNull(FLUSH_TIMEOUT_MILLIS) { pendingWrites.awaitAll() }
            pendingWrites.pause()
            val result = runCatching { repository.restore(path) }.getOrNull()
            if (result !is RestoreResult.Success) pendingWrites.resume()
            withContext(main) {
                state.update { it.copy(busy = false) }
                when (result) {
                    is RestoreResult.Success -> state.update { it.copy(restoredFromPreviousDataAt = result.previousDataPath) }

                    // O motivo vem do repositório, que já o escreve pra quem lê (ver RestoreResult.Failure).
                    is RestoreResult.Failure -> noticeChannel.trySend(
                        UiNotice(uiText(Res.string.settings_backup_notice_restore_reason, result.message), isError = true),
                    )

                    null -> noticeChannel.trySend(UiNotice(uiText(Res.string.settings_backup_notice_restore_failed), isError = true))
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
        preferencesRepository.update(preferences.copy(lastBackupEpochMillis = clock.nowMillis()))

    private companion object {
        /** Quanto esperar as gravações pendentes antes de restaurar; um disco com falha não pode travar a restauração. */
        const val FLUSH_TIMEOUT_MILLIS = 10_000L
    }
}
