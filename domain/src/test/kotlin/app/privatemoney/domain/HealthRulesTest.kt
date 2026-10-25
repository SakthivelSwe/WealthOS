package app.privatemoney.domain

import app.privatemoney.domain.planning.ForensicsEngine
import app.privatemoney.domain.planning.HealthRulesEngine
import app.privatemoney.domain.planning.HealthStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRulesTest {

    @Test
    fun testAppendixA6EmergencyTiers() {
        // Target: 7 * ₹17,710 = ₹1,23,970 (12,397,000 paise)
        val target = 12397000L
        val tiers = HealthRulesEngine.calculateEmergencyTiers(target)

        assertEquals(2479400L, tiers.instantCashPaise)    // ₹24,794
        assertEquals(6198500L, tiers.autoSweepFdRdPaise)  // ₹61,985
        assertEquals(3719100L, tiers.liquidFundGoldPaise) // ₹37,191
        assertEquals(target, tiers.instantCashPaise + tiers.autoSweepFdRdPaise + tiers.liquidFundGoldPaise)
    }

    @Test
    fun testAppendixA6EmiBoundaries() {
        // ₹4,600 on ₹32,200 passes 1/7 rule
        assertEquals(HealthStatus.GREEN, HealthRulesEngine.evaluateEmi(460000L, 3220000L))
        // ₹4,601 fails 1/7 rule, but 4601 * 100 <= 32200 * 30 (460100 <= 966000) so AMBER
        assertEquals(HealthStatus.AMBER, HealthRulesEngine.evaluateEmi(460100L, 3220000L))
        // > 30% is RED
        assertEquals(HealthStatus.RED, HealthRulesEngine.evaluateEmi(1000000L, 3220000L))
    }

    @Test
    fun testSavingsRateAndRunwayBoundaries() {
        // Savings rate: green >= 20%, amber >= 7%, else red
        assertEquals(HealthStatus.GREEN, HealthRulesEngine.evaluateSavingsRate(200000L, 1000000L))
        assertEquals(HealthStatus.AMBER, HealthRulesEngine.evaluateSavingsRate(150000L, 1000000L))
        assertEquals(HealthStatus.AMBER, HealthRulesEngine.evaluateSavingsRate(70000L, 1000000L))
        assertEquals(HealthStatus.RED, HealthRulesEngine.evaluateSavingsRate(60000L, 1000000L))

        // Runway: green >= 7, amber >= 3, else red
        assertEquals(HealthStatus.GREEN, HealthRulesEngine.evaluateRunway(700000L, 100000L))
        assertEquals(HealthStatus.AMBER, HealthRulesEngine.evaluateRunway(600000L, 100000L))
        assertEquals(HealthStatus.AMBER, HealthRulesEngine.evaluateRunway(300000L, 100000L))
        assertEquals(HealthStatus.RED, HealthRulesEngine.evaluateRunway(200000L, 100000L))
    }

    @Test
    fun testAppendixA6ForensicsGoldenCheck() {
        // ₹31,400 over 17 days -> 184,705 paise per day
        val debits = 3140000L // ₹31,400
        val days = 17
        val report = ForensicsEngine.analyze(
            openingBalancePaise = 3220000L,
            totalCreditsPaise = 0L,
            totalDebitsPaise = debits,
            actualClosingPaise = 80000L, // ₹800 left
            daysElapsed = days
        )

        assertEquals(184705L, report.averageBurnPerDayPaise)
        assertEquals(80000L, report.expectedClosingPaise)
        assertTrue(report.isReconciled)
        assertEquals(0L, report.discrepancyPaise)
    }
}
