package com.threedreport.core.model

/**
 * Um cadastro que dá pra arquivar (decisão 115): filamento, impressora, serviço e canal. Arquivado some
 * das escolhas de um orçamento novo, mas continua valendo pra quem já usou.
 */
interface Archivable<T : Archivable<T>> {
    val id: String
    val archived: Boolean

    /** Uma cópia com [archived] trocado. */
    fun withArchived(archived: Boolean): T
}
