package com.threedreport.app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateRulesTest {

    private fun asset(name: String) = ReleaseAsset(name = name, downloadUrl = "https://github.com/x/releases/download/v2.3.0/$name", sizeBytes = 100)

    /** Os nomes que a release da 2.2 publicou (jpackage + sufixo do .dmg no workflow), mais o arquivo de somas. */
    private val assets = listOf(
        asset("3DReport-2.3.0.msi"),
        asset("threedreport_2.3.0-1_amd64.deb"),
        asset("3DReport-2.3.0-apple-silicon.dmg"),
        asset("3DReport-2.3.0-intel.dmg"),
        asset("SHA256SUMS.txt"),
    )

    @Test
    fun windowsGetsTheMsi() {
        assertEquals("3DReport-2.3.0.msi", selectInstallerAsset(assets, HostOs.WINDOWS, HostArch.X64)?.name)
    }

    @Test
    fun linuxGetsTheDebOfItsArchitecture() {
        assertEquals("threedreport_2.3.0-1_amd64.deb", selectInstallerAsset(assets, HostOs.LINUX, HostArch.X64)?.name)
        assertNull(selectInstallerAsset(assets, HostOs.LINUX, HostArch.ARM64), "não há .deb pra ARM")
        val arm = assets + asset("threedreport_2.3.0-1_arm64.deb")
        assertEquals("threedreport_2.3.0-1_arm64.deb", selectInstallerAsset(arm, HostOs.LINUX, HostArch.ARM64)?.name)
    }

    @Test
    fun macGetsTheDmgOfItsProcessor() {
        assertEquals("3DReport-2.3.0-apple-silicon.dmg", selectInstallerAsset(assets, HostOs.MACOS, HostArch.ARM64)?.name)
        assertEquals("3DReport-2.3.0-intel.dmg", selectInstallerAsset(assets, HostOs.MACOS, HostArch.X64)?.name)
        // Um .dmg sem sufixo não diz pra qual Mac é: sem botão.
        assertNull(selectInstallerAsset(listOf(asset("3DReport-2.3.0.dmg")), HostOs.MACOS, HostArch.ARM64))
    }

    @Test
    fun noAssetForTheSystemMeansNoButton() {
        assertNull(selectInstallerAsset(assets, HostOs.OTHER, HostArch.X64))
        assertNull(selectInstallerAsset(assets, HostOs.WINDOWS, HostArch.OTHER))
        assertNull(selectInstallerAsset(emptyList(), HostOs.WINDOWS, HostArch.X64))
        assertNull(selectInstallerAsset(listOf(asset("SHA256SUMS.txt")), HostOs.LINUX, HostArch.X64))
    }

    @Test
    fun systemPropertiesBecomeOsAndArchitecture() {
        assertEquals(HostOs.WINDOWS, hostOsOf("Windows 11"))
        assertEquals(HostOs.LINUX, hostOsOf("Linux"))
        assertEquals(HostOs.MACOS, hostOsOf("Mac OS X"))
        assertEquals(HostOs.OTHER, hostOsOf("FreeBSD"))
        assertEquals(HostArch.X64, hostArchOf("amd64"))
        assertEquals(HostArch.X64, hostArchOf("x86_64"))
        assertEquals(HostArch.ARM64, hostArchOf("aarch64"))
        assertEquals(HostArch.ARM64, hostArchOf("arm64"))
        assertEquals(HostArch.OTHER, hostArchOf("x86"))
    }

    @Test
    fun checksumsAssetIsFoundByName() {
        assertEquals("SHA256SUMS.txt", checksumsAsset(assets)?.name)
        assertNull(checksumsAsset(assets.dropLast(1)))
    }

    private val deb = "5f1a" + "0".repeat(56) + "9abc"
    private val msi = "A".repeat(64)

    @Test
    fun readsTheSha256sumFormat() {
        val text = """
            $deb  threedreport_2.3.0-1_amd64.deb
            $msi *3DReport-2.3.0.msi

            linha que não é soma
            ${"1".repeat(64)}  macos-dmg/3DReport-2.3.0-intel.dmg
        """.trimIndent()

        assertEquals(
            mapOf(
                "threedreport_2.3.0-1_amd64.deb" to deb,
                "3DReport-2.3.0.msi" to msi.lowercase(),
                "3DReport-2.3.0-intel.dmg" to "1".repeat(64),
            ),
            parseSha256Sums(text),
        )
    }

    @Test
    fun checksumIsAcceptedOnlyWhenItMatches() {
        val sums = parseSha256Sums("$deb  threedreport_2.3.0-1_amd64.deb\r\n")

        assertEquals(ChecksumCheck.MATCH, checkChecksum(sums, "threedreport_2.3.0-1_amd64.deb", deb.uppercase()))
        assertEquals(ChecksumCheck.MISMATCH, checkChecksum(sums, "threedreport_2.3.0-1_amd64.deb", "f".repeat(64)))
        assertEquals(ChecksumCheck.MISSING, checkChecksum(sums, "3DReport-2.3.0.msi", deb))
        assertEquals(ChecksumCheck.MISSING, checkChecksum(emptyMap(), "threedreport_2.3.0-1_amd64.deb", deb))
    }
}
