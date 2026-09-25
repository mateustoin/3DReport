package com.threedreport.app.data

/**
 * Os repositórios dos testes, com os nomes das classes de antes das interfaces (decisão 108), gravando na
 * pasta de dados do teste (propriedade `threedreport.dataDir`). Cada chamada lê do disco de novo, como abrir
 * o app outra vez: é assim que os testes conferem que o dado foi mesmo gravado.
 */
internal fun testStorage(): LocalStorage = LocalStorage(appDataDir())

fun FilamentRepository(): FilamentRepository = testStorage().filaments()
fun PrinterRepository(): PrinterRepository = testStorage().printers()
fun ServiceRepository(): ServiceRepository = testStorage().services()
fun SalesChannelRepository(): SalesChannelRepository = testStorage().salesChannels()
fun TemplateRepository(): TemplateRepository = testStorage().templates()
fun ClientRepository(): ClientRepository = testStorage().clients()
fun MaintenanceRepository(): MaintenanceRepository = testStorage().maintenance()
fun QuoteHistoryRepository(): QuoteHistoryRepository = testStorage().quoteHistory()
fun SettingsRepository(): SettingsRepository = testStorage().settings()
fun BrandingRepository(): BrandingRepository = testStorage().branding()
fun ThemeRepository(): ThemeRepository = testStorage().theme()
fun CurrencyRepository(): CurrencyRepository = testStorage().currency()
fun UsageProfileRepository(): UsageProfileRepository = testStorage().usageProfile()
fun OnboardingRepository(): OnboardingRepository = testStorage().onboarding()
fun PreferencesRepository(): PreferencesRepository = testStorage().preferences()
fun BackupRepository(): BackupRepository = testStorage().backup()

/** O backup como bytes, pros testes que montam e comparam o `.zip` em memória. */
internal fun BackupRepository.createBackupZip(): ByteArray {
    val file = java.io.File.createTempFile("3dreport-backup", ".zip")
    try {
        createBackup(file.path)
        return file.readBytes()
    } finally {
        file.delete()
    }
}

/** Restaura um backup dado em bytes (ver [createBackupZip]). */
internal fun BackupRepository.restoreFromZip(bytes: ByteArray): RestoreResult {
    val file = java.io.File.createTempFile("3dreport-restore", ".zip")
    try {
        file.writeBytes(bytes)
        return restore(file.path)
    } finally {
        file.delete()
    }
}
