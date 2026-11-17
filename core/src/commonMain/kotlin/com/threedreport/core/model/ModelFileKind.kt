package com.threedreport.core.model

/**
 * Tipo do arquivo do modelo guardado no orçamento (decisão 141): o STL da peça ou o projeto do fatiador em 3MF.
 * Sai da extensão do nome, nunca de um campo gravado à parte.
 */
enum class ModelFileKind(val extension: String) {
    STL("stl"),
    THREE_MF("3mf"),
    ;

    companion object {
        /** O tipo pela extensão de [fileName] (sem diferenciar maiúscula), ou `null` se não é STL nem 3MF. */
        fun of(fileName: String): ModelFileKind? {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == extension }
        }
    }
}
