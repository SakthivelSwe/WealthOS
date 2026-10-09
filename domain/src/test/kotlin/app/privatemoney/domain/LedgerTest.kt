package app.privatemoney.domain

import app.privatemoney.domain.ledger.BalanceCalculator
import app.privatemoney.domain.ledger.InvalidTransactionException
import app.privatemoney.domain.ledger.JournalPosting
import app.privatemoney.domain.ledger.TransactionDraft
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.domain.model.Money
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LedgerTest {
    private val inr = Currency.INR

    @Test
    fun parsesRupeesToMinorUnits() {
        assertEquals(10050L, Money.parse("100.50", inr).amountMinor)
        assertEquals(10000L, Money.parse("100", inr).amountMinor)
        assertFailsWith<IllegalArgumentException> { Money.parse("1.234", inr) }
    }

    @Test
    fun rejectsCurrencyMixing() {
        assertFailsWith<IllegalArgumentException> { Money(1, inr) + Money(1, Currency.USD) }
    }

    @Test
    fun overflowThrowsInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE, inr) + Money(1, inr) }
    }

    @Test
    fun expenseDecreasesAccountByExactAmount() {
        val entries = JournalPosting.post(TransactionDraft("t1", TransactionType.EXPENSE, 13000, inr, "bank", "food"))
        assertTrue(JournalPosting.isBalanced(entries))
        assertEquals(100_000L - 13000L, BalanceCalculator.accountBalance(100_000, "bank", entries))
    }

    @Test
    fun incomeIncreasesAccountByExactAmount() {
        val entries = JournalPosting.post(TransactionDraft("t1", TransactionType.INCOME, 3_220_000, inr, "bank", "salary"))
        assertEquals(3_220_000L + 5L, BalanceCalculator.accountBalance(5, "bank", entries))
    }

    @Test
    fun transferNeverChangesNetWorthOrExpenses() {
        val random = Random(42)
        repeat(500) { i ->
            val amount = random.nextLong(1, 10_000_000)
            val entries = JournalPosting.post(TransactionDraft("t$i", TransactionType.TRANSFER, amount, inr, "a", toAccountId = "b"))
            val before = BalanceCalculator.netWorth(listOf(500_000_000L, -20_000L))
            val after = BalanceCalculator.netWorth(listOf(
                BalanceCalculator.accountBalance(500_000_000L, "a", entries),
                BalanceCalculator.accountBalance(-20_000L, "b", entries),
            ))
            assertEquals(before, after)
            assertEquals(0L, BalanceCalculator.expenseTotal(null, entries))
        }
    }

    @Test
    fun everyRandomTransactionIsBalanced() {
        val random = Random(7)
        val all = (0 until 1000).flatMap { i ->
            val type = TransactionType.entries[random.nextInt(3)]
            val draft = TransactionDraft(
                "t$i", type, random.nextLong(1, 1_000_000), inr, "a",
                categoryId = if (type == TransactionType.TRANSFER) null else "c",
                toAccountId = if (type == TransactionType.TRANSFER) "b" else null,
            )
            JournalPosting.post(draft)
        }
        assertTrue(JournalPosting.isBalanced(all))
    }

    @Test
    fun invalidDraftsAreRejected() {
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 0, inr, "a", "c"))
        }
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.TRANSFER, 5, inr, "a", toAccountId = "a"))
        }
        assertFailsWith<InvalidTransactionException> {
            JournalPosting.post(TransactionDraft("t", TransactionType.EXPENSE, 5, inr, "a"))
        }
    }
}
