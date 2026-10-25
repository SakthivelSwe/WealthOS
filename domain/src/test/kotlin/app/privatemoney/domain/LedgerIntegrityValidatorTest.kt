package app.privatemoney.domain

import app.privatemoney.domain.ledger.IntegrityViolation
import app.privatemoney.domain.ledger.JournalEntry
import app.privatemoney.domain.ledger.JournalPosting
import app.privatemoney.domain.ledger.LedgerIntegrityValidator
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.ledger.ValidationAccount
import app.privatemoney.domain.ledger.ValidationTxn
import app.privatemoney.domain.ledger.ViolationSeverity
import app.privatemoney.domain.model.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LedgerIntegrityValidatorTest {

    private val inr = Currency("INR", 2)
    private val usd = Currency("USD", 2)

    private val accounts = listOf(
        ValidationAccount("acc_bank", "HDFC Checking", inr, openingBalanceMinor = 100_000L, recordedBalanceMinor = 85_000L),
        ValidationAccount("acc_cash", "Physical Wallet", inr, openingBalanceMinor = 10_000L, recordedBalanceMinor = 15_000L),
        ValidationAccount("acc_usd", "Wise USD", usd, openingBalanceMinor = 50_000L, recordedBalanceMinor = 50_000L),
    )

    private val validCategories = setOf("cat_groceries", "cat_salary", "cat_fuel")

    @Test
    fun testValidLedgerPassesIntegrityCheck() {
        val draft1 = TransactionDraft("txn_1", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", "cat_groceries")
        val draft2 = TransactionDraft("txn_2", TransactionType.TRANSFER, 5_000L, inr, "acc_bank", toAccountId = "acc_cash")

        val txns = listOf(
            ValidationTxn("txn_1", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", categoryId = "cat_groceries"),
            ValidationTxn("txn_2", TransactionType.TRANSFER, 5_000L, inr, "acc_bank", toAccountId = "acc_cash"),
        )
        val legs = JournalPosting.post(draft1) + JournalPosting.post(draft2)

        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = legs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertTrue(report.isValid)
        assertEquals(0, report.criticalCount)
        assertEquals(0, report.violations.size)
        assertEquals(2, report.checkedTransactions)
        assertEquals(4, report.checkedJournalEntries)
        assertEquals(3, report.checkedAccounts)
    }

    @Test
    fun testUnbalancedJournalEntryDetected() {
        val txns = listOf(
            ValidationTxn("txn_unbalanced", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", categoryId = "cat_groceries")
        )
        val unbalancedLegs = listOf(
            JournalEntry("txn_unbalanced", "acc_bank", null, -10_000L),
            JournalEntry("txn_unbalanced", null, "cat_groceries", 9_999L), // Does not balance!
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = unbalancedLegs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "JOURNAL_UNBALANCED" })
    }

    @Test
    fun testOrphanJournalLegDetected() {
        val orphanLegs = listOf(
            JournalEntry("txn_ghost", "acc_bank", null, -5_000L),
            JournalEntry("txn_ghost", null, "cat_groceries", 5_000L),
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = emptyList(), // No transaction declared!
            journalEntries = orphanLegs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "ORPHAN_JOURNAL_LEG" })
    }

    @Test
    fun testInvalidLegNodeTargetDetected() {
        val txns = listOf(
            ValidationTxn("txn_bad", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", categoryId = "cat_groceries")
        )
        val badLegs = listOf(
            JournalEntry("txn_bad", "acc_bank", "cat_groceries", -10_000L), // Both account and category set!
            JournalEntry("txn_bad", null, null, 10_000L),                   // Neither set!
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = badLegs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertEquals(2, report.violations.count { it.code == "INVALID_LEG_TARGET" })
    }

    @Test
    fun testUnknownAccountAndCategoryReferencesDetected() {
        val txns = listOf(
            ValidationTxn("txn_1", TransactionType.EXPENSE, 10_000L, inr, "acc_unknown", categoryId = "cat_unknown")
        )
        val legs = listOf(
            JournalEntry("txn_1", "acc_unknown", null, -10_000L),
            JournalEntry("txn_1", null, "cat_unknown", 10_000L),
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = legs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "UNKNOWN_ACCOUNT_REFERENCE" })
        assertTrue(report.violations.any { it.code == "UNKNOWN_CATEGORY_REFERENCE" })
    }

    @Test
    fun testTransferInvariantsDetected() {
        // Transfer to same account
        val badTransferTxn = ValidationTxn(
            "txn_same", TransactionType.TRANSFER, 5_000L, inr, "acc_bank", toAccountId = "acc_bank"
        )
        val badTransferLegs = listOf(
            JournalEntry("txn_same", "acc_bank", null, -5_000L),
            JournalEntry("txn_same", "acc_bank", null, 5_000L),
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = listOf(badTransferTxn),
            journalEntries = badTransferLegs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "TRANSFER_SAME_ACCOUNT" })
    }

    @Test
    fun testBalanceReconciliationMismatchDetected() {
        val txns = listOf(
            ValidationTxn("txn_1", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", categoryId = "cat_groceries")
        )
        val legs = JournalPosting.post(
            TransactionDraft("txn_1", TransactionType.EXPENSE, 10_000L, inr, "acc_bank", "cat_groceries")
        )
        // Opening balance 100_000 - 10_000 = 90_000 derived.
        // But recorded balance says 85_000!
        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = legs,
            accounts = accounts, // acc_bank has recorded 85_000L
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "BALANCE_RECONCILIATION_MISMATCH" })
    }

    @Test
    fun testDuplicateRecurringOccurrenceKeysDetected() {
        val keys = listOf(
            "txn_recur_rent_20261101",
            "txn_recur_netflix_20261105",
            "txn_recur_rent_20261101", // Duplicate!
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = emptyList(),
            journalEntries = emptyList(),
            accounts = accounts,
            validCategoryIds = validCategories,
            recurringOccurrenceKeys = keys,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "DUPLICATE_RECURRING_KEY" })
    }

    @Test
    fun testCurrencyMismatchWarning() {
        val txns = listOf(
            // Txn is INR but account is USD
            ValidationTxn("txn_fx", TransactionType.EXPENSE, 10_000L, inr, "acc_usd", categoryId = "cat_groceries")
        )
        val legs = listOf(
            JournalEntry("txn_fx", "acc_usd", null, -10_000L),
            JournalEntry("txn_fx", null, "cat_groceries", 10_000L),
        )
        val usdAccount = listOf(
            ValidationAccount("acc_usd", "Wise USD", usd, openingBalanceMinor = 50_000L, recordedBalanceMinor = 40_000L)
        )

        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = legs,
            accounts = usdAccount,
            validCategoryIds = validCategories,
        )

        // Warnings don't fail validity, but are logged
        assertTrue(report.isValid)
        assertEquals(1, report.warningCount)
        assertTrue(report.violations.any { it.code == "CURRENCY_MISMATCH" && it.severity == ViolationSeverity.WARNING })
    }

    @Test
    fun testDuplicateRrnFailsIntegrityCheck() {
        val txns = listOf(
            ValidationTxn("txn_1", TransactionType.EXPENSE, 50000L, inr, "acc_bank", categoryId = "cat_groceries", rrn = "327918291048"),
            ValidationTxn("txn_2", TransactionType.EXPENSE, 50000L, inr, "acc_bank", categoryId = "cat_groceries", rrn = "327918291048"),
        )
        val legs = listOf(
            JournalEntry("txn_1", "acc_bank", null, -50000L),
            JournalEntry("txn_1", null, "cat_groceries", 50000L),
            JournalEntry("txn_2", "acc_bank", null, -50000L),
            JournalEntry("txn_2", null, "cat_groceries", 50000L),
        )
        val report = LedgerIntegrityValidator.validate(
            transactions = txns,
            journalEntries = legs,
            accounts = accounts,
            validCategoryIds = validCategories,
        )

        assertFalse(report.isValid)
        assertTrue(report.violations.any { it.code == "DUPLICATE_RRN" })
    }
}
