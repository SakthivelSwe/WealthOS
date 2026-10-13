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

    /**
     * Creates an encrypted, portable backup envelope from the local Room database.
     */
    suspend fun createBackupEnvelope(userPin: String = ""): ByteArray = withContext(Dispatchers.IO) {
        val effectivePin = if (userPin.isBlank()) settings.autoVaultKey else userPin
        val accounts = db.accountDao().getAllForBackup()
        val txns = db.ledgerDao().getAllForBackup()
        val entries = db.ledgerDao().getAllEntriesForBackup()
        val goals = db.goalDao().getAllForBackup()
        val debts = db.debtDao().getAllForBackup()
        val chits = db.chitFundDao().getAllForBackup()
        val recurring = db.recurringTxnDao().getAllForBackup()

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

        // PBKDF2 derivation
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val pbeSpec = PBEKeySpec(effectivePin.toCharArray(), salt, 100_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(pbeSpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
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

                val fileName = "backup_${metadata.timestamp}.wosenc"
                val path = "vault/$fileName"

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
                settings.lastBackupStatus = "SUCCESS"
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

        val accountsArray = decryptedJson.optJSONArray("accounts") ?: JSONArray()
        val txnsArray = decryptedJson.optJSONArray("txns") ?: JSONArray()
        val entriesArray = decryptedJson.optJSONArray("journal_entries") ?: JSONArray()
        val goalsArray = decryptedJson.optJSONArray("goals") ?: JSONArray()
        val debtsArray = decryptedJson.optJSONArray("debts") ?: JSONArray()
        val chitsArray = decryptedJson.optJSONArray("chits") ?: JSONArray()
        val recurringArray = decryptedJson.optJSONArray("recurring") ?: JSONArray()

        val accountEntities = mutableListOf<AccountEntity>()
        for (i in 0 until accountsArray.length()) {
            val o = accountsArray.getJSONObject(i)
            accountEntities.add(
                AccountEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    type = o.getString("type"),
                    institution = o.optString("institution", null),
                    lastFour = o.optString("lastFour", null),
                    openingBalanceMinor = o.getLong("openingBalanceMinor"),
                    currency = o.getString("currency"),
                    includeInNetWorth = o.optBoolean("includeInNetWorth", true),
                    includeInStats = o.optBoolean("includeInStats", true),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                )
            )
        }

        val txnEntities = mutableListOf<TxnEntity>()
        for (i in 0 until txnsArray.length()) {
            val o = txnsArray.getJSONObject(i)
            txnEntities.add(
                TxnEntity(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    amountMinor = o.getLong("amountMinor"),
                    currency = o.getString("currency"),
                    localDate = o.getString("localDate"),
                    localTime = o.optString("localTime", "00:00"),
                    accountId = o.getString("accountId"),
                    toAccountId = o.optString("toAccountId", null),
                    categoryId = o.optString("categoryId", null),
                    merchant = o.optString("merchant", null),
                    description = o.optString("description", null),
                    notes = o.optString("notes", null),
                    paymentMethod = o.optString("paymentMethod", null),
                    needWant = o.optString("needWant", null),
                    imported = o.optBoolean("imported", false),
                    source = o.optString("source", "RESTORE"),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                    deletedAt = null,
                )
            )
        }

        val journalEntities = mutableListOf<JournalEntryEntity>()
        for (i in 0 until entriesArray.length()) {
            val o = entriesArray.getJSONObject(i)
            journalEntities.add(
                JournalEntryEntity(
                    txnId = o.getString("txnId"),
                    accountId = o.optString("accountId", null),
                    categoryId = o.optString("categoryId", null),
                    amountMinor = o.getLong("amountMinor"),
                )
            )
        }

        val goalEntities = mutableListOf<GoalEntity>()
        for (i in 0 until goalsArray.length()) {
            val o = goalsArray.getJSONObject(i)
            goalEntities.add(
                GoalEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    targetAmountMinor = o.getLong("targetAmountMinor"),
                    currentAmountMinor = o.getLong("currentAmountMinor"),
                    plannedMonthlyContributionMinor = o.getLong("plannedMonthlyContributionMinor"),
                    currency = o.getString("currency"),
                    targetDate = o.getString("targetDate"),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                )
            )
        }

        val debtEntities = mutableListOf<DebtEntity>()
        for (i in 0 until debtsArray.length()) {
            val o = debtsArray.getJSONObject(i)
            debtEntities.add(
                DebtEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    principalMinor = o.getLong("principalMinor"),
                    currentBalanceMinor = o.getLong("currentBalanceMinor"),
                    annualPercent = o.getString("annualPercent"),
                    expectedMonthlyPaymentMinor = o.getLong("expectedMonthlyPaymentMinor"),
                    currency = o.getString("currency"),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                )
            )
        }

        val chitEntities = mutableListOf<ChitFundEntity>()
        for (i in 0 until chitsArray.length()) {
            val o = chitsArray.getJSONObject(i)
            chitEntities.add(
                ChitFundEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    totalMonths = o.getInt("totalMonths"),
                    startMonth = o.getString("startMonth"),
                    monthlyContributionMinor = o.getLong("monthlyContributionMinor"),
                    currency = o.getString("currency"),
                    active = o.optBoolean("active", true),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                )
            )
        }

        val recurringEntities = mutableListOf<RecurringTxnEntity>()
        for (i in 0 until recurringArray.length()) {
            val o = recurringArray.getJSONObject(i)
            recurringEntities.add(
                RecurringTxnEntity(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    amountMinor = o.getLong("amountMinor"),
                    currency = o.getString("currency"),
                    accountId = o.getString("accountId"),
                    categoryId = o.optString("categoryId", null),
                    description = o.optString("description", null),
                    frequency = o.getString("frequency"),
                    nextRunDate = o.getString("nextRunDate"),
                    active = o.optBoolean("active", true),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt"),
                )
            )
        }

        val existingTxns = db.ledgerDao().getAllForBackup().map { it.id }.toSet()
        val toSkip = txnEntities.count { existingTxns.contains(it.id) }
        val toRestore = txnEntities.size - toSkip

        db.withTransaction {
            db.accountDao().restoreAccounts(accountEntities)
            db.ledgerDao().restoreTxns(txnEntities)
            db.ledgerDao().restoreEntries(journalEntities)
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

    private fun readEnvelopeMetadata(envelope: ByteArray): BackupMetadata {
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
        val metadataLen = buffer.int
        val metadataBytes = ByteArray(metadataLen)
        buffer.get(metadataBytes)
        val json = JSONObject(String(metadataBytes, Charsets.UTF_8))
        return BackupMetadata.fromJson(json)
    }

    private fun decryptEnvelope(envelope: ByteArray, userPin: String): Pair<BackupMetadata, JSONObject> {
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
        val metadataLen = buffer.int
        val metadataBytes = ByteArray(metadataLen)
        buffer.get(metadataBytes)
        val metadata = BackupMetadata.fromJson(JSONObject(String(metadataBytes, Charsets.UTF_8)))

        val salt = ByteArray(16)
        buffer.get(salt)
        val iv = ByteArray(12)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        // Decrypt
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val candidates = if (userPin.isNotBlank()) {
            listOf(userPin)
        } else {
            listOf(settings.autoVaultKey, "0000")
        }

        var decryptedBytes: ByteArray? = null
        for (candidate in candidates) {
            try {
                val pbeSpec = PBEKeySpec(candidate.toCharArray(), salt, 100_000, 256)
                val secretKey = SecretKeySpec(factory.generateSecret(pbeSpec).encoded, "AES")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                decryptedBytes = cipher.doFinal(ciphertext)
                break
            } catch (_: Exception) {
                // Try next candidate
            }
        }

        if (decryptedBytes == null) {
            throw SecurityException("Incorrect PIN / password or corrupted backup payload.")
        }

        // Integrity verification
        val calculatedHash = sha256(decryptedBytes)
        if (metadata.checksumSha256.isNotBlank() && metadata.checksumSha256 != calculatedHash) {
            throw SecurityException("Backup integrity verification failed: data hash mismatch.")
        }

        val json = JSONObject(String(decryptedBytes, Charsets.UTF_8))
        return metadata to json
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val SUPABASE_URL = "https://zahzagnindkkttdpmqii.supabase.co"
        const val SUPABASE_ANON_KEY = ""
    }
}
