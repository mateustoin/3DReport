package com.threedreport.app.data

import com.threedreport.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class ThemeRepository actual constructor() {
    private val file = File(appDataDir(), "theme.json")
    private val state = MutableStateFlow(readJsonFile(file, ThemeMode.SYSTEM))

    actual val mode: StateFlow<ThemeMode> = state.asStateFlow()

    actual fun update(mode: ThemeMode) {
        state.value = mode
        writeJsonFile(file, mode)
    }
}
