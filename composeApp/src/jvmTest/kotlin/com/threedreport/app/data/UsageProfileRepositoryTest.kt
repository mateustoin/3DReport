package com.threedreport.app.data

import com.threedreport.core.model.QuoteKind
import com.threedreport.core.model.UsageProfile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Valida o perfil de uso (decisão 103): quem nunca escolheu fica como vendedor, e a escolha persiste. */
class UsageProfileRepositoryTest {

    @BeforeTest
    fun setDataDir() {
        System.setProperty("threedreport.dataDir", createTempDirectory("3dreport-test").toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    @Test
    fun startsAsSellerSoExistingUsersSeeNoChange() {
        assertEquals(UsageProfile.SELLER, UsageProfileRepository().profile.value)
        assertEquals(QuoteKind.ORDER, UsageProfile.SELLER.defaultKind)
    }

    @Test
    fun chosenProfileSurvivesNewRepositoryInstance() {
        UsageProfileRepository().update(UsageProfile.STARTER)

        assertEquals(UsageProfile.STARTER, UsageProfileRepository().profile.value)
        assertEquals(QuoteKind.PRODUCT, UsageProfileRepository().profile.value.defaultKind)
    }
}
