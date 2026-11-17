package com.threedreport.app.update

import com.threedreport.app.ui.about.LatestRelease

/**
 * Como o app vai se atualizar no sistema em uso (decisão 143). A tela usa pra escolher o texto de "o que vai
 * acontecer" antes de instalar.
 */
enum class InstallMethod {
    /** Abre o `msiexec` (o Windows pede a confirmação de administrador) e fecha o app. */
    WINDOWS_MSI,

    /** Pede a senha pelo `pkexec`, instala o `.deb`, reabre o app na versão nova e fecha a atual. */
    LINUX_DEB,

    /** Linux sem `pkexec`: abre a pasta do arquivo baixado e mostra o comando de instalar. */
    LINUX_DEB_MANUAL,

    /** Abre o `.dmg` e fecha o app; a pessoa arrasta o 3DReport pra Aplicativos. */
    MACOS_DMG,
}

/** O instalador escolhido pra uma release e como ele vai ser usado. */
data class UpdatePlan(val release: LatestRelease, val asset: ReleaseAsset, val method: InstallMethod)

/** Instalador baixado e com a soma conferida, numa pasta temporária do sistema (nunca na pasta de dados). */
data class DownloadedUpdate(val plan: UpdatePlan, val filePath: String)

/** Por que a atualização não seguiu. Sem texto: a tela escolhe a mensagem. */
enum class UpdateFailure {
    /** Falha de rede ou resposta inesperada do GitHub ("Tentar de novo"). */
    NETWORK,

    /** A release não tem `SHA256SUMS.txt`, ou ele não lista o instalador (releases antigas). */
    NO_CHECKSUMS,

    /** A soma do arquivo baixado não bate. O arquivo já foi apagado. */
    CHECKSUM_MISMATCH,
}

/** Resultado de [UpdateInstaller.download]. */
sealed interface DownloadOutcome {
    data class Ready(val update: DownloadedUpdate) : DownloadOutcome

    data class Failed(val reason: UpdateFailure) : DownloadOutcome
}

/** Resultado de [UpdateInstaller.install]. */
sealed interface InstallOutcome {
    /**
     * O instalador foi aberto (Windows, macOS) ou a versão nova já foi instalada e vai abrir assim que este app
     * fechar (Linux). A tela fecha o app pelo caminho normal, que grava o que está pendente.
     */
    data object CloseApp : InstallOutcome

    /** A pessoa cancelou a senha. Nada mudou. */
    data object Cancelled : InstallOutcome

    /** O instalador falhou. Nada mudou. */
    data object Failed : InstallOutcome

    /**
     * Sem `pkexec`: a pasta [folderPath] com o arquivo foi aberta, e [command] é o comando de terminal que instala
     * (não é texto de interface: é pra copiar e colar).
     */
    data class Manual(val folderPath: String, val command: String) : InstallOutcome
}

/**
 * Baixa, confere e instala a versão nova (decisão 143). Injetado nos ViewModels; os testes usam um falso, e o
 * desktop usa a implementação do `jvmMain`. Só conecta quando a pessoa pede ([download]).
 */
interface UpdateInstaller {

    /**
     * O plano pra atualizar pra [release], ou `null` quando não há botão "Atualizar agora": app rodando fora de um
     * instalador (pelo Gradle, por exemplo) ou release sem instalador pro sistema e arquitetura em uso.
     */
    fun plan(release: LatestRelease): UpdatePlan?

    /**
     * Baixa o `SHA256SUMS.txt` e o instalador pra uma pasta temporária, informando o progresso (0 a 1) do
     * instalador, e confere a soma. Cancelar a corrotina interrompe e apaga o arquivo parcial; soma ausente ou
     * diferente apaga o arquivo e devolve [DownloadOutcome.Failed].
     */
    suspend fun download(plan: UpdatePlan, onProgress: (Float) -> Unit): DownloadOutcome

    /** Instala [update] do jeito de [UpdatePlan.method]. Chame depois de perguntar sobre orçamento não salvo. */
    suspend fun install(update: DownloadedUpdate): InstallOutcome

    /** Apaga o arquivo baixado (a pessoa desistiu depois de baixar). */
    fun discard(update: DownloadedUpdate)
}

/** Sem atualização por dentro: nunca há plano. Padrão do contêiner e das capturas de tela. */
object NoUpdateInstaller : UpdateInstaller {
    override fun plan(release: LatestRelease): UpdatePlan? = null

    override suspend fun download(plan: UpdatePlan, onProgress: (Float) -> Unit): DownloadOutcome = DownloadOutcome.Failed(UpdateFailure.NETWORK)

    override suspend fun install(update: DownloadedUpdate): InstallOutcome = InstallOutcome.Failed

    override fun discard(update: DownloadedUpdate) = Unit
}
