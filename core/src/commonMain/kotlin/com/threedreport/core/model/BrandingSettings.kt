package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Personalização do documento exportado (PDF). Separado de [PricingSettings]
 * porque não é um parâmetro de custo — é sobre a apresentação do documento.
 *
 * @property watermarkText texto da marca d'água no PDF exportado, ou `null`/vazio para não mostrar nenhuma.
 * @property showWatermark exibe [watermarkText] como marca d'água diagonal no PDF.
 * @property showFooter exibe [watermarkText] no rodapé do PDF.
 * @property showPrintTime mostra o tempo de impressão do pedido no PDF e no texto de
 *   copiar/colar/WhatsApp. Desligado por padrão: é tempo de máquina, não custo nem margem (não
 *   fere a decisão 19), mas nem todo vendedor quer dar ao cliente o argumento "só 2 h de máquina?"
 *   pra barganhar.
 */
@Serializable
data class BrandingSettings(
    val watermarkText: String? = null,
    val showWatermark: Boolean = true,
    val showFooter: Boolean = true,
    val showPrintTime: Boolean = false,
)
