package app.privatemoney.domain

import app.privatemoney.domain.planning.EmiSimulatorEngine
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmiSimulatorTest {

    @Test
    fun testAppendixA5EmiSimulatorGoldenValues() {
        // ₹5,00,000 at 12% for 36 months
        val principal = 50000000L
        val annualRate = BigDecimal("12.0")
        val months = 36

        val res = EmiSimulatorEngine.calculate(principal, annualRate, months)
        assertEquals(1660715L, res.emiPaise)
        assertEquals(9785763L, res.totalInterestPaise)
        assertEquals(59785763L, res.totalRepaymentPaise)
    }

    @Test
    fun testAppendixA6EmiAffordabilityBoundary() {
        // EMI test: ₹4,600 on ₹32,200 passes the 1/7 rule (4600*7 = 32200 <= 32200); ₹4,601 does not.
        val salary = 3220000L
        val baseline = 1771000L
        val emg = 12397000L

        val passCheck = EmiSimulatorEngine.evaluateAffordability(460000L, salary, baseline, emg)
        assertTrue(passCheck.passesOneSeventhRule)

        val failCheck = EmiSimulatorEngine.evaluateAffordability(460100L, salary, baseline, emg)
        assertFalse(failCheck.passesOneSeventhRule)
    }
}
