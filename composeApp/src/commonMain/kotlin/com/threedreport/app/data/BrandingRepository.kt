package com.threedreport.app.data

import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda a personalização do documento exportado ([BrandingSettings]), hoje
 * só a marca d'água opcional do PDF.
 *
 * A implementação persiste em disco (ver `actual` na fonte de cada
 * plataforma), começando sem marca d'água (`watermarkText = null`).
 */
expect class BrandingRepository() {
    val branding: StateFlow<BrandingSettings>
    fun update(branding: BrandingSettings)
}
