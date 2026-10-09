package app.privatemoney.domain.planning

import app.privatemoney.domain.model.NeedWant
import java.math.BigDecimal
import java.math.RoundingMode

enum class Lane {
    NEEDS, WANTS, SAVINGS;

    companion object {
        fun from(needWant: NeedWant): Lane = when (needWant) {
            NeedWant.NEED -> NEEDS
            NeedWant.WANT -> WANTS
            NeedWant.SAVING -> SAVINGS
        }

        fun fromName(name: String?): Lane? = name?.let { n -> NeedWant.entries.firstOrNull { it.name == n }?.let(::from) }
    }
}

data class LaneStatus(
    val lane: Lane,
    val progress: BudgetProgress,
    /** Where a perfectly even pace would be today. Drawn as a marker on the lane. */
    val expectedByNowMinor: Long,
)

object PlanReport {
    /**
     * Builds the three budget lanes. Planned amounts come from [BudgetAllocator] on [basisIncomeMinor];
     * everything is integer arithmetic. Savings is a target rather than a limit, so callers must word
     * "projected below planned" differently for [Lane.SAVINGS].
     */
    fun build(
        basisIncomeMinor: Long,
        split: BudgetSplit,
        spentByLane: Map<Lane, Long>,
        elapsedDays: Int,
        totalDays: Int,
    ): List<LaneStatus> {
        val planned = BudgetAllocator.allocate(basisIncomeMinor, split)
        val plannedByLane = mapOf(Lane.NEEDS to planned.needsMinor, Lane.WANTS to planned.wantsMinor, Lane.SAVINGS to planned.savingsMinor)
        return Lane.entries.map { lane ->
            val plan = plannedByLane.getValue(lane)
            val expected = BigDecimal(plan).multiply(BigDecimal(elapsedDays)).divide(BigDecimal(totalDays), 0, RoundingMode.FLOOR).longValueExact()
            LaneStatus(lane, BudgetEngine.progress(plan, spentByLane[lane] ?: 0L, elapsedDays, totalDays), expected)
        }
    }
}
