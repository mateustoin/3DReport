package com.threedreport.app.ui.filaments

import com.threedreport.app.ui.components.suggestionsFor
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor

/** Rascunho de uma variante de cor dentro do formulário de filamento, ver [FilamentColor]. */
data class FilamentColorFormState(
    val id: String,
    val name: String = "",
    val hex: String? = null,
    val inStock: Boolean = true,
)

/**
 * Rascunho do formulário de filamento. `id == null` significa que é um filamento novo (ainda não salvo); caso
 * contrário, é uma edição. Cada erro fica no campo a que se refere.
 */
data class FilamentFormState(
    val id: String? = null,
    val name: String = "",
    val pricePerKgText: String = "",
    val densityGPerCm3Text: String = "",
    val diameterMmText: String = Filament.DEFAULT_DIAMETER_MM.toInputText(),
    val brand: String = "",
    val materialType: String = "",
    /** `true` quando o tipo veio do campo livre ("Outro tipo"), não de um dos [FILAMENT_MATERIAL_TYPE_PRESETS]. */
    val isCustomMaterialType: Boolean = false,
    val colors: List<FilamentColorFormState> = emptyList(),
    val nameError: UiText? = null,
    val priceError: UiText? = null,
    val densityError: UiText? = null,
    val diameterError: UiText? = null,
) {
    /** A opção marcada na lista de tipo de material; `null` quando ainda não há tipo. */
    val materialOption: MaterialOption?
        get() = when {
            isCustomMaterialType -> MaterialOption.Custom
            else -> FILAMENT_MATERIAL_TYPE_PRESETS.find { it.label == materialType }?.let { MaterialOption.Preset(it) }
        }

    /** O tipo escolhido não tem densidade padrão (compósitos, madeira): a pessoa precisa olhar a ficha do rolo. */
    val densityNeedsCheck: Boolean
        get() = FILAMENT_MATERIAL_TYPE_PRESETS.any { it.label == materialType && it.defaultDensityGPerCm3 == null }

    /** Marcas conhecidas que combinam com o que foi digitado em [brand] (todas, com o campo vazio). */
    val brandSuggestions: List<String>
        get() = (FILAMENT_BRAND_PRESETS_INTERNATIONAL + FILAMENT_BRAND_PRESETS_BRAZIL).suggestionsFor(brand)
}

internal fun Filament.toFormState() = FilamentFormState(
    id = id,
    name = name,
    pricePerKgText = pricePerKg.toInputText(),
    densityGPerCm3Text = densityGPerCm3.toInputText(),
    diameterMmText = diameterMm.toInputText(),
    brand = brand.orEmpty(),
    materialType = materialType.orEmpty(),
    isCustomMaterialType = materialType != null && FILAMENT_MATERIAL_TYPE_PRESETS.none { it.label == materialType },
    colors = colors.map { FilamentColorFormState(id = it.id, name = it.name.orEmpty(), hex = it.hex, inStock = it.inStock) },
)
