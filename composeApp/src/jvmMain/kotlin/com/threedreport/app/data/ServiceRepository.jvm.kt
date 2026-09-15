package com.threedreport.app.data

import com.threedreport.core.model.Service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class ServiceRepository actual constructor() {
    private val file = File(appDataDir(), "services.json")
    private val state = MutableStateFlow(readJsonFile(file, emptyList<Service>()))

    actual val services: StateFlow<List<Service>> = state.asStateFlow()

    actual fun add(service: Service) {
        state.value = state.value + service
        persist()
    }

    actual fun update(service: Service) {
        state.value = state.value.map { if (it.id == service.id) service else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}
