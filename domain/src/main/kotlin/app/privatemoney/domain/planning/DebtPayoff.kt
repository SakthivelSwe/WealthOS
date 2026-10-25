package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money

enum class DebtStrategy {
    SNOWBALL,
    AVALANCHE
}

data class DebtLoan(
    val name: String,
    val balancePaise: Long,
    val rateBps: Int, // annual percent * 100 (e.g. 18.00% = 1800)
    val minimumPaymentPaise: Long,
) {
    init {
        require(balancePaise >= 0L) { "Balance must be non-negative" }
        require(rateBps >= 0) { "Rate BPS must be non-negative" }
        require(minimumPaymentPaise >= 0L) { "Minimum payment must be non-negative" }
    }
}

data class DebtPayoffResult(
    val strategy: DebtStrategy,
    val months: Int,
    val totalInterestPaise: Long,
    val isTerminating: Boolean = true,
)

data class DebtComparisonResult(
    val snowball: DebtPayoffResult,
    val avalanche: DebtPayoffResult,
    val interestSavedPaise: Long,
    val monthsSaved: Int,
)

object DebtPayoffEngine {

    private const val MAX_MONTHS = 1200

    /**
     * Simulates debt payoff across multiple loans with a constant monthly budget.
     * Conforms to WEALTHOS_FINAL_SPEC.md §5.9 and Appendix B.
     */
    fun payoff(
        loans: List<DebtLoan>,
        extraMonthlyPaymentPaise: Long,
        strategy: DebtStrategy
    ): DebtPayoffResult {
        if (loans.isEmpty() || loans.all { it.balancePaise == 0L }) {
            return DebtPayoffResult(strategy, 0, 0L, true)
        }

        class LoanState(
            val name: String,
            var bal: Long,
            val rate: Int,
            val min: Long
        )

        val states = loans.map { LoanState(it.name, it.balancePaise, it.rateBps, it.minimumPaymentPaise) }
        val budget = states.sumOf { it.min } + extraMonthlyPaymentPaise

        // First month interest check
        val initialMonthInterest = states.sumOf { Money.roundHalfUp(it.bal * it.rate.toLong(), 120000L) }
        if (budget <= initialMonthInterest) {
            return DebtPayoffResult(strategy, MAX_MONTHS, initialMonthInterest * MAX_MONTHS, isTerminating = false)
        }

        var months = 0
        var totalInterest = 0L

        while (states.any { it.bal > 0L }) {
            if (months >= MAX_MONTHS) {
                return DebtPayoffResult(strategy, MAX_MONTHS, totalInterest, isTerminating = false)
            }
            months++

            // Accrue monthly interest
            for (l in states) {
                if (l.bal > 0L) {
                    val interest = Money.roundHalfUp(l.bal * l.rate.toLong(), 120000L)
                    l.bal += interest
                    totalInterest += interest
                }
            }

            var avail = budget

            // Pay minimums
            for (l in states) {
                if (l.bal > 0L) {
                    val p = minOf(l.min, l.bal)
                    l.bal -= p
                    avail -= p
                }
            }

            // Apply remaining budget to target loans in strategy order
            val active = states.filter { it.bal > 0L }.toMutableList()
            if (strategy == DebtStrategy.SNOWBALL) {
                active.sortWith(compareBy<LoanState> { it.bal }.thenBy { it.name })
            } else {
                active.sortWith(compareByDescending<LoanState> { it.rate }.thenBy { it.bal }.thenBy { it.name })
            }

            for (l in active) {
                if (avail <= 0L) break
                val p = minOf(avail, l.bal)
                l.bal -= p
                avail -= p
            }
        }

        return DebtPayoffResult(strategy, months, totalInterest, isTerminating = true)
    }

    fun compareStrategies(
        loans: List<DebtLoan>,
        extraMonthlyPaymentPaise: Long
    ): DebtComparisonResult {
        val snowball = payoff(loans, extraMonthlyPaymentPaise, DebtStrategy.SNOWBALL)
        val avalanche = payoff(loans, extraMonthlyPaymentPaise, DebtStrategy.AVALANCHE)
        val interestSaved = snowball.totalInterestPaise - avalanche.totalInterestPaise
        val monthsSaved = snowball.months - avalanche.months
        return DebtComparisonResult(
            snowball = snowball,
            avalanche = avalanche,
            interestSavedPaise = interestSaved,
            monthsSaved = monthsSaved
        )
    }
}
