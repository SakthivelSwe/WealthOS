package app.privatemoney.domain

import app.privatemoney.domain.model.NeedWant
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.Lane
import app.privatemoney.domain.planning.PlanReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanReportTest {

    @Test
    fun lanesPlanAndProjectDeterministically() {
        val lanes = PlanReport.build(
            basisIncomeMinor = 100_000,
            split = BudgetSplit.DEFAULT,
            spentByLane = mapOf(Lane.NEEDS to 30_000L, Lane.WANTS to 3_000L),
            elapsedDays = 10,
            totalDays = 30,
        ).associateBy { it.lane }

        val needs = lanes.getValue(Lane.NEEDS)
        assertEquals(50_000L, needs.progress.plannedMinor)
        assertEquals(20_000L, needs.progress.remainingMinor)
        assertEquals(90_000L, needs.progress.projectedFinalMinor)
        assertEquals(40_000L, needs.progress.varianceMinor)
        assertEquals(16_666L, needs.expectedByNowMinor)

        val wants = lanes.getValue(Lane.WANTS)
        assertEquals(30_000L, wants.progress.plannedMinor)
        assertEquals(9_000L, wants.progress.projectedFinalMinor)

        val savings = lanes.getValue(Lane.SAVINGS)
        assertEquals(20_000L, savings.progress.plannedMinor)
        assertEquals(0L, savings.progress.spentMinor)
    }

    @Test
    fun plannedLanesAlwaysSumToIncome() {
        for (income in listOf(0L, 1L, 77_777L, 3_220_001L)) {
            val total = PlanReport.build(income, BudgetSplit(45, 25, 30), emptyMap(), 1, 31).sumOf { it.progress.plannedMinor }
            assertEquals(income, total)
        }
    }

    @Test
    fun needWantMapsToLane() {
        assertEquals(Lane.NEEDS, Lane.from(NeedWant.NEED))
        assertEquals(Lane.WANTS, Lane.from(NeedWant.WANT))
        assertEquals(Lane.SAVINGS, Lane.from(NeedWant.SAVING))
        assertEquals(Lane.SAVINGS, Lane.fromName("SAVING"))
        assertNull(Lane.fromName(null))
        assertNull(Lane.fromName("bogus"))
    }
}
