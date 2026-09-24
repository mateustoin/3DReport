package com.threedreport.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class NoticesRepository actual constructor() {
    private val file = File(appDataDir(), "notices.json")
    private val state = MutableStateFlow(readJsonFile(file, emptySet<String>()))

    actual val seen: StateFlow<Set<String>> = state.asStateFlow()

    actual fun markSeen(id: String) {
        if (id in state.value) return
        state.value = state.value + id
        writeJsonFile(file, state.value)
    }
}
