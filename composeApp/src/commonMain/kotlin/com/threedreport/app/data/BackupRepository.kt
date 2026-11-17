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

    /** @property reason o motivo da falha; a interface escolhe o texto (ver `RestoreFailure`). */
    data class Failure(val reason: RestoreFailure) : RestoreResult
}

/** Por que uma restauração não foi feita. Nenhum caso mexe nos dados atuais, salvo [CurrentDataMoved]. */
sealed interface RestoreFailure {

    /** A pasta de dados não tem pasta-mãe onde preparar a troca. */
    data object NoDataFolder : RestoreFailure

    /** O arquivo escolhido não abriu como `.zip` (ou tem entradas fora da pasta de destino). */
    data object UnreadableFile : RestoreFailure

    /** É um `.zip` que não é backup do 3DReport. */
    data object NotABackup : RestoreFailure

    /** O backup é de uma versão mais nova do app. */
    data object NewerVersion : RestoreFailure

    /** O backup é do formato de dados antigo (1.x) e não tem como ser convertido. */
    data object OldFormat : RestoreFailure

    /** Não deu pra guardar uma cópia dos dados atuais, e nada foi alterado. */
    data object CouldNotKeepCurrentData : RestoreFailure

    /** Não deu pra aplicar o backup e os dados atuais continuam onde estavam. */
    data object ApplyFailedKept : RestoreFailure

    /**
     * Não deu pra aplicar o backup e os dados atuais ficaram na pasta [keptAt], que precisa voltar a se chamar [dataFolderName].
     */
    data class CurrentDataMoved(val keptAt: String, val dataFolderName: String) : RestoreFailure
}
