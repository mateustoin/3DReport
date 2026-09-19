package com.threedreport.app.ui.filaments

/**
 * Marcas de filamento conhecidas, sugeridas no cadastro (decisão 54) — mera
 * ajuda pra não digitar do zero, não uma lista fechada: `Filament.brand`
 * continua texto livre, uma marca fora daqui é digitada normalmente.
 */
val FILAMENT_BRAND_PRESETS_INTERNATIONAL: List<String> = listOf(
    "eSUN", "Polymaker", "Prusament", "Overture", "SUNLU", "Hatchbox",
    "ColorFabb", "Bambu Lab", "Fillamentum", "Elegoo", "Inland",
)

/** Marcas vendidas no mercado brasileiro (decisão 54) — mesmo espírito da lista internacional acima. */
val FILAMENT_BRAND_PRESETS_BRAZIL: List<String> = listOf(
    "Voolt3D", "GTMax3D", "3D Lab", "Cliever", "3D Fila",
)

/**
 * Tipo de material com densidade padrão sugerida — a pessoa escolhe um tipo
 * e a densidade do formulário já vem preenchida, mas continua editável
 * (decisão 54). `defaultDensityGPerCm3 == null` quando a variação real entre
 * fabricantes/percentual de carga é grande demais pra um padrão confiável
 * (compósitos com fibra de carbono, madeira) — nesses casos o campo de
 * densidade fica como está, pra pessoa preencher olhando a ficha técnica do
 * rolo específico que tem em mãos.
 */
data class MaterialTypePreset(val label: String, val defaultDensityGPerCm3: Double?)

val FILAMENT_MATERIAL_TYPE_PRESETS: List<MaterialTypePreset> = listOf(
    MaterialTypePreset("PLA", 1.24),
    MaterialTypePreset("PETG", 1.27),
    MaterialTypePreset("ABS", 1.04),
    MaterialTypePreset("ASA", 1.07),
    MaterialTypePreset("TPU", 1.21),
    MaterialTypePreset("Nylon (PA)", 1.14),
    MaterialTypePreset("PC", 1.20),
    MaterialTypePreset("HIPS", 1.04),
    MaterialTypePreset("PVA", 1.23),
    MaterialTypePreset("PLA-CF", null),
    MaterialTypePreset("PETG-CF", null),
    MaterialTypePreset("Madeira (wood-fill)", null),
)

/** Rótulo da opção de digitar um tipo que não está em [FILAMENT_MATERIAL_TYPE_PRESETS]. */
const val CUSTOM_MATERIAL_TYPE_LABEL = "Personalizado"
