package com.threedreport.app.data

import com.threedreport.core.model.BrandingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class BrandingRepository actual constructor() {
    private val file = File(appDataDir(), "branding.json")
    private val logoDir = File(appDataDir(), "branding")
    private val state = MutableStateFlow(readJsonFile(file, BrandingSettings()))

    actual val branding: StateFlow<BrandingSettings> = state.asStateFlow()

    actual fun update(branding: BrandingSettings, logo: LogoChange) {
        val previousLogo = state.value.logoFileName
        val logoFileName = when (logo) {
            LogoChange.Keep -> previousLogo
            LogoChange.Remove -> null
            is LogoChange.Replace -> {
                // Nome novo a cada troca, em vez de sobrescrever "logo.png": o arquivo antigo só
                // some depois que a configuração já aponta pro novo.
                val extension = logo.file.fileName.substringAfterLast('.', "png").lowercase()
                val fileName = "logo-${System.currentTimeMillis()}.$extension"
                logoDir.mkdirs()
                File(logoDir, fileName).writeBytes(logo.file.bytes)
                fileName
            }
        }

        val updated = branding.copy(logoFileName = logoFileName)
        state.value = updated
        writeJsonFile(file, updated)

        if (previousLogo != null && previousLogo != logoFileName) File(logoDir, previousLogo).delete()
    }

    actual fun logoBytes(): ByteArray? {
        val fileName = state.value.logoFileName ?: return null
        val logoFile = File(logoDir, fileName)
        return if (logoFile.exists()) logoFile.readBytes() else null
    }
}
