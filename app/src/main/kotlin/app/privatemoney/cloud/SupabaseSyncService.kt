package app.privatemoney.cloud

import android.content.Context
import androidx.room.withTransaction
import app.privatemoney.data.AppSettings
import app.privatemoney.data.local.AccountEntity
import app.privatemoney.data.local.AppDatabase
import app.privatemoney.data.local.CategoryEntity
import app.privatemoney.data.local.ChitFundEntity
import app.privatemoney.data.local.DebtEntity
import app.privatemoney.data.local.GoalEntity
import app.privatemoney.data.local.JournalEntryEntity
import app.privatemoney.data.local.RecurringTxnEntity
import app.privatemoney.data.local.TxnEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SyncResult(
    val accountsPulled: Int = 0,
    val txnsPulled: Int = 0,
    val accountsPushed: Int = 0,
    val txnsPushed: Int = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * High-performance, idempotent bidirectional synchronization service between
 * Android Room SQLite and Supabase PostgreSQL tables.
 */
class SupabaseSyncService(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: AppSettings = AppSettings(context),
) {

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        val userId = settings.supabaseUserId
        val token = settings.supabaseAccessToken
        val url = settings.supabaseUrl.ifBlank { AppSettings.DEFAULT_SUPABASE_URL }.trimEnd('/')
        val anonKey = settings.supabaseAnonKey

        if (userId.isNullOrBlank() || token.isNullOrBlank()) {
            val res = SyncResult(isSuccess = false, errorMessage = "Please sign in with your email to sync.")
            settings.lastSyncStatus = "NOT_AUTHENTICATED"
            return@withContext res
        }

        if (anonKey.isBlank()) {
            val res = SyncResult(isSuccess = false, errorMessage = "Supabase Anon Key is missing.")
            settings.lastSyncStatus = "CONFIG_MISSING"
            return@withContext res
        }

        try {
            // Step 1: Pull from Supabase
            val pullResult = pullFromSupabase(url, anonKey, token, userId)

            // Step 2: Push local changes up to Supabase
            val pushResult = pushToSupabase(url, anonKey, token, userId)

            settings.lastSyncTimestamp = System.currentTimeMillis()
            settings.lastSyncStatus = "SUCCESS"

            SyncResult(
                accountsPulled = pullResult.accountsPulled,
                txnsPulled = pullResult.txnsPulled,
                accountsPushed = pushResult.accountsPushed,
                txnsPushed = pushResult.txnsPushed,
                isSuccess = true,
            )
        } catch (e: Exception) {
            val msg = e.message ?: "Sync encountered an unexpected error"
            settings.lastSyncStatus = "FAILED"
            SyncResult(isSuccess = false, errorMessage = msg)
        }
    }

    private suspend fun pullFromSupabase(
        baseUrl: String,
        anonKey: String,
        token: String,
        userId: String,
    ): SyncResult {
        // 1. Categories
        val categoriesJson = httpGet("$baseUrl/rest/v1/user_categories?user_id=eq.$userId&archived=eq.false", anonKey, token)
        val categoryEntities = mutableListOf<CategoryEntity>()
        for (i in 0 until categoriesJson.length()) {
            val o = categoriesJson.getJSONObject(i)
            categoryEntities.add(
                CategoryEntity(
                    id = o.optStringLenient("id", "id"),
                    parentId = o.optNullableStringLenient("parentId", "parent_id"),
                    name = o.optStringLenient("name", "name"),
                    kind = o.optStringLenient("kind", "kind", "EXPENSE"),
                    needWant = o.optNullableStringLenient("needWant", "need_want"),
                    icon = o.optStringLenient("icon", "icon", "category"),
                    color = o.optLongLenient("color", "color", 0xFF8E8E96),
                    archived = o.optBooleanLenient("archived", "archived", false),
                    sortOrder = o.optIntLenient("sortOrder", "sort_order", 0),
                )
            )
        }

        // 2. Accounts
        val accountsJson = httpGet("$baseUrl/rest/v1/user_accounts?user_id=eq.$userId&archived=eq.false", anonKey, token)
        val accountEntities = mutableListOf<AccountEntity>()
        for (i in 0 until accountsJson.length()) {
            val o = accountsJson.getJSONObject(i)
            accountEntities.add(
                AccountEntity(
                    id = o.optStringLenient("id", "id"),
                    name = o.optStringLenient("name", "name"),
                    type = o.optStringLenient("type", "type", "BANK"),
                    institution = o.optNullableStringLenient("institution", "institution"),
                    lastFour = o.optNullableStringLenient("lastFour", "last_four"),
                    openingBalanceMinor = o.optLongLenient("openingBalanceMinor", "opening_balance_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    includeInNetWorth = o.optBooleanLenient("includeInNetWorth", "include_in_net_worth", true),
                    includeInStats = o.optBooleanLenient("includeInStats", "include_in_stats", true),
                    archived = o.optBooleanLenient("archived", "archived", false),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                )
            )
        }

        // 3. Transactions
        val txnsJson = httpGet("$baseUrl/rest/v1/user_transactions?user_id=eq.$userId&deleted_at=is.null", anonKey, token)
        val txnEntities = mutableListOf<TxnEntity>()
        for (i in 0 until txnsJson.length()) {
            val o = txnsJson.getJSONObject(i)
            val dateStr = o.optStringLenient("localDate", "local_date", o.optStringLenient("date", "date"))
            txnEntities.add(
                TxnEntity(
                    id = o.optStringLenient("id", "id"),
                    type = o.optStringLenient("type", "type", "EXPENSE"),
                    amountMinor = o.optLongLenient("amountMinor", "amount_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    localDate = if (dateStr.length >= 10) dateStr.substring(0, 10) else dateStr,
                    localTime = o.optStringLenient("localTime", "local_time", "00:00"),
                    accountId = o.optStringLenient("accountId", "account_id"),
                    toAccountId = o.optNullableStringLenient("toAccountId", "to_account_id"),
                    categoryId = o.optNullableStringLenient("categoryId", "category_id"),
                    merchant = o.optNullableStringLenient("merchant", "merchant"),
                    description = o.optNullableStringLenient("description", "description"),
                    notes = o.optNullableStringLenient("notes", "notes"),
                    paymentMethod = o.optNullableStringLenient("paymentMethod", "payment_method"),
                    needWant = o.optNullableStringLenient("needWant", "need_want"),
                    imported = o.optBooleanLenient("imported", "imported", false),
                    source = o.optStringLenient("source", "source", "WEB_SYNC"),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                    deletedAt = null,
                )
            )
        }

        // 4. Journal Entries
        val journalJson = httpGet("$baseUrl/rest/v1/user_journal_entries?user_id=eq.$userId", anonKey, token)
        val journalEntities = mutableListOf<JournalEntryEntity>()
        for (i in 0 until journalJson.length()) {
            val o = journalJson.getJSONObject(i)
            journalEntities.add(
                JournalEntryEntity(
                    txnId = o.optStringLenient("txnId", "txn_id"),
                    accountId = o.optNullableStringLenient("accountId", "account_id"),
                    categoryId = o.optNullableStringLenient("categoryId", "category_id"),
                    amountMinor = o.optLongLenient("amountMinor", "amount_minor", 0L),
                )
            )
        }

        // 5. Goals
        val goalsJson = httpGet("$baseUrl/rest/v1/user_goals?user_id=eq.$userId&archived=eq.false", anonKey, token)
        val goalEntities = mutableListOf<GoalEntity>()
        for (i in 0 until goalsJson.length()) {
            val o = goalsJson.getJSONObject(i)
            goalEntities.add(
                GoalEntity(
                    id = o.optStringLenient("id", "id"),
                    name = o.optStringLenient("name", "name"),
                    targetAmountMinor = o.optLongLenient("targetAmountMinor", "target_amount_minor", 0L),
                    currentAmountMinor = o.optLongLenient("currentAmountMinor", "current_amount_minor", 0L),
                    plannedMonthlyContributionMinor = o.optLongLenient("plannedMonthlyContributionMinor", "planned_monthly_contribution_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    targetDate = o.optStringLenient("targetDate", "target_date"),
                    archived = o.optBooleanLenient("archived", "archived", false),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                )
            )
        }

        // 6. Debts
        val debtsJson = httpGet("$baseUrl/rest/v1/user_debts?user_id=eq.$userId&archived=eq.false", anonKey, token)
        val debtEntities = mutableListOf<DebtEntity>()
        for (i in 0 until debtsJson.length()) {
            val o = debtsJson.getJSONObject(i)
            debtEntities.add(
                DebtEntity(
                    id = o.optStringLenient("id", "id"),
                    name = o.optStringLenient("name", "name"),
                    principalMinor = o.optLongLenient("principalMinor", "principal_minor", 0L),
                    currentBalanceMinor = o.optLongLenient("currentBalanceMinor", "current_balance_minor", 0L),
                    annualPercent = o.optStringLenient("annualPercent", "annual_percent", "0"),
                    expectedMonthlyPaymentMinor = o.optLongLenient("expectedMonthlyPaymentMinor", "expected_monthly_payment_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    archived = o.optBooleanLenient("archived", "archived", false),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                )
            )
        }

        // Deduplicate transactions against local state
        val existingTxns = db.ledgerDao().getAllForBackup().map { it.id }.toSet()
        val newTxns = txnEntities.filterNot { existingTxns.contains(it.id) }
        val newTxnIds = newTxns.map { it.id }.toSet()
        val newJournalEntries = journalEntities.filter { newTxnIds.contains(it.txnId) }

        db.withTransaction {
            if (categoryEntities.isNotEmpty()) {
                db.categoryDao().insertAll(categoryEntities)
            }
            if (accountEntities.isNotEmpty()) {
                db.accountDao().restoreAccounts(accountEntities)
            }
            if (newTxns.isNotEmpty()) {
                db.ledgerDao().restoreTxns(newTxns)
            }
            if (newJournalEntries.isNotEmpty()) {
                db.ledgerDao().restoreEntries(newJournalEntries)
            }
            if (goalEntities.isNotEmpty()) {
                db.goalDao().restoreGoals(goalEntities)
            }
            if (debtEntities.isNotEmpty()) {
                db.debtDao().restoreDebts(debtEntities)
            }
        }

        return SyncResult(
            accountsPulled = accountEntities.size,
            txnsPulled = newTxns.size,
        )
    }

    private suspend fun pushToSupabase(
        baseUrl: String,
        anonKey: String,
        token: String,
        userId: String,
    ): SyncResult {
        val accounts = db.accountDao().getAllForBackup()
        val txns = db.ledgerDao().getAllForBackup()
        val entries = db.ledgerDao().getAllEntriesForBackup()
        val goals = db.goalDao().getAllForBackup()
        val debts = db.debtDao().getAllForBackup()
        val categories = db.categoryDao().getAllForBackup()

        // Upsert categories
        if (categories.isNotEmpty()) {
            val arr = JSONArray()
            categories.forEach { c ->
                arr.put(JSONObject().apply {
                    put("id", c.id)
                    put("user_id", userId)
                    put("parent_id", c.parentId ?: JSONObject.NULL)
                    put("name", c.name)
                    put("kind", c.kind)
                    put("need_want", c.needWant ?: JSONObject.NULL)
                    put("icon", c.icon)
                    put("color", c.color)
                    put("archived", c.archived)
                    put("sort_order", c.sortOrder)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_categories", anonKey, token, arr)
        }

        // Upsert accounts
        if (accounts.isNotEmpty()) {
            val arr = JSONArray()
            accounts.forEach { a ->
                arr.put(JSONObject().apply {
                    put("id", a.id)
                    put("user_id", userId)
                    put("name", a.name)
                    put("type", a.type)
                    put("institution", a.institution ?: JSONObject.NULL)
                    put("last_four", a.lastFour ?: JSONObject.NULL)
                    put("opening_balance_minor", a.openingBalanceMinor)
                    put("currency", a.currency)
                    put("include_in_net_worth", a.includeInNetWorth)
                    put("include_in_stats", a.includeInStats)
                    put("archived", a.archived)
                    put("created_at", a.createdAt)
                    put("updated_at", a.updatedAt)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_accounts", anonKey, token, arr)
        }

        // Upsert transactions
        if (txns.isNotEmpty()) {
            val arr = JSONArray()
            txns.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("user_id", userId)
                    put("type", t.type)
                    put("amount_minor", t.amountMinor)
                    put("currency", t.currency)
                    put("local_date", t.localDate)
                    put("local_time", t.localTime)
                    put("account_id", t.accountId)
                    put("to_account_id", t.toAccountId ?: JSONObject.NULL)
                    put("category_id", t.categoryId ?: JSONObject.NULL)
                    put("merchant", t.merchant ?: JSONObject.NULL)
                    put("description", t.description ?: JSONObject.NULL)
                    put("notes", t.notes ?: JSONObject.NULL)
                    put("payment_method", t.paymentMethod ?: JSONObject.NULL)
                    put("need_want", t.needWant ?: JSONObject.NULL)
                    put("imported", t.imported)
                    put("source", t.source)
                    put("created_at", t.createdAt)
                    put("updated_at", t.updatedAt)
                    put("deleted_at", t.deletedAt ?: JSONObject.NULL)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_transactions", anonKey, token, arr)
        }

        // Upsert journal entries
        if (entries.isNotEmpty()) {
            val arr = JSONArray()
            entries.forEach { j ->
                arr.put(JSONObject().apply {
                    put("user_id", userId)
                    put("txn_id", j.txnId)
                    put("account_id", j.accountId ?: JSONObject.NULL)
                    put("category_id", j.categoryId ?: JSONObject.NULL)
                    put("amount_minor", j.amountMinor)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_journal_entries", anonKey, token, arr)
        }

        // Upsert goals
        if (goals.isNotEmpty()) {
            val arr = JSONArray()
            goals.forEach { g ->
                arr.put(JSONObject().apply {
                    put("id", g.id)
                    put("user_id", userId)
                    put("name", g.name)
                    put("target_amount_minor", g.targetAmountMinor)
                    put("current_amount_minor", g.currentAmountMinor)
                    put("planned_monthly_contribution_minor", g.plannedMonthlyContributionMinor)
                    put("currency", g.currency)
                    put("target_date", g.targetDate)
                    put("archived", g.archived)
                    put("created_at", g.createdAt)
                    put("updated_at", g.updatedAt)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_goals", anonKey, token, arr)
        }

        // Upsert debts
        if (debts.isNotEmpty()) {
            val arr = JSONArray()
            debts.forEach { d ->
                arr.put(JSONObject().apply {
                    put("id", d.id)
                    put("user_id", userId)
                    put("name", d.name)
                    put("principal_minor", d.principalMinor)
                    put("current_balance_minor", d.currentBalanceMinor)
                    put("annual_percent", d.annualPercent)
                    put("expected_monthly_payment_minor", d.expectedMonthlyPaymentMinor)
                    put("currency", d.currency)
                    put("archived", d.archived)
                    put("created_at", d.createdAt)
                    put("updated_at", d.updatedAt)
                })
            }
            httpPostUpsert("$baseUrl/rest/v1/user_debts", anonKey, token, arr)
        }

        return SyncResult(
            accountsPushed = accounts.size,
            txnsPushed = txns.size,
        )
    }

    private fun httpGet(urlString: String, anonKey: String, token: String): JSONArray {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw IllegalStateException("Cloud fetch failed ($code): $err")
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        return JSONArray(response)
    }

    private fun httpPostUpsert(urlString: String, anonKey: String, token: String, data: JSONArray) {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "resolution=merge-duplicates")
            connectTimeout = 15000
            readTimeout = 15000
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(data.toString())
            writer.flush()
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw IllegalStateException("Cloud save failed ($code): $err")
        }
    }

    private fun JSONObject.optStringLenient(camel: String, snake: String, default: String = ""): String {
        return when {
            has(camel) && !isNull(camel) -> optString(camel, default)
            has(snake) && !isNull(snake) -> optString(snake, default)
            else -> default
        }
    }

    private fun JSONObject.optNullableStringLenient(camel: String, snake: String): String? {
        val v = when {
            has(camel) && !isNull(camel) -> optString(camel)
            has(snake) && !isNull(snake) -> optString(snake)
            else -> null
        }
        return if (v == null || v == "null" || v.isBlank()) null else v
    }

    private fun JSONObject.optLongLenient(camel: String, snake: String, default: Long = 0L): Long {
        if (has(camel) && !isNull(camel)) {
            val v = opt(camel)
            if (v is Number) return v.toLong()
            if (v is String) return v.toLongOrNull() ?: default
        }
        if (has(snake) && !isNull(snake)) {
            val v = opt(snake)
            if (v is Number) return v.toLong()
            if (v is String) return v.toLongOrNull() ?: default
        }
        return default
    }

    private fun JSONObject.optIntLenient(camel: String, snake: String, default: Int = 0): Int {
        if (has(camel) && !isNull(camel)) {
            val v = opt(camel)
            if (v is Number) return v.toInt()
            if (v is String) return v.toIntOrNull() ?: default
        }
        if (has(snake) && !isNull(snake)) {
            val v = opt(snake)
            if (v is Number) return v.toInt()
            if (v is String) return v.toIntOrNull() ?: default
        }
        return default
    }

    private fun JSONObject.optBooleanLenient(camel: String, snake: String, default: Boolean = false): Boolean {
        if (has(camel) && !isNull(camel)) return optBoolean(camel, default)
        if (has(snake) && !isNull(snake)) return optBoolean(snake, default)
        return default
    }
}
