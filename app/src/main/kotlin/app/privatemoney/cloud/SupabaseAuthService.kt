package app.privatemoney.cloud

import android.content.Context
import app.privatemoney.data.AppSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages Supabase GoTrue authentication for Android, matching WealthOS Web OTP flow.
 */
class SupabaseAuthService(
    private val context: Context,
    private val settings: AppSettings = AppSettings(context),
) {

    @Volatile
    private var clientInstance: SupabaseClient? = null
    private var cachedKey: String = ""
    private var cachedUrl: String = ""

    @Synchronized
    fun getClient(): SupabaseClient {
        val url = settings.supabaseUrl.ifBlank { AppSettings.DEFAULT_SUPABASE_URL }
        val anonKey = settings.supabaseAnonKey

        if (anonKey.isBlank()) {
            throw IllegalStateException("Supabase Publishable Anon Key is required. Please configure it in Settings.")
        }

        if (clientInstance == null || cachedKey != anonKey || cachedUrl != url) {
            cachedKey = anonKey
            cachedUrl = url
            clientInstance = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = anonKey,
            ) {
                install(Auth)
                install(Storage)
            }
        }
        return clientInstance!!
    }

    suspend fun sendEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                throw IllegalArgumentException("Please enter a valid email address.")
            }
            val client = getClient()
            client.auth.signInWith(OTP) {
                this.email = cleanEmail
            }
        }
    }

    suspend fun verifyEmailOtp(email: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            val cleanToken = token.trim()
            if (cleanToken.length < 6) {
                throw IllegalArgumentException("Please enter the complete 6-digit verification code.")
            }
            val client = getClient()
            client.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = cleanEmail,
                token = cleanToken,
            )

            val session = client.auth.currentSessionOrNull()
                ?: throw IllegalStateException("Authentication failed: No active session received.")
            val user = client.auth.currentUserOrNull()
                ?: throw IllegalStateException("Authentication failed: User details missing.")

            val userId = user.id
            val userEmail = user.email ?: cleanEmail
            val accessToken = session.accessToken

            settings.supabaseUserId = userId
            settings.supabaseUserEmail = userEmail
            settings.supabaseAccessToken = accessToken

            userId
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                clientInstance?.auth?.signOut()
            } catch (_: Exception) {}

            settings.supabaseUserId = null
            settings.supabaseUserEmail = null
            settings.supabaseAccessToken = null
            settings.lastSyncStatus = "SIGNED_OUT"
        }
    }

    fun isLoggedIn(): Boolean {
        return !settings.supabaseUserId.isNullOrBlank() && !settings.supabaseAccessToken.isNullOrBlank()
    }

    fun currentUserEmail(): String? = settings.supabaseUserEmail

    fun currentUserId(): String? = settings.supabaseUserId

    fun currentAccessToken(): String? = settings.supabaseAccessToken
}
