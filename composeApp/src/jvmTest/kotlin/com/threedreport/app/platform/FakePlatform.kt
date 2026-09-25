package com.threedreport.app.platform

/**
 * [PlatformServices] dos testes: sem diálogo nenhum. Cada escolha devolve o que o teste configurou, e o
 * que foi "salvo", "copiado" ou "aberto" fica registrado pra conferir.
 */
class FakePlatform : PlatformServices {
    var nextPick: PickResult = PickResult.Cancelled
    var nextSaveLocation: String? = null
    var clipboardWorks = true
    var browserWorks = true

    val written = mutableMapOf<String, ByteArray>()
    val copied = mutableListOf<String>()
    val opened = mutableListOf<String>()

    override fun pickFile(kind: FileKind): PickResult = nextPick
    override fun pickFilePath(kind: FileKind): String? = null
    override fun pickFolder(title: String, initialDirectory: String?): String? = null
    override fun chooseSaveLocation(suggestedFileName: String, initialDirectory: String?): String? =
        nextSaveLocation?.let { if (it.endsWith("/")) it + suggestedFileName else it }

    override fun writeFile(path: String, bytes: ByteArray): SaveResult {
        written[path] = bytes
        return SaveResult.Saved(path)
    }

    override fun saveFile(bytes: ByteArray, suggestedFileName: String, initialDirectory: String?): SaveResult {
        val path = chooseSaveLocation(suggestedFileName, initialDirectory) ?: return SaveResult.Cancelled
        return writeFile(path, bytes)
    }

    override fun copyToClipboard(text: String): Boolean {
        if (clipboardWorks) copied += text
        return clipboardWorks
    }

    override fun openUrl(url: String): Boolean {
        if (browserWorks) opened += url
        return browserWorks
    }

    override fun openFolder(path: String): Boolean = true
    override fun documentsDirectory(): String? = null
    override fun exitApp() = Unit
}
