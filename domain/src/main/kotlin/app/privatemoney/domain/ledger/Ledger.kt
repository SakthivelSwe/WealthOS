package app.privatemoney.domain.ledger

import app.privatemoney.domain.model.Currency

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
        if (draft.amountMinor <= 0L) throw InvalidTransactionException("Amount must be positive")
        val amount = draft.amountMinor
        return when (draft.type) {
            TransactionType.EXPENSE -> {
                val category = draft.categoryId ?: throw InvalidTransactionException("Category required")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, -amount),
                    JournalEntry(draft.id, null, category, amount),
                )
            }
            TransactionType.INCOME -> {
                val category = draft.categoryId ?: throw InvalidTransactionException("Category required")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, amount),
                    JournalEntry(draft.id, null, category, -amount),
                )
            }
            TransactionType.TRANSFER -> {
                val to = draft.toAccountId ?: throw InvalidTransactionException("Destination required")
                if (to == draft.accountId) throw InvalidTransactionException("Accounts must differ")
                listOf(
                    JournalEntry(draft.id, draft.accountId, null, -amount),
                    JournalEntry(draft.id, to, null, amount),
                )
            }
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
}
