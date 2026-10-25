package app.privatemoney.domain

import app.privatemoney.domain.planning.DebtLoan
import app.privatemoney.domain.planning.DebtPayoffEngine
import app.privatemoney.domain.planning.DebtStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class DebtPayoffTest {

    @Test
    fun testAppendixA4DebtPayoffGoldenValues() {
        val loans = listOf(
            DebtLoan(name = "Card", balancePaise = 6000000L, rateBps = 3600, minimumPaymentPaise = 300000L),
            DebtLoan(name = "Personal", balancePaise = 2000000L, rateBps = 1800, minimumPaymentPaise = 150000L),
            DebtLoan(name = "Vehicle", balancePaise = 12000000L, rateBps = 1400, minimumPaymentPaise = 400000L)
        )
        val extra = 200000L

        // Snowball
        val snowball = DebtPayoffEngine.payoff(loans, extra, DebtStrategy.SNOWBALL)
        assertEquals(24, snowball.months)
        assertEquals(4165614L, snowball.totalInterestPaise)

        // Avalanche
        val avalanche = DebtPayoffEngine.payoff(loans, extra, DebtStrategy.AVALANCHE)
        assertEquals(23, avalanche.months)
        assertEquals(3970646L, avalanche.totalInterestPaise)

        // Comparison
        val comparison = DebtPayoffEngine.compareStrategies(loans, extra)
        assertEquals(194968L, comparison.interestSavedPaise)
        assertEquals(1, comparison.monthsSaved)
    }
}
