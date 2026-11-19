package com.threedreport.app.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

/** Um elemento que recebeu o foco: [owner] é a camada (0 é a tela, as seguintes são diálogos e menus). */
internal data class Stop(val owner: Int, val id: Int, val name: String, val role: String?, val visible: Boolean) {
    /** O nome com o papel, como o relatório mostra: "Salvar (Button)". */
    val label: String get() = name + (role?.let { " ($it)" } ?: "")

    override fun toString() = "$label${if (owner > 0) " [camada $owner]" else ""}${if (!visible) " [FORA DA TELA]" else ""}"
}

/** O que o Tab fez numa tela: a sequência de paradas pra frente e pra trás, e o que ficou de fora. */
internal class TabRun(
    val name: String,
    val initial: Stop?,
    val forward: List<Stop?>,
    val backward: List<Stop?>,
    /** Elementos focáveis presentes na tela que nenhuma parada do ciclo alcançou. */
    val unreached: List<String>,
    /** Elementos interativos (clique, toque, texto) que não são focáveis, ou seja, o Tab nunca para neles. */
    val notFocusable: List<String>,
    val layers: Int,
    val dialog: Boolean = false,
) {
    /** Tamanho do ciclo: quantos Tabs até voltar à primeira parada; nulo se não voltou dentro do limite. */
    val cycle: Int?
        get() {
            val first = forward.firstOrNull() ?: return null
            val back = forward.drop(1).indexOfFirst { it?.id == first.id && it.owner == first.owner }
            return if (back < 0) null else back + 1
        }

    val stuck: Boolean get() = forward.size > 1 && forward.zipWithNext().any { (a, b) -> a != null && a == b }
    val lost: Boolean get() = forward.any { it == null }
    val escaped: Boolean get() = dialog && (forward + backward).any { it != null && it.owner == 0 }
    val hidden: List<Stop> get() = (forward + backward).filterNotNull().filter { !it.visible }.distinct()

    fun report(): String = buildString {
        appendLine("== $name (camadas: $layers)")
        appendLine("inicial: ${initial ?: "nada focado"}")
        appendLine("Tab: " + forward.withIndex().joinToString(" > ") { (i, s) -> "${i + 1}:${s ?: "NADA"}" })
        appendLine("Shift+Tab: " + backward.withIndex().joinToString(" > ") { (i, s) -> "${i + 1}:${s ?: "NADA"}" })
        appendLine("ciclo: ${cycle ?: "não fechou"}; parado: $stuck; perdeu o foco: $lost; saiu da camada: $escaped; fora da tela: ${hidden.size}")
        if (unreached.isNotEmpty()) appendLine("focáveis que o Tab não alcançou: $unreached")
        if (notFocusable.isNotEmpty()) appendLine("interativos sem foco: $notFocusable")
    }
}

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
internal class FocusProbe(val scene: ImageComposeScene, private val width: Int, private val height: Int) {

    private fun layers(): List<SemanticsNode> = scene.semanticsOwners.map { it.rootSemanticsNode }

    private fun walk(node: SemanticsNode, action: (SemanticsNode) -> Unit) {
        action(node)
        node.children.forEach { walk(it, action) }
    }

    private fun nameOf(node: SemanticsNode): String {
        val config = node.config
        val description = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("/")
        val text = config.getOrNull(SemanticsProperties.Text)?.joinToString("/") { it.text }
        return (description ?: text ?: "?").take(48)
    }

    private fun roleOf(node: SemanticsNode): String? = node.config.getOrNull(SemanticsProperties.Role)?.toString()

    private fun labelOf(node: SemanticsNode): String = nameOf(node) + (roleOf(node)?.let { " ($it)" } ?: "")

    private fun visible(node: SemanticsNode): Boolean {
        val b = node.boundsInRoot
        if (b.width <= 0f || b.height <= 0f) return false
        if (b.bottom < 0f || b.right < 0f || b.top > height || b.left > width) return false
        // Dentro de todo ancestral que rola, o centro do elemento precisa estar à vista.
        var parent = node.parent
        while (parent != null) {
            if (parent.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null) {
                val p: Rect = parent.boundsInRoot
                if (b.center.y < p.top || b.center.y > p.bottom) return false
            }
            parent = parent.parent
        }
        return true
    }

