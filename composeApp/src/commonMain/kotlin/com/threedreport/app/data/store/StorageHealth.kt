package com.threedreport.app.data.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * O que deu errado com os arquivos de dados e que a pessoa precisa saber (decisão 108). Antes, um
 * arquivo que não abria virava o valor padrão em silêncio, e uma gravação que falhava fechava o app.
 * Agora os dois viram aviso na tela.
 */
class StorageHealth {

    private val unreadable = MutableStateFlow<List<UnreadableFile>>(emptyList())

    /** Arquivos que não deu pra ler e foram guardados à parte, desde que o app abriu. */
    val unreadableFiles: StateFlow<List<UnreadableFile>> = unreadable.asStateFlow()

    private val failures = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Arquivos cuja última gravação falhou, com o motivo. Vazio quando está tudo gravado. */
    val writeFailures: StateFlow<Map<String, String>> = failures.asStateFlow()

    fun reportUnreadable(file: UnreadableFile) = unreadable.update { it + file }

    /** A pessoa já viu o aviso dos arquivos ilegíveis. */
    fun dismissUnreadable() = unreadable.update { emptyList() }

    fun reportWriteFailure(fileName: String, reason: String) = failures.update { it + (fileName to reason) }

    fun clearWriteFailure(fileName: String) = failures.update { if (fileName in it) it - fileName else it }
}

/**
 * Um arquivo de dados que não abriu.
 *
 * @property keptAs onde o arquivo com problema ficou guardado (nada é apagado).
 * @property recoveredFromBackup se os dados vieram da cópia anterior (`.bak`), e portanto só a última
 *   mudança pode ter se perdido.
 */
data class UnreadableFile(val name: String, val keptAs: String, val recoveredFromBackup: Boolean)
