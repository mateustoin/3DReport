package com.threedreport.app.data

import kotlinx.coroutines.flow.StateFlow

/** Aviso sobre a assinatura "Gerado com 3DReport" nos PDFs, mostrado uma vez a quem já usava o app. */
const val NOTICE_APP_SIGNATURE = "app-signature"

/**
 * Lembra quais avisos de novidade a pessoa já viu, pra cada um aparecer uma vez só. Mesmo motivo
 * do [OnboardingRepository] pra ficar num arquivo próprio: é estado da interface, não configuração
 * do negócio. Guarda identificadores (ex.: [NOTICE_APP_SIGNATURE]), então um aviso novo no futuro
 * não precisa de campo nem de arquivo novo.
 */
expect class NoticesRepository() {
    val seen: StateFlow<Set<String>>
    fun markSeen(id: String)
}
