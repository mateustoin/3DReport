package com.threedreport.app.platform

import com.threedreport.core.model.ModelFileKind

/**
 * Os tipos de arquivo que o app pede pra escolher, com as extensões aceitas. O título do diálogo do sistema vem de
 * recurso, lido pela plataforma (ver `PlatformTexts`).
 */
enum class FileKind(val extensions: Set<String>) {
    IMAGE(setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")),
    GCODE(setOf("gcode", "gco", "g")),

    /** O arquivo do modelo de um orçamento (decisão 141): STL ou 3MF. */
    MODEL(setOf("stl", "3mf")),
    BACKUP(setOf("zip")),
}

/**
 * Se [fileName] é um arquivo do modelo (STL ou 3MF). O `.gcode.3mf` (arquivo fatiado do Bambu Studio) não é: ele
 * continua sendo tratado como G-code que o app ainda não lê.
 */
fun isModelFileName(fileName: String): Boolean =
    ModelFileKind.of(fileName) != null && !fileName.lowercase().endsWith(".gcode.3mf")

/** Resultado de escolher e ler um arquivo. */
sealed interface PickResult {
    data class Picked(val file: PickedFile) : PickResult
    data object Cancelled : PickResult

    /**
     * @property reason por que não deu pra ler; a interface escolhe o texto (ver `PickResult.Failed.text`)
     * @property fileName o arquivo que falhou, pro texto citar
     */
    data class Failed(val reason: PickFailure, val fileName: String) : PickResult

    /**
     * Arquivo arrastado que ainda não foi lido: [read] lê (bloqueia, e pode devolver [Picked] ou [Failed]) e quem
     * recebe chama fora da thread da tela. Só o arquivo do modelo chega assim: um STL pesado travaria a janela.
     */
    class Deferred(val fileName: String, val read: () -> PickResult) : PickResult
}

/** Se este arquivo, escolhido ou arrastado, é um arquivo do modelo (ver [isModelFileName]). */
fun PickResult.isModelFile(): Boolean = when (this) {
    is PickResult.Picked -> isModelFileName(file.fileName)
    is PickResult.Failed -> isModelFileName(fileName)
    is PickResult.Deferred -> isModelFileName(fileName)
    PickResult.Cancelled -> false
}

/** Por que um arquivo escolhido ou arrastado não foi lido. */
enum class PickFailure {
    /** Não abriu: está aberto em outro programa ou foi movido. */
    UNREADABLE,

    /** É grande demais pra caber na memória do app. */
    TOO_LARGE,
}

/** Resultado de gravar um arquivo onde a pessoa escolheu. */
sealed interface SaveResult {
    /** @property path onde o arquivo ficou, pra oferecer "Abrir pasta". */
    data class Saved(val path: String) : SaveResult
    data object Cancelled : SaveResult

    /** @property fileName o arquivo que não deu pra gravar (o motivo provável é sempre o mesmo, e a tela o explica). */
    data class Failed(val fileName: String) : SaveResult
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
