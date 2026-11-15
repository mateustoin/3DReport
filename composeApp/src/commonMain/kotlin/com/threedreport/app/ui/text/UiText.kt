package com.threedreport.app.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Um texto da interface que o ViewModel escolhe mas não resolve: carrega a chave do recurso e os argumentos
 * (erro de campo, aviso). A tela transforma em `String` com [asString] (dentro de um `@Composable`) ou
 * [resolve] (dentro de uma corrotina, pra um aviso). Por ser dado puro, o teste do ViewModel compara
 * `UiText.Res(Res.string.chave)` em vez de uma frase.
 *
 * Os argumentos são `String` ou números, nunca outro [UiText].
 */
sealed interface UiText {
    data class Res(val resource: StringResource, val args: List<Any> = emptyList()) : UiText

    data class Plural(val resource: PluralStringResource, val quantity: Int, val args: List<Any> = emptyList()) : UiText

    /** Vários textos em sequência ("Não salvei:" e o motivo), separados por [separator]. */
    data class Joined(val parts: List<UiText>, val separator: String = " ") : UiText
}

/** `uiText(Res.string.consumables_notice_saved)`, ou com argumentos: `uiText(Res.string.x, nome)`. */
fun uiText(resource: StringResource, vararg args: Any): UiText = UiText.Res(resource, args.toList())

/** O texto, pra desenhar numa tela. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(resource, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(resource, quantity, *args.toTypedArray())
    is UiText.Joined -> parts.map { it.asString() }.joinToString(separator)
}

/** O texto, fora de um `@Composable` (por exemplo, o aviso que vai pra barra de mensagens). */
suspend fun UiText.resolve(): String = when (this) {
    is UiText.Res -> getString(resource, *args.toTypedArray())
    is UiText.Plural -> getPluralString(resource, quantity, *args.toTypedArray())
    is UiText.Joined -> parts.map { it.resolve() }.joinToString(separator)
}
