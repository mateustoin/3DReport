package com.threedreport.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.threedreport.app.data.DataDirResult
import com.threedreport.app.data.DataReadException
import com.threedreport.app.data.appDataDir
import com.threedreport.app.data.prepareDataDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** Trava de instância única; guardada aqui pra não ser liberada pelo coletor de lixo enquanto o app roda. */
private var instanceLock: FileLock? = null

/** Ponto de entrada do app desktop. */
fun main() {
    DesktopLog.install(File(appDataDir(), "logs"))
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        AppLog.error("Erro não tratado no thread ${thread.name}", error)
        showErrorDialog(error, fatal = false)
    }

    // Dois 3DReport abertos gravariam os mesmos arquivos a partir de estados diferentes, e a última
    // gravação apagaria a outra (decisão 108).
    if (!acquireSingleInstanceLock()) {
        JOptionPane.showMessageDialog(
            null,
            "O 3DReport já está aberto. Use a janela que já está aberta (ela pode estar minimizada ou atrás de outra).",
            "3DReport",
            JOptionPane.INFORMATION_MESSAGE,
        )
        exitProcess(0)
    }

    // Antes de qualquer repositório abrir um arquivo: dados de outro formato são convertidos ou vão pra
    // uma pasta à parte (decisões 104 e 106).
    val prepared = runCatching { prepareDataDir() }.getOrElse { error ->
        AppLog.error("Não consegui preparar a pasta de dados", error)
        fatalDialog("Não consegui abrir a pasta de dados\n${appDataDir().path}\n\n${error.message}\n\nNada foi apagado.")
    }
    val notice = when (prepared) {
        is DataDirResult.Failed -> fatalDialog(
            "Esta versão usa um formato de dados novo e precisa guardar os dados da versão anterior à " +
                "parte, mas não conseguiu mover a pasta\n${prepared.dataDir.path}\n\n" +
                "Feche os programas que estejam usando essa pasta (explorador de arquivos, antivírus, " +
                "backup) e abra o 3DReport de novo. Nada foi apagado.",
        )
        is DataDirResult.Ready -> when {
            prepared.migrated != null -> DataFolderNotice(DataFolderNotice.Kind.MIGRATED, prepared.migrated.originalCopy.path)
            prepared.moved != null -> DataFolderNotice(
                if (prepared.moved.fromNewerVersion) DataFolderNotice.Kind.MOVED_FROM_NEWER else DataFolderNotice.Kind.MOVED_FROM_OLDER,
                prepared.moved.path.path,
            )
            else -> null
        }
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val container = try {
        createDesktopContainer(appScope)
    } catch (e: DataReadException) {
        AppLog.error("Não consegui ler ${e.file.path}", e)
        fatalDialog(
            "Não consegui ler o arquivo\n${e.file.path}\n\nEle pode estar aberto em outro programa (antivírus, " +
                "OneDrive, backup). Feche esses programas e abra o 3DReport de novo. Nada foi apagado.",
        )
    }
    scheduleAutomaticBackup(appScope, container)
    runApp(container, notice)
}

private fun runApp(container: AppContainer, notice: DataFolderNotice?) = application {
    val windowState = rememberWindowState(size = initialWindowSize())
    @OptIn(ExperimentalComposeUiApi::class)
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides crashHandlerFactory(container)) {
        Window(
            onCloseRequest = { if (flushBeforeClosing(container)) exitApplication() },
            title = "3DReport",
            state = windowState,
        ) {
            window.minimumSize = Dimension(960, 640)
            FixMultiMonitorDpiRedrawBug()
            App(container, notice)
        }
    }
}

/**
 * Largo o bastante pra tela de Orçamento abrir em duas colunas sem a pessoa precisar redimensionar, mas
 * nunca maior que a área útil da tela: num notebook de 1366×768, a altura padrão escondia o rodapé
 * atrás da barra de tarefas.
 */
private fun initialWindowSize(): DpSize {
    val bounds = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }.getOrNull()
        ?: return DpSize(1280.dp, 820.dp)
    val scale = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
    }.getOrDefault(1.0).takeIf { it > 0 } ?: 1.0
    // maximumWindowBounds já vem em pixels lógicos no Java 9+, que é o que o Dp representa aqui.
    val width = minOf(1280, bounds.width - 16)
    val height = minOf(820, bounds.height - 16)
    AppLog.info("Área útil da tela: ${bounds.width}x${bounds.height} (escala $scale)")
    return DpSize(width.coerceAtLeast(960).dp, height.coerceAtLeast(600).dp)
}

/**
 * Espera as gravações pendentes antes de fechar. Se alguma não der certo, pergunta: fechar assim perde
 * a última mudança.
 */
private fun flushBeforeClosing(container: AppContainer): Boolean {
    // Depois de restaurar um backup as gravações ficam seguradas de propósito: o que está na memória é o
    // estado antigo e não pode ir pra pasta restaurada.
    if (container.pendingWrites.isPaused) return true
    while (true) {
        val written = runBlocking { withTimeoutOrNull(5_000) { container.pendingWrites.awaitAll() } != null }
        if (written && container.pendingWrites.allWritten) return true
        val failures = container.storageHealth.writeFailures.value.entries.joinToString("\n") { "• ${it.key}: ${it.value}" }
        val choice = JOptionPane.showOptionDialog(
            null,
            "Não consegui gravar as últimas mudanças no disco.\n\n$failures\n\n" +
                "Feche programas que possam estar usando a pasta de dados e tente de novo.",
            "3DReport",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            arrayOf("Tentar de novo", "Fechar mesmo assim", "Voltar pro app"),
            "Tentar de novo",
        )
        when (choice) {
            0 -> container.pendingWrites.retry()
            1 -> return true
            else -> return false
        }
    }
}