    private fun focusedStop(): Stop? {
        layers().forEachIndexed { index, root ->
            var found: Stop? = null
            walk(root) { node ->
                if (found == null && node.config.getOrNull(SemanticsProperties.Focused) == true) {
                    found = Stop(index, node.id, nameOf(node), roleOf(node), visible(node))
                }
            }
            if (found != null) return found
        }
        return null
    }

    fun currentLabel(): String = focusedStop()?.toString() ?: "NADA"

    fun press(shift: Boolean = false) {
        scene.sendKeyEvent(KeyEvent(Key.Tab, KeyEventType.KeyDown, isShiftPressed = shift))
        scene.sendKeyEvent(KeyEvent(Key.Tab, KeyEventType.KeyUp, isShiftPressed = shift))
        repeat(3) {
            scene.render(System.nanoTime())
            Thread.sleep(25)
        }
        // A rolagem até o foco é animada: espera o elemento chegar à vista antes de medir.
        var waits = 0
        while (waits++ < 15 && focusedStop()?.visible == false) {
            scene.render(System.nanoTime())
            Thread.sleep(40)
        }
    }

    /** Aperta [key] (Enter, Espaço...) no elemento focado, como o teclado faz. */
    fun key(key: Key) {
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
        repeat(3) {
            scene.render(System.nanoTime())
            Thread.sleep(25)
        }
    }

    /** Quantas camadas (tela, diálogos, menus) existem agora. */
    fun layerCount(): Int = scene.semanticsOwners.size

    /** Se o elemento focado marca e está marcado; nulo se ele não marca. */
    fun focusedChecked(): Boolean? {
        var found: Boolean? = null
        layers().forEach { root ->
            walk(root) { node ->
                if (found == null && node.config.getOrNull(SemanticsProperties.Focused) == true) {
                    found = node.config.getOrNull(SemanticsProperties.ToggleableState)?.let { it == androidx.compose.ui.state.ToggleableState.On }
                }
            }
        }
        return found
    }

    fun run(name: String, maxTabs: Int, dialog: Boolean = false, walkBack: Boolean = true): TabRun {
        val initial = focusedStop()
        val forward = mutableListOf<Stop?>()
        for (i in 0 until maxTabs) {
            press()
            forward += focusedStop()
            // Fechou o ciclo: não precisa passar mais.
            val first = forward.first()
            if (forward.size > 1 && first != null && forward.last()?.let { it.id == first.id && it.owner == first.owner } == true) break
            if (forward.size >= 3 && forward.takeLast(3).distinct().size == 1) break
        }
        val trimmed = forward.toList()
        val backward = mutableListOf<Stop?>()
        repeat(if (walkBack) trimmed.size.coerceAtMost(maxTabs) else 0) {
            press(shift = true)
            backward += focusedStop()
        }

        val visited = (trimmed + backward).filterNotNull().map { it.owner to it.id }.toSet()
        val unreached = mutableListOf<String>()
        val notFocusable = mutableListOf<String>()
        val layerNodes = layers()
        layerNodes.forEachIndexed { index, root ->
            // Com um diálogo aberto só a camada dele está ao alcance do Tab.
            if (dialog && index < layerNodes.size - 1) return@forEachIndexed
            walk(root) { node ->
                val config = node.config
                if (!visible(node)) return@walk
                val focusable = config.contains(SemanticsProperties.Focused)
                val disabled = config.contains(SemanticsProperties.Disabled)
                val label = labelOf(node)
                // A dica de ajuda sai da ordem do Tab de propósito (continua clicável e descrita).
                if (focusable && (index to node.id) !in visited && !label.startsWith(HELP_PREFIX)) unreached += label
                val interactive = config.contains(SemanticsActions.OnClick) || config.contains(SemanticsActions.SetText) || config.contains(SemanticsProperties.ToggleableState)
                if (!focusable && interactive && !disabled) notFocusable += labelOf(node)
            }
        }
        return TabRun(name, initial, trimmed, backward, unreached, notFocusable, layerNodes.size, dialog)
    }
}

private const val HELP_PREFIX = "Ajuda: "
