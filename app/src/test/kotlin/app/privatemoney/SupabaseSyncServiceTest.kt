package app.privatemoney

import app.privatemoney.cloud.SyncResult
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncServiceTest {

    @Test
    fun testSyncResultProperties() {
        val success = SyncResult(
            accountsPulled = 2,
            txnsPulled = 5,
            accountsPushed = 1,
            txnsPushed = 3,
            isSuccess = true,
            errorMessage = null,
        )

        assertTrue(success.isSuccess)
        assertEquals(2, success.accountsPulled)
        assertEquals(5, success.txnsPulled)
        assertEquals(1, success.accountsPushed)
        assertEquals(3, success.txnsPushed)
    }

    @Test
    fun testSyncResultErrorState() {
        val failure = SyncResult(
            isSuccess = false,
            errorMessage = "Please sign in with your email to sync.",
        )

        assertFalse(failure.isSuccess)
        assertEquals("Please sign in with your email to sync.", failure.errorMessage)
    }

    @Test
    fun testAccountJsonBilingualParsing() {
        val snakeJson = JSONObject().apply {
            put("id", "acc_101")
            put("name", "HDFC Salary")
            put("type", "BANK")
            put("opening_balance_minor", 15000000L)
            put("currency", "INR")
            put("include_in_net_worth", true)
            put("archived", false)
            put("created_at", 1729000000000L)
            put("updated_at", 1729000000000L)
        }

        assertEquals("acc_101", snakeJson.getString("id"))
        assertEquals("HDFC Salary", snakeJson.getString("name"))
        assertEquals("BANK", snakeJson.getString("type"))
        assertEquals(15000000L, snakeJson.getLong("opening_balance_minor"))
        assertEquals("INR", snakeJson.getString("currency"))
        assertTrue(snakeJson.getBoolean("include_in_net_worth"))
        assertFalse(snakeJson.getBoolean("archived"))
    }

    @Test
    fun testTransactionJsonParsingAndFormat() {
        val txnJson = JSONObject().apply {
            put("id", "txn_web_001")
            put("type", "EXPENSE")
            put("amount_minor", 250000L)
            put("currency", "INR")
            put("local_date", "2026-10-20")
            put("local_time", "12:30")
            put("account_id", "acc_101")
            put("merchant", "Blue Tokai Coffee")
            put("description", "Filter Coffee")
            put("need_want", "WANT")
        }

        assertEquals("txn_web_001", txnJson.getString("id"))
        assertEquals(250000L, txnJson.getLong("amount_minor"))
        assertEquals("2026-10-20", txnJson.getString("local_date"))
        assertEquals("acc_101", txnJson.getString("account_id"))
        assertEquals("Blue Tokai Coffee", txnJson.getString("merchant"))
        assertEquals("WANT", txnJson.getString("need_want"))
    }

    @Test
    fun testJournalEntriesBalanceIntegrity() {
        val entries = JSONArray().apply {
            put(JSONObject().apply {
                put("txn_id", "txn_web_001")
                put("account_id", "acc_101")
                put("amount_minor", -250000L)
            })
            put(JSONObject().apply {
                put("txn_id", "txn_web_001")
                put("category_id", "cat_food")
                put("amount_minor", 250000L)
            })
        }

        assertEquals(2, entries.length())
        var sum = 0L
        for (i in 0 until entries.length()) {
            sum += entries.getJSONObject(i).getLong("amount_minor")
        }
        assertEquals("Journal entries must sum to exactly zero", 0L, sum)
    }

    @Test
    fun testSalaryCycleSchemaMapping() {
        val cycle = app.privatemoney.data.local.SalaryCycleEntity(
            id = "cycle_2026_10",
            userId = "user_456",
            cycleStartDate = "2026-10-05",
            salaryPaise = 3220000L,
            baselinePaise = 1771000L,
            emiPaise = 500000L,
            framework = "SIX_JAR",
            jarAmountsJson = """{"NEEDS":2271000,"FREEDOM_SIP":322000,"EDUCATION":322000,"PLAY":0,"LONG_TERM":144000,"GIVING":161000}""",
            daysInCycle = 30,
            dailySafeUpiPaise = 0L,
            netWorthSnapshotPaise = 5000000L,
            checklistStateJson = """{"salaryAllocated":true}""",
        )

        val cycleJson = JSONObject().apply {
            put("id", cycle.id)
            put("user_id", cycle.userId)
            put("cycle_start_date", cycle.cycleStartDate)
            put("salary_paise", cycle.salaryPaise)
            put("baseline_paise", cycle.baselinePaise)
            put("emi_paise", cycle.emiPaise)
            put("framework", cycle.framework)
            put("jar_amounts", JSONObject(cycle.jarAmountsJson))
            put("days_in_cycle", cycle.daysInCycle)
            put("daily_safe_upi_paise", cycle.dailySafeUpiPaise)
            put("net_worth_snapshot_paise", cycle.netWorthSnapshotPaise)
        }

        assertEquals("cycle_2026_10", cycleJson.getString("id"))
        assertEquals(3220000L, cycleJson.getLong("salary_paise"))
        assertEquals(1771000L, cycleJson.getLong("baseline_paise"))
        assertEquals(500000L, cycleJson.getLong("emi_paise"))
        assertEquals("SIX_JAR", cycleJson.getString("framework"))
        assertEquals(30, cycleJson.getInt("days_in_cycle"))
        assertEquals(0L, cycleJson.getLong("daily_safe_upi_paise"))
        assertEquals(5000000L, cycleJson.getLong("net_worth_snapshot_paise"))
        assertEquals(2271000L, cycleJson.getJSONObject("jar_amounts").getLong("NEEDS"))
    }

    @Test
    fun testTransactionPhase3FieldsMapping() {
        val txn = app.privatemoney.data.local.TxnEntity(
            id = "txn_upi_001",
            type = "EXPENSE",
            amountMinor = 45000L,
            currency = "INR",
            localDate = "2026-10-06",
            localTime = "13:45",
            accountId = "acc_bank",
            toAccountId = null,
            categoryId = "cat_food",
            merchant = "SWIGGY",
            description = "Swiggy order",
            notes = null,
            paymentMethod = "UPI",
            needWant = "WANT",
            imported = true,
            source = "CSV_IMPORT",
            createdAt = 1728200000000L,
            updatedAt = 1728200000000L,
            deletedAt = null,
            jarBucket = "PLAY",
            spendEmotionTag = "PLANNED_WANT",
            paymentRail = "UPI_P2M",
            rrn = "327918291048",
            counterpartyName = "SWIGGY",
            vpa = "swiggy@hdfcbank",
            importBatchId = "batch_001",
            userOverridden = false,
            isRecurringMandate = false,
        )

        assertEquals("PLAY", txn.jarBucket)
        assertEquals("PLANNED_WANT", txn.spendEmotionTag)
        assertEquals("UPI_P2M", txn.paymentRail)
        assertEquals("327918291048", txn.rrn)
        assertEquals("SWIGGY", txn.counterpartyName)
        assertEquals("swiggy@hdfcbank", txn.vpa)
        assertEquals("batch_001", txn.importBatchId)
        assertFalse(txn.userOverridden)
        assertFalse(txn.isRecurringMandate)
    }

    @Test
    fun testAccountPhase3ExtensionsMapping() {
        val acc = app.privatemoney.data.local.AccountEntity(
            id = "acc_loan_001",
            name = "HDFC Personal Loan",
            type = "LOAN",
            institution = "HDFC Bank",
            lastFour = "4321",
            openingBalanceMinor = -50000000L,
            currency = "INR",
            includeInNetWorth = true,
            includeInStats = false,
            archived = false,
            createdAt = 1728000000000L,
            updatedAt = 1728000000000L,
            accountRole = "LIABILITY_LOAN",
            bankName = "HDFC Bank",
            interestRatePercentBps = 1250, // 12.5%
            monthlyEmiPaise = 500000L, // ₹5,000
            outstandingPaise = 50000000L,
            isBnpl = false,
        )

        assertEquals("LIABILITY_LOAN", acc.accountRole)
        assertEquals("HDFC Bank", acc.bankName)
        assertEquals(1250, acc.interestRatePercentBps)
        assertEquals(500000L, acc.monthlyEmiPaise)
        assertEquals(50000000L, acc.outstandingPaise)
        assertFalse(acc.isBnpl)
    }
}
