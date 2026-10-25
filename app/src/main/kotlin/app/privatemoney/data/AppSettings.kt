package app.privatemoney.data

import android.content.Context
import app.privatemoney.domain.model.Currency

/** Non-secret preferences. Excluded from Android backup via data_extraction_rules. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("pmos_settings", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK, value).apply()

    /** Time in background before the app locks again. 0 means lock immediately. */
    var lockTimeoutMs: Long
        get() = prefs.getLong(KEY_TIMEOUT, DEFAULT_TIMEOUT_MS)
        set(value) = prefs.edit().putLong(KEY_TIMEOUT, value).apply()

    var currency: Currency
        get() = Currency.fromCode(prefs.getString(KEY_CURRENCY, null)) ?: Currency.INR
        set(value) = prefs.edit().putString(KEY_CURRENCY, value.code).apply()

    var lastAccountId: String?
        get() = prefs.getString(KEY_LAST_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_LAST_ACCOUNT, value).apply()

    var basisIncomeMinor: Long
        get() = prefs.getLong(KEY_BASIS_INCOME, 0L)
        set(value) = prefs.edit().putLong(KEY_BASIS_INCOME, value).apply()

    var needsPct: Int
        get() = prefs.getInt(KEY_NEEDS_PCT, 50)
        set(value) = prefs.edit().putInt(KEY_NEEDS_PCT, value).apply()

    var wantsPct: Int
        get() = prefs.getInt(KEY_WANTS_PCT, 30)
        set(value) = prefs.edit().putInt(KEY_WANTS_PCT, value).apply()

    var savingsPct: Int
        get() = prefs.getInt(KEY_SAVINGS_PCT, 20)
        set(value) = prefs.edit().putInt(KEY_SAVINGS_PCT, value).apply()

    var themeModeStr: String
        get() = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var maskAmounts: Boolean
        get() = prefs.getBoolean(KEY_MASK_AMOUNTS, false)
        set(value) = prefs.edit().putBoolean(KEY_MASK_AMOUNTS, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "WealthOS User") ?: "WealthOS User"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var blockScreenshots: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, value).apply()

    var supabaseUrl: String
        get() {
            val saved = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
            if (saved.isNotBlank()) return saved
            return runCatching { app.privatemoney.BuildConfig.SUPABASE_URL.ifBlank { DEFAULT_SUPABASE_URL } }
                .getOrDefault(DEFAULT_SUPABASE_URL)
        }
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value.trim()).apply()

    var supabaseAnonKey: String
        get() {
            val saved = prefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
            if (saved.isNotBlank()) return saved
            return runCatching {
                val b64 = app.privatemoney.BuildConfig.SUPABASE_ANON_KEY_B64
                if (b64.isNotBlank()) {
                    String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
                } else ""
            }.getOrDefault("")
        }
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON_KEY, value.trim()).apply()

    var supabaseUserId: String?
        get() = prefs.getString(KEY_SUPABASE_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_SUPABASE_USER_ID, value?.trim()).apply()

    var supabaseUserEmail: String?
        get() = prefs.getString(KEY_SUPABASE_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_SUPABASE_USER_EMAIL, value?.trim()).apply()

    var supabaseAccessToken: String?
        get() = prefs.getString(KEY_SUPABASE_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SUPABASE_ACCESS_TOKEN, value?.trim()).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var lastSyncStatus: String
        get() = prefs.getString(KEY_LAST_SYNC_STATUS, "IDLE") ?: "IDLE"
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_STATUS, value).apply()

    var autoVaultKey: String
        get() {
            val saved = prefs.getString(KEY_AUTO_VAULT_KEY, null)
            if (!saved.isNullOrBlank()) return saved
            val generated = "wos_vault_" + java.util.UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_AUTO_VAULT_KEY, generated).apply()
            return generated
        }
        set(value) = prefs.edit().putString(KEY_AUTO_VAULT_KEY, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    var lastBackupStatus: String
        get() = prefs.getString(KEY_LAST_BACKUP_STATUS, "IDLE") ?: "IDLE"
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_STATUS, value).apply()

    var lastBackupError: String?
        get() = prefs.getString(KEY_LAST_BACKUP_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP_ERROR, value).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, value).apply()

    var dailyReminderHour: Int
        get() = prefs.getInt(KEY_DAILY_REMINDER_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_HOUR, value).apply()

    var dailyReminderMinute: Int
        get() = prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_MINUTE, value).apply()

    var remindOnlyWhenNoTxnLogged: Boolean
        get() = prefs.getBoolean(KEY_REMIND_ONLY_NO_TXN, false)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_ONLY_NO_TXN, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietHoursStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_HOUR, value).apply()

    var quietHoursEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_HOUR, value).apply()

    var lastDailyReviewDate: String
        get() = prefs.getString(KEY_LAST_REVIEW_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_REVIEW_DATE, value).apply()

    var hideSensitiveNotificationDetails: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SENSITIVE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SENSITIVE_NOTIFICATIONS, value).apply()

    // WealthOS Spec 4.1 Financial Profile Additions
    var incomeScheduleType: String
        get() = prefs.getString(KEY_INCOME_SCHEDULE_TYPE, "VARIABLE_WINDOW") ?: "VARIABLE_WINDOW"
        set(value) = prefs.edit().putString(KEY_INCOME_SCHEDULE_TYPE, value).apply()

    var paydayWindowStartDay: Int
        get() = prefs.getInt(KEY_PAYDAY_WINDOW_START_DAY, 2)
        set(value) = prefs.edit().putInt(KEY_PAYDAY_WINDOW_START_DAY, value).apply()

    var paydayWindowEndDay: Int
        get() = prefs.getInt(KEY_PAYDAY_WINDOW_END_DAY, 5)
        set(value) = prefs.edit().putInt(KEY_PAYDAY_WINDOW_END_DAY, value).apply()

    var expectedSalaryMinPaise: Long
        get() = prefs.getLong(KEY_EXPECTED_SALARY_MIN_PAISE, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPECTED_SALARY_MIN_PAISE, value).apply()

    var expectedSalaryMaxPaise: Long
        get() = prefs.getLong(KEY_EXPECTED_SALARY_MAX_PAISE, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPECTED_SALARY_MAX_PAISE, value).apply()

    var fixedBaselinePaise: Long?
        get() = if (prefs.contains(KEY_FIXED_BASELINE_PAISE)) prefs.getLong(KEY_FIXED_BASELINE_PAISE, 0L) else null
        set(value) {
            if (value != null) prefs.edit().putLong(KEY_FIXED_BASELINE_PAISE, value).apply()
            else prefs.edit().remove(KEY_FIXED_BASELINE_PAISE).apply()
        }

    var baselineMethod: String
        get() = prefs.getString(KEY_BASELINE_METHOD, "ESTIMATE_55_PERCENT") ?: "ESTIMATE_55_PERCENT"
        set(value) = prefs.edit().putString(KEY_BASELINE_METHOD, value).apply()

    var lifestyleCreepLock: Boolean
        get() = prefs.getBoolean(KEY_LIFESTYLE_CREEP_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_LIFESTYLE_CREEP_LOCK, value).apply()

    var needsBufferPaise: Long
        get() = prefs.getLong(KEY_NEEDS_BUFFER_PAISE, 150000L) // ₹1,500
        set(value) = prefs.edit().putLong(KEY_NEEDS_BUFFER_PAISE, value).apply()

    var allocationFramework: String
        get() = prefs.getString(KEY_ALLOCATION_FRAMEWORK, "SIX_JAR") ?: "SIX_JAR"
        set(value) = prefs.edit().putString(KEY_ALLOCATION_FRAMEWORK, value).apply()

    var customBpsJson: String
        get() = prefs.getString(KEY_CUSTOM_BPS_JSON, "{}") ?: "{}"
        set(value) = prefs.edit().putString(KEY_CUSTOM_BPS_JSON, value).apply()

    var playToLongTermShift: Boolean
        get() = prefs.getBoolean(KEY_PLAY_TO_LONG_TERM_SHIFT, false)
        set(value) = prefs.edit().putBoolean(KEY_PLAY_TO_LONG_TERM_SHIFT, value).apply()

    var upiNeedsAllowancePaise: Long
        get() = prefs.getLong(KEY_UPI_NEEDS_ALLOWANCE_PAISE, 0L)
        set(value) = prefs.edit().putLong(KEY_UPI_NEEDS_ALLOWANCE_PAISE, value).apply()

    var emergencyTargetMonths: Int
        get() = prefs.getInt(KEY_EMERGENCY_TARGET_MONTHS, 7)
        set(value) = prefs.edit().putInt(KEY_EMERGENCY_TARGET_MONTHS, value).apply()

    var survivalThresholdPaise: Long?
        get() = if (prefs.contains(KEY_SURVIVAL_THRESHOLD_PAISE)) prefs.getLong(KEY_SURVIVAL_THRESHOLD_PAISE, 0L) else null
        set(value) {
            if (value != null) prefs.edit().putLong(KEY_SURVIVAL_THRESHOLD_PAISE, value).apply()
            else prefs.edit().remove(KEY_SURVIVAL_THRESHOLD_PAISE).apply()
        }

    var smallScanThresholdPaise: Long
        get() = prefs.getLong(KEY_SMALL_SCAN_THRESHOLD_PAISE, 20000L) // ₹200
        set(value) = prefs.edit().putLong(KEY_SMALL_SCAN_THRESHOLD_PAISE, value).apply()

    var egoCheckThresholdPaise: Long
        get() = prefs.getLong(KEY_EGO_CHECK_THRESHOLD_PAISE, 200000L) // ₹2,000
        set(value) = prefs.edit().putLong(KEY_EGO_CHECK_THRESHOLD_PAISE, value).apply()

    var cooldownHours: Int
        get() = prefs.getInt(KEY_COOLDOWN_HOURS, 48)
        set(value) = prefs.edit().putInt(KEY_COOLDOWN_HOURS, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val DEFAULT_SUPABASE_URL = "https://zahzagnindkkttdpmqii.supabase.co"
        private const val KEY_HIDE_SENSITIVE_NOTIFICATIONS = "hide_sensitive_notifications"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_TIMEOUT = "lock_timeout_ms"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_LAST_ACCOUNT = "last_account"
        private const val KEY_BASIS_INCOME = "basis_income"
        private const val KEY_NEEDS_PCT = "needs_pct"
        private const val KEY_WANTS_PCT = "wants_pct"
        private const val KEY_SAVINGS_PCT = "savings_pct"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_MASK_AMOUNTS = "mask_amounts"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_BLOCK_SCREENSHOTS = "block_screenshots"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_SUPABASE_USER_ID = "supabase_user_id"
        private const val KEY_SUPABASE_USER_EMAIL = "supabase_user_email"
        private const val KEY_SUPABASE_ACCESS_TOKEN = "supabase_access_token"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_STATUS = "last_sync_status"
        private const val KEY_AUTO_VAULT_KEY = "auto_vault_key"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
        private const val KEY_LAST_BACKUP_ERROR = "last_backup_error"
        private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_DAILY_REMINDER_HOUR = "daily_reminder_hour"
        private const val KEY_DAILY_REMINDER_MINUTE = "daily_reminder_minute"
        private const val KEY_REMIND_ONLY_NO_TXN = "remind_only_no_txn"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_LAST_REVIEW_DATE = "last_review_date"

        // Financial Profile Keys
        private const val KEY_INCOME_SCHEDULE_TYPE = "income_schedule_type"
        private const val KEY_PAYDAY_WINDOW_START_DAY = "payday_window_start_day"
        private const val KEY_PAYDAY_WINDOW_END_DAY = "payday_window_end_day"
        private const val KEY_EXPECTED_SALARY_MIN_PAISE = "expected_salary_min_paise"
        private const val KEY_EXPECTED_SALARY_MAX_PAISE = "expected_salary_max_paise"
        private const val KEY_FIXED_BASELINE_PAISE = "fixed_baseline_paise"
        private const val KEY_BASELINE_METHOD = "baseline_method"
        private const val KEY_LIFESTYLE_CREEP_LOCK = "lifestyle_creep_lock"
        private const val KEY_NEEDS_BUFFER_PAISE = "needs_buffer_paise"
        private const val KEY_ALLOCATION_FRAMEWORK = "allocation_framework"
        private const val KEY_CUSTOM_BPS_JSON = "custom_bps_json"
        private const val KEY_PLAY_TO_LONG_TERM_SHIFT = "play_to_long_term_shift"
        private const val KEY_UPI_NEEDS_ALLOWANCE_PAISE = "upi_needs_allowance_paise"
        private const val KEY_EMERGENCY_TARGET_MONTHS = "emergency_target_months"
        private const val KEY_SURVIVAL_THRESHOLD_PAISE = "survival_threshold_paise"
        private const val KEY_SMALL_SCAN_THRESHOLD_PAISE = "small_scan_threshold_paise"
        private const val KEY_EGO_CHECK_THRESHOLD_PAISE = "ego_check_threshold_paise"
        private const val KEY_COOLDOWN_HOURS = "cooldown_hours"
        private const val KEY_LANGUAGE = "language"
    }
}
