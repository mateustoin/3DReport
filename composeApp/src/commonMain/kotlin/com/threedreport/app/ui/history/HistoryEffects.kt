package com.threedreport.app.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.threedreport.app.ui.components.ObserveUiNotices
import kotlinx.coroutines.delay

/**
 * O que Pedidos e o Catálogo fazem enquanto estão na tela, sem desenhar nada: mostrar os avisos do ViewModel, apagar o
 * "Copiado" depois de um instante e avisar o ViewModel de que o dia pode ter virado (ao entrar e a cada minuto), pra os
 * selos de prazo e os blocos de resumo não ficarem presos no "hoje" de quando a tela abriu.
 */
@Composable
fun ObserveHistory(viewModel: QuoteHistoryViewModel, copiedId: String?) {
    ObserveUiNotices(viewModel.notices)
    LaunchedEffect(copiedId) {
        if (copiedId == null) return@LaunchedEffect
        delay(COPIED_MILLIS)
        viewModel.clearCopied()
    }
    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.onShown()
            delay(DAY_CHECK_MILLIS)
        }
    }
}

private const val COPIED_MILLIS = 2000L
private const val DAY_CHECK_MILLIS = 60_000L
