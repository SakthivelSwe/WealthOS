package app.privatemoney.domain

import app.privatemoney.domain.planning.SipProjectionEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class SipProjectionTest {

    @Test
    fun testAppendixA3SipMilestones() {
        // Start SIP ₹10,000 (1,000,000 paise), 10% annual step-up
        val startSip = 1000000L
        val stepUpPct = 10

        // 12% expected return milestones
        // ₹10L (100,000,000 paise) -> month 61
        val m10L_12 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 12, 100000000L)
        assertEquals(61, m10L_12)

        // ₹50L (500,000,000 paise) -> month 144
        val m50L_12 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 12, 500000000L)
        assertEquals(144, m50L_12)

        // ₹1Cr (1,000,000,000 paise) -> month 190
        val m1Cr_12 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 12, 1000000000L)
        assertEquals(190, m1Cr_12)

        // ₹1.5Cr (1,500,000,000 paise) -> month 219
        val m1_5Cr_12 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 12, 1500000000L)
        assertEquals(219, m1_5Cr_12)

        // 8% margin-of-safety milestones
        // ₹10L -> month 65
        val m10L_8 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 8, 100000000L)
        assertEquals(65, m10L_8)

        // ₹50L -> month 162
        val m50L_8 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 8, 500000000L)
        assertEquals(162, m50L_8)

        // ₹1Cr -> month 216
        val m1Cr_8 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 8, 1000000000L)
        assertEquals(216, m1Cr_8)

        // ₹1.5Cr -> month 251
        val m1_5Cr_8 = SipProjectionEngine.monthsToTarget(startSip, stepUpPct, 8, 1500000000L)
        assertEquals(251, m1_5Cr_8)
    }
}
