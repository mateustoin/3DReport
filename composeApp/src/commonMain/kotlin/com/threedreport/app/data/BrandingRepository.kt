package com.threedreport.app.data

import com.threedreport.app.data.store.DocumentValue
import com.threedreport.app.platform.PickedFile
import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.flow.StateFlow

/** O que fazer com a logo ao salvar a marca: manter a atual, trocar por outra ou tirar. */
sealed interface LogoChange {
    data object Keep : LogoChange
    data class Replace(val file: PickedFile) : LogoChange
    data object Remove : LogoChange
}

/**
 * Guarda a personalização do documento exportado ([BrandingSettings]): nome da marca, logo,
 * contato e as opções de apresentação do PDF. Começa sem marca (`brandName = null`).
 *
 * A logo mora no [AttachmentStore], como os outros anexos, e [BrandingSettings.logoFileName] guarda a
 * chave dela.
 */
interface BrandingRepository {
    val branding: StateFlow<BrandingSettings>

    /**
     * Grava [branding]. [BrandingSettings.logoFileName] é ignorado aqui: quem decide a logo é
     * [logo]. Com [LogoChange.Keep], mantém a logo atual; com [LogoChange.Replace], guarda o arquivo
     * novo antes de trocar a referência. O arquivo antigo não é apagado aqui (sai na limpeza de
     * anexos sem uso, ao abrir o app).
     */
    fun update(branding: BrandingSettings, logo: LogoChange = LogoChange.Keep)

    /** Bytes da logo atual, ou `null` sem logo ou se o arquivo sumiu da pasta. */
    fun logoBytes(): ByteArray?
}

class StoredBrandingRepository(
    private val document: DocumentValue<BrandingSettings>,
    private val attachments: AttachmentStore,
) : BrandingRepository {
    override val branding: StateFlow<BrandingSettings> = document.value

    override fun update(branding: BrandingSettings, logo: LogoChange) {
        val logoKey = when (logo) {
            LogoChange.Keep -> document.value.value.logoFileName
            LogoChange.Remove -> null
            is LogoChange.Replace -> attachments.put(logo.file.bytes, logo.file.fileName)
        }
        document.set(branding.copy(logoFileName = logoKey))
    }

    override fun logoBytes(): ByteArray? = document.value.value.logoFileName?.let(attachments::read)
}
