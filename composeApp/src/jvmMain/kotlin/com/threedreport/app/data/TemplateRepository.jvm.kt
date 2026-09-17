package com.threedreport.app.data

import com.threedreport.core.model.QuoteTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class TemplateRepository actual constructor() {
    private val file = File(appDataDir(), "templates.json")
    private val state = MutableStateFlow(readJsonFile(file, emptyList<QuoteTemplate>()))

    actual val templates: StateFlow<List<QuoteTemplate>> = state.asStateFlow()

    actual fun add(template: QuoteTemplate) {
        state.value = state.value + template
        persist()
    }

    actual fun update(template: QuoteTemplate) {
        state.value = state.value.map { if (it.id == template.id) template else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}
