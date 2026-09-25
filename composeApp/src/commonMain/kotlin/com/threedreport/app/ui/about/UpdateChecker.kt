package com.threedreport.app.ui.about

import com.threedreport.app.AppLog
import com.threedreport.app.data.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A versão publicada mais recente e a página dela. */
data class LatestRelease(val version: String, val url: String)

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
) {
    private val enabledState = MutableStateFlow(preferences.preferences.value.checkForUpdates)
    val enabled: StateFlow<Boolean> = enabledState.asStateFlow()

    private val stateFlow = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = stateFlow.asStateFlow()

    /** Liga ou desliga; ao ligar, já verifica. */
    fun setEnabled(enabled: Boolean) {
        preferences.update(preferences.preferences.value.copy(checkForUpdates = enabled))
        enabledState.value = enabled
        if (enabled) checkNow() else stateFlow.value = UpdateState.Idle
    }

    /** Ao abrir o app, só com a opção ligada. */
    fun checkOnStart() {
        if (preferences.preferences.value.checkForUpdates) checkNow()
    }

    /** "Verificar agora". */
    fun checkNow() {
        if (stateFlow.value == UpdateState.Checking) return
        stateFlow.value = UpdateState.Checking
        scope.launch(background) {
            val latest = runCatching { source.latest() }
                .onFailure { AppLog.warn("Não consegui verificar atualizações", it) }
                .getOrNull()
            withContext(main) {
                stateFlow.value = when {
                    latest == null -> UpdateState.Failed
                    isNewerVersion(latest.version, currentVersion) -> UpdateState.Available(latest)
                    else -> UpdateState.UpToDate
                }
            }
        }
    }
}
