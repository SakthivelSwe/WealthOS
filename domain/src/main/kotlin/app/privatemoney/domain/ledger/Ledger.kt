package app.privatemoney.domain.ledger

import app.privatemoney.domain.model.Currency

/**
 * DOUBLE-ENTRY LEDGER SEMANTICS & ACCOUNT SIGN CONVENTIONS
 * -------------------------------------------------------------------------------------
 * 1. Assets (BANK, CASH, UPI_WALLET, SAVINGS, INVESTMENT, OTHER_ASSET):
 *    - Positive balance represents funds held / owned by user (e.g., ₹50,000 = +5,000,000 minor).
 *    - An expense CREDITS the asset (-amountMinor), reducing balance.
 *    - An income DEBITS the asset (+amountMinor), increasing balance.
 *
 * 2. Liabilities (CREDIT_CARD, LOAN, OTHER_LIABILITY):
 *    - Negative balance represents debt owed by user (e.g., ₹10,000 credit card bill = -1,000,000 minor).
 *    - A credit card purchase (expense) CREDITS the liability (-amountMinor), making it more negative.
 *    - A payment/transfer to the credit card DEBITS the liability (+amountMinor), reducing debt towards 0.
 *    - Zero balance represents fully paid / no outstanding liability.
 *
 * 3. Income & Expense Nodes (Category legs):
 *    - Expense transactions debit the category (+amountMinor) and credit the account (-amountMinor).
 *    - Income transactions credit the category (-amountMinor) and debit the account (+amountMinor).
 *    - Every transaction's legs sum to exactly ZERO: sum(amountMinor) == 0.
 *
 * 4. Transfers (Between any two accounts):
 *    - Source account CREDITED (-amountMinor).
 *    - Destination account DEBITED (+amountMinor).
 *    - Net worth change = 0. No income or expense category legs generated.
 * -------------------------------------------------------------------------------------
 */

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/**
 * A validated request to record money movement, in minor units.
 * EXPENSE/INCOME use [accountId] + [categoryId]. TRANSFER uses [accountId] (source) + [toAccountId].
 */
data class TransactionDraft(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: Currency,
    val accountId: String,
    val categoryId: String? = null,
    val toAccountId: String? = null,
)

/**
 * One leg of a transaction. Exactly one of [accountId]/[categoryId] is set.
 * [amountMinor] is signed: for accounts it is the balance change (liabilities are negative balances);
 * the category leg carries the opposite sign so each transaction sums to zero.
 */
data class JournalEntry(
    val txnId: String,
    val accountId: String?,
    val categoryId: String?,
    val amountMinor: Long,
)

class InvalidTransactionException(message: String) : IllegalArgumentException(message)

object JournalPosting {

    fun post(draft: TransactionDraft): List<JournalEntry> {
        if (draft.id.isBlank()) throw InvalidTransactionException("Transaction ID required")
        if (draft.amountMinor <= 0L) throw InvalidTransactionException("Amount must be positive")
        if (draft.accountId.isBlank()) throw InvalidTransactionException("Account required")
        val amount = draft.amountMinor
        return when (draft.type) {
            TransactionType.EXPENSE -> {
                val category = draft.categoryId?.takeIf { it.isNotBlank() }
                    ?: throw InvalidTransactionException("Category required")
                if (draft.toAccountId != null) throw InvalidTransactionException("Expense cannot have a destination account")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, -amount),
                    JournalEntry(draft.id, null, category, amount),
                )
            }
            TransactionType.INCOME -> {
                val category = draft.categoryId?.takeIf { it.isNotBlank() }
                    ?: throw InvalidTransactionException("Category required")
                if (draft.toAccountId != null) throw InvalidTransactionException("Income cannot have a destination account")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, amount),
                    JournalEntry(draft.id, null, category, -amount),
                )
            }
            TransactionType.TRANSFER -> {
                val to = draft.toAccountId?.takeIf { it.isNotBlank() }
                    ?: throw InvalidTransactionException("Destination required")
                if (to == draft.accountId) throw InvalidTransactionException("Accounts must differ")
                if (draft.categoryId != null) throw InvalidTransactionException("Transfer cannot have a category")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, -amount),
                    JournalEntry(draft.id, to, null, amount),
                )
            }
        }
    }

    /**
     * Creates balanced reversal entries for an existing set of journal legs.
     * Inverts the sign of every leg under a new transaction ID.
     */
    fun reverse(originalEntries: List<JournalEntry>, reversalTxnId: String): List<JournalEntry> {
        if (reversalTxnId.isBlank()) throw InvalidTransactionException("Reversal transaction ID required")
        return originalEntries.map { entry ->
            JournalEntry(
                txnId = reversalTxnId,
                accountId = entry.accountId,
                categoryId = entry.categoryId,
                amountMinor = Math.negateExact(entry.amountMinor),
            )
        }
    }

    /** True when every transaction's legs sum to exactly zero and each leg targets one node. */
    fun isBalanced(entries: List<JournalEntry>): Boolean {
        if (entries.any { (it.accountId == null) == (it.categoryId == null) }) return false
        return entries.groupBy { it.txnId }.values.all { legs ->
            legs.size >= 2 && legs.fold(0L) { acc, e -> Math.addExact(acc, e.amountMinor) } == 0L
        }
    }
}

object BalanceCalculator {

    /** Balance = opening balance + sum of account legs. */
    fun accountBalance(openingMinor: Long, accountId: String, entries: List<JournalEntry>): Long =
        entries.asSequence()
            .filter { it.accountId == accountId }
            .fold(openingMinor) { acc, e -> Math.addExact(acc, e.amountMinor) }

    /** Net worth = sum of signed balances of included accounts. Transfers cancel out. */
    fun netWorth(balancesMinor: Collection<Long>): Long =
        balancesMinor.fold(0L) { acc, b -> Math.addExact(acc, b) }

    /** Total spent in a category set; transfers are never included because they have no category leg. */
    fun expenseTotal(categoryIds: Set<String>?, entries: List<JournalEntry>): Long =
        entries.asSequence()
            .filter { it.categoryId != null && it.amountMinor > 0 }
            .filter { categoryIds == null || it.categoryId in categoryIds }
            .fold(0L) { acc, e -> Math.addExact(acc, e.amountMinor) }

    /** Total earned across income categories. */
    fun incomeTotal(categoryIds: Set<String>?, entries: List<JournalEntry>): Long =
        entries.asSequence()
            .filter { it.categoryId != null && it.amountMinor < 0 }
            .filter { categoryIds == null || it.categoryId in categoryIds }
            .fold(0L) { acc, e -> Math.addExact(acc, -e.amountMinor) }

    /** Net spent in a category set (expenses minus refunds/reversals on category legs). */
    fun netExpenseTotal(categoryIds: Set<String>?, entries: List<JournalEntry>): Long =
        entries.asSequence()
            .filter { it.categoryId != null }
            .filter { categoryIds == null || it.categoryId in categoryIds }
            .fold(0L) { acc, e -> Math.addExact(acc, e.amountMinor) }
}
