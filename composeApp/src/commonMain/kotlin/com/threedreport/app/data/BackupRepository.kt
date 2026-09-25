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
 * O backup é escrito direto no arquivo de destino (decisão 108), sem montar o `.zip` inteiro na
 * memória: com vários STLs, a versão antiga podia estourar a memória. A restauração é feita numa pasta
 * temporária e só troca a pasta de dados de verdade no fim, quando já deu tudo certo — se qualquer passo
 * falhar, os dados atuais continuam intactos (ver [restore]).
 */
interface BackupRepository {

    /** Grava um `.zip` com toda a pasta de dados em [targetPath]. Lança exceção se não conseguir. */
    fun createBackup(targetPath: String)

    /** Nome sugerido pro arquivo de backup, com a data de hoje (ex.: `3dreport-backup-2026-09-22.zip`). */
    fun suggestedBackupFileName(): String

    /**
     * Substitui a pasta de dados do app pelo conteúdo do `.zip` em [sourcePath].
     *
     * Os dados atuais **não são apagados**: viram uma cópia de segurança ao
     * lado da pasta de dados (caminho devolvido em [RestoreResult.Success]),
     * pra dar como desfazer no braço se a pessoa restaurar o backup errado.
     */
    fun restore(sourcePath: String): RestoreResult

    /** Pasta padrão dos backups automáticos (`~/3DReport Backups`). */
    fun defaultAutomaticBackupDirectory(): String

    /**
     * Backup automático (decisão 108): grava um backup do dia em [directory] se ainda não houver um de
     * hoje, e apaga os mais antigos, deixando os últimos [keep]. Devolve o caminho do arquivo criado, ou
     * `null` se já havia um de hoje. Apontar [directory] pra uma pasta do Drive, OneDrive ou Dropbox
     * leva a cópia pra nuvem sem o app depender de servidor.
     */
    fun createAutomaticBackup(directory: String, keep: Int = 7): String?
}

/** Resultado de [BackupRepository.restore]. */
sealed interface RestoreResult {

    /** @property previousDataPath onde os dados que existiam antes da restauração foram guardados. */
    data class Success(val previousDataPath: String) : RestoreResult

    /** @property message motivo da falha, já escrito pra ser exibido direto pro usuário. */
    data class Failure(val message: String) : RestoreResult
}
