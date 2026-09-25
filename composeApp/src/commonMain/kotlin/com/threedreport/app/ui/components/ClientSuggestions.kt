package com.threedreport.app.ui.components

import com.threedreport.core.model.Client

/** Clientes do cadastro que batem com o que foi digitado, pra sugerir (até cinco, a partir de 2 letras). */
fun matchingClients(typed: String, clients: List<Client>): List<Client> {
    val query = typed.trim().lowercase()
    if (query.length < 2) return emptyList()
    return clients.filter { it.name.lowercase().contains(query) && it.name.trim().lowercase() != query }.take(5)
}
