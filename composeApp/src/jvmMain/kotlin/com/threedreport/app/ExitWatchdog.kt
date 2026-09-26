package com.threedreport.app

import java.io.File
import java.time.LocalDateTime
import kotlin.concurrent.thread

/**
 * Garante que o processo termina depois que o app decidiu sair (decisão 117). Vez ou outra, o encerramento
 * da JVM travava depois da janela fechar e das gravações irem pro disco: o processo ficava vivo sem janela,
 * segurando a trava de instância única, e abrir o app de novo dizia "o 3DReport já está aberto".
 *
 * Chamado só depois de tudo gravado. Se o processo ainda estiver vivo passado [graceMillis], grava as pilhas
 * de todas as threads em [dumpFile] (direto no arquivo, sem passar pelo log, que tem trava e pode ser
 * justamente o que travou) e força a saída.
 */
object ExitWatchdog {

    private const val DEFAULT_GRACE_MILLIS = 10_000L

    @Volatile
    private var armed = false

    fun arm(dumpFile: File?, graceMillis: Long = DEFAULT_GRACE_MILLIS, halt: (Int) -> Unit = { Runtime.getRuntime().halt(it) }) {
        if (armed) return
        armed = true
        thread(isDaemon = true, name = "3dreport-exit-watchdog") {
            try {
                Thread.sleep(graceMillis)
            } catch (_: InterruptedException) {
                return@thread
            }
            runCatching { dumpFile?.let(::writeThreadDump) }
            halt(0)
        }
    }

    /** Só pros testes, que armam mais de uma vez no mesmo processo. */
    internal fun disarmForTest() {
        armed = false
    }

    private fun writeThreadDump(file: File) {
        val text = buildString {
            append("${LocalDateTime.now()}: o encerramento travou; saída forçada. Pilhas das threads:\n\n")
            Thread.getAllStackTraces().forEach { (thread, stack) ->
                append("\"${thread.name}\" daemon=${thread.isDaemon} state=${thread.state}\n")
                stack.forEach { append("    at $it\n") }
                append('\n')
            }
        }
        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}
