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

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_TIMEOUT = "lock_timeout_ms"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_LAST_ACCOUNT = "last_account"
        private const val KEY_BASIS_INCOME = "basis_income"
        private const val KEY_NEEDS_PCT = "needs_pct"
        private const val KEY_WANTS_PCT = "wants_pct"
        private const val KEY_SAVINGS_PCT = "savings_pct"
    }
}
