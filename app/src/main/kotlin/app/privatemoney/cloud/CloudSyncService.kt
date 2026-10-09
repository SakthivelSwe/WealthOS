package app.privatemoney.cloud

import android.content.Context
import app.privatemoney.data.local.AppDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.UploadData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles encrypted backup to Supabase Storage.
 *
 * Encryption: PBKDF2WithHmacSHA256 (100 000 iterations, 256-bit key) + AES-GCM-128.
 * Payload layout: [16 bytes salt][12 bytes IV][ciphertext + 16 byte GCM tag]
 */
class CloudSyncService(private val context: Context, private val db: AppDatabase) {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl  = SUPABASE_URL,
        supabaseKey  = SUPABASE_ANON_KEY,
    ) { install(Storage) }

    suspend fun backupDatabase(userPin: String) = withContext(Dispatchers.IO) {
        // Checkpoint WAL so the db file is current before reading it
        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")

        val dbFile = context.getDatabasePath("wealthos.db")
        if (!dbFile.exists()) return@withContext

        val rawBytes = dbFile.readBytes()

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv   = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val pbeSpec = PBEKeySpec(userPin.toCharArray(), salt, 100_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(pbeSpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(rawBytes)

        val payload = salt + iv + cipherText

        val fileName = "backup_${System.currentTimeMillis()}.enc"
        supabase.storage.from("backups").upload(
            path = fileName,
            data = payload,
            upsert = false,
        )
    }

    companion object {
        // Supabase project URL (non-secret — public REST endpoint)
        const val SUPABASE_URL      = "https://zahzagnindkkttdpmqii.supabase.co"
        // Fill in the anon key from Supabase Dashboard → Settings → API
        // Store it in local.properties as: supabase.anon.key=<value>
        // Do NOT commit the real key to source control.
        const val SUPABASE_ANON_KEY = "" // injected via BuildConfig.SUPABASE_ANON_KEY in prod builds
    }
}
