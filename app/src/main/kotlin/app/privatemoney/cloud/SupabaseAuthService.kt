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

    suspend fun sendEmailOtp(
        email: String,
        redirectUrl: String = "wealthos://login-callback",
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                throw IllegalArgumentException("Please enter a valid email address.")
            }
            val client = getClient()
            client.auth.signInWith(OTP, redirectUrl = redirectUrl) {
                this.email = cleanEmail
            }
        }
    }

    suspend fun verifyEmailOtp(email: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            val cleanToken = token.trim()
            if (cleanToken.length < 6) {
                throw IllegalArgumentException("Please enter the complete verification code or link.")
            }
            val client = getClient()
            try {
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = cleanEmail,
                    token = cleanToken,
                )
            } catch (e: Exception) {
                // Fallback to MAGIC_LINK OTP type if EMAIL type fails
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.MAGIC_LINK,
                    email = cleanEmail,
                    token = cleanToken,
                )
            }

            persistAuthenticatedSession(cleanEmail)
        }
    }

    suspend fun verifyTokenOrLink(email: String, input: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            val trimmed = input.trim()

            if (trimmed.isBlank()) {
                throw IllegalArgumentException("Verification code or link cannot be blank.")
            }

            val client = getClient()

            // Case 1: Pure 6-digit OTP code
            if (trimmed.length == 6 && trimmed.all { it.isDigit() }) {
                return@runCatching verifyEmailOtp(cleanEmail, trimmed).getOrThrow()
            }

            // Case 2: URL containing #access_token=... (e.g. copied from browser address bar)
            if (trimmed.contains("access_token=")) {
                val tokenRegex = "access_token=([^&]+)".toRegex()
                val refreshRegex = "refresh_token=([^&]+)".toRegex()
                val accessToken = tokenRegex.find(trimmed)?.groupValues?.get(1)
                    ?: throw IllegalArgumentException("Could not find access_token in link.")
                val refreshToken = refreshRegex.find(trimmed)?.groupValues?.get(1) ?: ""

                client.auth.importAuthToken(accessToken, refreshToken, autoRefresh = true, retrieveUser = true)
                return@runCatching persistAuthenticatedSession(cleanEmail)
            }

            // Case 3: Direct Supabase verification URL from email (contains /auth/v1/verify or token=)
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("wealthos://")) {
                val tokenRegex = "[?&]token=([^&]+)".toRegex()
                val typeRegex = "[?&]type=([^&]+)".toRegex()
                val tokenMatch = tokenRegex.find(trimmed)?.groupValues?.get(1)
                val typeMatch = typeRegex.find(trimmed)?.groupValues?.get(1) ?: "magiclink"

                if (tokenMatch != null) {
                    val otpType = if (typeMatch.equals("email", ignoreCase = true)) OtpType.Email.EMAIL else OtpType.Email.MAGIC_LINK
                    try {
                        client.auth.verifyEmailOtp(
                            type = otpType,
                            email = cleanEmail,
                            token = tokenMatch,
                        )
                        return@runCatching persistAuthenticatedSession(cleanEmail)
                    } catch (_: Exception) {
                        // If direct API verify fails with token hash, follow HTTP redirect to extract session
                        resolveSessionFromHttpVerification(trimmed, client)
                        return@runCatching persistAuthenticatedSession(cleanEmail)
                    }
                } else {
                    resolveSessionFromHttpVerification(trimmed, client)
                    return@runCatching persistAuthenticatedSession(cleanEmail)
                }
            }

            // Case 4: Raw token / hash pasted directly
            try {
                client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = cleanEmail, token = trimmed)
            } catch (_: Exception) {
                client.auth.verifyEmailOtp(type = OtpType.Email.MAGIC_LINK, email = cleanEmail, token = trimmed)
            }
            persistAuthenticatedSession(cleanEmail)
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                throw IllegalArgumentException("Please enter a valid email address.")
            }
            if (password.isBlank()) {
                throw IllegalArgumentException("Password cannot be blank.")
            }
            val client = getClient()
            client.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = cleanEmail
                this.password = password
            }
            persistAuthenticatedSession(cleanEmail)
        }
    }

    private suspend fun resolveSessionFromHttpVerification(urlStr: String, client: SupabaseClient) = withContext(Dispatchers.IO) {
        val url = java.net.URL(urlStr)
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "WealthOS-Android/3.0")
            connectTimeout = 12000
            readTimeout = 12000
        }
        conn.connect()
        val location = conn.getHeaderField("Location") ?: ""
        conn.disconnect()

        if (location.contains("error_description=")) {
            val errorDesc = "[?&#]error_description=([^&]+)".toRegex().find(location)?.groupValues?.get(1)
            val decoded = java.net.URLDecoder.decode(errorDesc ?: "Verification failed", "UTF-8")
            throw IllegalStateException(decoded)
        }

        if (location.contains("access_token=")) {
            val tokenRegex = "access_token=([^&]+)".toRegex()
            val refreshRegex = "refresh_token=([^&]+)".toRegex()
            val accessToken = tokenRegex.find(location)?.groupValues?.get(1)
                ?: throw IllegalStateException("No access token found in redirect.")
            val refreshToken = refreshRegex.find(location)?.groupValues?.get(1) ?: ""
            client.auth.importAuthToken(accessToken, refreshToken, autoRefresh = true, retrieveUser = true)
            return@withContext
        }

        throw IllegalStateException("Sign-in link is invalid or already used. Please request a new link.")
    }

    private suspend fun persistAuthenticatedSession(fallbackEmail: String): String {
        val client = getClient()
        val session = client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Authentication failed: No active session received.")
        val user = client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Authentication failed: User details missing.")

        val userId = user.id
        val userEmail = user.email ?: fallbackEmail
        val accessToken = session.accessToken

        settings.supabaseUserId = userId
        settings.supabaseUserEmail = userEmail
        settings.supabaseAccessToken = accessToken

        return userId
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
