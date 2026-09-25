package com.threedreport.app.data

import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de canais de venda (Shopee, Mercado Livre, cartão, Pix), editável
 * em Configurações e escolhido por orçamento na tela de Orçamento.
 *
 * Começa vazio pra quem nunca configurou taxa nenhuma: a venda direta (sem
 * taxa) é a opção padrão da tela de Orçamento e não precisa de cadastro.
 */
expect class SalesChannelRepository() {
    val channels: StateFlow<List<SalesChannel>>
    fun add(channel: SalesChannel)
    fun update(channel: SalesChannel)
    fun delete(id: String)
}
