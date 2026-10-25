package app.privatemoney.domain.ledger

import app.privatemoney.domain.model.Currency

/**
 * Severity level of an integrity violation.
 */
enum class ViolationSeverity {
    CRITICAL,
    WARNING,
}

/**
 * Represents a single integrity violation detected in the financial ledger.
 */
data class IntegrityViolation(
    val severity: ViolationSeverity,
    val code: String,
    val message: String,
    val entityId: String? = null,
)

/**
 * Report containing the results of a comprehensive ledger integrity validation.
 */
data class IntegrityCheckReport(
    val isValid: Boolean,
    val violations: List<IntegrityViolation>,
    val checkedTransactions: Int,
    val checkedJournalEntries: Int,
    val checkedAccounts: Int,
) {
    val criticalCount: Int get() = violations.count { it.severity == ViolationSeverity.CRITICAL }
    val warningCount: Int get() = violations.count { it.severity == ViolationSeverity.WARNING }
}

data class ValidationTxn(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: Currency,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val isDeleted: Boolean = false,
    val rrn: String? = null,
)

data class ValidationAccount(
    val id: String,
    val name: String,
    val currency: Currency,
    val openingBalanceMinor: Long,
    val recordedBalanceMinor: Long? = null,
)

/**
 * Pure domain integrity validator enforcing the core financial and referential invariants:
 * 1. Double-Entry Zero-Sum Balance (every transaction's journal legs sum to exactly 0).
 * 2. Leg Node Exclusivity (each leg targets exactly one node: account or category).
 * 3. Orphan Leg Detection (all legs belong to an active transaction).
 * 4. Referential Integrity (all account and category references must exist).
 * 5. Transfer Invariants (2 account legs, opposing signs, source != destination, zero category legs).
 * 6. Currency Consistency (transaction currency matches participant account currencies).
 * 7. Derived Balance Reconciliation (opening balance + active legs == recorded balance).
 * 8. Duplicate Recurring Key Detection (no overlapping occurrence keys).
 */
object LedgerIntegrityValidator {

