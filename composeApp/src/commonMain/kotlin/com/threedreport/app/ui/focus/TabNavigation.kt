package com.threedreport.app.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Faz Tab avançar para o próximo campo e Shift+Tab voltar para o anterior,
 * em vez do comportamento padrão de campo de texto no desktop (inserir uma
 * tabulação no texto, que aparece como espaço).
 */
@Composable
fun Modifier.tabToNavigate(): Modifier {
    val focusManager = LocalFocusManager.current
    return onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
            focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
            true
        } else {
            false
        }
    }
}

/**
 * Leva o foco pro elemento deste pedido assim que a tela estiver medida: o primeiro quadro de um painel ou diálogo recém aberto
 * nega o pedido (nada está posicionado ainda), e o foco inicial nunca chegava. Tenta por alguns quadros e para ao conseguir, ou
 * quando [alreadyFocused] diz que o foco já está no lugar (um campo com `autoFocus`).
 */
suspend fun FocusRequester.requestFocusWhenReady(alreadyFocused: () -> Boolean = { false }) {
    repeat(FOCUS_ATTEMPTS) {
        withFrameNanos { }
        if (alreadyFocused() || runCatching { requestFocus() }.getOrDefault(false)) return
    }
}

private const val FOCUS_ATTEMPTS = 10
