package com.threedreport.core.model

import kotlinx.serialization.Serializable

/** Preferência de tema do app. O nome que a pessoa lê vem dos recursos traduzíveis do app. */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}
