package com.threedreport.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class OnboardingRepository actual constructor() {
    private val file = File(appDataDir(), "onboarding.json")
    private val state = MutableStateFlow(readJsonFile(file, false))

    actual val completed: StateFlow<Boolean> = state.asStateFlow()

    actual fun markCompleted() {
        state.value = true
        writeJsonFile(file, true)
    }
}
