package com.threedreport.app.data

import com.threedreport.app.AppLog
import com.threedreport.app.data.store.DataFile
import com.threedreport.app.data.store.StorageHealth
import com.threedreport.app.data.store.UnreadableFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Formato dos arquivos de dados. `encodeDefaults` grava todo campo, inclusive os iguais ao padrão
 * (decisão 106): o significado do arquivo não depende de um valor padrão no código, que pode mudar, e
 * outro programa (um servidor, uma importação) lê o arquivo sem conhecer esses padrões.
 * `ignoreUnknownKeys` protege abrir um arquivo de uma versão mais nova.
 */
internal val dataJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Um arquivo JSON da pasta de dados (decisão 108).
 *
 * **Gravar** nunca deixa o arquivo pela metade: o conteúdo vai pra `<nome>.tmp` (forçado no disco), o
 * arquivo atual vira a cópia anterior `<nome>.bak` e o `.tmp` toma o lugar dele. Queda de energia no
 * meio deixa o arquivo novo, o anterior ou a cópia anterior, nunca um pedaço.
 *
 * **Ler** um arquivo que não abre (mexido à mão, disco com problema) não perde nada: ele é guardado ao
 * lado como `<nome>.ilegivel-<millis>.json`, a cópia anterior é usada se abrir, e [StorageHealth] avisa a
 * tela. Uma falha de leitura do disco (arquivo travado por outro programa) é tentada de novo e, se
 * continuar, vira [DataReadException]: abrir com os dados vazios e gravar por cima seria pior.
 */
class JsonDataFile<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val health: StorageHealth? = null,
) : DataFile<T> {

    override val name: String
        get() = file.name

    private val tmp = File(file.parentFile, "${file.name}.tmp")
    private val bak = File(file.parentFile, "${file.name}.bak")

    override fun read(): T? {
        if (!file.exists()) {
            // O arquivo só some entre as duas trocas de nome da gravação: o .tmp é o estado mais novo e
            // o .bak, o anterior.
            return listOf(tmp, bak).firstNotNullOfOrNull { candidate -> candidate.takeIf { it.exists() }?.let(::decodeOrNull) }
        }
        decodeOrNull(file)?.let { return it }

        val keptAs = File(file.parentFile, "${file.nameWithoutExtension}.ilegivel-${System.currentTimeMillis()}.${file.extension}")
        if (!file.renameTo(keptAs)) {
            runCatching {
                file.copyTo(keptAs, overwrite = true)
                file.delete()
            }
        }
        val recovered = bak.takeIf { it.exists() }?.let(::decodeOrNull)
        AppLog.warn("${file.name} não abriu; guardado como ${keptAs.name}" + if (recovered != null) " e recuperado da cópia anterior" else "")
        health?.reportUnreadable(UnreadableFile(file.name, keptAs.path, recoveredFromBackup = recovered != null))
        return recovered
    }

    override fun write(value: T) {
        // Serializa antes de tocar no disco: um valor que não vira JSON não estraga o arquivo.
        val bytes = dataJson.encodeToString(serializer, value).encodeToByteArray()
        file.parentFile?.mkdirs()
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (file.exists()) move(file, bak)
        move(tmp, file)
    }

    private fun decodeOrNull(source: File): T? {
        val text = readWithRetry(source)
        return try {
            dataJson.decodeFromString(serializer, text)
        } catch (_: IllegalArgumentException) {
            // Só erro de conteúdo: a SerializationException e um `require` de modelo são os dois
            // IllegalArgumentException.
            null
        }
    }

    private fun readWithRetry(source: File): String = retrying("ler ${source.name}") { source.readText() }
        ?: throw DataReadException(source)

    private fun move(from: File, to: File) {
        retrying("gravar ${to.name}") {
            try {
                Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } ?: throw IOException("Não consegui gravar ${to.name}: o arquivo pode estar aberto em outro programa.")
    }
}

/** Um arquivo de dados existe e não deu pra ler do disco, mesmo tentando de novo. */
class DataReadException(val file: File) : IOException("Não consegui ler ${file.path}")

/**
 * Tenta [action] algumas vezes com uma pausa curta, porque no Windows o antivírus ou o OneDrive às
 * vezes seguram um arquivo por alguns milissegundos logo depois de ele mudar. `null` se não der.
 */
internal fun <R> retrying(what: String, attempts: Int = 5, action: () -> R): R? {
    repeat(attempts) { attempt ->
        try {
            return action()
        } catch (e: IOException) {
            if (attempt == attempts - 1) {
                AppLog.warn("Falha ao $what", e)
            } else {
                Thread.sleep(40L * (attempt + 1))
            }
        }
    }
    return null
}
