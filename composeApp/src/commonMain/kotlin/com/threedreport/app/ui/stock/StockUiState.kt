package com.threedreport.app.ui.stock

import com.threedreport.app.ui.text.UiText
import com.threedreport.core.model.ManualUseReason
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.model.StockMovementKind
import com.threedreport.core.report.ShoppingLine
import com.threedreport.core.report.StockStatus

/** As três vistas da tela Estoque (decisão 163). */
enum class StockView { BALANCE, PURCHASES, SHOPPING }

/**
 * Um item do estoque com os nomes que a tela escreve: a cor de um filamento ([filamentName] e [name], que é a cor) ou um
 * insumo ([name]). [name] vazio numa cor é a cor sem nome nem hex; [removed] é um item que já saiu do cadastro (uma compra
 * antiga ainda cita ele).
 */
data class StockEntry(
    val ref: StockItemRef,
    val filamentName: String? = null,
    val name: String? = null,
    val colorHex: String? = null,
    val removed: Boolean = false,
) {
    /** "PLA Voolt Vermelho" ou "Argola de chaveiro": o nome inteiro, pra aviso, lista copiada e seletor. */
    val displayName: String
        get() = listOfNotNull(filamentName, name).joinToString(" ")
}

/** Um item controlado, com os números do momento. */
data class StockRowState(val entry: StockEntry, val status: StockStatus) {
    val ref: StockItemRef get() = entry.ref
}

/** As cores controladas de um filamento, juntas sob o nome dele. */
data class StockFilamentGroup(val filamentName: String, val rows: List<StockRowState>)

/** Uma linha da compra no resumo da lista de compras registradas. */
data class PurchaseSummaryLine(val entry: StockEntry, val amount: Double)

data class PurchaseRowState(
    val id: String,
    val dateEpochDay: Long,
    val supplier: String?,
    val lines: List<PurchaseSummaryLine>,
    val total: Double,
    val selected: Boolean,
)

/**
 * Uma linha da lista de compras. [amount] é o que comprar, já arredondado pra cima (gramas inteiras, unidades com duas casas): é
 * o que a linha mostra, o que a lista copiada diz e o que o formulário de compra recebe.
 */
data class ShoppingRowState(val entry: StockEntry, val line: ShoppingLine, val amount: Double)

/** Cor sem controle marcada "Acabou" que [orderCount] pedidos aprovados ou em impressão usam. */
data class OutOfStockRowState(val entry: StockEntry, val orderCount: Int)

/** @property hasTracked há algum item controlado; sem nenhum, a lista vazia explica como ligar o controle. */
data class ShoppingState(
    val lines: List<ShoppingRowState> = emptyList(),
    val outOfStock: List<OutOfStockRowState> = emptyList(),
    val hasTracked: Boolean = false,
) {
    val size: Int get() = lines.size + outOfStock.size
    val isEmpty: Boolean get() = size == 0
}

/** Um movimento no histórico do painel do item. */
data class MovementRowState(
    val id: String,
    val epochMillis: Long,
    val kind: StockMovementKind,
    val amount: Double,
    /** "#0042" do pedido de um movimento de pedido. */
    val orderLabel: String?,
    val reason: ManualUseReason?,
    val note: String?,
    /** Consumo avulso e ajuste: dá pra excluir à mão. */
    val removable: Boolean,
)

/** O painel de um item controlado: os números, o histórico e os dois campos que se salvam ("Avisar abaixo de", carretel vazio). */
data class StockItemPanel(
    val entry: StockEntry,
    val status: StockStatus,
    /** "Média das compras" na unidade do cadastro (R$/kg, R$/un); `null` sem compra. Só informação. */
    val averageCost: Double?,
    /** O link da última compra que tinha um, pro "Comprar de novo". */
    val buyAgainLink: String?,
    val movements: List<MovementRowState>,
    val thresholdText: String,
    val spoolText: String,
    val thresholdError: UiText? = null,
    val spoolError: UiText? = null,
) {
    val isFilament: Boolean get() = entry.ref is StockItemRef.ColorRef
}

/** O rascunho de uma linha do formulário de compra. */
data class PurchaseLineForm(
    val id: Int,
    val ref: StockItemRef? = null,
    val amountText: String = "",
    val priceText: String = "",
    val refError: UiText? = null,
    val amountError: UiText? = null,
    val priceError: UiText? = null,
)

