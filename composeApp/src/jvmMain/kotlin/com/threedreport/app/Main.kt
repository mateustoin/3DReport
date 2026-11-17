package com.threedreport.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.threedreport.app.data.DataDirResult
import com.threedreport.app.data.DataReadException
import com.threedreport.app.data.LOGS_DIR_NAME
import com.threedreport.app.data.SavedWindowBounds
import com.threedreport.app.data.appDataDir
import com.threedreport.app.data.prepareDataDir
import com.threedreport.app.platform.DesktopPlatform
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
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import javax.swing.JOptionPane
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/** Trava de instância única; guardada aqui pra não ser liberada pelo coletor de lixo enquanto o app roda. */
private var instanceLock: FileLock? = null

/** Ponto de entrada do app desktop. */
fun main() {
    DesktopLog.install(File(appDataDir(), LOGS_DIR_NAME))
    // Os diálogos abaixo e os seletores de arquivo são Swing/AWT: os textos são lidos aqui, uma vez, antes de qualquer janela.
    StartupTexts.load()
    DesktopPlatform.loadDialogTitles()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        AppLog.error("Erro não tratado no thread ${thread.name}", error)
        showErrorDialog(error, fatal = false)
    }

    // Dois 3DReport abertos gravariam os mesmos arquivos a partir de estados diferentes, e a última
    // gravação apagaria a outra (decisão 108).
    if (!acquireSingleInstanceLock()) {
        JOptionPane.showMessageDialog(
            null,
            StartupTexts.alreadyRunning,
            "3DReport",
            JOptionPane.INFORMATION_MESSAGE,
        )
        exitProcess(0)
    }

    // Antes de qualquer repositório abrir um arquivo: dados de outro formato são convertidos ou vão pra
    // uma pasta à parte (decisões 104 e 106).
    val prepared = runCatching { prepareDataDir() }.getOrElse { error ->
        AppLog.error("Não consegui preparar a pasta de dados", error)
        fatalDialog(StartupTexts.dataDirFailed(appDataDir().path, error.message))
    }
    val notice = when (prepared) {
        is DataDirResult.Failed -> fatalDialog(StartupTexts.moveFailed(prepared.dataDir.path))

        is DataDirResult.Ready -> {
            val moved = prepared.moved
            when {
                prepared.migrated != null -> DataFolderNotice(DataFolderNotice.Kind.MIGRATED, prepared.migrated.originalCopy.path)

                moved == null -> null

                moved.fromNewerVersion -> DataFolderNotice(DataFolderNotice.Kind.MOVED_FROM_NEWER, moved.path.path)

                else -> {
                    // Dados da 1.x: guardados à parte, sem aviso (decisão 110).
                    AppLog.info("Dados da 1.x guardados em ${moved.path.path}")
                    null
                }
            }
        }
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val container = try {
        createDesktopContainer(appScope)
    } catch (e: DataReadException) {
        AppLog.error("Não consegui ler ${e.file.path}", e)
        fatalDialog(StartupTexts.readFailed(e.file.path))
    }
    scheduleAutomaticBackup(appScope, container)
    runApp(container, notice)
}

private fun runApp(container: AppContainer, notice: DataFolderNotice?) = application {
    val saved = container.preferences.preferences.value.window?.takeIf(::isOnSomeScreen)
    val windowState = rememberWindowState(
        placement = if (saved?.maximized == true) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = saved?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
        size = saved?.let { DpSize(it.width.dp, it.height.dp) } ?: initialWindowSize(),
    )
    val windowIcon = remember { loadWindowIcon() }
    // O caminho normal de fechar a janela; a atualização por dentro também fecha o app por ele (decisão 143).
    val closeApp: () -> Unit = {
        rememberWindowBounds(container, windowState)
        if (flushBeforeClosing(container)) {
            ExitWatchdog.arm(exitDumpFile())
            exitApplication()
        }
    }
    @OptIn(ExperimentalComposeUiApi::class)
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides crashHandlerFactory(container)) {
        Window(
            onCloseRequest = closeApp,
            title = "3DReport",
            icon = windowIcon,
            state = windowState,
        ) {
            window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
            FixMultiMonitorDpiRedrawBug()
            App(container, notice, onCloseApp = closeApp)
        }
    }
}

