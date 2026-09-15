package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Personalização do documento exportado (PDF). Separado de [PricingSettings]
 * porque não é um parâmetro de custo — é sobre a apresentação do documento.
 *
 * @property watermarkText texto da marca d'água no PDF exportado, ou `null`/vazio para não mostrar nenhuma.
 */
@Serializable
data class BrandingSettings(
    val watermarkText: String? = null,
)
