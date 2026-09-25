package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Como a pessoa usa o app (decisão 103), perguntado no onboarding e mudado em Configurações. Só
 * define **padrões**: o que já vem escolhido ao salvar e o que o Histórico mostra primeiro. Nada
 * fica escondido por perfil.
 *
 * @property label como a opção aparece na tela.
 * @property description a linha que explica a opção.
 */
@Serializable
enum class UsageProfile(val label: String, val description: String) {
    SELLER("Já vendo sob encomenda", "O cliente pede, você orça e acompanha até a entrega."),
    STARTER("Estou começando", "Quero saber quanto cobrar e montar um catálogo das peças que sei fazer."),
    ;

    /** O que vem escolhido ao salvar um orçamento novo e na primeira lista do Histórico. */
    val defaultKind: QuoteKind
        get() = when (this) {
            SELLER -> QuoteKind.ORDER
            STARTER -> QuoteKind.PRODUCT
        }
}