/**
 * Largo o bastante pra tela de Orçamento abrir em duas colunas sem a pessoa precisar redimensionar, mas
 * nunca maior que a área útil da tela: num notebook de 1366×768, a altura padrão escondia o rodapé
 * atrás da barra de tarefas.
 */
private const val MIN_WINDOW_WIDTH = 960
private const val MIN_WINDOW_HEIGHT = 640

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
 * Guarda onde a janela está (decisão 111). Maximizada, o tamanho do estado é o da tela inteira: guarda só
 * a marca e mantém o retângulo de antes, pra "restaurar" voltar pra ele na próxima vez.
 */
private fun rememberWindowBounds(container: AppContainer, state: WindowState) {
    val prefs = container.preferences.preferences.value
    val maximized = state.placement == WindowPlacement.Maximized
    val position = state.position
    val bounds = when {
        // Maximizada sem retângulo guardado (maximizou logo na primeira vez): guarda o tamanho inicial, que é
        // pra onde "restaurar" volta.
        maximized -> prefs.window?.copy(maximized = true) ?: initialWindowSize().let { size ->
            val origin = position as? WindowPosition.Absolute
            SavedWindowBounds(
                x = origin?.x?.value?.roundToInt() ?: 0,
                y = origin?.y?.value?.roundToInt() ?: 0,
                width = size.width.value.roundToInt(),
                height = size.height.value.roundToInt(),
                maximized = true,
            )
        }

        state.placement == WindowPlacement.Floating && position is WindowPosition.Absolute -> SavedWindowBounds(
            x = position.x.value.roundToInt(),
            y = position.y.value.roundToInt(),
            width = state.size.width.value.roundToInt(),
            height = state.size.height.value.roundToInt(),
        )

        else -> null
    } ?: return
    if (bounds != prefs.window) container.preferences.update(prefs.copy(window = bounds))
}

/**
 * A janela guardada só volta pro mesmo lugar se a barra de título dela ainda cair numa tela: com o
 * monitor de antes desligado, abrir lá deixava a janela invisível. O tamanho também precisa caber.
 */
private fun isOnSomeScreen(bounds: SavedWindowBounds): Boolean {
    if (bounds.width < MIN_WINDOW_WIDTH || bounds.height < MIN_WINDOW_HEIGHT) return false
    val screens = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration.bounds } }
        .getOrNull() ?: return false
    val titleBar = Rectangle(bounds.x, bounds.y, bounds.width, 40)
    return screens.any { screen ->
        val visible = screen.intersection(titleBar)
        !visible.isEmpty && visible.width >= 120 && bounds.width <= screen.width && bounds.height <= screen.height
    }
}

/** Onde o [ExitWatchdog] grava as pilhas das threads se o encerramento travar: junto do log. */
internal fun exitDumpFile(): File = File(File(appDataDir(), LOGS_DIR_NAME), "encerramento-travado.txt")

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
            StartupTexts.writeFailed(failures),
            "3DReport",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            StartupTexts.writeFailedButtons,
            StartupTexts.writeFailedButtons[0],
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
    val message = StartupTexts.errorMessage(fatal, DesktopLog.currentFile?.path)
    val choice = JOptionPane.showOptionDialog(
        null,
        message,
        "3DReport",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.ERROR_MESSAGE,
        null,
        arrayOf(StartupTexts.errorCopy, StartupTexts.close),
        StartupTexts.close,
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

/**
 * O ícone da janela e da barra de tarefas (decisão 128). O instalador traz o seu próprio; este é o que aparece com o
 * app aberto. Sem o recurso, a janela fica com o ícone padrão do sistema em vez de falhar.
 */
private fun loadWindowIcon(): Painter? = runCatching {
    val bytes = object {}.javaClass.getResourceAsStream("/window-icon.png")?.use { it.readBytes() } ?: return null
    BitmapPainter(org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap())
}.getOrNull()
