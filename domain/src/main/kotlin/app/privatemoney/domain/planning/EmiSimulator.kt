package app.privatemoney.domain.planning

import app.privatemoney.domain.model.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class EmiSimulationResult(
    val principalPaise: Long,
    val annualPercent: BigDecimal,
    val tenureMonths: Int,
    val emiPaise: Long,
    val totalInterestPaise: Long,
    val totalRepaymentPaise: Long,
    val finalPaymentPaise: Long,
)

data class EmiAffordabilityCheck(
    val emiPaise: Long,
    val salaryPaise: Long,
    val baselinePaise: Long,
    val liquidEmergencyPaise: Long,
    val passesOneSeventhRule: Boolean, // emi * 7 <= salary
    val passesThirtyPercentCap: Boolean, // emi * 100 <= salary * 30
    val emiToIncomePctOneDecimal: String,
    val jobLossRunwayMonths: Int,
)

object EmiSimulatorEngine {

    private val MC = MathContext.DECIMAL128

    /**
     * Calculates reducing-balance EMI and amortisation conforming to WEALTHOS_FINAL_SPEC.md §5.10.
     * The last payment absorbs rounding.
     */
    fun calculate(
        principalPaise: Long,
        annualPercent: BigDecimal,
        tenureMonths: Int
    ): EmiSimulationResult {
        require(principalPaise > 0L) { "Principal must be positive" }
        require(tenureMonths > 0) { "Tenure must be positive" }
        require(annualPercent.signum() >= 0) { "Annual interest rate must be non-negative" }

        val p = BigDecimal(principalPaise)

        if (annualPercent.signum() == 0) {
            val emi = Money.roundHalfUp(principalPaise, tenureMonths.toLong())
            val totalRepayment = principalPaise
            val finalPayment = principalPaise - (emi * (tenureMonths - 1))
            return EmiSimulationResult(
                principalPaise = principalPaise,
                annualPercent = annualPercent,
                tenureMonths = tenureMonths,
                emiPaise = emi,
                totalInterestPaise = 0L,
                totalRepaymentPaise = totalRepayment,
                finalPaymentPaise = finalPayment
            )
        }

        // r = annualPercent / 1200
        val r = annualPercent.divide(BigDecimal(1200), MC)
        val growth = BigDecimal.ONE.add(r).pow(tenureMonths, MC)
        val emiExact = p.multiply(r, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC)
        val emi = emiExact.setScale(0, RoundingMode.HALF_UP).longValueExact()

        // Amortisation schedule to determine exact interest and final payment
        var balance = principalPaise
        var totalInterest = 0L
        var finalPayment = emi

        for (m in 1..tenureMonths) {
            val interestExact = BigDecimal(balance).multiply(r, MC)
            val interest = interestExact.setScale(0, RoundingMode.HALF_UP).longValueExact()
            totalInterest += interest

            if (m == tenureMonths) {
                finalPayment = balance + interest
                balance = 0L
            } else {
                val principalPaid = emi - interest
                balance -= principalPaid
            }
        }

        val totalRepayment = principalPaise + totalInterest

        return EmiSimulationResult(
            principalPaise = principalPaise,
            annualPercent = annualPercent,
            tenureMonths = tenureMonths,
            emiPaise = emi,
            totalInterestPaise = totalInterest,
            totalRepaymentPaise = totalRepayment,
            finalPaymentPaise = finalPayment
        )
    }

    /**
     * Evaluates EMI affordability and job loss runway.
     */
    fun evaluateAffordability(
        emiPaise: Long,
        salaryPaise: Long,
        baselinePaise: Long,
        liquidEmergencyPaise: Long
    ): EmiAffordabilityCheck {
        val passesOneSeventh = emiPaise * 7L <= salaryPaise
        val passesThirtyPercent = emiPaise * 100L <= salaryPaise * 30L

        val ratioBps = if (salaryPaise > 0L) (emiPaise * 1000L) / salaryPaise else 0L
        val whole = ratioBps / 10L
        val dec = ratioBps % 10L
        val emiToIncomeStr = "$whole.$dec%"

        val monthlyBurn = baselinePaise + emiPaise
        val runwayMonths = if (monthlyBurn > 0L) (liquidEmergencyPaise / monthlyBurn).toInt() else 0

        return EmiAffordabilityCheck(
            emiPaise = emiPaise,
            salaryPaise = salaryPaise,
            baselinePaise = baselinePaise,
            liquidEmergencyPaise = liquidEmergencyPaise,
            passesOneSeventhRule = passesOneSeventh,
            passesThirtyPercentCap = passesThirtyPercent,
            emiToIncomePctOneDecimal = emiToIncomeStr,
            jobLossRunwayMonths = runwayMonths
        )
    }
}
