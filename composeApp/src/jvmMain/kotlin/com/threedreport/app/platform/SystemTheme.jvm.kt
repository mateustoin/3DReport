package com.threedreport.app.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.threedreport.app.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private val isLinux: Boolean = System.getProperty("os.name").orEmpty().lowercase().startsWith("linux")

/**
 * No Windows e no macOS, o do Compose. No Linux, a preferência do portal do desktop
 * (`org.freedesktop.appearance color-scheme`), que GNOME, KDE, Cinnamon e os demais com xdg-desktop-portal publicam:
 * lida uma vez ao abrir e acompanhada pelo sinal de mudança. Sem portal (ou sem `gdbus`), fica claro, como antes.
 */
@Composable
actual fun systemPrefersDark(): Boolean {
    if (!isLinux) return isSystemInDarkTheme()
    var dark by remember { mutableStateOf(LinuxColorScheme.cached ?: false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            LinuxColorScheme.read()?.let { dark = it }
            LinuxColorScheme.follow { dark = it }
        }
    }
    return dark
}

/** Leitura da preferência de cor do portal do desktop no Linux, pelo `gdbus` (vem com a GLib, presente nos desktops comuns). */
internal object LinuxColorScheme {
    /** A última leitura, pra a janela não piscar claro quando o tema é recomposto. */
    @Volatile
    var cached: Boolean? = null
        private set

    private val portalArgs = listOf(
        "--session",
        "--dest", "org.freedesktop.portal.Desktop",
        "--object-path", "/org/freedesktop/portal/desktop",
    )

    /** Lê a preferência agora; `null` se o portal não respondeu. */
    fun read(): Boolean? = runCatching {
        val process = ProcessBuilder(
            listOf("gdbus", "call") + portalArgs +
                listOf("--method", "org.freedesktop.portal.Settings.ReadOne", "org.freedesktop.appearance", "color-scheme"),
        ).redirectErrorStream(true).start()
        if (!process.waitFor(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        parseColorScheme(process.inputStream.bufferedReader().readText())
    }.getOrNull().also { if (it != null) cached = it }

    /**
     * Fica ouvindo o sinal `SettingChanged` do portal e chama [onChange] a cada troca de `color-scheme`. Bloqueia
     * enquanto a corrotina estiver ativa; o processo é encerrado quando ela é cancelada.
     */
    suspend fun follow(onChange: (Boolean) -> Unit) {
        val process = runCatching {
            ProcessBuilder(listOf("gdbus", "monitor") + portalArgs).redirectErrorStream(true).start()
        }.getOrElse {
            AppLog.info("Tema do sistema: sem gdbus pra acompanhar a troca (${it.message})")
            return
        }
        try {
            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (!currentCoroutineContext().isActive) break
                    if ("SettingChanged" in line && "color-scheme" in line) {
                        parseColorScheme(line)?.let {
                            cached = it
                            onChange(it)
                        }
                    }
                }
            }
        } finally {
            process.destroy()
        }
    }

    /**
     * O valor de `color-scheme` numa resposta do `gdbus` ("(<<uint32 1>>,)" ou o sinal com "<uint32 1>"):
     * 1 é escuro, 0 (sem preferência) e 2 (claro) são claro. `null` se não houver número.
     */
    fun parseColorScheme(output: String): Boolean? =
        Regex("""uint32 (\d+)""").find(output)?.groupValues?.get(1)?.toIntOrNull()?.let { it == PREFER_DARK }

    private const val PREFER_DARK = 1
    private const val READ_TIMEOUT_SECONDS = 2L
}
