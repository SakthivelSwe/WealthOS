package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money

data class SipMilestoneResult(
    val targetPaise: Long,
    val targetName: String,
    val expectedMonths12Pct: Int?,
    val marginOfSafetyMonths8Pct: Int?,
)

data class SipPoint(
    val month: Int,
    val investedPaise: Long,
    val expectedValuePaise: Long,
    val marginOfSafetyValuePaise: Long,
)

object SipProjectionEngine {

    /**
     * Determines the month in which the SIP portfolio reaches the target amount.
     * Conforms to WEALTHOS_FINAL_SPEC.md §5.8 and Appendix B.
     */
    fun monthsToTarget(
        startSipPaise: Long,
        annualStepUpPct: Int,
        annualReturnPct: Int,
        targetPaise: Long,
        capMonths: Int = 600
    ): Int? {
        require(startSipPaise > 0L) { "SIP amount must be positive" }
        require(annualReturnPct >= 0) { "Annual return must be non-negative" }
        require(annualStepUpPct >= 0) { "Step-up percentage must be non-negative" }

        var currentSip = startSipPaise
        var balance = 0L

        for (m in 1..capMonths) {
            balance = Money.roundHalfUp(
                (balance + currentSip) * (1200L + annualReturnPct.toLong()),
                1200L
            )
            if (balance >= targetPaise) return m
            if (m % 12 == 0) {
                currentSip = Money.roundHalfUp(
                    currentSip * (100L + annualStepUpPct.toLong()),
                    100L
                )
            }
        }
        return null
    }

    /**
     * Projects the dual-curve (expected e.g. 12% vs margin of safety e.g. 8%) trajectory.
     */
    fun projectTrajectory(
        startSipPaise: Long,
        annualStepUpPct: Int,
        months: Int = 360,
        expectedReturnPct: Int = 12,
        marginOfSafetyReturnPct: Int = 8
    ): List<SipPoint> {
        val points = mutableListOf<SipPoint>()
        var sipExpected = startSipPaise
        var sipSafety = startSipPaise
        var balExpected = 0L
        var balSafety = 0L
        var cumulativeInvested = 0L

        for (m in 1..months) {
            cumulativeInvested += sipExpected
            balExpected = Money.roundHalfUp(
                (balExpected + sipExpected) * (1200L + expectedReturnPct.toLong()),
                1200L
            )
            balSafety = Money.roundHalfUp(
                (balSafety + sipSafety) * (1200L + marginOfSafetyReturnPct.toLong()),
                1200L
            )

            points.add(
                SipPoint(
                    month = m,
                    investedPaise = cumulativeInvested,
                    expectedValuePaise = balExpected,
                    marginOfSafetyValuePaise = balSafety
                )
            )

            if (m % 12 == 0) {
                sipExpected = Money.roundHalfUp(sipExpected * (100L + annualStepUpPct.toLong()), 100L)
                sipSafety = Money.roundHalfUp(sipSafety * (100L + annualStepUpPct.toLong()), 100L)
            }
        }
        return points
    }
}