/**
 * O rascunho do formulário de compra. `id == null` é compra nova. As caixas "Atualizar o preço" nascem marcadas numa compra
 * nova e desmarcadas ao editar uma que já existe (reaplicar um preço antigo sem querer é pior que um clique a mais);
 * [toggledUpdates] guarda as que a pessoa virou.
 */
data class PurchaseFormState(
    val id: String? = null,
    val dateEpochDay: Long = 0L,
    val supplier: String = "",
    val link: String = "",
    val shippingText: String = "",
    val note: String = "",
    val lines: List<PurchaseLineForm> = emptyList(),
    val updatesMarkedByDefault: Boolean = true,
    val toggledUpdates: Set<String> = emptySet(),
    val shippingError: UiText? = null,
    val linesError: UiText? = null,
    val nextLineId: Int = 1,
)

/** Uma linha do formulário com o que se calcula dela. */
data class PurchaseLineView(
    val lineId: Int,
    val entry: StockEntry?,
    /** O item tem controle de saldo: a compra soma gramas ou unidades. Sem controle, o formulário avisa que só registra. */
    val tracked: Boolean,
    /** Custo por kg (cor) ou por unidade (insumo), com o frete rateado; `null` enquanto a linha está incompleta. */
    val catalogUnitCost: Double?,
)

/** "Atualizar o preço de PLA Voolt de R$ 89,90/kg pra R$ 97,40/kg" (ou o custo de um insumo). */
data class PriceUpdateRow(
    val key: String,
    val name: String,
    val isFilament: Boolean,
    val currentValue: Double,
    val newValue: Double,
    val checked: Boolean,
)

/** O painel do formulário de compra, com o que se deriva do rascunho. */
data class PurchasePanel(
    val form: PurchaseFormState,
    val total: Double,
    val lines: List<PurchaseLineView>,
    val updates: List<PriceUpdateRow>,
    val suppliers: List<String>,
    /** Os itens que se pode comprar, na ordem da tela. */
    val choices: List<StockEntry>,
) {
    val isNew: Boolean get() = form.id == null
}

/** O que está aberto no painel lateral (um só por vez). */
sealed interface StockPanel {
    data class Item(val panel: StockItemPanel) : StockPanel

    data class Purchase(val panel: PurchasePanel) : StockPanel
}

enum class StockDialogKind { WEIGH, MANUAL_USE, ADJUST, TRACK, STOP_TRACKING, DELETE_MOVEMENT, DELETE_PURCHASE }

/**
 * O diálogo aberto. Um tipo só pros sete, porque cada um usa poucos campos: [amountText] é o número principal (peso na
 * balança, quanto saiu, saldo novo, saldo atual), [spoolText] só o de pesar.
 *
 * @property currentBalance o saldo de agora do item.
 * @property newBalance só ao pesar: o saldo que a balança dá ("Saldo novo: X g (era Y g)"), ou `null` com o peso ainda inválido.
 * @property targetId o movimento ou a compra de uma exclusão.
 */
data class StockDialog(
    val kind: StockDialogKind,
    val entry: StockEntry? = null,
    val targetId: String? = null,
    val amountText: String = "",
    val spoolText: String = "",
    val reason: ManualUseReason = ManualUseReason.FAILURE,
    val noteText: String = "",
    val error: UiText? = null,
    val currentBalance: Double = 0.0,
    val newBalance: Double? = null,
)

/**
 * Tudo o que a tela Estoque mostra.
 *
 * @property alertCount itens controlados baixos ou sem saldo: o aviso do alto e o selo da barra lateral.
 * @property untracked os itens sem controle de saldo, que a tela guarda numa seção recolhida.
 * @property today hoje, em dias desde 01/01/1970 (o seletor de data da compra).
 */
data class StockUiState(
    val view: StockView = StockView.BALANCE,
    val alertCount: Int = 0,
    val filamentGroups: List<StockFilamentGroup> = emptyList(),
    val consumables: List<StockRowState> = emptyList(),
    val untracked: List<StockEntry> = emptyList(),
    val purchases: List<PurchaseRowState> = emptyList(),
    val shopping: ShoppingState = ShoppingState(),
    val panel: StockPanel? = null,
    val dialog: StockDialog? = null,
    val today: Long = 0L,
) {
    /** Nenhum filamento nem insumo no cadastro: não há o que controlar. */
    val hasNoItems: Boolean
        get() = filamentGroups.isEmpty() && consumables.isEmpty() && untracked.isEmpty()
}
