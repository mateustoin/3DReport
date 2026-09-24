package com.threedreport.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Os vetores são texto colado de SVG ou desenhado à mão: um path quebrado só apareceria como um
 * ícone em branco na tela. Aqui cada ícone é montado de verdade e precisa ter algum traço.
 */
class AppIconsTest {

    private val allIcons: List<Pair<String, ImageVector>> =
        AppIcons::class.java.declaredMethods
            .filter { it.name.startsWith("get") && it.returnType == ImageVector::class.java }
            .map { it.name.removePrefix("get") to it.invoke(AppIcons) as ImageVector }

    private fun VectorGroup.pathNodeCount(): Int = sumOf { node ->
        when (node) {
            is VectorPath -> node.pathData.size
            is VectorGroup -> node.pathNodeCount()
        }
    }

    @Test
    fun everyIconBuildsAndDrawsSomething() {
        assertTrue(allIcons.size >= 40, "esperava todos os ícones do app, achei ${allIcons.size}")
        allIcons.forEach { (name, icon) ->
            assertTrue(icon.root.pathNodeCount() > 0, "$name não desenha nada")
        }
    }

    @Test
    fun everyIconUsesTheSameGridAndSize() {
        // Um ícone fora da grade 960 sairia em outra escala ao lado dos outros.
        allIcons.forEach { (name, icon) ->
            assertTrue(icon.viewportWidth == 960f && icon.viewportHeight == 960f, "$name fora da grade 960")
            assertTrue(icon.defaultWidth.value == 24f && icon.defaultHeight.value == 24f, "$name fora de 24 dp")
        }
    }
}
