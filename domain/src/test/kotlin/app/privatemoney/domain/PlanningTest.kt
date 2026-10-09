package app.privatemoney.domain

import app.privatemoney.domain.planning.BudgetAllocator
import app.privatemoney.domain.planning.BudgetEngine
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.GoalEngine
import app.privatemoney.domain.planning.LoanEngine
import app.privatemoney.domain.planning.SafeToSpendEngine
import app.privatemoney.domain.planning.SafeToSpendInput
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanningTest {

    @Test
    fun safeToSpendExposesEveryTerm() {
        val input = SafeToSpendInput(100_000, 20_000, 10_000, 5_000, 15_000, 10_000)
        val result = SafeToSpendEngine.calculate(input, LocalDate.of(2026, 10, 22)) // 10 days left
        assertEquals(50_000L, result.committedMinor)
        assertEquals(40_000L, result.discretionaryMinor)
        assertEquals(10, result.daysRemainingInMonth)
        assertEquals(4_000L, result.safeTodayMinor)
        assertEquals(28_000L, result.safeThisWeekMinor)
        assertEquals(0L, result.shortfallMinor)
    }

    @Test
    fun safeToSpendReportsShortfallAndNeverNegativeSpend() {
        val input = SafeToSpendInput(10_000, 20_000, 0, 0, 0, 0)
        val result = SafeToSpendEngine.calculate(input, LocalDate.of(2026, 10, 1))
        assertEquals(10_000L, result.shortfallMinor)
        assertEquals(0L, result.safeTodayMinor)
    }

    @Test
    fun budgetSplitMustTotalHundred() {
        assertFailsWith<IllegalArgumentException> { BudgetSplit(50, 30, 30) }
    }

    @Test
    fun allocationAlwaysSumsToIncome() {
        for (income in listOf(0L, 1L, 99L, 3_220_001L, 9_999_999L)) {
            for (split in listOf(BudgetSplit.DEFAULT, BudgetSplit(60, 20, 20), BudgetSplit(45, 25, 30))) {
                val a = BudgetAllocator.allocate(income, split)
                assertEquals(income, a.needsMinor + a.wantsMinor + a.savingsMinor)
            }
        }
    }

    @Test
    fun budgetProjection() {
        val p = BudgetEngine.progress(plannedMinor = 3_000_000, spentMinor = 1_000_000, elapsedDays = 10, totalDays = 30)
        assertEquals(2_000_000L, p.remainingMinor)
        assertEquals(100_000L, p.burnPerDayMinor)
        assertEquals(3_000_000L, p.projectedFinalMinor)
        assertEquals(0L, p.varianceMinor)
    }

    @Test
    fun goalMonthlyRequiredRoundsUp() {
        val plan = GoalEngine.plan(1_500_000, 250_000, LocalDate.of(2027, 1, 31), LocalDate.of(2026, 10, 7), 0)
        assertEquals(3, plan.remainingPeriods)
        assertEquals(416_667L, plan.monthlyRequiredMinor) // ceil(1_250_000 / 3)
        assertNull(plan.projectedCompletion)
    }

    @Test
    fun goalProjectionOnTrack() {
        val plan = GoalEngine.plan(1_500_000, 250_000, LocalDate.of(2027, 1, 31), LocalDate.of(2026, 10, 7), 500_000)
        assertEquals(LocalDate.of(2027, 1, 7), plan.projectedCompletion)
        assertTrue(plan.onTrack == true)
    }

    @Test
    fun completedGoalNeedsNothing() {
        val plan = GoalEngine.plan(100, 200, LocalDate.of(2027, 1, 1), LocalDate.of(2026, 10, 7), 0)
        assertEquals(0L, plan.monthlyRequiredMinor)
    }

    @Test
    fun emiKnownValue() {
        // 10,00,000 at 10% for 12 months: EMI is about 87,915.89
        assertEquals(8_791_589L, LoanEngine.emi(100_000_000, BigDecimal("10"), 12))
    }

    @Test
    fun zeroRateEmiIsSimpleDivision() {
        assertEquals(1_000L, LoanEngine.emi(12_000, BigDecimal.ZERO, 12))
    }

    @Test
    fun payoffRejectsPaymentBelowInterest() {
        assertNull(LoanEngine.payoff(100_000_000, BigDecimal("12"), 500_000))
    }

    @Test
    fun extraPaymentReducesMonthsAndInterest() {
        val rate = BigDecimal("10")
        val base = LoanEngine.emi(100_000_000, rate, 60)
        val normal = LoanEngine.payoff(100_000_000, rate, base)!!
        val extra = LoanEngine.payoff(100_000_000, rate, base + 1_000_000)!!
        assertTrue(extra.months < normal.months)
        assertTrue(extra.totalInterestMinor < normal.totalInterestMinor)
        assertFalse(normal.months > 61)
    }
}
