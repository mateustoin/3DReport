package com.threedreport.app.data.store

import com.threedreport.app.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Grava em segundo plano, fora do thread da tela (decisão 108). O repositório muda o estado em memória
 * na hora e pede a gravação; várias mudanças seguidas (arrastar um card no Kanban, digitar) viram uma
 * gravação só, sempre do estado mais recente, porque o `StateFlow` guarda só o último valor.
 *
 * Uma gravação que falha não perde nada: o estado continua em memória, [StorageHealth] avisa a tela, e a
 * próxima mudança (ou [retry]) tenta de novo com o estado inteiro. Ao fechar, o app espera
 * [awaitWritten] pra não sair com mudança por gravar.
 *
 * Cada gravação passa por [gate], compartilhado entre os arquivos da pasta: quem trava o [gate] (restaurar
 * um backup) espera a gravação em andamento terminar e segura as próximas até destravar. Sem isso, uma
 * gravação atrasada levaria o estado antigo da memória pra pasta recém-restaurada.
 */
class WriteBehindFile<T>(
    private val target: DataFile<T>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    private val health: StorageHealth,
    private val gate: Mutex = Mutex(),
) : DataFile<T> {

    private data class Pending<V>(val version: Long, val value: V)

    private val latest = MutableStateFlow<Pending<T>?>(null)
    private val written = MutableStateFlow(0L)

    override val name: String
        get() = target.name

    init {
        scope.launch(dispatcher) {
            latest.filterNotNull().collect { pending ->
                try {
                    gate.withLock { target.write(pending.value) }
                    written.value = pending.version
                    health.clearWriteFailure(name)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.error("Falha ao gravar $name", e)
                    health.reportWriteFailure(name, e.message ?: e::class.simpleName ?: "erro desconhecido")
                }
            }
        }
    }

    override fun read(): T? = target.read()

    override fun write(value: T) {
        latest.update { Pending((it?.version ?: 0L) + 1, value) }
    }

    /** Tenta gravar de novo o estado mais recente (depois de uma falha). */
    fun retry() {
        latest.update { it?.copy(version = it.version + 1) }
    }

    /** Se tudo o que foi pedido já está no disco. */
    val isWritten: Boolean
        get() = (latest.value?.version ?: 0L) <= written.value

    /** Espera a última gravação pedida até agora chegar ao disco. */
    suspend fun awaitWritten() {
        val target = latest.value?.version ?: return
        written.first { it >= target }
    }
}
