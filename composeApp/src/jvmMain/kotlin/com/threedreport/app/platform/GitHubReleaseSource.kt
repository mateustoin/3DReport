package com.threedreport.app.platform

import com.threedreport.app.ui.about.LatestRelease
import com.threedreport.app.ui.about.ReleaseSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * A versão mais recente publicada no GitHub (decisão 116), pela API pública de releases: um GET sem
 * autenticação, sem nada do usuário no pedido além do que qualquer navegador manda. Rascunhos e pré-versões
 * ficam de fora, porque o `/releases/latest` só devolve a última publicada.
 */
class GitHubReleaseSource(private val appVersion: String) : ReleaseSource {

    @Serializable
    private data class Release(@SerialName("tag_name") val tagName: String, @SerialName("html_url") val htmlUrl: String)

    private val json = Json { ignoreUnknownKeys = true }

    // Só nasce na primeira verificação: com a opção desligada, o app não abre nenhuma conexão.
    private val client: HttpClient by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build() }

    override suspend fun latest(): LatestRelease? {
        val request = HttpRequest.newBuilder(URI(LATEST_URL))
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "3DReport/$appVersion")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        val release = json.decodeFromString<Release>(response.body())
        return LatestRelease(version = release.tagName.removePrefix("v"), url = release.htmlUrl)
    }

    private companion object {
        const val LATEST_URL = "https://api.github.com/repos/mateustoin/3DReport/releases/latest"
    }
}
