package com.threedreport.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Qual item do estoque (decisão 158): uma cor de um filamento, controlada em gramas, ou um insumo, controlado em
 * unidades (decisão 162). A unidade de controle é a cor, e não o carretel: dois carretéis da mesma cor somam no mesmo
 * saldo. Igualdade por valor, pra servir de chave.
 *
 * No arquivo vai com o tipo (`"type": "color"` ou `"consumable"`). O id da cor só é único dentro do filamento (a cor
 * sem nome de todo filamento é `"default"`), então a cor é sempre identificada pelo par.
 */
@Serializable
sealed interface StockItemRef {
    /** Em que unidade o saldo é contado. */
    val unit: StockUnit

    /** A cor [colorId] do filamento [filamentId] ([FilamentColor.id] dentro de [Filament.colors]). */
    @Serializable
    @SerialName("color")
    data class ColorRef(val filamentId: String, val colorId: String) : StockItemRef {
        override val unit: StockUnit
            get() = StockUnit.GRAMS
    }

    /** O insumo [consumableId] ([Consumable.id]). */
    @Serializable
    @SerialName("consumable")
    data class ConsumableRef(val consumableId: String) : StockItemRef {
        override val unit: StockUnit
            get() = StockUnit.UNITS
    }
}

/** Unidade do saldo: gramas pra cor de filamento, unidades (decimais, pra quem controla metro ou ml) pro insumo. */
enum class StockUnit { GRAMS, UNITS }

/**
 * O controle de saldo ligado num item (decisão 158). Existir é estar controlado: sem ele, o item continua só com o
 * "tem / acabou" manual. O saldo não fica aqui: é a soma dos [StockMovement] do item.
 *
 * @property trackedSinceEpochMillis quando o controle foi ligado. Pedido que já tinha ficado pronto antes disso não
 *   desconta, porque o saldo informado no início já está "depois" dele ([com.threedreport.core.report.StockLedger]).
 * @property lowThreshold "Avisar abaixo de" (decisão 159), em gramas ou unidades; `null` = sem limite.
 * @property emptySpoolGrams peso do carretel vazio usado na última vez em "Pesei o carretel" (só cor de filamento).
 */
@Serializable
data class StockItem(
    val id: String,
    val ref: StockItemRef,
    val trackedSinceEpochMillis: Long,
    val lowThreshold: Double? = null,
    val emptySpoolGrams: Double? = null,
)

/** Tipo de um [StockMovement]. */
@Serializable
enum class StockMovementKind {
    /** O saldo informado ao ligar o controle. */
    @SerialName("start")
    START,

    /** Uma linha de compra de item controlado (decisão 160). Some com a compra. */
    @SerialName("purchase")
    PURCHASE,

    /** O consumo de um pedido que ficou pronto (item 4 da Leva 12). Some se o pedido volta. */
    @SerialName("order")
    ORDER,

    /** Consumo avulso: falha, teste, uso próprio. */
    @SerialName("manual_use")
    MANUAL_USE,

    /** "Pesei o carretel" ou "Corrigir saldo": a diferença até o saldo novo informado. */
    @SerialName("adjustment")
    ADJUSTMENT,
}

/** Motivo de um consumo avulso ([StockMovementKind.MANUAL_USE]). */
@Serializable
enum class ManualUseReason {
    @SerialName("failure")
    FAILURE,

    @SerialName("test")
    TEST,

    @SerialName("own_use")
    OWN_USE,

    @SerialName("other")
    OTHER,
}

/**
 * Um fato gravado no saldo de um item (decisão 158). O saldo é a soma de [amount] de todos os movimentos do item: a
 * baixa de um pedido não é refeita a partir dos pedidos, então não depende de o pedido continuar existindo.
 *
 * @property epochMillis quando o movimento foi gravado.
 * @property amount quantidade com sinal, em gramas ou unidades ([StockItemRef.unit]): compra e início somam, pedido e
 *   consumo avulso subtraem, ajuste vai pra onde precisar.
 * @property quoteId o pedido de um [StockMovementKind.ORDER].
 * @property orderNumber o número do pedido ([SavedQuote.number]) no momento da baixa, pra mostrar "Pedido #0042" mesmo
 *   depois de o pedido ser excluído.
 * @property purchaseId a compra de um [StockMovementKind.PURCHASE].
 * @property reason o motivo de um [StockMovementKind.MANUAL_USE].
 * @property note observação livre.
 */
@Serializable
data class StockMovement(
    val id: String,
    val ref: StockItemRef,
    val epochMillis: Long,
    val amount: Double,
    val kind: StockMovementKind,
    val quoteId: String? = null,
    val orderNumber: Int? = null,
    val purchaseId: String? = null,
    val reason: ManualUseReason? = null,
    val note: String? = null,
) {
    /**
     * Se dá pra excluir à mão (consumo avulso e ajuste). Movimento de pedido some pela regra do andamento, o de compra
     * com a compra, e o início só parando de controlar.
     */
    val isRemovableByHand: Boolean
        get() = kind == StockMovementKind.MANUAL_USE || kind == StockMovementKind.ADJUSTMENT
}
