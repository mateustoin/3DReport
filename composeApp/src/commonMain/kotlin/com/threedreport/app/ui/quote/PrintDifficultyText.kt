package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_stl_difficulty_easy
import _3dreport.composeapp.generated.resources.quote_stl_difficulty_hard
import _3dreport.composeapp.generated.resources.quote_stl_difficulty_medium
import com.threedreport.core.stl.PrintDifficulty
import org.jetbrains.compose.resources.StringResource

/** O nível de dificuldade como aparece em "Nível fácil" (o core não tem texto, Leva 10B, parte 7). */
fun PrintDifficulty.labelRes(): StringResource = when (this) {
    PrintDifficulty.FACIL -> Res.string.quote_stl_difficulty_easy
    PrintDifficulty.MEDIO -> Res.string.quote_stl_difficulty_medium
    PrintDifficulty.DIFICIL -> Res.string.quote_stl_difficulty_hard
}
