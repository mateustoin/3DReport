package com.threedreport.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChannelFeeScheduleTest {

    /** Parecida com a Shopee de 2026: o fixo sobe com a faixa. */
    private val shopeeLike = ChannelFeeSchedule(
        tiers = listOf(
            ChannelFeeTier(upToUnitPrice = 79.99, feeRate = 0.20, fixedFeePerItem = 4.0),
            ChannelFeeTier(upToUnitPrice = 99.99, feeRate = 0.14, fixedFeePerItem = 16.0),
            ChannelFeeTier(upToUnitPrice = null, feeRate = 0.14, fixedFeePerItem = 20.0),
        ),
    )

    @Test
    fun theTierIsChosenByTheUnitPriceWithTheLimitIncluded() {
        assertEquals(ChannelFee(0.20, 4.0), shopeeLike.feeAt(79.99))
        assertEquals(ChannelFee(0.14, 16.0), shopeeLike.feeAt(80.0))
        assertEquals(ChannelFee(0.14, 20.0), shopeeLike.feeAt(500.0))
    }

    @Test
    fun withoutTiersThePriceIsTheDirectFormula() {
        val schedule = ChannelFeeSchedule(feeRate = 0.20, fixedFeePerItem = 4.0)
        val price = schedule.priceLeaving(receipt = 50.0, extras = 10.0, quantity = 3, taxRate = 0.06)
        assertEquals((50.0 + 10.0 + 4.0 * 3) / (1 - 0.26) - 10.0, price, 1e-9)
        assertEquals(50.0 + 10.0, schedule.receiptAt(price, extras = 10.0, quantity = 3, taxRate = 0.06), 1e-9)
    }

    @Test
    fun aFreeScheduleChargesNothing() {
        assertTrue(ChannelFeeSchedule.NONE.isFree)
        assertEquals(42.0, ChannelFeeSchedule.NONE.priceLeaving(42.0, extras = 0.0, quantity = 1, taxRate = 0.0), 1e-9)
    }

    @Test
    fun withTiersTheCheapestConsistentPriceWins() {
        // Na primeira faixa: (50 + 4) / 0,8 = 67,50, que cai nela mesma.
        assertEquals(67.5, shopeeLike.priceLeaving(receipt = 50.0, extras = 0.0, quantity = 1, taxRate = 0.0), 1e-9)
    }

    @Test
    fun aPriceThatCrossesIntoTheNextTierUsesThatTiersFee() {
        // Na primeira faixa daria (70 + 4) / 0,8 = 92,50, que já é da segunda: lá, (70 + 16) / 0,86 = 100,00,
        // que é da terceira: lá, (70 + 20) / 0,86 = 104,65, coerente.
        val price = shopeeLike.priceLeaving(receipt = 70.0, extras = 0.0, quantity = 1, taxRate = 0.0)
        assertEquals((70.0 + 20.0) / 0.86, price, 1e-9)
        assertEquals(70.0, shopeeLike.receiptAt(price, extras = 0.0, quantity = 1, taxRate = 0.0), 1e-9)
    }

    @Test
    fun whenNoTierFitsExactlyTheFirstCentOfTheNextTierIsUsed() {
        // A faixa de cima é mais barata: a R$ 50,00 (primeira faixa) sobram 35,00; a conta exata da segunda
        // (42,22) cai na primeira. O menor preço que cobre os 38,00 é um centavo acima do limite.
        val schedule = ChannelFeeSchedule(
            tiers = listOf(
                ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.20, fixedFeePerItem = 5.0),
                ChannelFeeTier(upToUnitPrice = null, feeRate = 0.10, fixedFeePerItem = 0.0),
            ),
        )
        val price = schedule.priceLeaving(receipt = 38.0, extras = 0.0, quantity = 1, taxRate = 0.0)
        assertEquals(50.01, price, 1e-9)
        assertTrue(schedule.receiptAt(price, extras = 0.0, quantity = 1, taxRate = 0.0) >= 38.0)
    }

    @Test
    fun theFixedFeeIsPerItem() {
        val schedule = ChannelFeeSchedule(feeRate = 0.0, fixedFeePerItem = 4.0)
        assertEquals(30.0 + 4.0 * 5, schedule.priceLeaving(receipt = 30.0, extras = 0.0, quantity = 5, taxRate = 0.0), 1e-9)
    }

    @Test
    fun tiersMustBeAscendingAndEndWithoutALimit() {
        assertFailsWith<IllegalArgumentException> {
            ChannelFeeSchedule(tiers = listOf(ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.1)))
        }
        assertFailsWith<IllegalArgumentException> {
            ChannelFeeSchedule(
                tiers = listOf(
                    ChannelFeeTier(upToUnitPrice = 80.0, feeRate = 0.1),
                    ChannelFeeTier(upToUnitPrice = 50.0, feeRate = 0.1),
                    ChannelFeeTier(upToUnitPrice = null, feeRate = 0.1),
                ),
            )
        }
    }

    @Test
    fun aChannelValidatesItsTiersToo() {
        assertFailsWith<IllegalArgumentException> {
            SalesChannel(id = "s", name = "Shopee", feeRate = 0.2, tiers = listOf(ChannelFeeTier(upToUnitPrice = 10.0, feeRate = 0.1)))
        }
    }

    @Test
    fun deductionsReachingAllTheMoneyAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ChannelFeeSchedule(feeRate = 0.5).priceLeaving(receipt = 10.0, extras = 0.0, quantity = 1, taxRate = 0.5)
        }
    }
}
