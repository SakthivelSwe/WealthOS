package app.privatemoney.security

import android.content.Context
import java.io.File
import java.security.SecureRandom

/**
 * Provides the SQLCipher passphrase: 32 random bytes from [SecureRandom], persisted only as a
 * Keystore-wrapped blob in app-private storage. The raw key is never written to disk or logged.
 */
class DatabaseKeyProvider(context: Context) {
    private val file = File(context.noBackupFilesDir, "db.key.enc")
    private val wrapper = KeystoreKeyWrapper(KEY_ALIAS)

    /** Caller should zero the returned array once the database is opened. */
    @Synchronized
    fun getOrCreate(): ByteArray {
        if (file.exists()) return wrapper.unwrap(file.readBytes())
        val key = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(wrapper.wrap(key))
        check(tmp.renameTo(file)) { "Could not persist key" }
        return key
    }

    @Synchronized
    fun destroy() {
        file.delete()
        wrapper.destroy()
    }

    private companion object {
        const val KEY_ALIAS = "pmos_db_key_wrap_v1"
        const val KEY_BYTES = 32
    }
}
