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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise-grade, versioned, tamper-evident Backup & Restore Service.
 *
 * Security: PBKDF2WithHmacSHA256 (100,000 iterations, 256-bit key) + AES-GCM-128.
 * Format:
 * [4 bytes MAGIC "WOSB"]
 * [4 bytes VERSION = 1]
 * [4 bytes METADATA_LEN]
 * [METADATA_BYTES]
 * [16 bytes SALT]
 * [12 bytes IV]
 * [CIPHERTEXT + 16 byte GCM tag]
 */
class CloudSyncService(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: AppSettings = AppSettings(context),
) {

    private val magic = byteArrayOf(0x57, 0x4F, 0x53, 0x42) // "WOSB"
    private val currentVersion = 1

    private data class BackupDataSnapshot(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val txns: List<TxnEntity>,
        val entries: List<JournalEntryEntity>,
        val goals: List<GoalEntity>,
        val debts: List<DebtEntity>,
        val chits: List<ChitFundEntity>,
        val recurring: List<RecurringTxnEntity>,
    )

    /**
     * Creates an encrypted, portable backup envelope from the local Room database.
     * Reads all tables atomically within a single database transaction.
     * Uses PBKDF2WithHmacSHA256 (100,000 iterations, 256-bit key) and AES/GCM/NoPadding (128-bit tag).
     * Binds unencrypted metadata via AEAD Associated Authenticated Data (AAD).
     */
    suspend fun createBackupEnvelope(userPin: String = ""): ByteArray = withContext(Dispatchers.IO) {
        val effectivePin = if (userPin.isBlank()) settings.autoVaultKey else userPin

        val snapshot = db.withTransaction {
            BackupDataSnapshot(
                accounts = db.accountDao().getAllForBackup(),
                categories = db.categoryDao().getAllForBackup(),
                txns = db.ledgerDao().getAllForBackup(),
                entries = db.ledgerDao().getAllEntriesForBackup(),
                goals = db.goalDao().getAllForBackup(),
                debts = db.debtDao().getAllForBackup(),
                chits = db.chitFundDao().getAllForBackup(),
                recurring = db.recurringTxnDao().getAllForBackup(),
            )
        }

        val accounts = snapshot.accounts
        val categories = snapshot.categories
        val txns = snapshot.txns
        val entries = snapshot.entries
        val goals = snapshot.goals
        val debts = snapshot.debts
        val chits = snapshot.chits
        val recurring = snapshot.recurring

        val rootJson = JSONObject().apply {
            put("accounts", JSONArray().apply {
                accounts.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("type", a.type)
                        put("institution", a.institution ?: JSONObject.NULL)
                        put("lastFour", a.lastFour ?: JSONObject.NULL)
                        put("openingBalanceMinor", a.openingBalanceMinor)
                        put("currency", a.currency)
                        put("includeInNetWorth", a.includeInNetWorth)
                        put("includeInStats", a.includeInStats)
                        put("archived", a.archived)
                        put("createdAt", a.createdAt)
                        put("updatedAt", a.updatedAt)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("parentId", c.parentId ?: JSONObject.NULL)
                        put("name", c.name)
                        put("kind", c.kind)
                        put("needWant", c.needWant ?: JSONObject.NULL)
                        put("icon", c.icon)
                        put("color", c.color)
                        put("archived", c.archived)
                        put("sortOrder", c.sortOrder)
                    })
                }
            })
            put("txns", JSONArray().apply {
                txns.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("type", t.type)
                        put("amountMinor", t.amountMinor)
                        put("currency", t.currency)
                        put("localDate", t.localDate)
                        put("localTime", t.localTime)
                        put("accountId", t.accountId)
                        put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                        put("categoryId", t.categoryId ?: JSONObject.NULL)
                        put("merchant", t.merchant ?: JSONObject.NULL)
                        put("description", t.description ?: JSONObject.NULL)
                        put("notes", t.notes ?: JSONObject.NULL)
                        put("paymentMethod", t.paymentMethod ?: JSONObject.NULL)
                        put("needWant", t.needWant ?: JSONObject.NULL)
                        put("imported", t.imported)
                        put("source", t.source)
                        put("createdAt", t.createdAt)
                        put("updatedAt", t.updatedAt)
                    })
                }
            })
            put("journal_entries", JSONArray().apply {
                entries.forEach { j ->
                    put(JSONObject().apply {
                        put("txnId", j.txnId)
                        put("accountId", j.accountId ?: JSONObject.NULL)
                        put("categoryId", j.categoryId ?: JSONObject.NULL)
                        put("amountMinor", j.amountMinor)
                    })
                }
            })
            put("goals", JSONArray().apply {
                goals.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("targetAmountMinor", g.targetAmountMinor)
                        put("currentAmountMinor", g.currentAmountMinor)
                        put("plannedMonthlyContributionMinor", g.plannedMonthlyContributionMinor)
                        put("currency", g.currency)
                        put("targetDate", g.targetDate)
                        put("archived", g.archived)
                        put("createdAt", g.createdAt)
                        put("updatedAt", g.updatedAt)
                    })
                }
            })
            put("debts", JSONArray().apply {
                debts.forEach { d ->
                    put(JSONObject().apply {
                        put("id", d.id)
                        put("name", d.name)
                        put("principalMinor", d.principalMinor)
                        put("currentBalanceMinor", d.currentBalanceMinor)
                        put("annualPercent", d.annualPercent)
                        put("expectedMonthlyPaymentMinor", d.expectedMonthlyPaymentMinor)
                        put("currency", d.currency)
                        put("archived", d.archived)
                        put("createdAt", d.createdAt)
                        put("updatedAt", d.updatedAt)
                    })
                }
            })
            put("chits", JSONArray().apply {
                chits.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("totalMonths", c.totalMonths)
                        put("startMonth", c.startMonth)
                        put("monthlyContributionMinor", c.monthlyContributionMinor)
                        put("currency", c.currency)
                        put("active", c.active)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
            })
            put("recurring", JSONArray().apply {
                recurring.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("type", r.type)
                        put("amountMinor", r.amountMinor)
                        put("currency", r.currency)
                        put("accountId", r.accountId)
                        put("categoryId", r.categoryId ?: JSONObject.NULL)
                        put("description", r.description ?: JSONObject.NULL)
                        put("frequency", r.frequency)
                        put("nextRunDate", r.nextRunDate)
                        put("active", r.active)
                        put("createdAt", r.createdAt)
                        put("updatedAt", r.updatedAt)
                        put("autoPost", r.autoPost)
                    })
                }
            })
        }

        val rawJsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)
        val checksum = sha256(rawJsonBytes)

        val metadata = BackupMetadata(
            version = currentVersion,
            appVersion = "0.2.0",
            timestamp = System.currentTimeMillis(),
            accountCount = accounts.size,
            txnCount = txns.size,
            goalCount = goals.size,
            debtCount = debts.size,
            chitFundCount = chits.size,
            recurringCount = recurring.size,
            checksumSha256 = checksum,
        )

        val metadataBytes = metadata.toJson().toString().toByteArray(Charsets.UTF_8)

        // Generate salt & IV
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        // PBKDF2 derivation with explicit password clearing
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val pinChars = effectivePin.toCharArray()
        val pbeSpec = PBEKeySpec(pinChars, salt, 100_000, 256)
        val secretKeyBytes = factory.generateSecret(pbeSpec).encoded
        pbeSpec.clearPassword()
        java.util.Arrays.fill(pinChars, '\u0000')

        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        // Encrypt with AEAD AAD (Associated Authenticated Data) binding metadataBytes
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        cipher.updateAAD(metadataBytes)
        val ciphertext = cipher.doFinal(rawJsonBytes)

        // Assemble envelope
        val buffer = ByteBuffer.allocate(
            4 + 4 + 4 + metadataBytes.size + salt.size + iv.size + ciphertext.size
        )
        buffer.put(magic)
        buffer.putInt(currentVersion)
        buffer.putInt(metadataBytes.size)
        buffer.put(metadataBytes)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)

        buffer.array()
    }

    /**
     * Uploads the encrypted backup to Supabase Storage and secures a copy in local vault.
     * Operates seamlessly without forcing manual user PIN entry or blocking on missing cloud key.
     */
    suspend fun backupToCloud(userPin: String = ""): BackupMetadata = withContext(Dispatchers.IO) {
        val effectivePin = if (userPin.isBlank()) settings.autoVaultKey else userPin
        val anonKey = settings.supabaseAnonKey.ifBlank { SUPABASE_ANON_KEY }
        val url = settings.supabaseUrl.ifBlank { SUPABASE_URL }

        val envelope = createBackupEnvelope(effectivePin)
        val metadata = readEnvelopeMetadata(envelope)

        // Always save latest snapshot to local internal vault first for instant offline safety
        try {
            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val localVaultFile = File(backupDir, "wealthos_latest_backup.wosenc")
            localVaultFile.writeBytes(envelope)
        } catch (_: Exception) {}

        // If Anon Key is present, attempt upload to Supabase Storage
        if (anonKey.isNotBlank()) {
            try {
                val supabase: SupabaseClient = createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = anonKey,
                ) { install(Storage) }

                val userId = settings.supabaseUserId?.ifBlank { null } ?: "vault"
                val fileName = "backup_${metadata.timestamp}.wosenc"
                val path = "$userId/$fileName"

                supabase.storage.from("backups").upload(
                    path = path,
                    data = envelope,
                    upsert = true,
                )
            } catch (e: Exception) {
                val userMsg = when {
                    e.message?.contains("code 0") == true || e.message?.contains("Queries can be performed") == true ->
                        "Supabase API key invalid or unauthenticated. Saved locally."
                    e.message?.contains("bucket") == true || e.message?.contains("not found") == true ->
                        "Storage bucket 'backups' not found. Saved locally."
                    else -> "Cloud upload notice: ${e.message}. Saved to local vault."
                }
                settings.lastBackupTimestamp = metadata.timestamp
                settings.lastBackupStatus = "LOCAL_ONLY"
                settings.lastBackupError = userMsg
                return@withContext metadata
            }
        }

        settings.lastBackupTimestamp = metadata.timestamp
        settings.lastBackupStatus = "SUCCESS"
        settings.lastBackupError = null
        metadata
    }

    /**
     * Returns the latest encrypted backup file stored on device, if available.
     */
    fun getLatestLocalBackupFile(): File? {
        val file = File(context.filesDir, "backups/wealthos_latest_backup.wosenc")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Reads metadata and decrypts payload in memory without writing to database.
     * Validates integrity checksum before showing the preview.
     */
    suspend fun previewBackup(envelope: ByteArray, userPin: String = ""): RestorePreview = withContext(Dispatchers.IO) {
        val (metadata, decryptedJson) = decryptEnvelope(envelope, userPin)
        val txnsArray = decryptedJson.optJSONArray("txns") ?: JSONArray()
        val accountsArray = decryptedJson.optJSONArray("accounts") ?: JSONArray()
        val goalsArray = decryptedJson.optJSONArray("goals") ?: JSONArray()
        val debtsArray = decryptedJson.optJSONArray("debts") ?: JSONArray()

        val existingTxns = db.ledgerDao().getAllForBackup().map { it.id }.toSet()
        var toRestoreTxns = 0
        var toSkipTxns = 0

        for (i in 0 until txnsArray.length()) {
            val obj = txnsArray.getJSONObject(i)
            val id = obj.getString("id")
            if (existingTxns.contains(id)) {
                toSkipTxns++
            } else {
                toRestoreTxns++
            }
        }

        RestorePreview(
            metadata = metadata,
            accountsToRestore = accountsArray.length(),
            txnsToRestore = toRestoreTxns,
            existingTxnsToSkip = toSkipTxns,
            goalsToRestore = goalsArray.length(),
            debtsToRestore = debtsArray.length(),
        )
    }

    /**
     * Restores entities safely and idempotently into Room database.
     * Never duplicates existing transactions or overwrites newer state.
     */
    suspend fun executeRestore(envelope: ByteArray, userPin: String = ""): RestorePreview = withContext(Dispatchers.IO) {
        val (metadata, decryptedJson) = decryptEnvelope(envelope, userPin)

        val categoriesArray = decryptedJson.optJSONArray("categories") ?: JSONArray()
        val accountsArray = decryptedJson.optJSONArray("accounts") ?: JSONArray()
        val txnsArray = decryptedJson.optJSONArray("txns") ?: JSONArray()
        val entriesArray = decryptedJson.optJSONArray("journal_entries") ?: JSONArray()
        val goalsArray = decryptedJson.optJSONArray("goals") ?: JSONArray()
        val debtsArray = decryptedJson.optJSONArray("debts") ?: JSONArray()
        val chitsArray = decryptedJson.optJSONArray("chits") ?: JSONArray()
        val recurringArray = decryptedJson.optJSONArray("recurring") ?: JSONArray()

        val categoryEntities = mutableListOf<CategoryEntity>()
        for (i in 0 until categoriesArray.length()) {
            val o = categoriesArray.getJSONObject(i)
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

        val accountEntities = mutableListOf<AccountEntity>()
        for (i in 0 until accountsArray.length()) {
            val o = accountsArray.getJSONObject(i)
            accountEntities.add(
                AccountEntity(
                    id = o.optStringLenient("id", "id"),
                    name = o.optStringLenient("name", "name"),
                    type = o.optStringLenient("type", "type", "CHECKING"),
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

        val txnEntities = mutableListOf<TxnEntity>()
        for (i in 0 until txnsArray.length()) {
            val o = txnsArray.getJSONObject(i)
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
                    source = o.optStringLenient("source", "source", "RESTORE"),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                    deletedAt = null,
                )
            )
        }

        val journalEntities = mutableListOf<JournalEntryEntity>()
        for (i in 0 until entriesArray.length()) {
            val o = entriesArray.getJSONObject(i)
            journalEntities.add(
                JournalEntryEntity(
                    txnId = o.optStringLenient("txnId", "txn_id"),
                    accountId = o.optNullableStringLenient("accountId", "account_id"),
                    categoryId = o.optNullableStringLenient("categoryId", "category_id"),
                    amountMinor = o.optLongLenient("amountMinor", "amount_minor", 0L),
                )
            )
        }

        val goalEntities = mutableListOf<GoalEntity>()
        for (i in 0 until goalsArray.length()) {
            val o = goalsArray.getJSONObject(i)
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

        val debtEntities = mutableListOf<DebtEntity>()
        for (i in 0 until debtsArray.length()) {
            val o = debtsArray.getJSONObject(i)
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

        val chitEntities = mutableListOf<ChitFundEntity>()
        for (i in 0 until chitsArray.length()) {
            val o = chitsArray.getJSONObject(i)
            chitEntities.add(
                ChitFundEntity(
                    id = o.optStringLenient("id", "id"),
                    name = o.optStringLenient("name", "name"),
                    totalMonths = o.optIntLenient("totalMonths", "total_months", 0),
                    startMonth = o.optStringLenient("startMonth", "start_month"),
                    monthlyContributionMinor = o.optLongLenient("monthlyContributionMinor", "monthly_contribution_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    active = o.optBooleanLenient("active", "active", true),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                )
            )
        }

        val recurringEntities = mutableListOf<RecurringTxnEntity>()
        for (i in 0 until recurringArray.length()) {
            val o = recurringArray.getJSONObject(i)
            recurringEntities.add(
                RecurringTxnEntity(
                    id = o.optStringLenient("id", "id"),
                    type = o.optStringLenient("type", "type", "EXPENSE"),
                    amountMinor = o.optLongLenient("amountMinor", "amount_minor", 0L),
                    currency = o.optStringLenient("currency", "currency", "INR"),
                    accountId = o.optStringLenient("accountId", "account_id"),
                    categoryId = o.optNullableStringLenient("categoryId", "category_id"),
                    description = o.optNullableStringLenient("description", "description"),
                    frequency = o.optStringLenient("frequency", "frequency", "MONTHLY"),
                    nextRunDate = o.optStringLenient("nextRunDate", "next_run_date"),
                    active = o.optBooleanLenient("active", "active", true),
                    createdAt = o.optLongLenient("createdAt", "created_at", System.currentTimeMillis()),
                    updatedAt = o.optLongLenient("updatedAt", "updated_at", System.currentTimeMillis()),
                    autoPost = o.optBooleanLenient("autoPost", "auto_post", false),
                )
            )
        }

        val existingTxns = db.ledgerDao().getAllForBackup().map { it.id }.toSet()
        val toSkip = txnEntities.count { existingTxns.contains(it.id) }
        val toRestore = txnEntities.size - toSkip

        val newTxns = txnEntities.filterNot { existingTxns.contains(it.id) }
        val newTxnIds = newTxns.map { it.id }.toSet()
        val journalEntitiesToInsert = journalEntities.filter { newTxnIds.contains(it.txnId) }

        db.withTransaction {
            if (categoryEntities.isNotEmpty()) {
                db.categoryDao().insertAll(categoryEntities)
            }
            db.accountDao().restoreAccounts(accountEntities)
            db.ledgerDao().restoreTxns(newTxns)
            db.ledgerDao().restoreEntries(journalEntitiesToInsert)
            db.goalDao().restoreGoals(goalEntities)
            db.debtDao().restoreDebts(debtEntities)
            db.chitFundDao().restoreChitFunds(chitEntities)
            db.recurringTxnDao().restoreRecurringTxns(recurringEntities)
        }

        RestorePreview(
            metadata = metadata,
            accountsToRestore = accountEntities.size,
            txnsToRestore = toRestore,
            existingTxnsToSkip = toSkip,
            goalsToRestore = goalEntities.size,
            debtsToRestore = debtEntities.size,
        )
    }

    fun readEnvelopeMetadata(envelope: ByteArray): BackupMetadata {
        if (envelope.size < MIN_ENVELOPE_SIZE) {
            throw IllegalArgumentException("Invalid backup format: envelope truncated (${envelope.size} bytes).")
        }
        if (envelope.size > MAX_ENVELOPE_SIZE) {
            throw IllegalArgumentException("Invalid backup format: envelope exceeds maximum size ($MAX_ENVELOPE_SIZE bytes).")
        }
        val buffer = ByteBuffer.wrap(envelope)
        val fileMagic = ByteArray(4)
        buffer.get(fileMagic)
        if (!fileMagic.contentEquals(magic)) {
            throw IllegalArgumentException("Invalid backup format: unrecognized signature.")
        }
        val version = buffer.int
        if (version > currentVersion) {
            throw IllegalArgumentException("Backup was created with newer app version ($version). Please update WealthOS.")
        }
        if (version < 1) {
            throw IllegalArgumentException("Invalid backup format: unsupported version ($version).")
        }
        val metadataLen = buffer.int
        if (metadataLen <= 0 || metadataLen > MAX_METADATA_SIZE) {
            throw IllegalArgumentException("Invalid backup metadata size: $metadataLen bytes.")
        }
        if (buffer.remaining() < metadataLen + 16 + 12 + 16) {
            throw IllegalArgumentException("Invalid backup format: payload truncated for specified metadata length.")
        }
        val metadataBytes = ByteArray(metadataLen)
        buffer.get(metadataBytes)
        val json = JSONObject(String(metadataBytes, Charsets.UTF_8))
        return BackupMetadata.fromJson(json)
    }

    private fun decryptEnvelope(envelope: ByteArray, userPin: String): Pair<BackupMetadata, JSONObject> {
        if (envelope.size < MIN_ENVELOPE_SIZE) {
            throw IllegalArgumentException("Invalid backup format: envelope truncated (${envelope.size} bytes).")
        }
        if (envelope.size > MAX_ENVELOPE_SIZE) {
            throw IllegalArgumentException("Invalid backup format: envelope exceeds maximum size ($MAX_ENVELOPE_SIZE bytes).")
        }
        val buffer = ByteBuffer.wrap(envelope)
        val fileMagic = ByteArray(4)
        buffer.get(fileMagic)
        if (!fileMagic.contentEquals(magic)) {
            throw IllegalArgumentException("Invalid backup format: unrecognized signature.")
        }
        val version = buffer.int
        if (version > currentVersion) {
            throw IllegalArgumentException("Incompatible backup version ($version). Please update WealthOS.")
        }
        if (version < 1) {
            throw IllegalArgumentException("Invalid backup format: unsupported version ($version).")
        }
        val metadataLen = buffer.int
        if (metadataLen <= 0 || metadataLen > MAX_METADATA_SIZE) {
            throw IllegalArgumentException("Invalid backup metadata size: $metadataLen bytes.")
        }
        if (buffer.remaining() < metadataLen + 16 + 12 + 16) {
            throw IllegalArgumentException("Invalid backup format: payload truncated.")
        }
        val metadataBytes = ByteArray(metadataLen)
        buffer.get(metadataBytes)
        val metadata = BackupMetadata.fromJson(JSONObject(String(metadataBytes, Charsets.UTF_8)))

        val salt = ByteArray(16)
        buffer.get(salt)
        val iv = ByteArray(12)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        // Decrypt with derived key and verify AEAD AAD
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val candidates = if (userPin.isNotBlank()) {
            listOf(userPin)
        } else {
            listOf(settings.autoVaultKey)
        }

        var decryptedBytes: ByteArray? = null
        for (candidate in candidates) {
            if (candidate.isBlank()) continue
            val pinChars = candidate.toCharArray()
            try {
                val pbeSpec = PBEKeySpec(pinChars, salt, 100_000, 256)
                val secretKeyBytes = factory.generateSecret(pbeSpec).encoded
                pbeSpec.clearPassword()
                val secretKey = SecretKeySpec(secretKeyBytes, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                cipher.updateAAD(metadataBytes)
                decryptedBytes = cipher.doFinal(ciphertext)
                java.util.Arrays.fill(secretKeyBytes, 0.toByte())
                break
            } catch (_: Exception) {
                // Try next candidate or fail
            } finally {
                java.util.Arrays.fill(pinChars, '\u0000')
            }
        }

        if (decryptedBytes == null) {
            throw SecurityException("Incorrect PIN / password or corrupted backup payload.")
        }

        // Secondary data integrity verification
        val calculatedHash = sha256(decryptedBytes)
        if (metadata.checksumSha256.isNotBlank() && metadata.checksumSha256 != calculatedHash) {
            throw SecurityException("Backup integrity verification failed: data hash mismatch.")
        }

        val json = JSONObject(String(decryptedBytes, Charsets.UTF_8))
        return metadata to json
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) {
            val v = optString(name)
            if (v == "null") null else v
        } else null

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
            if (v is String) return v.toLongOrNull() ?: parseTimestampOrNull(v) ?: default
        }
        if (has(snake) && !isNull(snake)) {
            val v = opt(snake)
            if (v is Number) return v.toLong()
            if (v is String) return v.toLongOrNull() ?: parseTimestampOrNull(v) ?: default
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

    private fun parseTimestampOrNull(str: String): Long? {
        return try {
            java.time.Instant.parse(str).toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(str).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        const val SUPABASE_URL = "https://zahzagnindkkttdpmqii.supabase.co"
        const val SUPABASE_ANON_KEY = ""
        const val MIN_ENVELOPE_SIZE = 58
        const val MAX_ENVELOPE_SIZE = 50 * 1024 * 1024 // 50 MB
        const val MAX_METADATA_SIZE = 64 * 1024 // 64 KB
    }
}
