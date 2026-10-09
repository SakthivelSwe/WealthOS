package app.privatemoney.domain.planning

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Deterministic Safe-to-Spend. Every term is exposed so the UI can explain the result. */
data class SafeToSpendInput(
    val liquidBalanceMinor: Long,
    val upcomingMandatoryBillsMinor: Long,
    val expectedDebtPaymentsMinor: Long,
    val plannedGoalContributionsMinor: Long,
    val creditCardDueMinor: Long,
    val safetyBufferMinor: Long,
)

data class SafeToSpendBreakdown(
    val input: SafeToSpendInput,
    val committedMinor: Long,
    /** Liquid minus committed minus buffer. May be negative, which signals a shortfall. */
    val discretionaryMinor: Long,
    val safeThisMonthMinor: Long,
    val safeTodayMinor: Long,
    val safeThisWeekMinor: Long,
    val shortfallMinor: Long,
    val daysRemainingInMonth: Int,
)

object SafeToSpendEngine {

    fun calculate(input: SafeToSpendInput, today: LocalDate): SafeToSpendBreakdown {
        require(listOf(
            input.upcomingMandatoryBillsMinor, input.expectedDebtPaymentsMinor,
            input.plannedGoalContributionsMinor, input.creditCardDueMinor, input.safetyBufferMinor,
        ).all { it >= 0 }) { "Commitments must not be negative" }

        val committed = input.upcomingMandatoryBillsMinor +
            input.expectedDebtPaymentsMinor +
            input.plannedGoalContributionsMinor +
            input.creditCardDueMinor
        val discretionary = Math.subtractExact(Math.subtractExact(input.liquidBalanceMinor, committed), input.safetyBufferMinor)
        val safeMonth = maxOf(0L, discretionary)
        val daysLeft = YearMonth.from(today).lengthOfMonth() - today.dayOfMonth + 1
        val perDay = safeMonth / daysLeft
        val week = perDay * minOf(7, daysLeft)
        return SafeToSpendBreakdown(
            input = input,
            committedMinor = committed,
            discretionaryMinor = discretionary,
            safeThisMonthMinor = safeMonth,
            safeTodayMinor = perDay,
            safeThisWeekMinor = week,
            shortfallMinor = if (discretionary < 0) -discretionary else 0L,
            daysRemainingInMonth = daysLeft,
        )
    }
}

/** Needs / Wants / Savings split in whole percent. A planning framework, user-editable. */
data class BudgetSplit(val needsPct: Int, val wantsPct: Int, val savingsPct: Int) {
    init {
        require(needsPct >= 0 && wantsPct >= 0 && savingsPct >= 0) { "Percentages must be non-negative" }
        require(needsPct + wantsPct + savingsPct == 100) { "Split must total 100" }
    }

    companion object {
        val DEFAULT = BudgetSplit(50, 30, 20)
    }
}

data class LaneAllocation(val needsMinor: Long, val wantsMinor: Long, val savingsMinor: Long)

object BudgetAllocator {
    /** Floors needs and wants; savings takes the remainder so allocations always sum to income. */
    fun allocate(incomeMinor: Long, split: BudgetSplit): LaneAllocation {
        require(incomeMinor >= 0) { "Income must be non-negative" }
        val needs = BigDecimal(incomeMinor).multiply(BigDecimal(split.needsPct)).divide(HUNDRED, 0, RoundingMode.FLOOR).longValueExact()
        val wants = BigDecimal(incomeMinor).multiply(BigDecimal(split.wantsPct)).divide(HUNDRED, 0, RoundingMode.FLOOR).longValueExact()
        return LaneAllocation(needs, wants, incomeMinor - needs - wants)
    }

    private val HUNDRED = BigDecimal(100)
}

data class BudgetProgress(
    val plannedMinor: Long,
    val spentMinor: Long,
    val remainingMinor: Long,
    val burnPerDayMinor: Long,
    val projectedFinalMinor: Long,
    val varianceMinor: Long,
)

