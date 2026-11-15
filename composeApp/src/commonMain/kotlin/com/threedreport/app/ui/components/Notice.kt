package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.export_open_folder
import _3dreport.composeapp.generated.resources.export_save_failed
import com.threedreport.app.platform.SaveResult
import com.threedreport.app.platform.openFolder
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText

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
