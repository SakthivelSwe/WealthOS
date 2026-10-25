package app.privatemoney.domain

import app.privatemoney.domain.planning.AllocationEngine
import app.privatemoney.domain.planning.FrameworkDefinition
import app.privatemoney.domain.planning.FrameworkType
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllocationTest {

    private fun findVectorsDir(): File {
        val candidates = listOf(
            File("shared-test-vectors"),
            File("../shared-test-vectors"),
            File("../../shared-test-vectors")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate shared-test-vectors directory")
    }

    @Test
    fun testAppendixA1AndA2GoldenCases() {
        // A.1 Six-jar allocation across salaries
        // 25,000
        val r25k = AllocationEngine.allocate(
            salaryPaise = 2500000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1771000L, r25k.jars["NEEDS"])
        assertEquals(250000L, r25k.jars["FREEDOM_SIP"])
        assertEquals(104000L, r25k.jars["EDUCATION"])
        assertEquals(0L, r25k.jars["PLAY"])
        assertEquals(250000L, r25k.jars["LONG_TERM"])
        assertEquals(125000L, r25k.jars["GIVING"])
        assertEquals(0L, r25k.dailySafePaise)
        assertEquals(0L, r25k.weeklySafePaise)
        assertEquals(listOf("SPEND_JAR_EMPTY"), r25k.flags)

        // 32,200
        val r32k = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1771000L, r32k.jars["NEEDS"])
        assertEquals(322000L, r32k.jars["FREEDOM_SIP"])
        assertEquals(322000L, r32k.jars["EDUCATION"])
        assertEquals(322000L, r32k.jars["PLAY"])
        assertEquals(322000L, r32k.jars["LONG_TERM"])
        assertEquals(161000L, r32k.jars["GIVING"])
        assertEquals(10733L, r32k.dailySafePaise)
        assertEquals(75133L, r32k.weeklySafePaise)
        assertEquals(emptyList(), r32k.flags)

        // 35,000
        val r35k = AllocationEngine.allocate(
            salaryPaise = 3500000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, r35k.jars["NEEDS"])
        assertEquals(350000L, r35k.jars["FREEDOM_SIP"])
        assertEquals(350000L, r35k.jars["EDUCATION"])
        assertEquals(350000L, r35k.jars["PLAY"])
        assertEquals(354000L, r35k.jars["LONG_TERM"])
        assertEquals(175000L, r35k.jars["GIVING"])
        assertEquals(11666L, r35k.dailySafePaise)
        assertEquals(81666L, r35k.weeklySafePaise)
        assertEquals(emptyList(), r35k.flags)

        // 40,000
        val r40k = AllocationEngine.allocate(
            salaryPaise = 4000000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, r40k.jars["NEEDS"])
        assertEquals(400000L, r40k.jars["FREEDOM_SIP"])
        assertEquals(400000L, r40k.jars["EDUCATION"])
        assertEquals(400000L, r40k.jars["PLAY"])
        assertEquals(679000L, r40k.jars["LONG_TERM"])
        assertEquals(200000L, r40k.jars["GIVING"])
        assertEquals(13333L, r40k.dailySafePaise)
        assertEquals(93333L, r40k.weeklySafePaise)
        assertEquals(emptyList(), r40k.flags)

        // 45,000
        val r45k = AllocationEngine.allocate(
            salaryPaise = 4500000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, r45k.jars["NEEDS"])
        assertEquals(450000L, r45k.jars["FREEDOM_SIP"])
        assertEquals(450000L, r45k.jars["EDUCATION"])
        assertEquals(450000L, r45k.jars["PLAY"])
        assertEquals(1004000L, r45k.jars["LONG_TERM"])
        assertEquals(225000L, r45k.jars["GIVING"])
        assertEquals(15000L, r45k.dailySafePaise)
        assertEquals(105000L, r45k.weeklySafePaise)
        assertEquals(emptyList(), r45k.flags)

        // 60,000
        val r60k = AllocationEngine.allocate(
            salaryPaise = 6000000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, r60k.jars["NEEDS"])
        assertEquals(600000L, r60k.jars["FREEDOM_SIP"])
        assertEquals(600000L, r60k.jars["EDUCATION"])
        assertEquals(600000L, r60k.jars["PLAY"])
        assertEquals(1979000L, r60k.jars["LONG_TERM"])
        assertEquals(300000L, r60k.jars["GIVING"])
        assertEquals(20000L, r60k.dailySafePaise)
        assertEquals(140000L, r60k.weeklySafePaise)
        assertEquals(emptyList(), r60k.flags)

        // A.2 Edge cases
        // 32,200 with EMI 5,000
        val rEmi = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 500000L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(2271000L, rEmi.jars["NEEDS"])
        assertEquals(322000L, rEmi.jars["FREEDOM_SIP"])
        assertEquals(144000L, rEmi.jars["EDUCATION"])
        assertEquals(0L, rEmi.jars["PLAY"])
        assertEquals(322000L, rEmi.jars["LONG_TERM"])
        assertEquals(161000L, rEmi.jars["GIVING"])
        assertEquals(listOf("SPEND_JAR_EMPTY"), rEmi.flags)

        // 45,000, emergency fund complete
        val rFull = AllocationEngine.allocate(
            salaryPaise = 4500000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 12397000L,
            emergencyTargetPaise = 12397000L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, rFull.jars["NEEDS"])
        assertEquals(1454000L, rFull.jars["FREEDOM_SIP"])
        assertEquals(450000L, rFull.jars["EDUCATION"])
        assertEquals(450000L, rFull.jars["PLAY"])
        assertEquals(0L, rFull.jars["LONG_TERM"])
        assertEquals(225000L, rFull.jars["GIVING"])
        assertEquals(15000L, rFull.dailySafePaise)
        assertEquals(105000L, rFull.weeklySafePaise)
        assertEquals(emptyList(), rFull.flags)

        // 32,200, baseline 20,000 + EMI 5,000 (deficit)
        val rDeficit = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 2000000L,
            emiPaise = 500000L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 14000000L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(2500000L, rDeficit.jars["NEEDS"])
        assertEquals(322000L, rDeficit.jars["FREEDOM_SIP"])
        assertEquals(0L, rDeficit.jars["EDUCATION"])
        assertEquals(0L, rDeficit.jars["PLAY"])
        assertEquals(322000L, rDeficit.jars["LONG_TERM"])
        assertEquals(76000L, rDeficit.jars["GIVING"])
        assertEquals(listOf("SPEND_JAR_EMPTY"), rDeficit.flags)

        // 32,200, baseline 30,000 + EMI 3,000 (shortfall)
        val rShortfall = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 3000000L,
            emiPaise = 300000L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 21000000L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(3220000L, rShortfall.jars["NEEDS"])
        assertEquals(0L, rShortfall.jars["FREEDOM_SIP"])
        assertEquals(listOf("SHORTFALL"), rShortfall.flags)

        // 32,200 with Play→Long-Term shift
        val rShift = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = true,
            daysInCycle = 30
        )
        assertEquals(1771000L, rShift.jars["NEEDS"])
        assertEquals(322000L, rShift.jars["FREEDOM_SIP"])
        assertEquals(322000L, rShift.jars["EDUCATION"])
        assertEquals(161000L, rShift.jars["PLAY"])
        assertEquals(483000L, rShift.jars["LONG_TERM"])
        assertEquals(161000L, rShift.jars["GIVING"])
        assertEquals(5366L, rShift.dailySafePaise)
        assertEquals(37566L, rShift.weeklySafePaise)

        // 45,000, creep lock off
        val rCreepOff = AllocationEngine.allocate(
            salaryPaise = 4500000L,
            framework = FrameworkDefinition.SIX_JAR,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = false,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(2475000L, rCreepOff.jars["NEEDS"])
        assertEquals(450000L, rCreepOff.jars["FREEDOM_SIP"])
        assertEquals(450000L, rCreepOff.jars["EDUCATION"])
        assertEquals(450000L, rCreepOff.jars["PLAY"])
        assertEquals(450000L, rCreepOff.jars["LONG_TERM"])
        assertEquals(225000L, rCreepOff.jars["GIVING"])

        // THREE_BOX, 40,000
        val rThreeBox = AllocationEngine.allocate(
            salaryPaise = 4000000L,
            framework = FrameworkDefinition.THREE_BOX,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1921000L, rThreeBox.jars["NEEDS"])
        assertEquals(479000L, rThreeBox.jars["EMERGENCY"])
        assertEquals(400000L, rThreeBox.jars["BUFFER"])
        assertEquals(400000L, rThreeBox.jars["MEDIUM_TERM"])
        assertEquals(800000L, rThreeBox.jars["LONG_TERM_SIP"])

        // 50/30/20, 32,200
        val r503020 = AllocationEngine.allocate(
            salaryPaise = 3220000L,
            framework = FrameworkDefinition.FIFTY_THIRTY_TWENTY,
            baselinePaise = 1771000L,
            emiPaise = 0L,
            emergencyBalancePaise = 0L,
            emergencyTargetPaise = 1771000L * 7L,
            creepLock = true,
            bufferPaise = 150000L,
            shiftPlayToLongTerm = false,
            daysInCycle = 30
        )
        assertEquals(1771000L, r503020.jars["NEEDS"])
        assertEquals(805000L, r503020.jars["WANTS"])
        assertEquals(644000L, r503020.jars["SAVINGS"])
        assertEquals(26833L, r503020.dailySafePaise)
        assertEquals(187833L, r503020.weeklySafePaise)
    }

    @Test
    fun propertyTestInvariantsOver100kRandomInputs() {
        val frameworks = listOf(
            FrameworkDefinition.SIX_JAR,
            FrameworkDefinition.THREE_BOX,
            FrameworkDefinition.FIFTY_THIRTY_TWENTY
        )
        val rng = Random(42)

        for (i in 0 until 100000) {
            val fw = frameworks[rng.nextInt(frameworks.size)]
            val salary = rng.nextLong(100000L, 20000000L) // ₹1,000 to ₹2,00,000
            val baseline = rng.nextLong(0L, (salary * 13L) / 10L) // up to 130% of salary
            val emi = rng.nextLong(0L, salary / 2L)
            val emgBal = rng.nextLong(0L, salary * 10L)
            val emgTarget = rng.nextLong(0L, salary * 10L)
            val creep = rng.nextBoolean()
            val buffer = rng.nextLong(0L, 500000L)
            val shift = rng.nextBoolean()
            val days = rng.nextInt(1, 35)

            val res = AllocationEngine.allocate(
                salaryPaise = salary,
                framework = fw,
                baselinePaise = baseline,
                emiPaise = emi,
                emergencyBalancePaise = emgBal,
                emergencyTargetPaise = emgTarget,
                creepLock = creep,
                bufferPaise = buffer,
                shiftPlayToLongTerm = shift,
                daysInCycle = days
            )

            // Invariant checks
            val sum = res.jars.values.sum()
            assertEquals(salary, sum, "Sum must equal salary on iteration $i")
            assertTrue(res.jars.values.all { it >= 0L }, "All jars must be non-negative on iteration $i")
            val mandatory = baseline + emi
            if (mandatory >= salary) {
                assertTrue("SHORTFALL" in res.flags)
                assertEquals(salary, res.jars["NEEDS"])
            } else {
                assertTrue(res.jars["NEEDS"]!! >= mandatory, "NEEDS must be >= mandatory on iteration $i")
            }
        }
    }
}
