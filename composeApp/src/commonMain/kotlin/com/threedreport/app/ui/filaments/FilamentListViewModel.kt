package com.threedreport.app.ui.filaments

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_name_required
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import _3dreport.composeapp.generated.resources.common_error_positive
import _3dreport.composeapp.generated.resources.common_error_required
import _3dreport.composeapp.generated.resources.filaments_export_done
import _3dreport.composeapp.generated.resources.filaments_import_already
import _3dreport.composeapp.generated.resources.filaments_import_done
import _3dreport.composeapp.generated.resources.filaments_import_invalid
import _3dreport.composeapp.generated.resources.filaments_import_newer_version
import _3dreport.composeapp.generated.resources.filaments_import_none
import _3dreport.composeapp.generated.resources.filaments_import_not_this_format
import _3dreport.composeapp.generated.resources.filaments_import_result
import _3dreport.composeapp.generated.resources.filaments_notice_archived
import _3dreport.composeapp.generated.resources.filaments_notice_deleted
import _3dreport.composeapp.generated.resources.filaments_notice_restored
import _3dreport.composeapp.generated.resources.filaments_notice_saved
import com.threedreport.app.data.FilamentRepository
import com.threedreport.app.data.StockRepository
import com.threedreport.app.data.exportFilamentsJson
import com.threedreport.app.data.importFilaments
import com.threedreport.app.data.setArchived
import com.threedreport.app.data.updateKeepingArchived
import com.threedreport.app.platform.AppLocale
import com.threedreport.app.platform.FileKind
import com.threedreport.app.platform.PickResult
import com.threedreport.app.platform.PlatformServices
import com.threedreport.app.platform.defaultPlatform
import com.threedreport.app.platform.todayEpochDay
import com.threedreport.app.ui.components.CatalogUsage
import com.threedreport.app.ui.components.PendingDelete
import com.threedreport.app.ui.components.UiNotice
import com.threedreport.app.ui.components.toNotice
import com.threedreport.app.ui.format.NumberKind
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.text
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Filament
import com.threedreport.core.model.FilamentColor
import com.threedreport.core.model.FilamentExchange
import com.threedreport.core.model.FilamentImportError
import com.threedreport.core.model.FilamentImportPlan
import com.threedreport.core.model.SavedQuote
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tudo o que a tela de Filamentos mostra: a lista (ativos e arquivados), o formulário aberto (nulo quando nenhum) e a
 * pergunta de exclusão pendente (nula quando nenhuma).
 */
