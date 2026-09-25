package com.threedreport.app.platform

/** Os tipos de arquivo que o app pede pra escolher, com o título do diálogo e as extensões aceitas. */
enum class FileKind(val dialogTitle: String, val extensions: Set<String>) {
    IMAGE("Escolher foto", setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")),
    GCODE("Escolher arquivo G-code", setOf("gcode", "gco", "g")),
    STL("Escolher arquivo STL", setOf("stl")),
    BACKUP("Escolher backup do 3DReport", setOf("zip")),
}

/** Resultado de escolher e ler um arquivo. */
sealed interface PickResult {
    data class Picked(val file: PickedFile) : PickResult
    data object Cancelled : PickResult

    /** @property message motivo, já escrito pra pessoa ("O arquivo está aberto em outro programa…"). */
    data class Failed(val message: String) : PickResult
}

/** Resultado de gravar um arquivo onde a pessoa escolheu. */
sealed interface SaveResult {
    /** @property path onde o arquivo ficou, pra oferecer "Abrir pasta". */
    data class Saved(val path: String) : SaveResult
    data object Cancelled : SaveResult

    /** @property message motivo, já escrito pra pessoa. */
    data class Failed(val message: String) : SaveResult
}

/**
 * O que depende do sistema operacional: escolher e gravar arquivos, área de transferência, abrir link
 * ou pasta (decisão 108). Interface injetada nos ViewModels, e não funções soltas chamadas direto: os
 * testes trocam por uma versão sem diálogo, e nenhuma dessas ações lança exceção — cada uma diz o que
 * aconteceu, e a tela mostra. Antes, salvar um PDF que estava aberto no leitor fechava o app.
 */
interface PlatformServices {

    /** Abre o seletor de arquivo e lê o escolhido. G-code grande vem só com o começo e o fim, onde ficam os metadados. */
    fun pickFile(kind: FileKind): PickResult

    /** Abre o seletor de arquivo e devolve só o caminho (pra arquivos grandes lidos aos poucos, como o backup). */
    fun pickFilePath(kind: FileKind): String?

    /** Abre o seletor de pasta e devolve a escolhida, ou `null` se cancelou. */
    fun pickFolder(title: String, initialDirectory: String? = null): String?

    /** Abre o "Salvar como" e devolve o caminho escolhido, sem gravar nada. */
    fun chooseSaveLocation(suggestedFileName: String, initialDirectory: String? = documentsDirectory()): String?

    /** Grava [bytes] em [path] (escolhido antes com [chooseSaveLocation]). */
    fun writeFile(path: String, bytes: ByteArray): SaveResult

    /** Abre o "Salvar como" e grava [bytes] onde a pessoa escolher. */
    fun saveFile(bytes: ByteArray, suggestedFileName: String, initialDirectory: String? = documentsDirectory()): SaveResult

    /** Copia [text] pra área de transferência. `false` se o sistema não deixou (outro programa usando). */
    fun copyToClipboard(text: String): Boolean

    /** Abre [url] no navegador padrão. `false` se não deu (sem navegador configurado, URL inválida). */
    fun openUrl(url: String): Boolean

    /** Abre a pasta [path] (ou a pasta do arquivo, se [path] for um arquivo) no gerenciador de arquivos. */
    fun openFolder(path: String): Boolean

    /** Pasta "Documentos" do usuário ("Documents" ou "Documentos"), ou a pasta pessoal se não houver. */
    fun documentsDirectory(): String?

    /** Fecha o app (depois de restaurar um backup, ver `BackupViewModel`). */
    fun exitApp()
}

/** Os serviços da plataforma em que o app está rodando. */
expect val defaultPlatform: PlatformServices

/** Atalho pros composables que abrem link ou pasta. */
fun openUrl(url: String): Boolean = defaultPlatform.openUrl(url)

fun openFolder(path: String): Boolean = defaultPlatform.openFolder(path)
