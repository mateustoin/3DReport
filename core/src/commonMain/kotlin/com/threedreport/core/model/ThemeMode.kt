package com.threedreport.core.model

import kotlinx.serialization.Serializable

/** Preferência de tema do app. */
@Serializable
enum class ThemeMode(val label: String) {
    LIGHT("Claro"),
    DARK("Escuro"),
    SYSTEM("Sistema"),
}
