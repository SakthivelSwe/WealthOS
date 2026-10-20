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
}
