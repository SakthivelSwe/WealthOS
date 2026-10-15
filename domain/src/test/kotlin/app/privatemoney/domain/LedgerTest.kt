package app.privatemoney.domain

import app.privatemoney.domain.ledger.BalanceCalculator
import app.privatemoney.domain.ledger.InvalidTransactionException
import app.privatemoney.domain.ledger.JournalEntry
import app.privatemoney.domain.ledger.JournalPosting
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import app.privatemoney.domain.planning.SafeToSpendEngine
import app.privatemoney.domain.planning.SafeToSpendInput
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LedgerTest {
    private val inr = Currency.INR
    private val usd = Currency.USD
    private val jpy = Currency.JPY

    // =========================================================================================
    // 1. One expense, one income, transfer between assets, credit-card, refund, reversal, soft delete
    // =========================================================================================

    @Test
    fun testOneExpenseReducesAccountAndIncreasesExpenseTotal() {
        val opening = 50_000_00L // ₹50,000
        val draft = TransactionDraft("txn_exp_1", TransactionType.EXPENSE, 12_500_00L, inr, "bank_sbi", "cat_food")
        val entries = JournalPosting.post(draft)

        assertTrue(JournalPosting.isBalanced(entries))
        assertEquals(2, entries.size)
        // Account leg: -12,500
        assertEquals(-12_500_00L, entries.first { it.accountId == "bank_sbi" }.amountMinor)
        // Category leg: +12,500
        assertEquals(12_500_00L, entries.first { it.categoryId == "cat_food" }.amountMinor)

        // Balance changes from 50k to 37.5k
        assertEquals(37_500_00L, BalanceCalculator.accountBalance(opening, "bank_sbi", entries))
        assertEquals(12_500_00L, BalanceCalculator.expenseTotal(setOf("cat_food"), entries))
        assertEquals(0L, BalanceCalculator.incomeTotal(null, entries))
    }

    @Test
    fun testOneIncomeIncreasesAccountAndIncreasesIncomeTotal() {
        val opening = 10_000_00L // ₹10,000
        val draft = TransactionDraft("txn_inc_1", TransactionType.INCOME, 80_000_00L, inr, "bank_hdfc", "cat_salary")
        val entries = JournalPosting.post(draft)

        assertTrue(JournalPosting.isBalanced(entries))
        // Account leg: +80,000
        assertEquals(80_000_00L, entries.first { it.accountId == "bank_hdfc" }.amountMinor)
        // Category leg: -80,000
        assertEquals(-80_000_00L, entries.first { it.categoryId == "cat_salary" }.amountMinor)

        // Balance changes from 10k to 90k
        assertEquals(90_000_00L, BalanceCalculator.accountBalance(opening, "bank_hdfc", entries))
        assertEquals(80_000_00L, BalanceCalculator.incomeTotal(setOf("cat_salary"), entries))
        assertEquals(0L, BalanceCalculator.expenseTotal(null, entries))
    }

    @Test
    fun testTransferBetweenTwoAssetsConservesNetWorth() {
        val bankOpening = 100_000_00L
        val cashOpening = 5_000_00L
        val draft = TransactionDraft("txn_trf_1", TransactionType.TRANSFER, 10_000_00L, inr, "bank_sbi", toAccountId = "cash_wallet")
        val entries = JournalPosting.post(draft)

        assertTrue(JournalPosting.isBalanced(entries))
        val newBank = BalanceCalculator.accountBalance(bankOpening, "bank_sbi", entries)
        val newCash = BalanceCalculator.accountBalance(cashOpening, "cash_wallet", entries)

        assertEquals(90_000_00L, newBank)
        assertEquals(15_000_00L, newCash)
        // Net worth before and after are identical
        assertEquals(105_000_00L, BalanceCalculator.netWorth(listOf(bankOpening, cashOpening)))
        assertEquals(105_000_00L, BalanceCalculator.netWorth(listOf(newBank, newCash)))
        // Transfers have no category legs -> 0 expense, 0 income
        assertEquals(0L, BalanceCalculator.expenseTotal(null, entries))
        assertEquals(0L, BalanceCalculator.incomeTotal(null, entries))
    }

    @Test
    fun testCreditCardPurchaseAndPaymentConventions() {
        val bankOpening = 50_000_00L
        val ccOpening = 0L // No debt initially

        // 1. User makes a ₹5,000 credit card purchase
        val purchaseDraft = TransactionDraft("txn_cc_buy", TransactionType.EXPENSE, 5_000_00L, inr, "cc_card", "cat_shopping")
        val purchaseEntries = JournalPosting.post(purchaseDraft)

        // CC balance becomes negative (-₹5,000) representing liability
        val ccAfterPurchase = BalanceCalculator.accountBalance(ccOpening, "cc_card", purchaseEntries)
        assertEquals(-5_000_00L, ccAfterPurchase)
        assertEquals(45_000_00L, BalanceCalculator.netWorth(listOf(bankOpening, ccAfterPurchase)))
        assertEquals(5_000_00L, BalanceCalculator.expenseTotal(null, purchaseEntries))

        // 2. User pays off the ₹5,000 credit card bill from Bank (Transfer)
        val paymentDraft = TransactionDraft("txn_cc_pay", TransactionType.TRANSFER, 5_000_00L, inr, "bank_sbi", toAccountId = "cc_card")
        val paymentEntries = JournalPosting.post(paymentDraft)

        val allEntries = purchaseEntries + paymentEntries
        val bankFinal = BalanceCalculator.accountBalance(bankOpening, "bank_sbi", allEntries)
        val ccFinal = BalanceCalculator.accountBalance(ccOpening, "cc_card", allEntries)

        // Bank is down to 45k; CC liability is back to 0 (fully paid)
        assertEquals(45_000_00L, bankFinal)
        assertEquals(0L, ccFinal)
        // Net worth after payment equals net worth after purchase (no consumption on transfer)
        assertEquals(45_000_00L, BalanceCalculator.netWorth(listOf(bankFinal, ccFinal)))
    }

    @Test
    fun testRefundReducesNetExpenseOrRestoresAccountBalance() {
        val bankOpening = 20_000_00L
        // 1. Initial purchase: ₹3,000 for clothes
        val purchase = TransactionDraft("txn_buy", TransactionType.EXPENSE, 3_000_00L, inr, "bank", "cat_shopping")
        val pEntries = JournalPosting.post(purchase)

        // 2. Merchant refunds ₹3,000 to bank account
        val refund = TransactionDraft("txn_ref", TransactionType.INCOME, 3_000_00L, inr, "bank", "cat_shopping")
        val rEntries = JournalPosting.post(refund)

        val allEntries = pEntries + rEntries
        val finalBalance = BalanceCalculator.accountBalance(bankOpening, "bank", allEntries)
        assertEquals(bankOpening, finalBalance) // Balance restored exactly

        // Net expense for shopping returns to 0
        assertEquals(0L, BalanceCalculator.netExpenseTotal(setOf("cat_shopping"), allEntries))
    }

    @Test
    fun testReversalRestoresPreTransactionBalanceAtomically() {
        val bankOpening = 100_000_00L
        val originalDraft = TransactionDraft("txn_orig", TransactionType.EXPENSE, 15_000_00L, inr, "bank", "cat_travel")
        val originalEntries = JournalPosting.post(originalDraft)

        val balanceAfterOrig = BalanceCalculator.accountBalance(bankOpening, "bank", originalEntries)
        assertEquals(85_000_00L, balanceAfterOrig)

        // Reversal generates exact opposite legs
        val reversalEntries = JournalPosting.reverse(originalEntries, "txn_rev")
        assertTrue(JournalPosting.isBalanced(reversalEntries))

        val combinedEntries = originalEntries + reversalEntries
        val restoredBalance = BalanceCalculator.accountBalance(bankOpening, "bank", combinedEntries)
        assertEquals(bankOpening, restoredBalance) // Exactly back to 100k
        assertEquals(0L, BalanceCalculator.netExpenseTotal(setOf("cat_travel"), combinedEntries))
    }

    @Test
    fun testSoftDeleteConsistentlyRemovesAnalyticEffect() {
        val bankOpening = 50_000_00L
        data class TxnRecord(val draft: TransactionDraft, val deleted: Boolean)

        val records = listOf(
            TxnRecord(TransactionDraft("t1", TransactionType.EXPENSE, 5_000_00L, inr, "bank", "cat_food"), false),
            TxnRecord(TransactionDraft("t2", TransactionType.EXPENSE, 10_000_00L, inr, "bank", "cat_bills"), true), // Soft-deleted
            TxnRecord(TransactionDraft("t3", TransactionType.INCOME, 20_000_00L, inr, "bank", "cat_salary"), false),
        )

        // Only active (deleted == false) entries are fed to calculations (matching SQL deletedAt IS NULL)
        val activeEntries = records.filterNot { it.deleted }.flatMap { JournalPosting.post(it.draft) }

        val activeBalance = BalanceCalculator.accountBalance(bankOpening, "bank", activeEntries)
        // 50k - 5k + 20k = 65k (t2's 10k expense is completely ignored)
        assertEquals(65_000_00L, activeBalance)
        assertEquals(5_000_00L, BalanceCalculator.expenseTotal(null, activeEntries))
        assertEquals(20_000_00L, BalanceCalculator.incomeTotal(null, activeEntries))
    }

    // =========================================================================================
    // 2. Two-account transfer balance conservation
    // =========================================================================================

    @Test
    fun testTwoAccountTransferBalanceConservationProperty() {
        val random = Random(42)
        repeat(500) { i ->
            val openingA = random.nextLong(-50_000_000L, 50_000_000L)
            val openingB = random.nextLong(-50_000_000L, 50_000_000L)
            val amount = random.nextLong(1, 10_000_000L)

            val draft = TransactionDraft("t$i", TransactionType.TRANSFER, amount, inr, "acct_a", toAccountId = "acct_b")
            val entries = JournalPosting.post(draft)

            val beforeNetWorth = BalanceCalculator.netWorth(listOf(openingA, openingB))
            val afterNetWorth = BalanceCalculator.netWorth(listOf(
                BalanceCalculator.accountBalance(openingA, "acct_a", entries),
                BalanceCalculator.accountBalance(openingB, "acct_b", entries),
            ))

            assertEquals(beforeNetWorth, afterNetWorth)
            assertEquals(0L, BalanceCalculator.expenseTotal(null, entries))
            assertEquals(0L, BalanceCalculator.incomeTotal(null, entries))
        }
    }

    // =========================================================================================
    // 3. Validation: same-account rejection, blank fields, invalid pairings
    // =========================================================================================

    @Test
    fun testRejectsInvalidDraftsAndSameAccountTransfer() {
        // Same account transfer
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.TRANSFER, 500, inr, "same", toAccountId = "same"))
        }
        // Zero amount
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 0, inr, "bank", "cat"))
        }
        // Negative amount
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, -100, inr, "bank", "cat"))
        }
        // Blank transaction ID
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("   ", TransactionType.EXPENSE, 100, inr, "bank", "cat"))
        }
        // Blank source account
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 100, inr, "", "cat"))
        }
        // Expense with blank category
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 100, inr, "bank", "  "))
        }
        // Expense with null category
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 100, inr, "bank", null))
        }
        // Expense with toAccountId set
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 100, inr, "bank", "cat", toAccountId = "other"))
        }
        // Transfer with category set
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.TRANSFER, 100, inr, "a", categoryId = "cat", toAccountId = "b"))
        }
        // Transfer with blank destination
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.TRANSFER, 100, inr, "a", toAccountId = "  "))
        }
    }

    // =========================================================================================
    // 4. Month boundaries and leap day handling
    // =========================================================================================

    @Test
    fun testLeapDayAndMonthBoundaryCalculations() {
        // Leap year 2024: Feb has 29 days
        val leapFeb28 = LocalDate.of(2024, 2, 28)
        val input = SafeToSpendInput(
            liquidBalanceMinor = 29_000_00L,
            upcomingMandatoryBillsMinor = 0L,
            expectedDebtPaymentsMinor = 0L,
            plannedGoalContributionsMinor = 0L,
            creditCardDueMinor = 0L,
            safetyBufferMinor = 0L,
        )

        val leapBreakdown = SafeToSpendEngine.calculate(input, leapFeb28)
        // Remaining days in Feb 2024 on Feb 28: 29 - 28 + 1 = 2 days
        assertEquals(2, leapBreakdown.daysRemainingInMonth)
        assertEquals(14_500_00L, leapBreakdown.safeTodayMinor)

        // Non-leap year 2025: Feb has 28 days
        val nonLeapFeb28 = LocalDate.of(2025, 2, 28)
        val nonLeapBreakdown = SafeToSpendEngine.calculate(input, nonLeapFeb28)
        // Remaining days in Feb 2025 on Feb 28: 28 - 28 + 1 = 1 day
        assertEquals(1, nonLeapBreakdown.daysRemainingInMonth)
        assertEquals(29_000_00L, nonLeapBreakdown.safeTodayMinor)

        // End of year boundary: Dec 31
        val dec31 = LocalDate.of(2026, 12, 31)
        val decBreakdown = SafeToSpendEngine.calculate(input, dec31)
        assertEquals(1, decBreakdown.daysRemainingInMonth)
        assertEquals(29_000_00L, decBreakdown.safeTodayMinor)
    }

    // =========================================================================================
    // 5. Cents/paise rounding, Currency minor units, Long overflow, percentage rounding
    // =========================================================================================

    @Test
    fun testMoneyParsingAndMinorUnitsAcrossCurrencies() {
        // INR: 2 minor units (paise)
        assertEquals(10050L, Money.parse("100.50", inr).amountMinor)
        assertEquals(10000L, Money.parse("100", inr).amountMinor)
        assertEquals(5L, Money.parse("0.05", inr).amountMinor)
        assertFailsWith<IllegalArgumentException> { Money.parse("1.234", inr) }

        // USD: 2 minor units (cents)
        assertEquals(4999L, Money.parse("49.99", usd).amountMinor)

        // JPY: 0 minor units (no decimals)
        assertEquals(5000L, Money.parse("5000", jpy).amountMinor)
        assertFailsWith<IllegalArgumentException> { Money.parse("5000.5", jpy) }
    }

    @Test
    fun testCurrencyMixingAndOverflowChecks() {
        // Rejects mixing currencies
        assertFailsWith<IllegalArgumentException> { Money(1, inr) + Money(1, usd) }
        assertFailsWith<IllegalArgumentException> { Money(1, inr) - Money(1, usd) }

        // Long overflow throws ArithmeticException rather than silently wrapping
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE, inr) + Money(1, inr) }
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE, inr) - Money(1, inr) }
        assertFailsWith<ArithmeticException> { -Money(Long.MIN_VALUE, inr) }
    }

    @Test
    fun testPercentageCalculationExplicitRoundingAndEdgeCases() {
        // Half-up rounding
        assertEquals(33, Money.calculatePercentage(3333L, 10000L))
        assertEquals(34, Money.calculatePercentage(3350L, 10000L))
        assertEquals(67, Money.calculatePercentage(2L, 3L))
        assertEquals(100, Money.calculatePercentage(5000L, 5000L))

        // Zero / negative denominator handling
        assertEquals(0, Money.calculatePercentage(100L, 0L))
        assertEquals(0, Money.calculatePercentage(100L, -500L))

        // Zero / negative numerator handling
        assertEquals(0, Money.calculatePercentage(0L, 1000L))
        assertEquals(0, Money.calculatePercentage(-50L, 1000L))
    }

    // =========================================================================================
    // 6. Summary equals source-ledger recomputation
    // =========================================================================================

    @Test
    fun testSummaryEqualsSourceLedgerRecomputation() {
        val openingBalances = mapOf(
            "bank" to 100_000_00L,
            "cash" to 5_000_00L,
            "cc" to -2_000_00L,
        )

        // Perform a series of realistic financial movements
        val events = listOf(
            TransactionDraft("t1", TransactionType.EXPENSE, 1_500_00L, inr, "bank", "cat_food"),
            TransactionDraft("t2", TransactionType.INCOME, 50_000_00L, inr, "bank", "cat_salary"),
            TransactionDraft("t3", TransactionType.TRANSFER, 3_000_00L, inr, "bank", toAccountId = "cash"),
            TransactionDraft("t4", TransactionType.EXPENSE, 800_00L, inr, "cash", "cat_food"),
            TransactionDraft("t5", TransactionType.EXPENSE, 4_000_00L, inr, "cc", "cat_shopping"),
            TransactionDraft("t6", TransactionType.TRANSFER, 6_000_00L, inr, "bank", toAccountId = "cc"),
        )

        val allLegs = events.flatMap { JournalPosting.post(it) }
        assertTrue(JournalPosting.isBalanced(allLegs))

        // Verify per-account balances from source journal legs
        // Bank: 100k - 1.5k + 50k - 3k - 6k = 139.5k
        assertEquals(139_500_00L, BalanceCalculator.accountBalance(openingBalances.getValue("bank"), "bank", allLegs))
        // Cash: 5k + 3k - 0.8k = 7.2k
        assertEquals(7_200_00L, BalanceCalculator.accountBalance(openingBalances.getValue("cash"), "cash", allLegs))
        // CC: -2k - 4k + 6k = 0
        assertEquals(0L, BalanceCalculator.accountBalance(openingBalances.getValue("cc"), "cc", allLegs))

        // Verify total net worth
        val finalBalances = listOf(139_500_00L, 7_200_00L, 0L)
        val initialNetWorth = BalanceCalculator.netWorth(openingBalances.values) // 100k + 5k - 2k = 103k
        val finalNetWorth = BalanceCalculator.netWorth(finalBalances) // 146.7k
        // Change in net worth = Income (50k) - Expense (1.5k + 0.8k + 4k = 6.3k) = 43.7k
        assertEquals(103_000_00L + 43_700_00L, finalNetWorth)

        // Verify category expenses
        assertEquals(2_300_00L, BalanceCalculator.expenseTotal(setOf("cat_food"), allLegs))
        assertEquals(4_000_00L, BalanceCalculator.expenseTotal(setOf("cat_shopping"), allLegs))
        assertEquals(6_300_00L, BalanceCalculator.expenseTotal(null, allLegs))
        assertEquals(50_000_00L, BalanceCalculator.incomeTotal(null, allLegs))
    }

    // =========================================================================================
    // 7. Fuzz / property test with 1,000 random transactions
    // =========================================================================================

    @Test
    fun testEveryRandomTransactionIsBalancedFuzz() {
        val random = Random(7)
        val accounts = listOf("acct_1", "acct_2", "acct_3", "acct_4")
        val categories = listOf("cat_1", "cat_2", "cat_3")

        val all = (0 until 1000).flatMap { i ->
            val type = TransactionType.entries[random.nextInt(3)]
            val src = accounts[random.nextInt(accounts.size)]
            val dst = accounts.filter { it != src }[random.nextInt(accounts.size - 1)]
            val cat = categories[random.nextInt(categories.size)]

            val draft = TransactionDraft(
                id = "fuzz_$i",
                type = type,
                amountMinor = random.nextLong(1, 10_000_000L),
                currency = inr,
                accountId = src,
                categoryId = if (type == TransactionType.TRANSFER) null else cat,
                toAccountId = if (type == TransactionType.TRANSFER) dst else null,
            )
            JournalPosting.post(draft)
        }

        assertTrue(JournalPosting.isBalanced(all))
        // The grand sum of every leg in the entire journal must equal 0
        assertEquals(0L, all.fold(0L) { acc, leg -> acc + leg.amountMinor })
    }
}

