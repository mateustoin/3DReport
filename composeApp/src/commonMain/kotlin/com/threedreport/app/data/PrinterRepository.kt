package com.threedreport.app.data

import com.threedreport.core.model.PrinterProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de perfis de impressora, editável pela tela de Impressoras e
 * escolhido por orçamento na tela de Orçamento.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), pré-carregada com um perfil padrão no primeiro uso.
 */
expect class PrinterRepository() {
    val printers: StateFlow<List<PrinterProfile>>
    fun add(printer: PrinterProfile)
    fun update(printer: PrinterProfile)
    fun delete(id: String)
}
