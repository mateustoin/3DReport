package com.threedreport.app.platform

import com.threedreport.app.update.ReleaseAsset
import kotlin.test.Test
import kotlin.test.assertEquals

class GitHubReleaseSourceTest {

    @Test
    fun readsVersionPageAndAssetsOfTheLatestRelease() {
        val body = """
            {"url":"https://api.github.com/repos/o/r/releases/1","tag_name":"v2.3.0","html_url":"https://github.com/o/r/releases/tag/v2.3.0",
             "draft":false,"assets":[
               {"id":1,"name":"3DReport-2.3.0.msi","size":123456,"content_type":"application/octet-stream",
                "browser_download_url":"https://github.com/o/r/releases/download/v2.3.0/3DReport-2.3.0.msi"},
               {"id":2,"name":"SHA256SUMS.txt","size":420,"browser_download_url":"https://github.com/o/r/releases/download/v2.3.0/SHA256SUMS.txt"}
             ]}
        """.trimIndent()

        val release = parseLatestRelease(body)

        assertEquals("2.3.0", release.version)
        assertEquals("https://github.com/o/r/releases/tag/v2.3.0", release.url)
        assertEquals(
            listOf(
                ReleaseAsset("3DReport-2.3.0.msi", "https://github.com/o/r/releases/download/v2.3.0/3DReport-2.3.0.msi", 123456),
                ReleaseAsset("SHA256SUMS.txt", "https://github.com/o/r/releases/download/v2.3.0/SHA256SUMS.txt", 420),
            ),
            release.assets,
        )
    }

    @Test
    fun releaseWithoutAssetsStillHasVersionAndPage() {
        val release = parseLatestRelease("""{"tag_name":"v2.2.0","html_url":"https://x"}""")
        assertEquals("2.2.0", release.version)
        assertEquals(emptyList(), release.assets)
    }
}
