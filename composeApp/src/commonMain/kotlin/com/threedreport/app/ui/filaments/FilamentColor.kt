package com.threedreport.app.ui.filaments

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.filaments_color_black
import _3dreport.composeapp.generated.resources.filaments_color_blue
import _3dreport.composeapp.generated.resources.filaments_color_brown
import _3dreport.composeapp.generated.resources.filaments_color_green
import _3dreport.composeapp.generated.resources.filaments_color_grey
import _3dreport.composeapp.generated.resources.filaments_color_natural
import _3dreport.composeapp.generated.resources.filaments_color_orange
import _3dreport.composeapp.generated.resources.filaments_color_pink
import _3dreport.composeapp.generated.resources.filaments_color_purple
import _3dreport.composeapp.generated.resources.filaments_color_red
import _3dreport.composeapp.generated.resources.filaments_color_white
import _3dreport.composeapp.generated.resources.filaments_color_yellow
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource

/** Uma cor pronta do cadastro: o nome (recurso, vira o nome da cor se ela ainda não tem um) e o hex. */
data class ColorPreset(val name: StringResource, val hex: String) {
    val color: Color get() = parseHexColor(hex) ?: Color.Unspecified
}

/** Paleta de cores comuns de filamento, pra escolher rápido no cadastro. */
val FILAMENT_COLOR_PRESETS: List<ColorPreset> = listOf(
    ColorPreset(Res.string.filaments_color_white, "#F5F5F5"),
    ColorPreset(Res.string.filaments_color_black, "#1A1A1A"),
    ColorPreset(Res.string.filaments_color_grey, "#9E9E9E"),
    ColorPreset(Res.string.filaments_color_natural, "#E4D9C3"),
    ColorPreset(Res.string.filaments_color_red, "#E53935"),
    ColorPreset(Res.string.filaments_color_orange, "#FB8C00"),
    ColorPreset(Res.string.filaments_color_yellow, "#FDD835"),
    ColorPreset(Res.string.filaments_color_green, "#43A047"),
    ColorPreset(Res.string.filaments_color_blue, "#1E88E5"),
    ColorPreset(Res.string.filaments_color_purple, "#8E24AA"),
    ColorPreset(Res.string.filaments_color_pink, "#EC407A"),
    ColorPreset(Res.string.filaments_color_brown, "#6D4C41"),
)

/** Converte um hex (`#RRGGBB` ou `#AARRGGBB`) pra [Color]; `null` se [hex] estiver vazio ou for inválido. */
fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return null
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> null
    }
}
