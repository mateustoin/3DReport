package com.threedreport.app.ui.about

import com.threedreport.app.update.DownloadedUpdate

/** Por que a atualização por dentro não seguiu. Sem texto: a janela escolhe a mensagem. */
enum class UpdateError {
    /** Falha de rede ao baixar. */
    NETWORK,

    /** A release não traz as somas SHA-256 (releases antigas): o app não instala sem conferir. */
    NO_CHECKSUMS,

    /** A soma do arquivo baixado não bate; o arquivo foi apagado. */
    CHECKSUM_MISMATCH,

    /** O instalador do sistema falhou. Nada mudou. */
    INSTALL,
}

/** Em que passo está o "Atualizar agora" (decisão 143): cada passo é um estado da janela de atualização. */
sealed interface UpdateInstall {
    /** A versão que está sendo instalada. */
    val release: LatestRelease

    /** Baixando o instalador; [progress] vai de 0 a 1. */
    data class Downloading(override val release: LatestRelease, val progress: Float = 0f) : UpdateInstall

    /** Baixou; conferindo a soma SHA-256. */
    data class Verifying(override val release: LatestRelease) : UpdateInstall

    /** Conferido e pronto pra instalar. [passwordCancelled]: a pessoa cancelou a senha da tentativa anterior. */
    data class Ready(override val release: LatestRelease, val update: DownloadedUpdate, val passwordCancelled: Boolean = false) : UpdateInstall

    /** Há um orçamento não salvo: pergunta antes de instalar (o app fecha). */
    data class ConfirmUnsaved(override val release: LatestRelease, val update: DownloadedUpdate) : UpdateInstall

    /** O instalador do sistema está trabalhando (no Linux, esperando a senha). */
    data class Installing(override val release: LatestRelease, val update: DownloadedUpdate) : UpdateInstall

    /** Sem `pkexec`: a pasta do arquivo foi aberta e [command] é o que a pessoa roda no terminal. */
    data class Manual(override val release: LatestRelease, val folderPath: String, val command: String) : UpdateInstall

    /** Não deu. [canRetry]: tentar de novo faz sentido (rede ou instalador), ao contrário de uma soma que não bate. */
    data class Failed(override val release: LatestRelease, val reason: UpdateError) : UpdateInstall {
        val canRetry: Boolean get() = reason == UpdateError.NETWORK || reason == UpdateError.INSTALL
    }
}
