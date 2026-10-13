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
        get() = prefs.getString(KEY_SUPABASE_URL, DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
        set(value) = prefs.edit().putString(KEY_SUPABASE_URL, value.trim()).apply()

    var supabaseAnonKey: String
        get() = prefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUPABASE_ANON_KEY, value.trim()).apply()

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
        get() = prefs.getInt(KEY_DAILY_REMINDER_HOUR, 20)
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_HOUR, value).apply()

    var dailyReminderMinute: Int
        get() = prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 30)
        set(value) = prefs.edit().putInt(KEY_DAILY_REMINDER_MINUTE, value).apply()

    var remindOnlyWhenNoTxnLogged: Boolean
        get() = prefs.getBoolean(KEY_REMIND_ONLY_NO_TXN, true)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_ONLY_NO_TXN, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietHoursStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_HOUR, value).apply()

    var quietHoursEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_HOUR, value).apply()

    var lastDailyReviewDate: String
        get() = prefs.getString(KEY_LAST_REVIEW_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_REVIEW_DATE, value).apply()

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val DEFAULT_SUPABASE_URL = "https://zahzagnindkkttdpmqii.supabase.co"
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
    }
}
