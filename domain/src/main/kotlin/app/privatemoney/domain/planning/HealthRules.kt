package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money

enum class HealthStatus {
    GREEN,
    AMBER,
    RED
}

data class EmergencyTiers(
    val instantCashPaise: Long,    // 20%
    val autoSweepFdRdPaise: Long,  // 50%
    val liquidFundGoldPaise: Long, // 30% (remainder)
)

data class VitalSigns(
    val netWorthPaise: Long,
    val cashFlowPaise: Long,
    val runwayMonths: Int,
    val freedomYears: Long, // investedAssets / (baseline * 12)
)

data class HealthAuditReport(
    val emiStatus: HealthStatus,
    val savingsRateStatus: HealthStatus,
    val emergencyRunwayStatus: HealthStatus,
    val emiToIncomePctOneDecimal: String,
    val savingsRatePct: Int,
    val runwayMonths: Int,
    val emergencyTiers: EmergencyTiers,
    val vitalSigns: VitalSigns,
    val recommendedTermCoverPaise: Long,
)

object HealthRulesEngine {

    /**
     * Evaluates EMI health conforming to §5.6.
     * All comparisons use pure integer math.
     * green if emi * 7 <= salary; amber if emi * 100 <= salary * 30; else red.
     */
    fun evaluateEmi(emiPaise: Long, salaryPaise: Long): HealthStatus {
        if (salaryPaise <= 0L) return if (emiPaise == 0L) HealthStatus.GREEN else HealthStatus.RED
        if (emiPaise * 7L <= salaryPaise) return HealthStatus.GREEN
        if (emiPaise * 100L <= salaryPaise * 30L) return HealthStatus.AMBER
        return HealthStatus.RED
    }

    /**
     * Evaluates Savings Rate health.
     * green >= 20%, amber >= 7%, else red.
     */
    fun evaluateSavingsRate(savingsPaise: Long, salaryPaise: Long): HealthStatus {
        if (salaryPaise <= 0L) return HealthStatus.RED
        val pct = Money.calculatePercentage(savingsPaise, salaryPaise)
        return when {
            pct >= 20 -> HealthStatus.GREEN
            pct >= 7 -> HealthStatus.AMBER
            else -> HealthStatus.RED
        }
    }

    /**
     * Evaluates Emergency Runway health.
     * green >= 7 months, amber >= 3 months, else red.
     */
    fun evaluateRunway(liquidEmergencyPaise: Long, monthlyBaselinePaise: Long): HealthStatus {
        if (monthlyBaselinePaise <= 0L) return HealthStatus.GREEN
        val months = (liquidEmergencyPaise / monthlyBaselinePaise).toInt()
        return when {
            months >= 7 -> HealthStatus.GREEN
            months >= 3 -> HealthStatus.AMBER
            else -> HealthStatus.RED
        }
    }

    /**
     * Calculates the three-tier emergency distribution on the target amount.
     * 20% instant-access cash (floor), 50% auto-sweep FD/RD (floor), 30% liquid fund or gold (remainder).
     */
    fun calculateEmergencyTiers(targetPaise: Long): EmergencyTiers {
        require(targetPaise >= 0L) { "Emergency target must be non-negative" }
        val instant = (targetPaise * 20L) / 100L
        val autoSweep = (targetPaise * 50L) / 100L
        val liquidGold = targetPaise - instant - autoSweep
        return EmergencyTiers(
            instantCashPaise = instant,
            autoSweepFdRdPaise = autoSweep,
            liquidFundGoldPaise = liquidGold
        )
    }

    /**
     * Calculates vital signs:
     * - netWorth = assets - liabilities
     * - cashFlow = inflow - outflow
     * - runwayMonths = liquid / baseline
     * - freedomYears = investedAssets / (baseline * 12)
     */
    fun calculateVitalSigns(
        totalAssetsPaise: Long,
        totalLiabilitiesPaise: Long,
        cycleInflowsPaise: Long,
        cycleOutflowsPaise: Long,
        liquidPaise: Long,
        investedAssetsPaise: Long,
        monthlyBaselinePaise: Long
    ): VitalSigns {
        val netWorth = totalAssetsPaise - totalLiabilitiesPaise
        val cashFlow = cycleInflowsPaise - cycleOutflowsPaise
        val runway = if (monthlyBaselinePaise > 0L) (liquidPaise / monthlyBaselinePaise).toInt() else 0
        val annualBaseline = monthlyBaselinePaise * 12L
        val freedomYears = if (annualBaseline > 0L) investedAssetsPaise / annualBaseline else 0L

        return VitalSigns(
            netWorthPaise = netWorth,
            cashFlowPaise = cashFlow,
            runwayMonths = runway,
            freedomYears = freedomYears
        )
    }
}
