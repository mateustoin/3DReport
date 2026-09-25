package com.threedreport.app.data

import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class SalesChannelRepository actual constructor() {
    private val file = File(appDataDir(), "channels.json")
    private val state = MutableStateFlow(readJsonFile(file, emptyList<SalesChannel>()))

    actual val channels: StateFlow<List<SalesChannel>> = state.asStateFlow()

    actual fun add(channel: SalesChannel) {
        state.value = state.value + channel
        persist()
    }

    actual fun update(channel: SalesChannel) {
        state.value = state.value.map { if (it.id == channel.id) channel else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}

