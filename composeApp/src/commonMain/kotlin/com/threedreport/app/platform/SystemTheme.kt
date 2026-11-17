package com.threedreport.app.platform

import androidx.compose.runtime.Composable

/**
 * Se o sistema pede tema escuro, pra resolver [com.threedreport.core.model.ThemeMode.SYSTEM]. Acompanha a troca
 * feita com o app aberto quando o sistema avisa.
 *
 * Existe porque o `isSystemInDarkTheme()` do Compose no desktop só sabe ler o Windows e o macOS: no Linux devolve
 * sempre claro, e o app abria claro num sistema escuro.
 */
@Composable
expect fun systemPrefersDark(): Boolean
