package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.core.model.OrderSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Configurações de Pedidos da Leva 11 ([OrderSettings]): sinal padrão, dias pra orçamento parado e site de rastreio.
 * Dado do negócio (vai junto no backup e numa sincronização futura), em `order-settings.json`.
 */
interface OrderSettingsRepository : DocumentRepository<OrderSettings> {
    val settings: StateFlow<OrderSettings>
        get() = value
}

class StoredOrderSettingsRepository(document: DocumentValue<OrderSettings>) :
    StoredDocumentRepository<OrderSettings>(document), OrderSettingsRepository
