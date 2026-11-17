package com.threedreport.app.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.threedreport.app.ui.icons.AppIcons
import com.threedreport.app.ui.theme.AppTheme
import com.threedreport.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O rótulo do campo que não cabe quebra em duas linhas em vez de truncar, e os campos de uma mesma linha da grade
 * continuam alinhados pela caixa (Leva 10B, parte 7).
 */
@OptIn(ExperimentalComposeUiApi::class)
class FieldLabelLayoutTest {

    private val longLabel = "Valor da sua hora de trabalho com um rótulo bem comprido que não cabe"
    private val shortLabel = "Margem"

    /** O topo, e a altura, do campo cuja descrição é o rótulo, numa grade de 600 de largura (colunas de uns 190). */
    private fun layoutOf(vararg labels: String): Map<String, Pair<Float, Float>> {
        val scene = ImageComposeScene(width = 600, height = 300, density = Density(1f)) {
            AppTheme(ThemeMode.LIGHT) {
                FormGrid {
                    labels.forEach { label -> cell(1) { AppTextField(label = label, value = "", onValueChange = {}) } }
                }
            }
        }
        try {
            repeat(3) { scene.render(it * 16_000_000L) }
            val found = mutableMapOf<String, Pair<Float, Float>>()
            fun walk(node: SemanticsNode) {
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.let { description ->
                    if (description in labels) found[description] = node.boundsInRoot.top to node.boundsInRoot.height
                }
                node.children.forEach(::walk)
            }
            scene.semanticsOwners.forEach { walk(it.rootSemanticsNode) }
            return found
        } finally {
            scene.close()
        }
    }

    @Test
    fun aLabelThatDoesNotFitWrapsAndTheBoxesOfTheRowStayAligned() {
        val layout = layoutOf(longLabel, shortLabel)

        assertEquals(2, layout.size)
        assertEquals(layout.getValue(longLabel).first, layout.getValue(shortLabel).first, "as caixas ficam na mesma altura")
    }

    @Test
    fun labelsThatFitLeaveTheRowExactlyAsBefore() {
        val layout = layoutOf(shortLabel, "Taxa")
        val alone = layoutOf(shortLabel)

        assertEquals(layout.getValue(shortLabel).first, layout.getValue("Taxa").first)
        assertEquals(alone.getValue(shortLabel).first, layout.getValue(shortLabel).first, "a altura da caixa é a de um rótulo de uma linha")
    }

    @Test
    fun aWrappedLabelPushesTheBoxDownByOneLine() {
        val wrapped = layoutOf(longLabel).getValue(longLabel).first
        val single = layoutOf(shortLabel).getValue(shortLabel).first

        assertTrue(wrapped > single, "o rótulo de duas linhas empurra a caixa: $wrapped contra $single")
    }

    /** O centro vertical dos nós cujas descrições são [descriptions], numa grade com dois campos e uma ação de linha. */
    private fun centersWithAction(vararg labels: String): Map<String, Float> {
        val scene = ImageComposeScene(width = 600, height = 300, density = Density(1f)) {
            AppTheme(ThemeMode.LIGHT) {
                FormGrid {
                    labels.forEach { label -> cell(1) { AppTextField(label = label, value = "", onValueChange = {}) } }
                    cell(1) { FieldRowAction { IconAction(AppIcons.Close, "remover filamento", onClick = {}) } }
                }
            }
        }
        try {
            repeat(3) { scene.render(it * 16_000_000L) }
            val found = mutableMapOf<String, Float>()
            fun walk(node: SemanticsNode) {
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.let { description ->
                    if (description in labels || description == "remover filamento") found[description] = node.boundsInRoot.center.y
                }
                node.children.forEach(::walk)
            }
            scene.semanticsOwners.forEach { walk(it.rootSemanticsNode) }
            return found
        } finally {
            scene.close()
        }
    }

    @Test
    fun aRowActionStaysAlignedWithTheBoxesWhenALabelWraps() {
        val centers = centersWithAction(longLabel, shortLabel)

        assertEquals(centers.getValue(shortLabel), centers.getValue("remover filamento"), "o botão fica na altura das caixas")
    }
}
