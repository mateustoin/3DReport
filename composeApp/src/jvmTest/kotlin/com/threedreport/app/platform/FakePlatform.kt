package com.threedreport.app.platform

/**
 * [PlatformServices] dos testes: sem diálogo nenhum. Cada escolha devolve o que o teste configurou, e o
 * que foi "salvo", "copiado" ou "aberto" fica registrado pra conferir.
 */
class FakePlatform : PlatformServices {
    var nextPick: PickResult = PickResult.Cancelled
    var nextPickPath: String? = null
    var nextSaveLocation: String? = null
    var clipboardWorks = true
    var browserWorks = true

    val written: MutableMap<String, ByteArray> = java.util.concurrent.ConcurrentHashMap()
    val copied = mutableListOf<String>()
    val opened = mutableListOf<String>()

    override fun pickFile(kind: FileKind): PickResult = nextPick
    override fun pickFilePath(kind: FileKind): String? = nextPickPath
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

    /**
     * Espera até haver [count] arquivos gravados (ou o tempo acabar) e devolve [written]. Salvar lê o nome sugerido e os
     * textos dos recursos; na primeira leitura eles chegam de outro thread, mesmo com `Unconfined`.
     */
    fun awaitWritten(count: Int = 1, timeoutMillis: Long = 10_000): Map<String, ByteArray> {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (written.size < count && System.currentTimeMillis() < deadline) Thread.sleep(10)
        return written
    }

    override fun openFolder(path: String): Boolean = true
    override fun documentsDirectory(): String? = null
    override fun exitApp() = Unit
}
