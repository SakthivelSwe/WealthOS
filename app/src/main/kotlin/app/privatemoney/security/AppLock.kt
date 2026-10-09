package app.privatemoney.security

import android.os.SystemClock
import app.privatemoney.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the UI must be hidden behind authentication. Locks on cold start and when the app
 * returns from the background after [AppSettings.lockTimeoutMs]. Uses the monotonic clock so
 * changing the wall clock cannot extend the unlocked window.
 */
class AppLock(
    private val settings: AppSettings,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _locked = MutableStateFlow(settings.lockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private var backgroundedAt: Long? = null

    fun onBackground() {
        backgroundedAt = now()
    }

    fun onForeground() {
        if (settings.lockEnabled) {
            val at = backgroundedAt
            if (at == null || now() - at >= settings.lockTimeoutMs) _locked.value = true
        }
        backgroundedAt = null
    }

    fun unlocked() {
        _locked.value = false
    }

    /** Call after the user changes lock settings. */
    fun refresh() {
        if (!settings.lockEnabled) _locked.value = false
    }
}
