package app.privatemoney.domain

import app.privatemoney.domain.imports.DuplicateCandidate
import app.privatemoney.domain.imports.DuplicateDetector
import app.privatemoney.domain.imports.Likelihood
import app.privatemoney.domain.imports.StatementParser
import app.privatemoney.domain.ledger.TransactionType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StatementParserTest {

    @Test
    fun parsesStandardBankCsvWithDebitCreditColumns() {
        val csv = """
            Txn Date,Description,Withdrawal (Dr),Deposit (Cr),Balance
            2026-03-10,UPI-Swiggy-12345678,450.00,,15000.00
            2026-03-11,Salary Credit from ACME Inc,,85000.00,100000.00
            2026-03-12,ATM CASH WDL MUMBAI,2000.00,,98000.00
        """.trimIndent()

        val result = StatementParser.parseCsv(csv)
        assertEquals(3, result.rows.size)
        assertEquals(0, result.invalidRows)

        // Row 1: Expense
        val row1 = result.rows[0]
        assertEquals(LocalDate.of(2026, 3, 10), row1.date)
        assertEquals(45000L, row1.amountMinor)
        assertEquals(TransactionType.EXPENSE, row1.type)
        assertTrue(row1.merchant?.contains("Swiggy", ignoreCase = true) == true)

        // Row 2: Income
        val row2 = result.rows[1]
        assertEquals(LocalDate.of(2026, 3, 11), row2.date)
        assertEquals(8500000L, row2.amountMinor)
        assertEquals(TransactionType.INCOME, row2.type)

        // Row 3: Expense
        val row3 = result.rows[2]
        assertEquals(200000L, row3.amountMinor)
        assertEquals(TransactionType.EXPENSE, row3.type)
    }

    @Test
    fun parsesSingleAmountColumnWithSigns() {
        val csv = """
            Date,Narration,Amount
            15/04/2026,Netflix Subscription,-649.00
            16/04/2026,Freelance Payment,12500.50
        """.trimIndent()

        val result = StatementParser.parseCsv(csv)
        assertEquals(2, result.rows.size)

        val netflix = result.rows[0]
        assertEquals(LocalDate.of(2026, 4, 15), netflix.date)
        assertEquals(64900L, netflix.amountMinor)
        assertEquals(TransactionType.EXPENSE, netflix.type)

        val freelance = result.rows[1]
        assertEquals(LocalDate.of(2026, 4, 16), freelance.date)
        assertEquals(1250050L, freelance.amountMinor)
        assertEquals(TransactionType.INCOME, freelance.type)
    }

    @Test
    fun parsesAmountMinorWithPenceAndPaiseAccurately() {
        assertEquals(10050L, StatementParser.parseAmountMinor("100.50"))
        assertEquals(10000L, StatementParser.parseAmountMinor("100"))
        assertEquals(10005L, StatementParser.parseAmountMinor("100.05"))
        assertEquals(9999999L, StatementParser.parseAmountMinor("99,999.99"))
        assertEquals(0L, StatementParser.parseAmountMinor(""))
        assertEquals(0L, StatementParser.parseAmountMinor("abc"))
    }

    @Test
    fun duplicateDetectionIdentifiesExactAndNearMatches() {
        val existing = DuplicateCandidate(
            date = LocalDate.of(2026, 3, 10),
            amountMinor = 45000L,
            merchant = "Swiggy",
            reference = null,
            accountId = "acc_1",
        )

        // Exact match
        val identical = DuplicateCandidate(
            date = LocalDate.of(2026, 3, 10),
            amountMinor = 45000L,
            merchant = "Swiggy",
            reference = null,
            accountId = "acc_1",
        )
        assertEquals(Likelihood.LIKELY, DuplicateDetector.compare(identical, existing))

        // Next day similar merchant is POSSIBLE
        val nextDay = DuplicateCandidate(
            date = LocalDate.of(2026, 3, 11),
            amountMinor = 45000L,
            merchant = "Swiggy",
            reference = null,
            accountId = "acc_1",
        )
        assertEquals(Likelihood.POSSIBLE, DuplicateDetector.compare(nextDay, existing))

        // Different amount is NONE
        val differentAmount = DuplicateCandidate(
            date = LocalDate.of(2026, 3, 10),
            amountMinor = 55000L,
            merchant = "Swiggy",
            reference = null,
            accountId = "acc_1",
        )
        assertEquals(Likelihood.NONE, DuplicateDetector.compare(differentAmount, existing))
    }
}
