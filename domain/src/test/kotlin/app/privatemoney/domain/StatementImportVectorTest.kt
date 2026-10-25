package app.privatemoney.domain

import app.privatemoney.domain.imports.StatementParser
import app.privatemoney.domain.ledger.TransactionType
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementImportVectorTest {

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
    fun testAxisStatementVector() {
        val file = File(findVectorsDir(), "parser/axis_statement.csv")
        assertTrue(file.exists(), "axis_statement.csv must exist")
        val content = file.readText()

        val result = StatementParser.parseCsv(content)
        assertEquals(5, result.rows.size, "Should parse 5 transaction rows")
        assertEquals(3220000L, result.totalCreditsMinor, "Credits should be ₹32,200.00")
        assertEquals(618400L, result.totalDebitsMinor, "Debits should be ₹6,184.00")
        assertEquals(0L, result.openingBalanceMinor, "Opening balance should be ₹0.00")
        assertEquals(2601600L, result.closingBalanceMinor, "Closing balance should be ₹26,016.00")
        assertTrue(result.isReconciled, "Statement must mathematically reconcile")

        assertEquals(1, result.smallScansCount, "Small scans count")
        assertEquals(8500L, result.smallScansTotalMinor, "Small scans total ₹85.00")
        assertEquals(2, result.detectedMandatesCount, "Mandates detected: Salary and Netflix")

        // First row: Salary credit
        val r1 = result.rows[0]
        assertEquals(LocalDate.of(2026, 10, 5), r1.date)
        assertEquals(3220000L, r1.amountMinor)
        assertEquals(TransactionType.INCOME, r1.type)
        assertEquals("TECHCORP SALARY", r1.merchant)
        assertEquals("N0981726451", r1.reference)

        // Second row: Swiggy
        val r2 = result.rows[1]
        assertEquals(LocalDate.of(2026, 10, 6), r2.date)
        assertEquals(45000L, r2.amountMinor)
        assertEquals(TransactionType.EXPENSE, r2.type)
        assertEquals("SWIGGY", r2.merchant)
        assertEquals("327918291048", r2.reference)
        assertEquals("PLAY", r2.jarBucket)
    }

    @Test
    fun testHdfcSingleColVector() {
        val file = File(findVectorsDir(), "parser/hdfc_single_col.csv")
        assertTrue(file.exists(), "hdfc_single_col.csv must exist")
        val content = file.readText()

        val result = StatementParser.parseCsv(content)
        assertEquals(2, result.rows.size, "Should parse 2 rows")
        assertEquals(4500000L, result.totalCreditsMinor)
        assertEquals(35000L, result.totalDebitsMinor)

        val r1 = result.rows[0]
        assertEquals(LocalDate.of(2026, 10, 1), r1.date)
        assertEquals(35000L, r1.amountMinor)
        assertEquals(TransactionType.EXPENSE, r1.type)
        assertEquals("SWIGGY", r1.merchant)
        assertEquals("482910394857", r1.reference)

        val r2 = result.rows[1]
        assertEquals(LocalDate.of(2026, 10, 2), r2.date)
        assertEquals(4500000L, r2.amountMinor)
        assertEquals(TransactionType.INCOME, r2.type)
        assertEquals("CMS987654321", r2.reference)
    }
}
