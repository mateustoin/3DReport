package com.threedreport.app.data

import com.threedreport.app.PendingWrites
import com.threedreport.app.data.store.DataFile
import com.threedreport.app.data.store.StorageHealth
import com.threedreport.app.data.store.WriteBehindFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex

/**
 * Faz cada arquivo de dados gravar em segundo plano ([WriteBehindFile]) e guarda a lista deles, pra o
 * app esperar tudo chegar ao disco antes de fechar.
 */
class WriteBehind(private val scope: CoroutineScope, private val health: StorageHealth) : DataFileWrapper, PendingWrites {

    private val files = mutableListOf<WriteBehindFile<*>>()

    /** Uma trava pra todos os arquivos: pausar segura as gravações da pasta inteira (ver [pause]). */
    private val gate = Mutex()

    override fun <T> wrap(file: DataFile<T>): DataFile<T> =
        WriteBehindFile(file, scope, Dispatchers.IO, health, gate).also { files += it }

    override val allWritten: Boolean
        get() = files.all { it.isWritten }

    override suspend fun awaitAll() = files.forEach { it.awaitWritten() }

    override fun retry() = files.forEach { it.retry() }

    override suspend fun pause() = gate.lock()

    override fun resume() {
        if (gate.isLocked) gate.unlock()
    }

    override val isPaused: Boolean
        get() = gate.isLocked
}
