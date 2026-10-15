package app.privatemoney.data.local

import android.content.Context
import androidx.room.Room
import app.privatemoney.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    private const val DB_NAME = "pmos.db"

    /** Opens the encrypted database. The passphrase array is zeroed after SQLCipher has copied it. */
    fun create(context: Context, keyProvider: DatabaseKeyProvider): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = keyProvider.getOrCreate()
        val factory = SupportOpenHelperFactory(passphrase, null, false, 0)
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .build()
        passphrase.fill(0)
        return db
    }
}
