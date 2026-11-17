package com.threedreport.app.ui.about

import com.threedreport.app.AppLog
import com.threedreport.app.data.PreferencesRepository
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.update.DownloadOutcome
import com.threedreport.app.update.DownloadedUpdate
import com.threedreport.app.update.InstallOutcome
import com.threedreport.app.update.NoUpdateInstaller
import com.threedreport.app.update.ReleaseAsset
import com.threedreport.app.update.UpdateFailure
import com.threedreport.app.update.UpdateInstaller
import com.threedreport.app.update.UpdatePlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A versão publicada mais recente, a página dela e os arquivos publicados ([assets], de onde sai o instalador do
 * "Atualizar agora", decisão 143).
 */
data class LatestRelease(val version: String, val url: String, val assets: List<ReleaseAsset> = emptyList())

/** De onde vem a versão mais recente (a API de releases do GitHub no desktop). `null` quando não deu pra saber. */
fun interface ReleaseSource {
    suspend fun latest(): LatestRelease?
}

/** O que a verificação de atualizações tem pra dizer. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: LatestRelease) : UpdateState
    data object Failed : UpdateState
}

/**
 * O que o Sobre e a janela de atualização mostram: se a verificação está ligada, o que ela achou, se dá pra instalar
 * por dentro ([canInstall], senão só "Ver novidades") e em que passo está o "Atualizar agora" ([install]).
 */
data class UpdateUiState(
    val enabled: Boolean = false,
    val status: UpdateState = UpdateState.Idle,
    val canInstall: Boolean = false,
    val install: UpdateInstall? = null,
)

/**
 * Se [candidate] ("v2.2.0", "2.1.10") é mais nova que [current], comparando MAJOR.MINOR.PATCH como números.
 * Um sufixo ("-beta") é ignorado; um texto que não é versão nunca é "mais nova".
 */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(version: String): List<Int>? = version.trim().removePrefix("v").substringBefore('-').split('.')
        .map { it.toIntOrNull() ?: return null }
        .takeIf { it.isNotEmpty() }
    val a = parts(candidate) ?: return false
    val b = parts(current) ?: return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/**
 * Verificação opcional de atualizações (decisão 116). Desligada por padrão: o app é 100% local, e só sai da
 * máquina o que a pessoa escolheu. Ligada, pergunta ao GitHub qual é a versão mais recente ao abrir e
 * quando a pessoa pede; nada além do pedido em si sai do computador. Falhar (sem internet) não incomoda:
 * fica no log e, só se a pessoa pediu, aparece como "não consegui verificar".
 */
