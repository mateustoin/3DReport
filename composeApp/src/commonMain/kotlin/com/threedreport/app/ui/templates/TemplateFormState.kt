package com.threedreport.app.ui.templates

import com.threedreport.core.model.QuoteTemplate

/**
 * Rascunho do formulário de template. `id == null` significa que é um
 * template novo (ainda não salvo); caso contrário, é uma edição.
 */
data class TemplateFormState(
    val id: String? = null,
    val name: String = "",
    val watermarkText: String = "",
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val errorMessage: String? = null,
)

internal fun QuoteTemplate.toFormState() = TemplateFormState(
    id = id,
    name = name,
    watermarkText = watermarkText.orEmpty(),
    showWatermark = showWatermark,
    showFooter = showFooter,
)
