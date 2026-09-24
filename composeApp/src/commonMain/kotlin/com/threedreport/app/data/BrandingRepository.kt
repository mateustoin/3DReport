package com.threedreport.app.data

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
 * contato e as opções de apresentação do PDF.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), começando sem marca d'água (`watermarkText = null`). A logo
 * fica como arquivo à parte dentro da pasta de dados, então entra no backup
 * sem nenhum tratamento especial.
 */
expect class BrandingRepository() {
    val branding: StateFlow<BrandingSettings>

    /**
     * Grava [branding]. [BrandingSettings.logoFileName] é ignorado aqui: quem decide a logo é
     * [logo]. Com [LogoChange.Keep], mantém a logo atual; com [LogoChange.Replace], grava o arquivo
     * novo **antes** de trocar a referência e só depois apaga o antigo, pra uma falha no meio nunca
     * deixar a configuração apontando pra um arquivo que não existe.
     */
    fun update(branding: BrandingSettings, logo: LogoChange = LogoChange.Keep)

    /** Bytes da logo atual, ou `null` sem logo ou se o arquivo sumiu da pasta. */
    fun logoBytes(): ByteArray?
}
