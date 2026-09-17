package com.threedreport.app.ui.filaments

import androidx.compose.ui.graphics.Color
import com.threedreport.core.model.FilamentColor

/** Paleta de cores comuns de filamento, pra escolher rápido no cadastro (nome de exibição a hex). */
val FILAMENT_COLOR_PRESETS: List<Pair<String, String>> = listOf(
    "Branco" to "#F5F5F5",
    "Preto" to "#1A1A1A",
    "Cinza" to "#9E9E9E",
    "Natural" to "#E4D9C3",
    "Vermelho" to "#E53935",
    "Laranja" to "#FB8C00",
    "Amarelo" to "#FDD835",
    "Verde" to "#43A047",
    "Azul" to "#1E88E5",
    "Roxo" to "#8E24AA",
    "Rosa" to "#EC407A",
    "Marrom" to "#6D4C41",
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

/** Texto de exibição de uma cor: nome escrito, ou o hex, ou um rótulo genérico se nenhum estiver preenchido. */
fun FilamentColor.displayLabel(): String = name?.takeIf { it.isNotBlank() } ?: hex ?: "Sem nome"
