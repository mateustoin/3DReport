package com.threedreport.app.ui.components

import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.openFolder

/**
 * O retorno de uma ação que não muda nada na tela (decisão 108): o PDF salvo, o pedido que saiu do filtro,
 * o item excluído. Antes, essas ações terminavam em silêncio, e a pessoa ficava sem saber se tinha dado
 * certo e onde o arquivo foi parar.
 *
 * [id] distingue dois avisos iguais seguidos (salvar o mesmo PDF duas vezes mostra os dois).
 */
data class UserNotice(
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val isError: Boolean = false,
    val id: Long = 0,
)

/** Fonte de ids pros avisos de um ViewModel. */
class NoticeIds {
    private var last = 0L
    fun next(): Long = ++last
}

/**
 * O aviso de um arquivo gravado: "Abrir pasta" quando deu certo, o motivo quando não deu, e nada quando
 * a pessoa cancelou o "Salvar como".
 */
fun SaveResult.toNotice(savedMessage: String, id: Long): UserNotice? = when (this) {
    is SaveResult.Saved -> UserNotice(savedMessage, actionLabel = "Abrir pasta", action = { openFolder(path) }, id = id)
    is SaveResult.Failed -> UserNotice(message, isError = true, id = id)
    SaveResult.Cancelled -> null
}
