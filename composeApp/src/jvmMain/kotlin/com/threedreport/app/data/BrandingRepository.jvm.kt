package com.threedreport.app.data

import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class BrandingRepository actual constructor() {
    private val file = File(appDataDir(), "branding.json")
    private val state = MutableStateFlow(readJsonFile(file, BrandingSettings()))

    actual val branding: StateFlow<BrandingSettings> = state.asStateFlow()

    actual fun update(branding: BrandingSettings) {
        state.value = branding
        writeJsonFile(file, branding)
    }
}
