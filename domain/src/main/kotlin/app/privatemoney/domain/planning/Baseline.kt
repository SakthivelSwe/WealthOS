package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money

enum class BaselineMethod {
    USER_ENTERED,
    HISTORY_MEDIAN,
    ESTIMATE_55_PERCENT
}

data class BaselineLineItems(
    val rentOrHomePaise: Long = 0L,
    val groceriesAndMealsPaise: Long = 0L,
    val transportAndCommutePaise: Long = 0L,
    val utilitiesAndMobilePaise: Long = 0L,
    val medicalAndToiletriesPaise: Long = 0L,
    val otherEssentialsPaise: Long = 0L,
) {
    val totalPaise: Long
        get() = rentOrHomePaise + groceriesAndMealsPaise + transportAndCommutePaise +
            utilitiesAndMobilePaise + medicalAndToiletriesPaise + otherEssentialsPaise
}

data class BaselineResult(
    val baselinePaise: Long,
    val method: BaselineMethod,
    val label: String,
)

object BaselineEngine {

    /**
     * Determines baseline according to priority order in Spec §4.3:
     * 1. User-entered line items if provided and > 0
     * 2. Else median of historical NEED cycles if available
     * 3. Else 55% of lowest recent salary (using exact integer roundHalfUp)
     */
    fun determineBaseline(
        userLineItems: BaselineLineItems? = null,
        recentCycleNeedTotalsPaise: List<Long> = emptyList(),
        lowestRecentSalaryPaise: Long? = null
    ): BaselineResult {
        // 1. User entered
        if (userLineItems != null && userLineItems.totalPaise > 0L) {
            return BaselineResult(
                baselinePaise = userLineItems.totalPaise,
                method = BaselineMethod.USER_ENTERED,
                label = "User entered"
            )
        }

        // 2. History median (needs at least 1 complete cycle)
        if (recentCycleNeedTotalsPaise.isNotEmpty()) {
            val sorted = recentCycleNeedTotalsPaise.sorted()
            val median = if (sorted.size % 2 == 1) {
                sorted[sorted.size / 2]
            } else {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2L
            }
            if (median > 0L) {
                return BaselineResult(
                    baselinePaise = median,
                    method = BaselineMethod.HISTORY_MEDIAN,
                    label = "Historical median (${recentCycleNeedTotalsPaise.size} cycles)"
                )
            }
        }

        // 3. 55% estimate of lowest recent salary
        if (lowestRecentSalaryPaise != null && lowestRecentSalaryPaise > 0L) {
            val estimate = Money.roundHalfUp(lowestRecentSalaryPaise * 55L, 100L)
            return BaselineResult(
                baselinePaise = estimate,
                method = BaselineMethod.ESTIMATE_55_PERCENT,
                label = "Estimate (55% of ₹${lowestRecentSalaryPaise / 100}), replace with your real number"
            )
        }

        return BaselineResult(
            baselinePaise = 0L,
            method = BaselineMethod.USER_ENTERED,
            label = "Not configured"
        )
    }
}
