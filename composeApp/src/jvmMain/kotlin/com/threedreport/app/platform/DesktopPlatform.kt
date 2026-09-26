package com.threedreport.app.platform

import com.threedreport.app.ExitWatchdog
import com.threedreport.app.exitDumpFile
import com.threedreport.app.AppLog
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import javax.swing.JFileChooser
import kotlin.system.exitProcess

actual val defaultPlatform: PlatformServices = DesktopPlatform

/**
 * [PlatformServices] do desktop: diálogos nativos (`java.awt.FileDialog`, decisão 59) e
 * `java.awt.Desktop`. Nenhuma ação lança exceção: cada uma devolve o que aconteceu.
 */
object DesktopPlatform : PlatformServices {

    /** G-code acima disso é lido só pelo começo e pelo fim (ver [readGCodeForMetadata]). */
    private const val GCODE_FULL_READ_LIMIT = 8L * 1024 * 1024
    private const val GCODE_EDGE_BYTES = 4 * 1024 * 1024

    override fun pickFile(kind: FileKind): PickResult {
        val file = showOpenDialog(kind) ?: return PickResult.Cancelled
        return readPicked(file, kind)
    }

    override fun pickFilePath(kind: FileKind): String? = showOpenDialog(kind)?.path

    /**
     * O `FileDialog` do AWT não escolhe pasta no Windows, então aqui é o seletor do Swing, com a aparência
     * do sistema (instalada no `main`). É o único diálogo do app que não é o nativo (decisão 59).
     */
    override fun pickFolder(title: String, initialDirectory: String?): String? {
        val chooser = JFileChooser(initialDirectory?.let(::File))
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.path else null
    }

    override fun chooseSaveLocation(suggestedFileName: String, initialDirectory: String?): String? {
        val dialog = FileDialog(null as Frame?, "Salvar como", FileDialog.SAVE)
        dialog.file = suggestedFileName
        initialDirectory?.let { dialog.directory = it }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val fileName = dialog.file ?: return null
        // Quem apaga a extensão no diálogo continua recebendo um arquivo que abre com dois cliques.
        val extension = suggestedFileName.substringAfterLast('.', "")
        val withExtension = if (extension.isNotEmpty() && !fileName.endsWith(".$extension", ignoreCase = true)) "$fileName.$extension" else fileName
        return File(directory, withExtension).path
    }

    override fun saveFile(bytes: ByteArray, suggestedFileName: String, initialDirectory: String?): SaveResult {
        val path = chooseSaveLocation(suggestedFileName, initialDirectory) ?: return SaveResult.Cancelled
        return writeFile(path, bytes)
    }

    override fun writeFile(path: String, bytes: ByteArray): SaveResult {
        return try {
            File(path).writeBytes(bytes)
            SaveResult.Saved(path)
        } catch (e: IOException) {
            AppLog.warn("Falha ao salvar $path", e)
            SaveResult.Failed(
                "Não consegui salvar ${File(path).name}. Se ele estiver aberto em outro programa (leitor de PDF, " +
                    "visualizador de imagens), feche e tente de novo, ou salve com outro nome.",
            )
        }
    }

    override fun copyToClipboard(text: String): Boolean = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }.onFailure { AppLog.warn("Falha ao copiar pra área de transferência", it) }.isSuccess

    override fun openUrl(url: String): Boolean = runCatching {
        val normalized = if (url.contains("://")) url else "https://$url"
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { "sem navegador" }
        Desktop.getDesktop().browse(URI(normalized))
    }.onFailure { AppLog.warn("Falha ao abrir o link $url", it) }.isSuccess

    override fun openFolder(path: String): Boolean = runCatching {
        val target = File(path).let { if (it.isDirectory) it else it.parentFile }
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) { "sem gerenciador de arquivos" }
        Desktop.getDesktop().open(target)
    }.onFailure { AppLog.warn("Falha ao abrir a pasta $path", it) }.isSuccess

    override fun documentsDirectory(): String? {
        val home = System.getProperty("user.home") ?: return null
        return listOf("Documents", "Documentos")
            .map { File(home, it) }
            .firstOrNull { it.isDirectory }
            ?.path
            ?: home
    }

    override fun exitApp() {
        // Depois de restaurar um backup: as gravações já estão seguradas, e o encerramento não pode travar.
        ExitWatchdog.arm(exitDumpFile())
        exitProcess(0)
    }

    private fun showOpenDialog(kind: FileKind): File? {
        val dialog = FileDialog(null as Frame?, kind.dialogTitle, FileDialog.LOAD)
        // O filtro funciona onde o diálogo nativo respeita o FilenameFilter (Linux); no Windows ele é
        // ignorado, limitação aceita pra manter o diálogo nativo (decisão 59).
        dialog.filenameFilter = FilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in kind.extensions }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val fileName = dialog.file ?: return null
        return File(directory, fileName)
    }

    /** Lê um arquivo escolhido ou arrastado, sem nunca lançar exceção. */
    fun readPicked(file: File, kind: FileKind): PickResult = try {
        val bytes = if (kind == FileKind.GCODE) readGCodeForMetadata(file) else file.readBytes()
        PickResult.Picked(PickedFile(file.name, bytes))
    } catch (e: IOException) {
        AppLog.warn("Falha ao ler ${file.path}", e)
        PickResult.Failed("Não consegui ler ${file.name}. Ele pode estar aberto em outro programa ou ter sido movido.")
    } catch (e: OutOfMemoryError) {
        AppLog.warn("Arquivo grande demais: ${file.path}", e)
        PickResult.Failed("${file.name} é grande demais pra abrir no app.")
    }

    /**
     * Os fatiadores gravam os metadados (tempo, consumo, miniatura, impressora, configurações) no começo
     * e no fim do G-code, e o meio são milhões de linhas de movimento. Arquivo grande é lido só pelas
     * pontas: ler 300 MB inteiros e transformar em texto travava o app e podia estourar a memória.
     */
    internal fun readGCodeForMetadata(file: File): ByteArray {
        val length = file.length()
        if (length <= GCODE_FULL_READ_LIMIT) return file.readBytes()
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(GCODE_EDGE_BYTES)
            raf.readFully(head)
            val tail = ByteArray(GCODE_EDGE_BYTES)
            raf.seek(length - GCODE_EDGE_BYTES)
            raf.readFully(tail)
            return head + "\n".encodeToByteArray() + tail
        }
    }
}