data class FilamentsUiState(
    val active: List<Filament> = emptyList(),
    val archived: List<Filament> = emptyList(),
    val form: FilamentFormState? = null,
    val pendingDelete: PendingDelete? = null,
    /** O saldo das cores com controle de saldo (Leva 12), pra o selo da cor mostrar "Vermelho · 640 g". */
    val colorStock: Map<StockItemRef, ColorStock> = emptyMap(),
) {
    /** Nenhum filamento cadastrado, nem arquivado: a tela mostra o estado vazio com o botão que cria o primeiro. */
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

/** O saldo de uma cor controlada: gramas e se está baixo ou acabando (o selo fica vermelho). */
data class ColorStock(val grams: Double, val needsAttention: Boolean, val isEmpty: Boolean = false)

/**
 * ViewModel da tela de Filamentos. Um [uiState] só, e os [notices] (avisos de uma frase, como "Filamento arquivado").
 * A lista acompanha o repositório dentro de [scope]; o resto muda na hora, pelos eventos.
 */
class FilamentListViewModel(
    private val repository: FilamentRepository,
    scope: CoroutineScope,
    /**
     * Pedidos e produtos salvos, inclusive os da lixeira, pra saber quem usa cada filamento antes de excluir
     * (decisão 115).
     */
    private val usageQuotes: () -> List<SavedQuote> = { emptyList() },
    /** A moeda em uso: os números são lidos e mostrados com o separador decimal dela, o mesmo que o campo usa pra validar. */
    private val currency: () -> Currency = { Currency.BRL },
    /** O estoque (Leva 12): com ele, o selo de cada cor controlada mostra o saldo. */
    private val stock: StockRepository? = null,
    private val platform: PlatformServices = defaultPlatform,
    /** Hoje, em dias desde 01/01/1970: o nome sugerido do arquivo exportado. */
    private val today: () -> Long = ::todayEpochDay,
) {
    private val separator: Char get() = currency().decimalSeparator

    private val state = MutableStateFlow(FilamentsUiState().withItems(repository.filaments.value))
    val uiState: StateFlow<FilamentsUiState> = state.asStateFlow()

    private val noticeFlow = MutableSharedFlow<UiText>(extraBufferCapacity = NOTICE_BUFFER)
    val notices: SharedFlow<UiText> = noticeFlow.asSharedFlow()

    private val fileNoticeFlow = MutableSharedFlow<UiNotice>(extraBufferCapacity = NOTICE_BUFFER)

    /** O resultado de exportar e de importar filamentos: com "Abrir pasta" ao gravar, e o erro que fica mais tempo na tela. */
    val fileNotices: SharedFlow<UiNotice> = fileNoticeFlow.asSharedFlow()

    init {
        repository.filaments.onEach { items -> state.update { it.withItems(items) } }.launchIn(scope)
        stock?.let { tracking ->
            state.update { it.copy(colorStock = colorStockOf(tracking)) }
            combine(tracking.items, tracking.movements) { _, _ -> }
                .onEach { state.update { it.copy(colorStock = colorStockOf(tracking)) } }
                .launchIn(scope)
        }
    }

    fun startAdd() {
        state.update {
            it.copy(
                form = FilamentFormState(
                    diameterMmText = Filament.DEFAULT_DIAMETER_MM.toInputText(AppLocale.decimalSeparator),
                    colors = listOf(FilamentColorFormState(id = newColorId())),
                ),
            )
        }
    }

    fun startEdit(id: String) {
        val filament = repository.filaments.value.find { it.id == id } ?: return
        state.update { it.copy(form = filament.toFormState(separator)) }
    }

    fun cancelEdit() {
        state.update { it.copy(form = null) }
    }

    fun updateForm(transform: (FilamentFormState) -> FilamentFormState) {
        state.update { s ->
            s.copy(
                form = s.form?.let {
                    transform(it).copy(nameError = null, priceError = null, densityError = null, diameterError = null)
                },
            )
        }
    }

    /**
     * Escolhe o tipo de material na lista. Com [preset], o tipo entra e a densidade sugerida preenche o campo (quando
     * o tipo tem uma); sem ele ([MaterialOption.Custom]), o campo do tipo fica livre e vazio.
     */
    fun chooseMaterialType(preset: MaterialTypePreset?) {
        updateForm { form ->
            if (preset == null) {
                form.copy(materialType = "", isCustomMaterialType = true)
            } else {
                form.copy(
                    materialType = preset.label,
                    isCustomMaterialType = false,
                    densityGPerCm3Text = preset.defaultDensityGPerCm3?.toInputText(AppLocale.decimalSeparator) ?: form.densityGPerCm3Text,
                )
            }
        }
    }

    fun addColorRow() {
        updateForm { it.copy(colors = it.colors + FilamentColorFormState(id = newColorId())) }
    }

    /** Não faz nada se for a última cor restante: o filamento sempre precisa de ao menos uma. */
    fun removeColorRow(colorId: String) {
        updateForm { form -> if (form.colors.size <= 1) form else form.copy(colors = form.colors.filterNot { it.id == colorId }) }
    }

    fun updateColorRow(colorId: String, transform: (FilamentColorFormState) -> FilamentColorFormState) {
        updateForm { form -> form.copy(colors = form.colors.map { if (it.id == colorId) transform(it) else it }) }
    }

    /** Escolhe uma cor pronta: o hex entra e o [name] vira o nome da cor se ela ainda não tem um. */
    fun applyColorPreset(colorId: String, name: String, hex: String) {
        updateColorRow(colorId) { it.copy(hex = hex, name = it.name.ifBlank { name }) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save() {
        val current = state.value.form ?: return
        val name = current.name.trim()
        val separator = separator
        val price = parseDecimal(current.pricePerKgText.trim(), NumberKind.AMOUNT, separator)
        val density = parseDecimal(current.densityGPerCm3Text.trim(), NumberKind.MEASURE, AppLocale.decimalSeparator)
        val diameter = parseDecimal(current.diameterMmText.trim(), NumberKind.MEASURE, AppLocale.decimalSeparator)

        val nameError = uiText(Res.string.common_error_name_required).takeIf { name.isEmpty() }
        val priceError = when {
            current.pricePerKgText.isBlank() -> uiText(Res.string.common_error_required)
            price == null -> uiText(Res.string.common_error_number_invalid)
            price < 0 -> uiText(Res.string.common_error_negative)
            else -> null
        }
        val densityError = positiveError(current.densityGPerCm3Text, density)
        val diameterError = positiveError(current.diameterMmText, diameter)
        if (nameError != null || priceError != null || densityError != null || diameterError != null || price == null || density == null || diameter == null) {
            state.update {
                it.copy(form = current.copy(nameError = nameError, priceError = priceError, densityError = densityError, diameterError = diameterError))
            }
            return
        }

        val filament = Filament(
            id = current.id ?: Uuid.random().toString(),
            name = name,
            pricePerKg = price,
            densityGPerCm3 = density,
            diameterMm = diameter,
            brand = current.brand.trim().ifEmpty { null },
            materialType = current.materialType.trim().ifEmpty { null },
            colors = current.colors.map {
                FilamentColor(id = it.id, name = it.name.trim().ifEmpty { null }, hex = it.hex, inStock = it.inStock)
            },
        )
        if (current.id == null) repository.add(filament) else repository.updateKeepingArchived(filament)
        state.update { it.copy(form = null) }
        noticeFlow.tryEmit(uiText(Res.string.filaments_notice_saved))
    }

    private fun positiveError(text: String, value: Double?): UiText? = when {
        text.isBlank() -> uiText(Res.string.common_error_required)
        value == null -> uiText(Res.string.common_error_number_invalid)
        value <= 0 -> uiText(Res.string.common_error_positive)
        else -> null
    }

    /** Pergunta se pode excluir o filamento [id], com quantos pedidos e produtos o usam (decisão 115). */
    fun requestDelete(id: String) {
        val filament = repository.filaments.value.find { it.id == id } ?: return
        state.update { it.copy(pendingDelete = PendingDelete(id, filament.name, usageCount(id), canArchive = !filament.archived)) }
    }

    fun dismissDelete() {
        state.update { it.copy(pendingDelete = null) }
    }

    /** Quantos pedidos e produtos usam o filamento [id]. */
    fun usageCount(id: String): Int = CatalogUsage.filament(id, usageQuotes())

    /** Arquiva (ou restaura) o filamento [id] (decisão 115). */
    fun setArchived(id: String, archived: Boolean) {
        repository.setArchived(id, archived)
        state.update { it.copy(pendingDelete = null) }
        noticeFlow.tryEmit(uiText(if (archived) Res.string.filaments_notice_archived else Res.string.filaments_notice_restored))
    }

    fun delete(id: String) {
        repository.delete(id)
        state.update { s -> s.copy(form = s.form?.takeUnless { it.id == id }, pendingDelete = null) }
        noticeFlow.tryEmit(uiText(Res.string.filaments_notice_deleted))
    }

    /** Alterna manualmente "Em estoque"/"Acabou" de uma cor específica, sem tentar calcular automaticamente. */
    fun toggleColorInStock(filamentId: String, colorId: String) {
        val filament = repository.filaments.value.find { it.id == filamentId } ?: return
        val updatedColors = filament.colors.map { if (it.id == colorId) it.copy(inStock = !it.inStock) else it }
        repository.update(filament.copy(colors = updatedColors))
        // Com esse filamento aberto no painel, o rascunho acompanha: senão o Salvar desfaria a marcação.
        val newInStock = updatedColors.find { it.id == colorId }?.inStock ?: return
        state.update { s ->
            val form = s.form?.takeIf { it.id == filamentId } ?: return@update s
            s.copy(form = form.copy(colors = form.colors.map { if (it.id == colorId) it.copy(inStock = newInStock) else it }))
        }
    }

    /** O saldo de cada cor controlada, com a situação (baixo ou acabando) que o `core` calcula. */
    private fun colorStockOf(tracking: StockRepository): Map<StockItemRef, ColorStock> {
        val balances = StockReport.balances(tracking.movements.value)
        return tracking.items.value.filter { it.ref is StockItemRef.ColorRef }.associate { item ->
            val balance = balances[item.ref] ?: 0.0
            item.ref to StockReport.state(item, balance).let { ColorStock(balance, it != StockState.OK, it == StockState.OUT) }
        }
    }

    /**
     * "Exportar filamentos" (decisão 164): grava os filamentos não arquivados num `.json`, sem saldo, compras nem ids. O
     * seletor de arquivo é o nativo do sistema; cancelar não avisa nada.
     */
    fun exportFilaments() {
        val text = repository.exportFilamentsJson()
        val result = platform.saveFile(text.encodeToByteArray(), FilamentExchange.suggestedFileName(today()), platform.documentsDirectory())
        result.toNotice(uiText(Res.string.filaments_export_done))?.let(fileNoticeFlow::tryEmit)
    }

    /**
     * "Importar filamentos": lê o arquivo escolhido e acrescenta os filamentos novos (cores "tem"), pulando os que já existem.
     * O aviso diz quantos entraram, quantos já estavam e quantos tinham dado inválido; arquivo de outro formato ou de versão
     * mais nova não importa nada.
     */
    fun importFilaments() {
        when (val picked = platform.pickFile(FileKind.FILAMENTS)) {
            PickResult.Cancelled -> Unit
            is PickResult.Failed -> fileNoticeFlow.tryEmit(UiNotice(picked.text(), isError = true))
            is PickResult.Deferred -> importPicked(picked.read())
            is PickResult.Picked -> importText(picked.file.bytes.decodeToString())
        }
    }

    private fun importPicked(picked: PickResult) {
        when (picked) {
            is PickResult.Picked -> importText(picked.file.bytes.decodeToString())
            is PickResult.Failed -> fileNoticeFlow.tryEmit(UiNotice(picked.text(), isError = true))
            else -> Unit
        }
    }

    private fun importText(text: String) {
        val notice = when (val plan = repository.importFilaments(text)) {
            is FilamentImportPlan.Rejected -> UiNotice(
                uiText(if (plan.error == FilamentImportError.NEWER_VERSION) Res.string.filaments_import_newer_version else Res.string.filaments_import_not_this_format),
                isError = true,
            )

            is FilamentImportPlan.Ready -> UiNotice(importSummary(plan), isError = false)
        }
        fileNoticeFlow.tryEmit(notice)
    }

    /** "5 filamentos importados, 2 já estavam no cadastro, 1 com dados inválidos." */
    private fun importSummary(plan: FilamentImportPlan.Ready): UiText {
        val parts = buildList {
            add(if (plan.toAdd.isEmpty()) uiText(Res.string.filaments_import_none) else UiText.Plural(Res.plurals.filaments_import_done, plan.toAdd.size, listOf(plan.toAdd.size)))
            if (plan.alreadyPresent > 0) add(UiText.Plural(Res.plurals.filaments_import_already, plan.alreadyPresent, listOf(plan.alreadyPresent)))
            if (plan.invalid > 0) add(UiText.Plural(Res.plurals.filaments_import_invalid, plan.invalid, listOf(plan.invalid)))
        }
        return uiText(Res.string.filaments_import_result, UiText.Joined(parts, separator = ", "))
    }

    private fun FilamentsUiState.withItems(items: List<Filament>): FilamentsUiState {
        val (archived, active) = items.partition { it.archived }
        return copy(active = active, archived = archived)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newColorId(): String = Uuid.random().toString()

    private companion object {
        const val NOTICE_BUFFER = 8
    }
}
