package com.threedreport.app.data

/**
 * Backup e restauração de tudo que o app guarda em disco (orçamentos,
 * clientes, catálogos, configurações, fotos e STLs).
 *
 * Existe porque o negócio inteiro de quem usa o app vive só na pasta de
 * dados local: formatar o computador, trocar de máquina ou corromper um
 * arquivo significaria perder o histórico todo, sem nenhum caminho de volta
 * dentro do app.
 *
 * A restauração é feita numa pasta temporária e só troca a pasta de dados
 * de verdade no fim, quando já deu tudo certo — se qualquer passo falhar, os
 * dados atuais continuam intactos (ver [restoreFromZip]).
 */
expect class BackupRepository() {

    /** Conteúdo de um `.zip` com toda a pasta de dados do app, pronto pra ser gravado onde o usuário escolher. */
    fun createBackupZip(): ByteArray

    /** Nome sugerido pro arquivo de backup, com a data de hoje (ex.: `3dreport-backup-2026-09-22.zip`). */
    fun suggestedBackupFileName(): String

    /**
     * Substitui a pasta de dados do app pelo conteúdo de [zipBytes].
     *
     * Os dados atuais **não são apagados**: viram uma cópia de segurança ao
     * lado da pasta de dados (caminho devolvido em [RestoreResult.Success]),
     * pra dar como desfazer no braço se a pessoa restaurar o backup errado.
     */
    fun restoreFromZip(zipBytes: ByteArray): RestoreResult
}

/** Resultado de [BackupRepository.restoreFromZip]. */
sealed interface RestoreResult {

    /** @property previousDataPath onde os dados que existiam antes da restauração foram guardados. */
    data class Success(val previousDataPath: String) : RestoreResult

    /** @property message motivo da falha, já escrito pra ser exibido direto pro usuário. */
    data class Failure(val message: String) : RestoreResult
}
