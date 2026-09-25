package com.threedreport.app

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Log em arquivo (decisão 108): `logs/3dreport.log` na pasta de dados, com o anterior guardado como
 * `3dreport.1.log` quando passa de 1 MB. Abre, escreve e fecha a cada linha, sem segurar o arquivo
 * aberto: no Windows, um arquivo aberto impediria renomear a pasta de dados (restauração de backup,
 * troca de formato).
 */
object DesktopLog {

    private const val MAX_BYTES = 1_000_000L
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Volatile
    private var logDir: File? = null

    /** O arquivo de log atual, pra mostrar ao pedir ajuda. */
    val currentFile: File?
        get() = logDir?.let { File(it, "3dreport.log") }

    fun install(dir: File) {
        logDir = dir
        AppLog.sink = { level, message, error -> write(level, message, error) }
        AppLog.info("3DReport $APP_VERSION iniciado (Java ${System.getProperty("java.version")}, ${System.getProperty("os.name")})")
    }

    @Synchronized
    private fun write(level: AppLog.Level, message: String, error: Throwable?) {
        val line = buildString {
            append(LocalDateTime.now().format(timeFormat)).append(" [").append(level).append("] ").append(message)
            error?.let { append('\n').append(it.stackTraceText()) }
            append('\n')
        }
        if (level != AppLog.Level.INFO) System.err.print(line)
        val dir = logDir ?: return
        runCatching {
            dir.mkdirs()
            val file = File(dir, "3dreport.log")
            if (file.length() > MAX_BYTES) {
                val previous = File(dir, "3dreport.1.log")
                previous.delete()
                file.renameTo(previous)
            }
            file.appendText(line)
        }
    }
}

internal fun Throwable.stackTraceText(): String = StringWriter().also { printStackTrace(PrintWriter(it)) }.toString()
