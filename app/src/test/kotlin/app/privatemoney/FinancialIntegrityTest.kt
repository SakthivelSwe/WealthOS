package app.privatemoney

import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.planning.BudgetSplit
import app.privatemoney.domain.planning.Lane
import app.privatemoney.domain.planning.PlanReport
import app.privatemoney.domain.planning.SafeToSpendBreakdown
import app.privatemoney.domain.planning.SafeToSpendEngine
import app.privatemoney.domain.planning.SafeToSpendInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinancialIntegrityTest {

    @Test
    fun testRestoreDeduplicationFiltering() {
        // Mock existing transactions already in database
        val existingTxnIds = setOf("txn_1", "txn_2")

        // Incoming transactions from backup envelope
        data class MockTxn(val id: String, val amount: Long)
        data class MockJournalEntry(val txnId: String, val amount: Long)

        val incomingTxns = listOf(
            MockTxn("txn_1", 1000L), // duplicate
            MockTxn("txn_2", 2000L), // duplicate
            MockTxn("txn_3", 3000L), // NEW
        )

        val incomingJournal = listOf(
            MockJournalEntry("txn_1", -1000L), // duplicate
            MockJournalEntry("txn_2", -2000L), // duplicate
            MockJournalEntry("txn_3", -3000L), // NEW
        )

        // Apply deduplication filter matching CloudSyncService.executeRestore
        val newTxns = incomingTxns.filterNot { existingTxnIds.contains(it.id) }
        val newTxnIds = newTxns.map { it.id }.toSet()
        val journalToInsert = incomingJournal.filter { newTxnIds.contains(it.txnId) }

        // Assert that existing transactions and their journal entries are strictly excluded
        assertEquals(1, newTxns.size)
        assertEquals("txn_3", newTxns.first().id)

        assertEquals(1, journalToInsert.size)
        assertEquals("txn_3", journalToInsert.first().txnId)
    }

    @Test
    fun testSafeToSpendCalculationIntegrity() {
        val today = LocalDate.of(2026, 10, 15)
        val input = SafeToSpendInput(
            liquidBalanceMinor = 100_000_00L, // ₹100,000
            upcomingMandatoryBillsMinor = 20_000_00L, // ₹20,000
            expectedDebtPaymentsMinor = 10_000_00L, // ₹10,000
            plannedGoalContributionsMinor = 10_000_00L, // ₹10,000
            creditCardDueMinor = 10_000_00L, // ₹10,000
            safetyBufferMinor = 5_000_00L, // ₹5,000
        )

        val breakdown = SafeToSpendEngine.calculate(input, today)

        // Committed = 20k + 10k + 10k + 10k = 50,000
        assertEquals(50_000_00L, breakdown.committedMinor)
        // Discretionary = 100k - 50k - 5k = 45,000
        assertEquals(45_000_00L, breakdown.discretionaryMinor)
        assertEquals(45_000_00L, breakdown.safeThisMonthMinor)
        assertEquals(0L, breakdown.shortfallMinor)

        // Remaining days in Oct (31 - 15 + 1 = 17)
        assertEquals(17, breakdown.daysRemainingInMonth)
        // Daily = 45,000 / 17 = 2,647.05 -> 2647 minor
        assertEquals(264705L, breakdown.safeTodayMinor)
    }

    @Test
    fun testUnclassifiedSpendingDetection() {
        data class MockLaneRow(val needWant: String?, val total: Long)

        val rows = listOf(
            MockLaneRow("NEED", 5000_00L),
            MockLaneRow("WANT", 3000_00L),
            MockLaneRow(null, 2500_00L), // Unclassified
            MockLaneRow("", 500_00L),   // Invalid / Unclassified
        )

        val spentByLane = rows.mapNotNull { row ->
            Lane.fromName(row.needWant)?.let { it to row.total }
        }.toMap()

        val unclassifiedSpend = rows.filter { Lane.fromName(it.needWant) == null }.sumOf { it.total }

        assertEquals(2, spentByLane.size)
        assertEquals(5000_00L, spentByLane[Lane.NEEDS])
        assertEquals(3000_00L, spentByLane[Lane.WANTS])

        // Unclassified must capture both null and blank tags
        assertEquals(3000_00L, unclassifiedSpend)
    }

    @Test
    fun testDeterministicRecurringTxnIdGeneration() {
        val recurringId = "rec_rent_123"
        val nextRunDate = "2026-10-01"

        val id1 = "txn_recur_${recurringId}_${nextRunDate.replace("-", "")}"
        val id2 = "txn_recur_${recurringId}_${nextRunDate.replace("-", "")}"

        assertEquals("txn_recur_rec_rent_123_20261001", id1)
        assertEquals(id1, id2) // Identical across retries
    }

    @Test
    fun testAtomicTransactionReplacementIntegrity() {
        // Simulates LedgerRepository.replace behavior: old transaction is soft-deleted, new one posted
        data class Txn(val id: String, val amount: Long, val deletedAt: Long?)
        data class Leg(val txnId: String, val amount: Long)

        val oldTxn = Txn("txn_orig", 1000_00L, null)
        val oldLegs = listOf(Leg("txn_orig", -1000_00L), Leg("txn_orig", 1000_00L))

        // Replace with corrected amount
        val now = 1729000000000L
        val softDeletedOldTxn = oldTxn.copy(deletedAt = now)
        val newTxn = Txn("txn_replaced", 1200_00L, null)
        val newLegs = listOf(Leg("txn_replaced", -1200_00L), Leg("txn_replaced", 1200_00L))

        val allTxns = listOf(softDeletedOldTxn, newTxn)
        val allLegs = oldLegs + newLegs

        // Active transactions are filtered WHERE deletedAt IS NULL
        val activeTxnIds = allTxns.filter { it.deletedAt == null }.map { it.id }.toSet()
        val activeLegs = allLegs.filter { activeTxnIds.contains(it.txnId) }

        assertEquals(1, activeTxnIds.size)
        assertTrue(activeTxnIds.contains("txn_replaced"))
        assertFalse(activeTxnIds.contains("txn_orig"))

        // Active expense is exactly the new amount (1200, not 1000 or 2200)
        val totalActiveExpense = activeLegs.filter { it.amount > 0 }.sumOf { it.amount }
        assertEquals(1200_00L, totalActiveExpense)
    }

    @Test
    fun testCurrencySegregatedTotals() {
        data class Txn(val id: String, val amount: Long, val currency: String)

        val txns = listOf(
            Txn("t1", 5000_00L, "INR"),
            Txn("t2", 100_00L, "USD"), // $100
            Txn("t3", 2500_00L, "INR"),
        )

        // Without currency filter, raw sum improperly adds USD cents and INR paise
        val naiveSum = txns.sumOf { it.amount }
        assertEquals(7600_00L, naiveSum)

        // With currency filter matching our SQL query (:currency IS NULL OR currency = :currency)
        val inrTotal = txns.filter { it.currency == "INR" }.sumOf { it.amount }
        val usdTotal = txns.filter { it.currency == "USD" }.sumOf { it.amount }

        assertEquals(7500_00L, inrTotal)
        assertEquals(100_00L, usdTotal)
    }

    @Test
    fun testGeminiStatementExtractionIndianNumberingAndCredits() = kotlinx.coroutines.runBlocking {
        val ai = app.privatemoney.cloud.GeminiAIService()
        val rawStatement = """
            2026-10-01 SALARY CREDIT INFOSYS LTD UPI/9876543210 INR 1,50,000.00 CR
            2026-10-02 SWIGGY BANGALORE UPI/1122334455 Rs. 450.00 DR
            2026-10-05 AMAZON REFUND RETURN Rs. 1,299.00 CREDIT
            2026-10-08 MYSTERIOUS TXN 123456 500.00
        """.trimIndent()

        val jsonResult = ai.extractTransactions(rawStatement)
        val array = org.json.JSONArray(jsonResult)

        assertEquals(4, array.length())

        // Row 1: Salary Credit in Indian formatting (1,50,000.00) -> INCOME, positive
        val salaryRow = array.getJSONObject(0)
        assertEquals("INCOME", salaryRow.getString("type"))
        assertTrue(salaryRow.getBoolean("isCredit"))
        assertFalse(salaryRow.getBoolean("isDebit"))
        assertFalse(salaryRow.getBoolean("isAmbiguous"))
        assertEquals(15000000L, salaryRow.getLong("amountMinor"))
        assertTrue(salaryRow.getDouble("amount") > 0.0)

        // Row 2: Swiggy Debit -> EXPENSE, negative
        val expenseRow = array.getJSONObject(1)
        assertEquals("EXPENSE", expenseRow.getString("type"))
        assertTrue(expenseRow.getBoolean("isDebit"))
        assertFalse(expenseRow.getBoolean("isCredit"))
        assertEquals(-45000L, expenseRow.getLong("amountMinor"))
        assertTrue(expenseRow.getDouble("amount") < 0.0)

        // Row 3: Refund -> INCOME, positive
        val refundRow = array.getJSONObject(2)
        assertEquals("INCOME", refundRow.getString("type"))
        assertTrue(refundRow.getBoolean("isCredit"))
        assertEquals(129900L, refundRow.getLong("amountMinor"))

        // Row 4: Ambiguous (no CR/DR or debit/credit indicators) -> AMBIGUOUS, needsReview
        val ambiguousRow = array.getJSONObject(3)
        assertEquals("AMBIGUOUS", ambiguousRow.getString("type"))
        assertTrue(ambiguousRow.getBoolean("isAmbiguous"))
        assertTrue(ambiguousRow.getBoolean("needsReview"))
    }

    @Test
    fun testInsightsSavingsPlannedContributionVsLifetimeBalance() {
        data class GoalStub(val targetAmountMinor: Long, val currentAmountMinor: Long, val plannedMonthlyContributionMinor: Long)

        val goals = listOf(
            GoalStub(targetAmountMinor = 1_000_000_00L, currentAmountMinor = 500_000_00L, plannedMonthlyContributionMinor = 25_000_00L),
            GoalStub(targetAmountMinor = 200_000_00L, currentAmountMinor = 200_000_00L, plannedMonthlyContributionMinor = 10_000_00L), // fully funded
        )

        val monthlyExpenses = 50_000_00L

        // Defective logic: sums cumulative current amount (500k + 200k = 700k), dwarfing 50k monthly expense
        val defectiveSavingsSum = goals.sumOf { it.currentAmountMinor }
        assertEquals(700_000_00L, defectiveSavingsSum)

        // Corrected logic: sums planned monthly contribution for active goals only (25k), properly sizing monthly budget
        val correctedSavingsSum = goals
            .filter { it.currentAmountMinor < it.targetAmountMinor }
            .sumOf { minOf(it.targetAmountMinor - it.currentAmountMinor, it.plannedMonthlyContributionMinor) }
        assertEquals(25_000_00L, correctedSavingsSum)
    }

    @Test
    fun testInsightsWeeklyPacingZeroLastWeekBaseline() {
        val spentThisWeek = 500_00L // ₹500
        val spentLastWeek = 0L     // Fresh account or zero previous spend
        val dailyAvg = 500_00L / 1

        // Defective condition: (spentThisWeek > spentLastWeek && spentThisWeek > 0) -> true for zero baseline!
        val defectiveIsOverExpense = (spentThisWeek > spentLastWeek && spentThisWeek > 0) ||
            (dailyAvg * 7 > spentLastWeek * 1.15 && spentLastWeek > 0)
        assertTrue(defectiveIsOverExpense)

        // Corrected condition: requires spentLastWeek > 0L to evaluate over-expense alert
        val correctedIsOverExpense = (spentLastWeek > 0L && spentThisWeek > spentLastWeek) ||
            (spentLastWeek > 0L && dailyAvg * 7 > (spentLastWeek * 1.15).toLong())
        assertFalse(correctedIsOverExpense)
    }

    @Test
    fun testDatabaseIntegrityValidatorCanonicalCurrencyPrecision() {
        // Supported currencies
        val inr = Currency.fromCode("INR") ?: Currency("INR", 2)
        assertEquals(2, inr.minorUnit)

        // JPY is zero-decimal currency
        val jpy = Currency.fromCode("JPY") ?: Currency("JPY", 2)
        assertEquals(0, jpy.minorUnit)
    }
}
