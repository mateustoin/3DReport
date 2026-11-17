package com.threedreport.app

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.startup_already_running
import _3dreport.composeapp.generated.resources.startup_close
import _3dreport.composeapp.generated.resources.startup_data_dir_failed
import _3dreport.composeapp.generated.resources.startup_error_copy
import _3dreport.composeapp.generated.resources.startup_error_fatal
import _3dreport.composeapp.generated.resources.startup_error_log
import _3dreport.composeapp.generated.resources.startup_error_nonfatal
import _3dreport.composeapp.generated.resources.startup_error_report
import _3dreport.composeapp.generated.resources.startup_move_failed
import _3dreport.composeapp.generated.resources.startup_move_failed_advice
import _3dreport.composeapp.generated.resources.startup_nothing_deleted
import _3dreport.composeapp.generated.resources.startup_read_failed
import _3dreport.composeapp.generated.resources.startup_read_failed_advice
import _3dreport.composeapp.generated.resources.startup_write_back
import _3dreport.composeapp.generated.resources.startup_write_close_anyway
import _3dreport.composeapp.generated.resources.startup_write_failed
import _3dreport.composeapp.generated.resources.startup_write_failed_advice
import _3dreport.composeapp.generated.resources.startup_write_retry
import com.threedreport.app.platform.fillTemplate
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Os textos dos diálogos do `main` (Swing, sem Compose): erros de inicialização, aviso de segunda instância,
 * erro na janela e falha de gravação ao fechar. Vêm de `strings.xml`, como o resto do app.
 *
 * Esses diálogos aparecem antes de o Compose subir ou dentro de um tratador de erro (que pode rodar na thread de
 * eventos), então nada é lido na hora: [load] lê tudo uma vez no começo do `main`, na thread principal e antes de
 * qualquer janela, e os diálogos só preenchem os modelos já guardados. Se a leitura falhar (arquivo de recursos
 * ilegível, o que não deveria acontecer com o app instalado inteiro), o diálogo continua aparecendo, só com os
 * detalhes técnicos (caminho e mensagem da exceção) e o nome da chave no lugar do texto, em vez de esconder o erro
 * de verdade atrás de outro.
 */
internal object StartupTexts {

    private val ALL: List<StringResource> = listOf(
        Res.string.startup_already_running,
        Res.string.startup_data_dir_failed,
        Res.string.startup_nothing_deleted,
        Res.string.startup_move_failed,
        Res.string.startup_move_failed_advice,
        Res.string.startup_read_failed,
        Res.string.startup_read_failed_advice,
        Res.string.startup_write_failed,
        Res.string.startup_write_failed_advice,
        Res.string.startup_write_retry,
        Res.string.startup_write_close_anyway,
        Res.string.startup_write_back,
        Res.string.startup_error_fatal,
        Res.string.startup_error_nonfatal,
        Res.string.startup_error_report,
        Res.string.startup_error_log,
        Res.string.startup_error_copy,
        Res.string.startup_close,
    )

    @Volatile
    private var templates: Map<StringResource, String> = emptyMap()

    /** Lê os textos. Chamar uma vez, no começo do `main`, antes de qualquer diálogo. */
    fun load() {
        templates = runCatching { runBlocking { ALL.associateWith { getString(it) } } }
            .onFailure { AppLog.warn("Não consegui ler os textos dos diálogos de inicialização", it) }
            .getOrDefault(emptyMap())
    }

    private fun text(resource: StringResource, vararg args: Any): String =
        templates[resource]?.let { fillTemplate(it, *args) } ?: args.joinToString("\n").ifEmpty { resource.key }

    val alreadyRunning: String get() = text(Res.string.startup_already_running)

    fun dataDirFailed(path: String, reason: String?): String =
        listOfNotNull(text(Res.string.startup_data_dir_failed) + "\n" + path, reason, text(Res.string.startup_nothing_deleted)).joinToString("\n\n")

    fun moveFailed(path: String): String =
        text(Res.string.startup_move_failed) + "\n" + path + "\n\n" + text(Res.string.startup_move_failed_advice)

    fun readFailed(path: String): String =
        text(Res.string.startup_read_failed) + "\n" + path + "\n\n" + text(Res.string.startup_read_failed_advice)

    /** [failures] já vem com um item por linha ("arquivo: motivo"). */
    fun writeFailed(failures: String): String =
        text(Res.string.startup_write_failed) + "\n\n" + failures + "\n\n" + text(Res.string.startup_write_failed_advice)

    /** Os três botões do diálogo de falha de gravação: tentar de novo, fechar assim mesmo, voltar pro app. */
    val writeFailedButtons: Array<String>
        get() = arrayOf(text(Res.string.startup_write_retry), text(Res.string.startup_write_close_anyway), text(Res.string.startup_write_back))

    fun errorMessage(fatal: Boolean, logPath: String?): String =
        text(if (fatal) Res.string.startup_error_fatal else Res.string.startup_error_nonfatal) + "\n\n" +
            text(Res.string.startup_error_report) +
            (logPath?.let { "\n" + text(Res.string.startup_error_log, it) } ?: "")

    val errorCopy: String get() = text(Res.string.startup_error_copy)
    val close: String get() = text(Res.string.startup_close)
}
