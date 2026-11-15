package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_open_folder
import _3dreport.composeapp.generated.resources.export_save_failed
import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.openFolder
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText

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

/**
 * Um aviso com o texto ainda por escolher ([UiText]) e, se houver, um botão de ação ("Desfazer"). O ViewModel
 * emite; a tela resolve o texto e mostra (ver [ObserveUiNotices]). Com [isError], o aviso fica mais tempo e ganha o "x".
 */
data class UiNotice(
    val message: UiText,
    val actionLabel: UiText? = null,
    val action: (() -> Unit)? = null,
    val isError: Boolean = false,
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
fun SaveResult.toNotice(savedMessage: UiText): UiNotice? = when (this) {
    is SaveResult.Saved -> UiNotice(savedMessage, actionLabel = uiText(Res.string.export_open_folder), action = { openFolder(path) })

    // O motivo exato vai pro log (quem grava já registra); a pessoa lê a causa provável e o que fazer.
    is SaveResult.Failed -> UiNotice(uiText(Res.string.export_save_failed), isError = true)

    SaveResult.Cancelled -> null
}
