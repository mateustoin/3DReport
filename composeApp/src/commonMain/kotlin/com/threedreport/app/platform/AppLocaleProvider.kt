package com.threedreport.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * O idioma em uso, pra quem desenha a tela. Existe principalmente pelo efeito de trocar de valor (ver [ProvideAppLocale]);
 * quem só formata uma data lê o [AppLocale] direto.
 */
val LocalAppLocale = staticCompositionLocalOf { AppLocaleState.BRAZILIAN_PORTUGUESE }

/**
 * Faz a tela inteira acompanhar a troca de idioma na hora, sem reiniciar (decisão 152).
 *
 * Cada `stringResource` guarda o texto lido junto do ambiente de recursos (idioma, tema, densidade), e só relê quando
 * recompõe e vê que o idioma da plataforma mudou. Sozinho, isso deixaria a tela meio traduzida: o que não recompõe (a
 * maior parte, porque o Compose pula quem não mudou) ficaria no idioma de antes. Aqui, a cada idioma novo do
 * [AppLocale], o [LocalAppLocale] (um `staticCompositionLocalOf`) recebe um valor novo, e trocar um local estático
 * recompõe tudo o que está dentro, sem pular nada e sem perder estado (`remember`, ViewModels, rascunhos, rolagem):
 * cada texto é relido no idioma padrão da plataforma, que quem troca o idioma já mudou antes (`DesktopLocale` no
 * desktop). Vale também pras listas preguiçosas, subcomposições e janelas por cima (ver `LanguageSwitchTest`).
 *
 * A leitura do recurso na troca é síncrona no desktop (como na primeira composição), então não há quadro com texto
 * misturado. Fica de fora só o que já virou `String` antes da troca: um aviso já na barra de mensagens, uma prévia de
 * PDF já gerada. O próximo sai no idioma novo.
 *
 * Não usa o `LocalComposeEnvironment` do Compose, que seria o caminho direto mas é interno da biblioteca.
 */
@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val locale by AppLocale.current.collectAsState()
    CompositionLocalProvider(LocalAppLocale provides locale, content = content)
}
