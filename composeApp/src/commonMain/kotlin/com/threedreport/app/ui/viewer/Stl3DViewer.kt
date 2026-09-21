package com.threedreport.app.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.threedreport.core.stl.StlMesh
import com.threedreport.core.stl.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizador 3D de uma [StlMesh] — rasterizador simples desenhado no próprio `Canvas` do
 * Compose (decisão 61): sombreamento plano por triângulo (normal · direção da câmera), ordenação
 * pintor pra profundidade, câmera orbital. Sem OpenGL nem WebView — ver decisão 61 pro raciocínio.
 *
 * Controles: arrastar com o mouse orbita a câmera; a roda do mouse dá zoom. Pan ainda não
 * implementado (fica pra uma iteração seguinte, ver roadmap).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Stl3DViewer(mesh: StlMesh, modifier: Modifier = Modifier) {
    var yawDegrees by remember(mesh) { mutableStateOf(35f) }
    var pitchDegrees by remember(mesh) { mutableStateOf(20f) }
    var zoom by remember(mesh) { mutableStateOf(1f) }

    val baseColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .pointerInput(mesh) {
                detectDragGestures { _, dragAmount ->
                    yawDegrees -= dragAmount.x * 0.4f
                    pitchDegrees = (pitchDegrees + dragAmount.y * 0.4f).coerceIn(-89f, 89f)
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                zoom = (zoom * (1f - scrollDelta * 0.1f)).coerceIn(0.2f, 6f)
            },
    ) {
        drawStlMesh(mesh, yawDegrees, pitchDegrees, zoom, baseColor)
    }
}

private class Renderable(val path: Path, val color: Color, val depth: Float)

private fun DrawScope.drawStlMesh(mesh: StlMesh, yawDegrees: Float, pitchDegrees: Float, zoom: Float, baseColor: Color) {
    if (mesh.triangles.isEmpty()) return

    val center = mesh.center
    val radius = mesh.boundingRadius.takeIf { it > 0f } ?: 1f
    val distance = (radius * 2.5f / zoom).coerceAtLeast(radius * 0.6f)

    val yaw = yawDegrees * (PI.toFloat() / 180f)
    val pitch = pitchDegrees * (PI.toFloat() / 180f)
    // Z como "pra cima" (convenção de impressão 3D: a peça fica em pé como na mesa de impressão) —
    // por isso pitch (elevação) entra na componente Z, não Y.
    val cameraPos = center + Vec3(
        distance * cos(pitch) * sin(yaw),
        distance * cos(pitch) * cos(yaw),
        distance * sin(pitch),
    )

    val worldUp = Vec3(0f, 0f, 1f)
    val forward = (center - cameraPos).normalized()
    var right = (forward cross worldUp).normalized()
    if (right.length < 0.001f) right = Vec3(1f, 0f, 0f)
    val camUp = right cross forward

    val focalLength = size.minDimension * 1.4f
    val screenCenter = Offset(size.width / 2f, size.height / 2f)

    fun project(v: Vec3): Pair<Offset, Float>? {
        val relative = v - cameraPos
        val viewX = relative dot right
        val viewY = relative dot camUp
        val viewZ = relative dot forward
        if (viewZ <= 0.01f) return null
        val screenX = screenCenter.x + (viewX / viewZ) * focalLength
        val screenY = screenCenter.y - (viewY / viewZ) * focalLength
        return Offset(screenX, screenY) to viewZ
    }

    fun buildRenderables(cullBackfaces: Boolean): List<Renderable> {
        val renderables = ArrayList<Renderable>(mesh.triangles.size)
        for (triangle in mesh.triangles) {
            val facing = triangle.computedNormal dot (cameraPos - triangle.centroid).normalized()
            if (cullBackfaces && facing <= 0f) continue

            val p1 = project(triangle.v1) ?: continue
            val p2 = project(triangle.v2) ?: continue
            val p3 = project(triangle.v3) ?: continue

            val intensity = (0.25f + 0.75f * abs(facing)).coerceIn(0f, 1f)
            val color = Color(red = baseColor.red * intensity, green = baseColor.green * intensity, blue = baseColor.blue * intensity)
            val path = Path().apply {
                moveTo(p1.first.x, p1.first.y)
                lineTo(p2.first.x, p2.first.y)
                lineTo(p3.first.x, p3.first.y)
                close()
            }
            renderables += Renderable(path, color, (p1.second + p2.second + p3.second) / 3f)
        }
        return renderables
    }

    // Malha sólida bem-formada: culling de face traseira economiza metade do trabalho. Se um STL
    // tiver as normais invertidas (exportador com bug), o culling esconderia a peça inteira —
    // detectado aqui (lista vazia com malha não-vazia) e refeito sem cull, pra nunca ficar em branco.
    val renderables = buildRenderables(cullBackfaces = true).ifEmpty { buildRenderables(cullBackfaces = false) }

    renderables.sortedByDescending { it.depth }.forEach { drawPath(it.path, color = it.color) }
}