class UpdateViewModel(
    private val preferences: PreferencesRepository,
    private val source: ReleaseSource,
    private val currentVersion: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val background: CoroutineDispatcher = Dispatchers.Unconfined,
    private val main: CoroutineDispatcher = Dispatchers.Unconfined,
    /** Baixa, confere e instala a versão nova (decisão 143). Sem ele, só há "Ver novidades". */
    private val installer: UpdateInstaller = NoUpdateInstaller,
    /** Se há um orçamento não salvo, pra perguntar antes de o app fechar na instalação. */
    private val hasUnsavedWork: () -> Boolean = { false },
    /** Fecha o app pelo caminho normal de fechar a janela: grava o que está pendente antes de sair. */
    private val closeApp: () -> Unit = {},
    private val platform: PlatformServices = defaultPlatform,
) {
    private val state = MutableStateFlow(UpdateUiState(enabled = preferences.preferences.value.checkForUpdates))

    /** Um estado só: a opção ligada e o resultado da verificação. */
    val uiState: StateFlow<UpdateUiState> = state.asStateFlow()

    /** Sobe a cada desligar: uma verificação que termina depois disso não mostra mais nada. */
    private var generation = 0

    /** Liga ou desliga; ao ligar, já verifica. */
    fun setEnabled(enabled: Boolean) {
        preferences.update(preferences.preferences.value.copy(checkForUpdates = enabled))
        if (enabled) {
            state.value = state.value.copy(enabled = true)
            checkNow()
        } else {
            generation++
            state.value = UpdateUiState(enabled = false, status = UpdateState.Idle)
        }
    }

    /**
     * A versão nova que ainda não foi avisada, pro aviso ao abrir aparecer uma vez por versão; quem quiser
     * ver de novo tem o Sobre.
     */
    fun unannounced(state: UpdateState): LatestRelease? =
        (state as? UpdateState.Available)?.release?.takeIf { it.version != preferences.preferences.value.announcedUpdateVersion }

    /** Marca [release] como avisada. */
    fun markAnnounced(release: LatestRelease) {
        preferences.update(preferences.preferences.value.copy(announcedUpdateVersion = release.version))
    }

    /** Ao abrir o app, só com a opção ligada. */
    fun checkOnStart() {
        if (preferences.preferences.value.checkForUpdates) checkNow()
    }

    /** "Verificar agora". */
    fun checkNow() {
        if (state.value.status == UpdateState.Checking) return
        state.value = state.value.copy(status = UpdateState.Checking)
        val started = generation
        scope.launch(background) {
            val latest = runCatching { source.latest() }
                .onFailure { AppLog.warn("Não consegui verificar atualizações", it) }
                .getOrNull()
            val newer = latest?.takeIf { isNewerVersion(it.version, currentVersion) }
            // O plano consulta o sistema (instalador, pkexec): feito aqui, fora da thread da tela.
            val plan = newer?.let { runCatching { installer.plan(it) }.getOrNull() }
            withContext(main) {
                if (started != generation) return@withContext
                state.value = state.value.copy(
                    status = when {
                        latest == null -> UpdateState.Failed
                        newer != null -> UpdateState.Available(newer)
                        else -> UpdateState.UpToDate
                    },
                    canInstall = plan != null,
                )
                updatePlan = plan
            }
        }
    }

    // Atualizar por dentro (decisão 143): baixar, conferir, perguntar e instalar.

    /** Sobe a cada passo novo: o resultado de um download cancelado ou refeito não mexe mais na janela. */
    private var installGeneration = 0
    private var installJob: Job? = null

    /** "Atualizar agora": baixa e confere o instalador da versão achada. Sem instalador pro sistema, não faz nada. */
    fun startUpdate() {
        val release = (state.value.status as? UpdateState.Available)?.release ?: return
        startDownload(release)
    }

    /** O que fazer pra instalar a versão achada na última verificação, ou `null` sem "Atualizar agora". */
    private var updatePlan: UpdatePlan? = null

    private fun startDownload(release: LatestRelease) {
        if (state.value.install.isBusy) return
        val plan = updatePlan?.takeIf { it.release == release } ?: return
        val token = ++installGeneration
        state.update { it.copy(install = UpdateInstall.Downloading(release)) }
        installJob = scope.launch(background) {
            val outcome = try {
                installer.download(plan) { progress -> reportDownload(token, release, progress) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.warn("Não consegui baixar a atualização", e)
                DownloadOutcome.Failed(UpdateFailure.NETWORK)
            }
            withContext(main) {
                if (token != installGeneration) {
                    // Cancelado enquanto terminava: o arquivo baixado não tem mais dono.
                    if (outcome is DownloadOutcome.Ready) installer.discard(outcome.update)
                    return@withContext
                }
                setInstall(
                    when (outcome) {
                        is DownloadOutcome.Ready -> UpdateInstall.Ready(release, outcome.update)
                        is DownloadOutcome.Failed -> UpdateInstall.Failed(release, outcome.reason.toError())
                    },
                )
            }
        }
    }

    /** Em 100% o arquivo já desceu e falta conferir a soma. */
    private fun reportDownload(token: Int, release: LatestRelease, progress: Float) {
        state.update { current ->
            if (token != installGeneration || current.install !is UpdateInstall.Downloading) {
                current
            } else if (progress >= 1f) {
                current.copy(install = UpdateInstall.Verifying(release))
            } else {
                current.copy(install = UpdateInstall.Downloading(release, progress))
            }
        }
    }

    /** "Tentar de novo" depois de uma falha de rede ou do instalador. */
    fun retryUpdate() {
        val failed = state.value.install as? UpdateInstall.Failed ?: return
        setInstall(null)
        startDownload(failed.release)
    }

    /**
     * "Instalar e fechar o app". Com orçamento não salvo, pergunta antes ([UpdateInstall.ConfirmUnsaved]); o resto
     * (gravar o que está pendente) é do caminho normal de fechar a janela, que [closeApp] segue.
     */
    fun installNow() {
        val ready = state.value.install as? UpdateInstall.Ready ?: return
        if (hasUnsavedWork()) {
            setInstall(UpdateInstall.ConfirmUnsaved(ready.release, ready.update))
        } else {
            runInstall(ready.release, ready.update)
        }
    }

    /** "Instalar mesmo assim", depois do aviso de orçamento não salvo. */
    fun installAnyway() {
        val confirm = state.value.install as? UpdateInstall.ConfirmUnsaved ?: return
        runInstall(confirm.release, confirm.update)
    }

    /** "Voltar", no aviso de orçamento não salvo. */
    fun backToReady() {
        val confirm = state.value.install as? UpdateInstall.ConfirmUnsaved ?: return
        setInstall(UpdateInstall.Ready(confirm.release, confirm.update))
    }

    private fun runInstall(release: LatestRelease, update: DownloadedUpdate) {
        val token = ++installGeneration
        setInstall(UpdateInstall.Installing(release, update))
        installJob = scope.launch(background) {
            val outcome = try {
                installer.install(update)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.warn("Não consegui instalar a atualização", e)
                InstallOutcome.Failed
            }
            withContext(main) {
                if (token != installGeneration) return@withContext
                when (outcome) {
                    InstallOutcome.CloseApp -> {
                        setInstall(null)
                        closeApp()
                    }

                    InstallOutcome.Cancelled -> setInstall(UpdateInstall.Ready(release, update, passwordCancelled = true))

                    InstallOutcome.Failed -> {
                        installer.discard(update)
                        setInstall(UpdateInstall.Failed(release, UpdateError.INSTALL))
                    }

                    is InstallOutcome.Manual -> setInstall(UpdateInstall.Manual(release, outcome.folderPath, outcome.command))
                }
            }
        }
    }

    /** Copia o comando de instalar do Linux sem `pkexec` ([UpdateInstall.Manual]). */
    fun copyManualCommand() {
        val manual = state.value.install as? UpdateInstall.Manual ?: return
        platform.copyToClipboard(manual.command)
    }

    /**
     * "Cancelar" ou fechar a janela: interrompe o download, apaga o arquivo que já estava pronto e volta ao que era.
     * Instalando, não cancela (o instalador do sistema já está com a pessoa).
     */
    fun dismissUpdate() {
        val current = state.value.install ?: return
        if (current is UpdateInstall.Installing) return
        installGeneration++
        installJob?.cancel()
        installJob = null
        when (current) {
            is UpdateInstall.Ready -> installer.discard(current.update)
            is UpdateInstall.ConfirmUnsaved -> installer.discard(current.update)
            else -> Unit
        }
        setInstall(null)
    }

    private fun setInstall(install: UpdateInstall?) = state.update { it.copy(install = install) }

    private val UpdateInstall?.isBusy: Boolean
        get() = this != null && this !is UpdateInstall.Failed

    private fun UpdateFailure.toError() = when (this) {
        UpdateFailure.NETWORK -> UpdateError.NETWORK
        UpdateFailure.NO_CHECKSUMS -> UpdateError.NO_CHECKSUMS
        UpdateFailure.CHECKSUM_MISMATCH -> UpdateError.CHECKSUM_MISMATCH
    }
}