object BudgetEngine {
    /**
     * @param elapsedDays days elapsed including today (>= 1)
     * @param totalDays length of the budget period in days
     */
    fun progress(plannedMinor: Long, spentMinor: Long, elapsedDays: Int, totalDays: Int): BudgetProgress {
        require(plannedMinor >= 0 && spentMinor >= 0) { "Amounts must be non-negative" }
        require(totalDays >= 1 && elapsedDays in 1..totalDays) { "Invalid period" }
        val spent = BigDecimal(spentMinor)
        val burn = spent.divide(BigDecimal(elapsedDays), 0, RoundingMode.HALF_UP).longValueExact()
        val projected = spent.multiply(BigDecimal(totalDays)).divide(BigDecimal(elapsedDays), 0, RoundingMode.HALF_UP).longValueExact()
        return BudgetProgress(
            plannedMinor = plannedMinor,
            spentMinor = spentMinor,
            remainingMinor = plannedMinor - spentMinor,
            burnPerDayMinor = burn,
            projectedFinalMinor = projected,
            varianceMinor = projected - plannedMinor,
        )
    }
}

data class GoalPlan(
    val remainingMinor: Long,
    val remainingPeriods: Int,
    val monthlyRequiredMinor: Long,
    val projectedCompletion: LocalDate?,
    val onTrack: Boolean?,
)

object GoalEngine {
    /** monthlyRequired = ceil(remaining / remainingPeriods); periods are whole calendar months, at least 1. */
    fun plan(
        targetMinor: Long,
        currentMinor: Long,
        targetDate: LocalDate,
        today: LocalDate,
        plannedMonthlyContributionMinor: Long,
    ): GoalPlan {
        require(targetMinor >= 0 && currentMinor >= 0 && plannedMonthlyContributionMinor >= 0) { "Amounts must be non-negative" }
        val remaining = maxOf(0L, targetMinor - currentMinor)
        val months = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1)).toInt()
        val periods = maxOf(1, months)
        val required = if (remaining == 0L) 0L else (remaining + periods - 1) / periods
        val projected = when {
            remaining == 0L -> today
            plannedMonthlyContributionMinor == 0L -> null
            else -> today.plusMonths(((remaining + plannedMonthlyContributionMinor - 1) / plannedMonthlyContributionMinor))
        }
        val onTrack = projected?.let { !it.isAfter(targetDate) }
        return GoalPlan(remaining, periods, required, projected, onTrack)
    }
}

data class PayoffResult(val months: Int, val totalInterestMinor: Long)

object LoanEngine {
    private val MC = MathContext.DECIMAL128
    private const val MAX_MONTHS = 1200

    private fun monthlyRate(annualPercent: BigDecimal): BigDecimal =
        annualPercent.divide(BigDecimal(1200), MC)

    /** Standard reducing-balance EMI, rounded half-up to minor units. */
    fun emi(principalMinor: Long, annualPercent: BigDecimal, months: Int): Long {
        require(principalMinor > 0 && months > 0) { "Invalid loan" }
        require(annualPercent.signum() >= 0) { "Rate must be non-negative" }
        val p = BigDecimal(principalMinor)
        if (annualPercent.signum() == 0) return p.divide(BigDecimal(months), 0, RoundingMode.HALF_UP).longValueExact()
        val r = monthlyRate(annualPercent)
        val growth = BigDecimal.ONE.add(r).pow(months, MC)
        val emi = p.multiply(r, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC)
        return emi.setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    /**
     * Simulates payoff with a fixed monthly payment. Returns null when the payment never
     * covers interest (or the horizon exceeds 100 years).
     */
    fun payoff(principalMinor: Long, annualPercent: BigDecimal, paymentMinor: Long): PayoffResult? {
        require(principalMinor >= 0 && paymentMinor > 0) { "Invalid loan" }
        val r = monthlyRate(annualPercent)
        var balance = principalMinor
        var interestTotal = 0L
        var months = 0
        while (balance > 0) {
            if (months >= MAX_MONTHS) return null
            val interest = BigDecimal(balance).multiply(r, MC).setScale(0, RoundingMode.HALF_UP).longValueExact()
            if (paymentMinor <= interest) return null
            val due = balance + interest
            val paid = minOf(paymentMinor, due)
            balance = due - paid
            interestTotal += interest
            months++
        }
        return PayoffResult(months, interestTotal)
    }
}
