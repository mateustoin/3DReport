package com.threedreport.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Mostra [notice] na barra de avisos, com o botão de ação quando houver, e chama [onShown] quando ele
 * sai. Aviso com ação ou de erro fica mais tempo e ganha o "x": ninguém lê um erro que some em 4 segundos.
 */
@Composable
fun ShowNotice(notice: UserNotice?, onShown: (UserNotice) -> Unit) {
    val hostState = LocalSnackbarHostState.current
    LaunchedEffect(notice) {
        notice ?: return@LaunchedEffect
        val lingers = notice.actionLabel != null || notice.isError
        val result = hostState.showSnackbar(
            message = notice.message,
            actionLabel = notice.actionLabel,
            withDismissAction = lingers,
            duration = if (lingers) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) notice.action?.invoke()
        onShown(notice)
    }
}