/**
 * Um erro dentro da janela (desenho da tela, clique) não fecha mais o app em silêncio: fica no log, as
 * gravações pendentes vão pro disco e a pessoa vê o que houve, com os detalhes pra copiar.
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun crashHandlerFactory(container: AppContainer) = WindowExceptionHandlerFactory { window ->
    WindowExceptionHandler { error ->
        AppLog.error("Erro na janela", error)
        runBlocking { withTimeoutOrNull(3_000) { container.pendingWrites.awaitAll() } }
        showErrorDialog(error, fatal = true)
        window.dispose()
        exitProcess(1)
    }
}

private fun showErrorDialog(error: Throwable, fatal: Boolean) {
    val message = (if (fatal) "O 3DReport encontrou um erro e precisa fechar." else "O 3DReport encontrou um erro.") +
        " Seus dados já gravados estão a salvo.\n\n" +
        "Se quiser ajudar a corrigir, copie os detalhes e mande numa issue do GitHub.\n" +
        (DesktopLog.currentFile?.let { "Registro completo em: ${it.path}" } ?: "")
    val choice = JOptionPane.showOptionDialog(
        null,
        message,
        "3DReport",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.ERROR_MESSAGE,
        null,
        arrayOf("Copiar detalhes", "Fechar"),
        "Fechar",
    )
    if (choice == 0) {
        runCatching {
            val details = "3DReport $APP_VERSION · ${System.getProperty("os.name")} · Java ${System.getProperty("java.version")}\n\n" +
                error.stackTraceText()
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(details), null)
        }
    }
}

private fun fatalDialog(message: String): Nothing {
    JOptionPane.showMessageDialog(null, message, "3DReport", JOptionPane.ERROR_MESSAGE)
    exitProcess(1)
}

/**
 * Trava num arquivo ao lado da pasta de dados (e não dentro dela: um arquivo aberto lá dentro impediria
 * renomear a pasta no Windows). `false` se outra instância já tem a trava.
 */
private fun acquireSingleInstanceLock(): Boolean {
    val dataDir = appDataDir()
    val lockFile = File(dataDir.parentFile ?: dataDir, "${dataDir.name}.lock")
    return try {
        val channel = RandomAccessFile(lockFile, "rw").channel
        val lock = channel.tryLock() ?: return false
        instanceLock = lock
        true
    } catch (_: OverlappingFileLockException) {
        false
    } catch (e: Exception) {
        // Sem conseguir criar a trava (pasta sem permissão), o app abre mesmo assim: impedir de abrir
        // seria pior do que o risco de duas instâncias.
        AppLog.warn("Não consegui criar a trava de instância única", e)
        true
    }
}

/**
 * Backup automático (decisão 108): uns segundos depois de abrir, se ligado e ainda sem backup de hoje,
 * grava um na pasta escolhida e guarda só os últimos 7.
 */
private fun scheduleAutomaticBackup(scope: CoroutineScope, container: AppContainer) {
    val preferences = container.preferences.preferences.value
    if (!preferences.autoBackupEnabled) return
    scope.launch(Dispatchers.IO) {
        delay(5_000)
        container.pendingWrites.awaitAll()
        val directory = preferences.backupDirectory ?: container.backup.defaultAutomaticBackupDirectory()
        runCatching { container.backup.createAutomaticBackup(directory) }
            .onSuccess { created ->
                if (created != null) {
                    AppLog.info("Backup automático gravado em $created")
                    withContext(Dispatchers.Main) {
                        container.preferences.update(container.preferences.preferences.value.copy(lastBackupEpochMillis = System.currentTimeMillis()))
                    }
                }
            }
            .onFailure { AppLog.warn("Backup automático falhou", it) }
    }
}

/**
 * Contorna um bug conhecido do Compose Desktop/Skiko: ao arrastar a janela pra
 * um monitor com DPI/escala diferente, o conteúdo (ex.: a barra de abas) fica
 * desenhado com o layout antigo até algo forçar um relayout — normalmente só
 * volta ao redimensionar a janela na mão. Não há correção oficial (ver
 * https://github.com/JetBrains/compose-multiplatform/issues/3685 e relacionadas);
 * a solução da comunidade é detectar a troca de monitor e simular esse
 * redimensionamento (1px pra frente e de volta) programaticamente.
 */
@Composable
private fun FrameWindowScope.FixMultiMonitorDpiRedrawBug() {
    DisposableEffect(window) {
        var lastGraphicsConfiguration = window.graphicsConfiguration
        val listener = object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) {
                val currentGraphicsConfiguration = window.graphicsConfiguration
                if (currentGraphicsConfiguration != lastGraphicsConfiguration) {
                    lastGraphicsConfiguration = currentGraphicsConfiguration
                    val size = window.size
                    window.size = Dimension(size.width, size.height + 1)
                    window.size = size
                }
            }
        }
        window.addComponentListener(listener)
        onDispose { window.removeComponentListener(listener) }
    }
}
