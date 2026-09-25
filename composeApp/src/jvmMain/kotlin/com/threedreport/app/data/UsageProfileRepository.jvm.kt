package com.threedreport.app.data

import com.threedreport.core.model.UsageProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class UsageProfileRepository actual constructor() {
    private val file = File(appDataDir(), "usage-profile.json")
    private val state = MutableStateFlow(readJsonFile(file, UsageProfile.SELLER))

    actual val profile: StateFlow<UsageProfile> = state.asStateFlow()

    actual fun update(profile: UsageProfile) {
        state.value = profile
        writeJsonFile(file, profile)
    }
}