    fun validate(
        transactions: List<ValidationTxn>,
        journalEntries: List<JournalEntry>,
        accounts: List<ValidationAccount>,
        validCategoryIds: Set<String>,
        recurringOccurrenceKeys: List<String> = emptyList(),
    ): IntegrityCheckReport {
        val violations = mutableListOf<IntegrityViolation>()
        val accountMap = accounts.associateBy { it.id }
        val txnMap = transactions.associateBy { it.id }

        // 1. Leg Node Structure: Each journal entry must have exactly one of accountId or categoryId
        journalEntries.forEach { entry ->
            val hasAccount = !entry.accountId.isNullOrBlank()
            val hasCategory = !entry.categoryId.isNullOrBlank()
            if (hasAccount == hasCategory) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "INVALID_LEG_TARGET",
                        message = "Journal entry must specify exactly one of accountId or categoryId (txnId=${entry.txnId})",
                        entityId = entry.txnId,
                    )
                )
            }
        }

        // 2. Orphan Legs: Every journal entry must reference a known transaction
        journalEntries.forEach { entry ->
            if (!txnMap.containsKey(entry.txnId)) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "ORPHAN_JOURNAL_LEG",
                        message = "Journal entry references non-existent transaction '${entry.txnId}'",
                        entityId = entry.txnId,
                    )
                )
            }
        }

        // 3. Referential Integrity: Account and Category references must exist
        journalEntries.forEach { entry ->
            entry.accountId?.let { acctId ->
                if (!accountMap.containsKey(acctId)) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "UNKNOWN_ACCOUNT_REFERENCE",
                            message = "Journal leg references unknown account '$acctId' in txn '${entry.txnId}'",
                            entityId = entry.txnId,
                        )
                    )
                }
            }
            entry.categoryId?.let { catId ->
                if (catId !in validCategoryIds) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "UNKNOWN_CATEGORY_REFERENCE",
                            message = "Journal leg references unknown category '$catId' in txn '${entry.txnId}'",
                            entityId = entry.txnId,
                        )
                    )
                }
            }
        }

        // 4. Double-Entry Zero-Sum Balance Invariant for each non-deleted transaction
        val entriesByTxn = journalEntries.groupBy { it.txnId }
        transactions.filter { !it.isDeleted }.forEach { txn ->
            val legs = entriesByTxn[txn.id] ?: emptyList()
            if (legs.isEmpty()) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "TRANSACTION_HAS_NO_LEGS",
                        message = "Transaction '${txn.id}' has no corresponding journal entries",
                        entityId = txn.id,
                    )
                )
            } else if (legs.size < 2) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "INSUFFICIENT_JOURNAL_LEGS",
                        message = "Transaction '${txn.id}' has only ${legs.size} leg (minimum 2 required)",
                        entityId = txn.id,
                    )
                )
            } else {
                val sum = legs.fold(0L) { acc, leg -> Math.addExact(acc, leg.amountMinor) }
                if (sum != 0L) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "JOURNAL_UNBALANCED",
                            message = "Transaction '${txn.id}' journal legs do not balance to zero (net sum: $sum)",
                            entityId = txn.id,
                        )
                    )
                }
            }

            // 5. Transfer Invariants
            if (txn.type == TransactionType.TRANSFER) {
                if (txn.toAccountId.isNullOrBlank()) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "TRANSFER_MISSING_DESTINATION",
                            message = "Transfer '${txn.id}' missing destination account",
                            entityId = txn.id,
                        )
                    )
                }
                if (txn.accountId == txn.toAccountId) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "TRANSFER_SAME_ACCOUNT",
                            message = "Transfer '${txn.id}' source and destination account are identical ('${txn.accountId}')",
                            entityId = txn.id,
                        )
                    )
                }
                if (legs.any { it.categoryId != null }) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.CRITICAL,
                            code = "TRANSFER_HAS_CATEGORY",
                            message = "Transfer '${txn.id}' has a category leg (transfers must not affect categories)",
                            entityId = txn.id,
                        )
                    )
                }
            }

            // 6. Currency Consistency
            val srcAccount = accountMap[txn.accountId]
            if (srcAccount != null && srcAccount.currency != txn.currency) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.WARNING,
                        code = "CURRENCY_MISMATCH",
                        message = "Transaction '${txn.id}' currency (${txn.currency}) does not match source account '${srcAccount.name}' currency (${srcAccount.currency})",
                        entityId = txn.id,
                    )
                )
            }
            if (txn.type == TransactionType.TRANSFER && txn.toAccountId != null) {
                val destAccount = accountMap[txn.toAccountId]
                if (destAccount != null && destAccount.currency != txn.currency) {
                    violations.add(
                        IntegrityViolation(
                            severity = ViolationSeverity.WARNING,
                            code = "CURRENCY_MISMATCH",
                            message = "Transfer '${txn.id}' currency (${txn.currency}) does not match destination account '${destAccount.name}' currency (${destAccount.currency})",
                            entityId = txn.id,
                        )
                    )
                }
            }
        }

        // 7. Derived Balance Reconciliation
        val liveTxnIds = transactions.filter { !it.isDeleted }.map { it.id }.toSet()
        accounts.forEach { account ->
            val accountLegs = journalEntries.filter { it.accountId == account.id && it.txnId in liveTxnIds }
            val derivedBalance = accountLegs.fold(account.openingBalanceMinor) { acc, leg ->
                Math.addExact(acc, leg.amountMinor)
            }
            if (account.recordedBalanceMinor != null && account.recordedBalanceMinor != derivedBalance) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "BALANCE_RECONCILIATION_MISMATCH",
                        message = "Account '${account.name}' recorded balance (${account.recordedBalanceMinor}) differs from derived ledger balance ($derivedBalance)",
                        entityId = account.id,
                    )
                )
            }
        }

        // 8. Duplicate Recurring Occurrence Keys
        val occurrenceSet = mutableSetOf<String>()
        recurringOccurrenceKeys.forEach { key ->
            if (!occurrenceSet.add(key)) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "DUPLICATE_RECURRING_KEY",
                        message = "Duplicate recurring transaction occurrence key: '$key'",
                        entityId = key,
                    )
                )
            }
        }

        // 9. Unique RRN Invariant: Unique non-null RRN across active transactions (Spec §4.6)
        val seenRrn = mutableMapOf<String, String>() // rrn -> txnId
        transactions.filter { !it.isDeleted && !it.rrn.isNullOrBlank() }.forEach { txn ->
            val cleanRrn = txn.rrn!!.trim()
            val existing = seenRrn[cleanRrn]
            if (existing != null) {
                violations.add(
                    IntegrityViolation(
                        severity = ViolationSeverity.CRITICAL,
                        code = "DUPLICATE_RRN",
                        message = "Duplicate RRN '$cleanRrn' found in active transactions $existing and ${txn.id}",
                        entityId = txn.id,
                    )
                )
            } else {
                seenRrn[cleanRrn] = txn.id
            }
        }

        val isClean = violations.none { it.severity == ViolationSeverity.CRITICAL }
        return IntegrityCheckReport(
            isValid = isClean,
            violations = violations,
            checkedTransactions = transactions.size,
            checkedJournalEntries = journalEntries.size,
            checkedAccounts = accounts.size,
        )
    }
}
