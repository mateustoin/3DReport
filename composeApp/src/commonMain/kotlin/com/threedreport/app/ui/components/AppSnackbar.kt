package com.threedreport.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.resolve
import kotlinx.coroutines.flow.Flow

/**
 * Avisos transitórios de sucesso ("Orçamento salvo", "Configurações salvas"), mostrados sempre no
 * mesmo canto da janela em vez de como texto solto embaixo de cada botão.
 *
 * O texto inline tinha dois problemas: sumia só quando a pessoa mexia em outro campo, e no layout
 * de duas colunas (decisão 81) podia nascer fora da parte visível da coluna, então a confirmação
 * de uma ação que deu certo simplesmente não aparecia.
 *
 * **Erro de validação continua inline, de propósito.** Ele precisa ficar ao lado do campo enquanto
 * a pessoa corrige, e um aviso que some sozinho depois de alguns segundos é justamente o contrário
 * disso.
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Mostra [message] uma vez, quando [show] passa a ser verdadeiro, e avisa [onShown] pra quem
 * controla o estado poder desmarcar a flag.
 *
 * Sem esse "consumir depois de mostrar", salvar duas vezes seguidas sem editar nada no meio não
 * mostraria o segundo aviso: a flag continuaria verdadeira e o efeito não rodaria de novo, dando a
 * impressão de que a segunda ação não aconteceu.
 */
@Composable
fun ShowSnackbarOnce(show: Boolean, message: String, onShown: () -> Unit) {
    val hostState = LocalSnackbarHostState.current
    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        // Consumir só DEPOIS de mostrar: desmarcar a flag antes trocaria a chave do LaunchedEffect
        // e cancelaria a própria corrotina no meio, engolindo o aviso.
        hostState.showSnackbar(message)
        onShown()
    }
}

/**
 * Mostra, na barra de mensagens, cada aviso que o ViewModel da tela emitir ([UiText] já escolhido por ele). É
 * como uma tela nova liga o `notices` do seu ViewModel: uma linha.
 */
@Composable
fun ObserveNotices(notices: Flow<UiText>) {
    val hostState = LocalSnackbarHostState.current
    LaunchedEffect(notices) {
        notices.collect { hostState.showSnackbar(it.resolve()) }
    }
}
