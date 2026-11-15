package cl.fuerzarank.core

import java.time.LocalDate
import kotlin.test.*

class CoreTest {
    @Test fun oneRepIsRealMaxNotEstimate() {
        val rm = StrengthCalculator.estimateOneRm(140.0, 1)
        assertEquals(140.0, rm.kg); assertFalse(rm.isEstimate)
    }
    @Test fun epleyEstimateIsFlagged() {
        val rm = StrengthCalculator.estimateOneRm(100.0, 5)
        assertEquals(100 * (1 + 5 / 30.0), rm.kg, 1e-9); assertTrue(rm.isEstimate)
    }
    @Test fun rejectsUnreliableReps() { assertFailsWith<IllegalArgumentException> { StrengthCalculator.estimateOneRm(40.0, 20) } }
    @Test fun demoBenchIsProAndMarkedDemo() {
        val pr = PrService.demoBenchPress("p1")
        assertEquals(StrengthLevel.PRO, pr.level); assertTrue(pr.isDemo)
    }
    @Test fun top3KeepsHistoryAndSortsBoth() {
        val d = LocalDate.of(2026, 1, 1)
        val all = listOf(
            PrService.create("a", "p", "press_banca", 100.0, 5, d, 80.0),
            PrService.create("b", "p", "press_banca", 110.0, 1, d, 80.0),
            PrService.create("c", "p", "press_banca", 90.0, 10, d, 80.0),
            PrService.create("d", "p", "press_banca", 60.0, 1, d, 80.0),
            PrService.create("e", "otro", "press_banca", 200.0, 1, d, 80.0)
        )
        assertEquals(listOf("b", "a", "c"), PrService.top3(all, "p", "press_banca", PrSort.LOAD).map { it.id })
        assertEquals(3, PrService.top3(all, "p", "press_banca", PrSort.ONE_RM).size)
        assertEquals(5, all.size)
    }
    @Test fun rewardOnlyOncePerKey() {
        val w = Wallet()
        assertIs<Result2.Ok>(w.earn("p", 100, "3 km", "run:2026-11-15"))
        assertEquals(Result2.Duplicate, w.earn("p", 100, "3 km", "run:2026-11-15"))
        assertEquals(100, w.balance("p"))
    }
    @Test fun purchaseChargesOnceAndChecksFunds() {
        val w = Wallet(); val item = ShopItem("gorro1", "Gorro", "ropa", 80)
        assertEquals(Result2.InsufficientFunds, w.buy("p", item))
        w.earn("p", 150, "PR", "pr:1")
        assertIs<Result2.Ok>(w.buy("p", item))
        assertEquals(Result2.AlreadyOwned, w.buy("p", item))
        assertEquals(70, w.balance("p"))
    }
    @Test fun balancesAreSeparatedPerProfile() {
        val w = Wallet(); w.earn("a", 50, "x", "k1")
        assertEquals(0, w.balance("b"))
    }
}
